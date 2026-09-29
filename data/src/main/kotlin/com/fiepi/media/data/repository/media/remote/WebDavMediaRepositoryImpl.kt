/*
 * IsekaiPlayer - Sovereign above myriad realms; shatter every mortal cipher.
 * Copyright (C) 2026 onlymash
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fiepi.media.data.repository.media.remote

import android.net.Uri
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.ktor.DavCollection
import at.bitfire.dav4jvm.ktor.MultiStatusItem
import at.bitfire.dav4jvm.property.webdav.GetContentLength
import at.bitfire.dav4jvm.property.webdav.GetLastModified
import at.bitfire.dav4jvm.property.webdav.ResourceType
import com.fiepi.media.data.utils.closeSilently
import com.fiepi.media.data.utils.resolveMimeType
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.media.RemoteMediaRepository
import com.fiepi.media.domain.utils.FileUtils
import com.fiepi.media.domain.utils.SubtitleUtils
import com.fiepi.media.domain.utils.baseUrl
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.URLBuilder
import io.ktor.http.decodeURLPart
import io.ktor.utils.io.cancel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.io.InputStream

class WebDavMediaRepositoryImpl(
    private val source: RemoteSource
) : RemoteMediaRepository {

    private val baseUrl: String = source.baseUrl
    private var _httpClient: HttpClient? = null
    private var _isConnected = false

    private fun createClient(): HttpClient = HttpClient(OkHttp) {
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = AppConstants.REMOTE_TIMEOUT_MS
            connectTimeoutMillis = AppConstants.REMOTE_TIMEOUT_MS
            socketTimeoutMillis = AppConstants.REMOTE_TIMEOUT_MS
        }
        defaultRequest {
            header("X-OC-Mtime", "1")
        }
        val username = source.username
        val password = source.password
        if (username != null && password != null) {
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(
                            username = username,
                            password = password
                        )
                    }
                    sendWithoutRequest { true }
                }
            }
//            install(io.ktor.client.plugins.logging.Logging) {
//                logger = object : io.ktor.client.plugins.logging.Logger {
//                    override fun log(message: String) {
//                        android.util.Log.w("Ktor", message)
//                    }
//                }
//                level = io.ktor.client.plugins.logging.LogLevel.HEADERS
//            }
        }
    }

    private fun getClient(): HttpClient {
        return _httpClient ?: createClient().also { _httpClient = it }
    }

    override suspend fun getMediaFiles(path: String): List<MediaFile> {
        val client = getClient()
        val currentPath = if (path.isEmpty()) {
            source.path?.let { if (it.startsWith("/")) it else "/$it" } ?: "/"
        } else {
            if (path.startsWith("/")) path else "/$path"
        }

        val dirPath = if (currentPath.endsWith("/")) currentPath else "$currentPath/"
        val fullUrl = URLBuilder(baseUrl).apply {
            pathSegments = dirPath.split('/').filter { it.isNotEmpty() }
        }.build()

        val davCollection = DavCollection(client, fullUrl)
        val resultFiles = mutableListOf<MediaFile>()

        try {
            davCollection.propfind(
                1,
                ResourceType.Factory.getName(),
                GetContentLength.Factory.getName(),
                GetLastModified.Factory.getName()
            ).collect { item ->
                if (item is MultiStatusItem.Response) {
                    val response = item.response
                    val itemUrl = response.href
                    val itemPath = itemUrl.encodedPath.decodeURLPart()

                    if (itemPath.removeSuffix("/") == dirPath.removeSuffix("/")) {
                        return@collect
                    }

                    val name = itemUrl.segments.lastOrNull() ?: ""
                    val properties = response.properties

                    val isDirectory = properties.any {
                        it is ResourceType && it.types.contains(Property.Name("DAV:", "collection"))
                    }

                    val contentLength = properties.filterIsInstance<GetContentLength>()
                        .firstOrNull()?.contentLength
                    val lastModified = properties.filterIsInstance<GetLastModified>()
                        .firstOrNull()?.lastModified?.toEpochMilli()

                    if (isDirectory) {
                        resultFiles.add(
                            MediaFile.Folder(
                                path = itemPath.removeSuffix("/"),
                                name = name,
                                sourceType = SourceType.WebDav,
                                size = contentLength,
                                lastModified = lastModified
                            )
                        )
                    } else {
                        when (val ext = FileUtils.extractExtension(name)) {
                            in AppConstants.VIDEO_EXTENSIONS -> {
                                val mimeType = resolveMimeType(ext)
                                val displayName = FileUtils.extractNameWithoutExtension(name)
                                resultFiles.add(
                                    MediaFile.Video(
                                        id = "${source.id}/$itemPath",
                                        path = itemPath,
                                        name = displayName,
                                        sourceId = source.id,
                                        sourceType = SourceType.WebDav,
                                        size = contentLength,
                                        lastModified = lastModified,
                                        mimeType = mimeType,
                                        extension = ext,
                                        thumbnailUrl = Uri.Builder()
                                            .scheme(AppConstants.MEDIA_FRAME_SCHEME)
                                            .authority(source.id)
                                            .path(itemPath)
                                            .build()
                                            .toString()
                                    )
                                )
                            }

                            in AppConstants.SUBTITLE_EXTENSIONS -> {
                                val subtitleInfo = SubtitleUtils.parse(name)
                                val mimeType = resolveMimeType(ext)
                                resultFiles.add(
                                    MediaFile.Subtitle(
                                        path = itemPath,
                                        name = subtitleInfo.baseName,
                                        sourceType = SourceType.WebDav,
                                        size = contentLength,
                                        lastModified = lastModified,
                                        language = subtitleInfo.language,
                                        extension = ext,
                                        mimeType = mimeType
                                    )
                                )
                            }
                        }
                    }
                }
            }
            _isConnected = true // If propfind succeeded, we are connected
        } catch (e: Exception) {
            _isConnected = false
            throw IOException("Failed to fetch WebDAV media files: ${e.message}", e)
        }

        val videos = resultFiles.filterIsInstance<MediaFile.Video>()
        val videoBaseNames = videos.map { it.name }

        val associatedSubtitles = resultFiles.filterIsInstance<MediaFile.Subtitle>().filter { sub ->
            videoBaseNames.any { SubtitleUtils.isAssociatedWithVideo(sub.name, it) }
        }

        return resultFiles.mapNotNull { item ->
            when (item) {
                is MediaFile.Video -> {
                    val videoBaseName = item.name
                    val associated = associatedSubtitles.filter {
                        SubtitleUtils.isAssociatedWithVideo(it.name, videoBaseName)
                    }
                    item.copy(externalSubtitles = associated)
                }

                is MediaFile.Subtitle -> {
                    if (associatedSubtitles.contains(item)) item else null
                }

                else -> item
            }
        }.sortedWith(compareBy({ it is MediaFile.Video }, { it.name }))
    }

    override fun searchMediaFiles(query: String, path: String): Flow<MediaFile.Video> = flow {
        val client = getClient()
        val foldersToProcess = mutableListOf(path)

        while (foldersToProcess.isNotEmpty()) {
            val currentFolder = foldersToProcess.removeAt(0)
            val dirPath = if (currentFolder.isEmpty()) {
                source.path?.let { if (it.startsWith("/")) it else "/$it" } ?: "/"
            } else {
                if (currentFolder.startsWith("/")) currentFolder else "/$currentFolder"
            }
            val normDirPath = if (dirPath.endsWith("/")) dirPath else "$dirPath/"

            val fullUrl = URLBuilder(baseUrl).apply {
                pathSegments = normDirPath.split('/').filter { it.isNotEmpty() }
            }.build()

            try {
                val davCollection = DavCollection(client, fullUrl)
                davCollection.propfind(
                    1,
                    ResourceType.Factory.getName(),
                    GetContentLength.Factory.getName(),
                    GetLastModified.Factory.getName()
                ).collect { item ->
                    if (item is MultiStatusItem.Response) {
                        val response = item.response
                        val itemUrl = response.href
                        val itemPath = itemUrl.encodedPath.decodeURLPart()

                        if (itemPath.removeSuffix("/") == normDirPath.removeSuffix("/")) {
                            return@collect
                        }

                        val name = itemUrl.segments.lastOrNull() ?: ""
                        val properties = response.properties

                        val isDirectory = properties.any {
                            it is ResourceType && it.types.contains(
                                Property.Name(
                                    "DAV:",
                                    "collection"
                                )
                            )
                        }

                        val contentLength = properties.filterIsInstance<GetContentLength>()
                            .firstOrNull()?.contentLength
                        val lastModified = properties.filterIsInstance<GetLastModified>()
                            .firstOrNull()?.lastModified?.toEpochMilli()

                        if (isDirectory) {
                            foldersToProcess.add(itemPath.removeSuffix("/"))
                        } else {
                            if (name.contains(query, ignoreCase = true)) {
                                val ext = FileUtils.extractExtension(name)
                                if (ext in AppConstants.VIDEO_EXTENSIONS) {
                                    val displayName = FileUtils.extractNameWithoutExtension(name)
                                    emit(
                                        MediaFile.Video(
                                            id = "${source.id}/$itemPath",
                                            path = itemPath,
                                            name = displayName,
                                            sourceType = SourceType.WebDav,
                                            size = contentLength,
                                            lastModified = lastModified,
                                            extension = ext,
                                            thumbnailUrl = "frame://${source.id}$itemPath"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun openFile(path: String, offset: Long): InputStream {
        val client = getClient()
        val fullUrl = URLBuilder(baseUrl).apply {
            pathSegments = path.split('/').filter { it.isNotEmpty() }
        }.build()

        val response = client.get(fullUrl) {
            if (offset > 0) {
                header("Range", "bytes=$offset-")
            }
        }
        val channel = response.bodyAsChannel()
        val stream = channel.toInputStream()

        return object : InputStream() {
            override fun read() = stream.read()
            override fun read(b: ByteArray) = stream.read(b)
            override fun read(b: ByteArray, off: Int, len: Int) = stream.read(b, off, len)
            override fun available() = stream.available()
            override fun skip(n: Long) = stream.skip(n)
            override fun close() {
                try {
                    stream.close()
                } finally {
                    channel.cancel()
                }
            }
        }
    }

    override suspend fun getFileSize(path: String): Long {
        val client = getClient()
        val fullUrl = URLBuilder(baseUrl).apply {
            pathSegments = path.split('/').filter { it.isNotEmpty() }
        }.build()
        return client.head(fullUrl).headers["Content-Length"]?.toLong() ?: 0L
    }

    override suspend fun readRange(path: String, range: LongRange): ByteArray {
        val client = getClient()
        val fullUrl = URLBuilder(baseUrl).apply {
            pathSegments = path.split('/').filter { it.isNotEmpty() }
        }.build()
        val response = client.get(fullUrl) {
            header("Range", "bytes=${range.first}-${range.last}")
        }
        return response.bodyAsBytes()
    }

    override suspend fun getSpaceInfo(): RemoteMediaRepository.SpaceInfo? = null

    override val isConnected: Boolean get() = _isConnected

    override suspend fun connect() {
        if (_isConnected) return
        try {
            // Test connection with a shallow propfind on the root path
            val client = getClient()
            val rootPath = source.path?.let { if (it.startsWith("/")) it else "/$it" } ?: "/"
            val fullUrl = URLBuilder(baseUrl).apply {
                pathSegments = rootPath.split('/').filter { it.isNotEmpty() }
            }.build()

            val davCollection = DavCollection(client, fullUrl)
            davCollection.propfind(0, ResourceType.Factory.getName()).collect { }
            _isConnected = true
        } catch (e: Exception) {
            _isConnected = false
            throw IOException("Failed to connect to WebDAV: ${e.message}", e)
        }
    }

    override suspend fun disconnect() {
        _httpClient?.closeSilently()
        _httpClient = null
        _isConnected = false
    }
}
