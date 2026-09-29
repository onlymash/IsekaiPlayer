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
import com.fiepi.media.data.utils.resolveMimeType
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.media.RemoteMediaRepository
import com.fiepi.media.domain.utils.FileUtils
import com.fiepi.media.domain.utils.SubtitleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class FtpMediaRepositoryImpl(
    private val source: RemoteSource
) : RemoteMediaRepository {

    companion object {
        private const val BUFFER_SIZE = 4 * 1024 * 1024 // 4MB buffer
    }

    private var ftpClient: FTPClient? = null
    private val mutex = Mutex()

    private fun createClient(): FTPClient = FTPClient().apply {
        controlEncoding = "UTF-8"
        connectTimeout = AppConstants.REMOTE_TIMEOUT_MS.toInt()
        defaultTimeout = AppConstants.REMOTE_TIMEOUT_MS.toInt()
        bufferSize = BUFFER_SIZE
    }

    private fun FTPClient.connectAndPrepare() {
        try {
            connect(source.host, source.port ?: 21)
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                val reply = replyString
                disconnect()
                throw IOException("FTP server refused connection: $reply")
            }

            val loginSuccess = if (source.username != null) {
                login(source.username, source.password ?: "")
            } else {
                login("anonymous", "")
            }

            if (!loginSuccess) {
                val reply = replyString
                disconnect()
                throw IOException("FTP login failed: $reply")
            }

            setFileType(FTP.BINARY_FILE_TYPE)
            enterLocalPassiveMode()
            runCatching { sendCommand("OPTS UTF8 ON") }
            @Suppress("UsePropertyAccessSyntax")
            setReceiveBufferSize(BUFFER_SIZE)
            @Suppress("UsePropertyAccessSyntax")
            setSendBufferSize(BUFFER_SIZE)
        } catch (e: Exception) {
            runCatching { if (isConnected) disconnect() }
            if (e is IOException) throw e else throw IOException(e)
        }
    }

    private fun getOrConnectShared(): FTPClient {
        val current = ftpClient
        if (current != null && current.isConnected && current.isAvailable) {
            try {
                if (current.sendNoOp()) return current
            } catch (_: IOException) {
            }
        }

        val newClient = createClient()
        newClient.connectAndPrepare()
        ftpClient = newClient
        return newClient
    }

    private fun normalizePath(path: String): String {
        val basePath = source.path?.trimEnd('/') ?: ""
        val cleanPath = ("/$path").replace(Regex("/+"), "/")

        return if (basePath.isNotEmpty() && cleanPath.startsWith(basePath)) {
            cleanPath
        } else {
            "$basePath/${cleanPath.removePrefix("/")}".replace(Regex("/+"), "/")
        }
    }

    override suspend fun getMediaFiles(path: String): List<MediaFile> = mutex.withLock {
        currentCoroutineContext().ensureActive()
        val client = getOrConnectShared()
        val workingPath = normalizePath(path)
        currentCoroutineContext().ensureActive()
        val ftpFiles =
            client.listFiles(workingPath) ?: throw IOException("Failed to list FTP files")

        val allFiles = ftpFiles.mapNotNull { info ->
            currentCoroutineContext().ensureActive()
            val name = info.name
            if (name == "." || name == "..") return@mapNotNull null
            val isDirectory = info.isDirectory
            val fullPath =
                if (workingPath.endsWith("/")) "$workingPath$name" else "$workingPath/$name"

            if (isDirectory) {
                MediaFile.Folder(
                    path = fullPath,
                    name = name,
                    sourceType = SourceType.Ftp,
                    lastModified = info.timestamp?.timeInMillis
                )
            } else {
                when (val ext = FileUtils.extractExtension(name)) {
                    in AppConstants.VIDEO_EXTENSIONS -> {
                        val mimeType = resolveMimeType(ext)
                        val displayName = FileUtils.extractNameWithoutExtension(name)
                        MediaFile.Video(
                            id = "${source.id}/$fullPath",
                            path = fullPath,
                            name = displayName,
                            sourceId = source.id,
                            sourceType = SourceType.Ftp,
                            size = info.size,
                            lastModified = info.timestamp?.timeInMillis,
                            mimeType = mimeType,
                            extension = ext,
                            thumbnailUrl = Uri.Builder()
                                .scheme(AppConstants.MEDIA_FRAME_SCHEME)
                                .authority(source.id)
                                .path(fullPath)
                                .build()
                                .toString()
                        )
                    }

                    in AppConstants.SUBTITLE_EXTENSIONS -> {
                        val subtitleInfo = SubtitleUtils.parse(name)
                        val mimeType = resolveMimeType(ext)
                        MediaFile.Subtitle(
                            path = fullPath,
                            name = subtitleInfo.baseName,
                            sourceType = SourceType.Ftp,
                            size = info.size,
                            lastModified = info.timestamp?.timeInMillis,
                            language = subtitleInfo.language,
                            extension = ext,
                            mimeType = mimeType
                        )
                    }

                    else -> null
                }
            }
        }

        val videos = allFiles.filterIsInstance<MediaFile.Video>()
        val videoBaseNames = videos.map { it.name }

        val associatedSubtitles = allFiles.filterIsInstance<MediaFile.Subtitle>().filter { sub ->
            videoBaseNames.any { SubtitleUtils.isAssociatedWithVideo(sub.name, it) }
        }

        return@withLock allFiles.mapNotNull { item ->
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
        val client = getOrConnectShared()
        val foldersToProcess = mutableListOf(path)

        while (foldersToProcess.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val currentFolder = foldersToProcess.removeAt(0)
            val workingPath = normalizePath(currentFolder)

            try {
                val ftpFiles = client.listFiles(workingPath) ?: continue
                ftpFiles.forEach { info ->
                    currentCoroutineContext().ensureActive()
                    val name = info.name
                    if (name == "." || name == "..") return@forEach

                    val fullPath =
                        if (workingPath.endsWith("/")) "$workingPath$name" else "$workingPath/$name"

                    if (info.isDirectory) {
                        foldersToProcess.add(fullPath)
                    } else {
                        if (name.contains(query, ignoreCase = true)) {
                            val ext = FileUtils.extractExtension(name)
                            if (ext in AppConstants.VIDEO_EXTENSIONS) {
                                val displayName = FileUtils.extractNameWithoutExtension(name)
                                emit(
                                    MediaFile.Video(
                                        id = "${source.id}/$fullPath",
                                        path = fullPath,
                                        name = displayName,
                                        sourceType = SourceType.Ftp,
                                        size = info.size,
                                        lastModified = info.timestamp?.timeInMillis,
                                        extension = ext,
                                        thumbnailUrl = null
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: IOException) {
            }
        }
    }

    override suspend fun openFile(path: String, offset: Long): InputStream {
        // Dedicated connection for streaming
        val streamClient = createClient()
        streamClient.connectAndPrepare()

        val workingPath = normalizePath(path)
        if (offset > 0) streamClient.restartOffset = offset
        val rawStream = streamClient.retrieveFileStream(workingPath) ?: run {
            runCatching { streamClient.disconnect() }
            throw IOException("Failed to open FTP stream for $workingPath")
        }

        return object : FilterInputStream(BufferedInputStream(rawStream, BUFFER_SIZE)) {
            override fun close() {
                runCatching { super.close() }
                runCatching {
                    if (streamClient.isConnected) {
                        streamClient.completePendingCommand()
                        streamClient.logout()
                        streamClient.disconnect()
                    }
                }
            }
        }
    }

    override suspend fun getFileSize(path: String): Long {
        val tempClient = createClient()
        try {
            try {
                tempClient.connectAndPrepare()
            } catch (_: Exception) {
                return -1L
            }
            val workingPath = normalizePath(path)
            return if (tempClient.sendCommand("SIZE", workingPath) == FTPReply.FILE_STATUS) {
                tempClient.replyString.trim().substringAfterLast(' ').toLongOrNull() ?: -1L
            } else {
                val parent = workingPath.substringBeforeLast('/', "").ifEmpty { "/" }
                val name = workingPath.substringAfterLast('/')
                tempClient.listFiles(parent)
                    .firstOrNull { it.name == name && !it.isDirectory }?.size
                    ?: -1L
            }
        } finally {
            runCatching {
                if (tempClient.isConnected) {
                    tempClient.logout()
                    tempClient.disconnect()
                }
            }
        }
    }

    override suspend fun readRange(path: String, range: LongRange): ByteArray {
        currentCoroutineContext().ensureActive()
        val tempClient = createClient()
        try {
            tempClient.connectAndPrepare()
            currentCoroutineContext().ensureActive()
            val workingPath = normalizePath(path)
            tempClient.restartOffset = range.first
            val stream = tempClient.retrieveFileStream(workingPath)
                ?: throw IOException("Failed to open FTP stream")

            try {
                val length = (range.last - range.first + 1).toInt()
                val buffer = ByteArray(length)
                val bufferedStream = BufferedInputStream(stream, BUFFER_SIZE)
                var totalRead = 0
                while (totalRead < length) {
                    currentCoroutineContext().ensureActive()
                    val read = withContext(Dispatchers.IO) {
                        bufferedStream.read(buffer, totalRead, length - totalRead)
                    }
                    if (read == -1) break
                    totalRead += read
                }
                return if (totalRead < length) buffer.copyOf(totalRead) else buffer
            } finally {
                runCatching { stream.close() }
                tempClient.completePendingCommand()
            }
        } finally {
            runCatching {
                if (tempClient.isConnected) {
                    tempClient.logout()
                    tempClient.disconnect()
                }
            }
        }
    }

    override suspend fun getSpaceInfo(): RemoteMediaRepository.SpaceInfo? = null

    override val isConnected: Boolean get() = ftpClient?.isConnected == true

    override suspend fun connect() {
        if (isConnected) {
            return
        }
        mutex.withLock {
            val client = createClient()
            client.connectAndPrepare()
            ftpClient = client
        }
    }

    override suspend fun disconnect() {
        runCatching {
            ftpClient?.let { if (it.isConnected) it.disconnect() }
        }
        ftpClient = null
    }
}
