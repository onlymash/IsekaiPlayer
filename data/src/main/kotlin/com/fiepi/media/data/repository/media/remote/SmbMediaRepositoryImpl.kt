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
import android.util.Log
import com.fiepi.media.data.utils.closeSilently
import com.fiepi.media.data.utils.resolveMimeType
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.media.RemoteMediaRepository
import com.fiepi.media.domain.utils.FileUtils
import com.fiepi.media.domain.utils.SubtitleUtils
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.transport.tcp.async.AsyncDirectTcpTransportFactory
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class SmbMediaRepositoryImpl(
    private val source: RemoteSource
) : RemoteMediaRepository {

    companion object {
        private const val BUFFER_SIZE = 4 * 1024 * 1024 // 4MB buffer for SmbConfig
        private const val STREAMING_BUFFER_SIZE = 8 * 1024 * 1024 // 8MB buffer for stream
    }

    private val mutex = Mutex()
    private var smbClient: SMBClient? = null
    private var smbConnection: Connection? = null
    private var smbSession: Session? = null
    private var smbShare: DiskShare? = null

    private val smbConfig = SmbConfig.builder()
        .withDialects(
            SMB2Dialect.SMB_3_1_1,
            SMB2Dialect.SMB_3_0_2,
            SMB2Dialect.SMB_3_0,
            SMB2Dialect.SMB_2_1,
            SMB2Dialect.SMB_2_0_2,
        )
        .withTimeout(AppConstants.REMOTE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .withSoTimeout(AppConstants.REMOTE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .withBufferSize(BUFFER_SIZE)
        .withTransportLayerFactory(AsyncDirectTcpTransportFactory())
        .withDfsEnabled(false)
        .withMultiProtocolNegotiate(true)
        .withSigningRequired(false)
        .withEncryptData(false)
        .build()

    private val shareName: String by lazy {
        source.path?.trim('/')?.substringBefore('/') ?: ""
    }

    private val baseSubPath: String by lazy {
        source.path?.trim('/')?.substringAfter('/', "") ?: ""
    }

    override suspend fun getMediaFiles(path: String): List<MediaFile> {

        val share = smbShare ?: error("Not connected")

        val allFiles = share.list(toSmbPath(path)).mapNotNull { info ->

            currentCoroutineContext().ensureActive()

            val name = info.fileName
            if (name == "." || name == ".." || name.endsWith("$")) return@mapNotNull null

            val isDirectory =
                info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L

            val fullPath = if (path.isEmpty()) {
                if (baseSubPath.isEmpty()) name else "$baseSubPath/$name"
            } else {
                "$path/$name"
            }

            if (isDirectory) {
                MediaFile.Folder(
                    path = fullPath,
                    name = name,
                    sourceType = SourceType.Smb,
                    lastModified = info.changeTime.toEpochMillis()
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
                            sourceType = SourceType.Smb,
                            size = info.endOfFile,
                            lastModified = info.changeTime.toEpochMillis(),
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
                            sourceId = source.id,
                            sourceType = SourceType.Smb,
                            size = info.endOfFile,
                            lastModified = info.changeTime.toEpochMillis(),
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

        return allFiles.mapNotNull { item ->
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
        val share = smbShare ?: error("Not connected")
        val foldersToProcess = mutableListOf(path)

        while (foldersToProcess.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val currentFolder = foldersToProcess.removeAt(0)

            try {
                share.list(toSmbPath(currentFolder)).forEach { info ->
                    val name = info.fileName
                    if (name == "." || name == ".." || name.endsWith("$")) return@forEach

                    val isDirectory =
                        info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L

                    val fullPath = if (currentFolder.isEmpty()) {
                        if (baseSubPath.isEmpty()) name else "$baseSubPath/$name"
                    } else {
                        "$currentFolder/$name"
                    }

                    if (isDirectory) {
                        foldersToProcess.add(fullPath)
                    } else {
                        if (name.contains(query, ignoreCase = true)) {
                            val ext = FileUtils.extractExtension(name)
                            if (ext != null && ext in AppConstants.VIDEO_EXTENSIONS) {
                                val mimeType = resolveMimeType(ext)
                                val displayName = FileUtils.extractNameWithoutExtension(name)
                                emit(
                                    MediaFile.Video(
                                        id = "${source.id}/$fullPath",
                                        path = fullPath,
                                        name = displayName,
                                        sourceType = SourceType.Smb,
                                        size = info.endOfFile,
                                        lastModified = info.changeTime.toEpochMillis(),
                                        mimeType = mimeType,
                                        extension = ext,
                                        thumbnailUrl = null
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip folders we can't list
            }
        }
    }

    override suspend fun openFile(path: String, offset: Long): InputStream {
        val share = smbShare ?: error("Not connected")

        val file = share.openFile(
            toSmbPath(path),
            EnumSet.of(AccessMask.GENERIC_READ),
            null,
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
            SMB2CreateDisposition.FILE_OPEN,
            null
        )

        val rawStream = object : InputStream() {
            private var position = offset

            override fun read(): Int {
                val one = ByteArray(1)
                return if (read(one, 0, 1) == -1) -1 else one[0].toInt() and 0xFF
            }

            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val read = file.read(b, position, off, len)
                if (read > 0) position += read
                return read
            }

            override fun close() {
                runCatching { file.close() }
            }
        }

        return rawStream.buffered(STREAMING_BUFFER_SIZE)
    }

    override suspend fun getFileSize(path: String): Long {
        val share = smbShare ?: error("Not connected")

        return try {
            share.getFileInformation(toSmbPath(path))
                .standardInformation
                .endOfFile
        } catch (e: Exception) {
            Log.e("SmbRepo", "Failed to get file info for $path", e)
            // If the connection is broken, signal it so the caller can retry
            if (e.cause is java.nio.channels.ClosedChannelException ||
                e.message?.contains("ClosedChannelException") == true
            ) {
                disconnect()
            }
            -1L
        }
    }

    override suspend fun readRange(path: String, range: LongRange): ByteArray {
        val share = smbShare ?: error("Not connected")
        val smbPath = toSmbPath(path)

        var retryCount = 0
        val maxRetries = 2

        while (true) {
            try {
                return share.openFile(
                    smbPath,
                    EnumSet.of(AccessMask.FILE_READ_DATA),
                    null,
                    EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                ).use { file ->
                    val length = (range.last - range.first + 1).toInt()
                    val buffer = ByteArray(length)
                    val read = file.read(buffer, range.first, 0, length)
                    if (read < length) buffer.copyOf(read) else buffer
                }
            } catch (e: Exception) {
                retryCount++
                if (retryCount > maxRetries) throw e

                Log.w("SmbRepo", "Read failed for $path, retrying ($retryCount/$maxRetries)...", e)
                if (e is java.io.IOException || e.message?.contains("Closed") == true) {
                    // Try to reconnect if connection seems broken
                    try {
                        connect()
                    } catch (_: Exception) {
                    }
                }
                kotlinx.coroutines.delay((500 * retryCount).milliseconds)
            }
        }
    }

    override suspend fun getSpaceInfo(): RemoteMediaRepository.SpaceInfo? {
        val share = smbShare ?: return null
        return try {
            val shareInfo = share.shareInformation
            RemoteMediaRepository.SpaceInfo(
                totalSpace = shareInfo.totalSpace,
                freeSpace = shareInfo.freeSpace
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun toSmbPath(path: String): String {
        // Path normalization logic:
        // If path is empty, return baseSubPath.
        // Otherwise, clean up slashes and strip the shareName if it was prepended by the UI.

        val cleanPath = path.trim('/')
        val relative = if (cleanPath.isEmpty()) {
            baseSubPath
        } else {
            // Strip shareName prefix if present
            cleanPath.removePrefix(shareName).trim('/')
        }

        return relative.replace('/', '\\')
    }

    override val isConnected: Boolean
        get() = smbClient != null && smbSession != null && smbConnection?.isConnected == true && smbShare?.isConnected == true

    override suspend fun connect() {
        if (isConnected) {
            return
        }
        mutex.withLock {
            try {
                val client = SMBClient(smbConfig)
                smbClient = client

                val connection = client.connect(source.host, source.port ?: 445)
                smbConnection = connection
                val authContext = if (source.username != null) {
                    AuthenticationContext(source.username, source.password?.toCharArray(), null)
                } else {
                    AuthenticationContext.anonymous()
                }

                val session = connection.authenticate(authContext)
                smbSession = session
                smbShare = session.connectShare(shareName) as DiskShare
            } catch (e: Exception) {
                disconnect()
                throw e
            }
        }
    }

    override suspend fun disconnect() {
        smbSession?.closeSilently()
        smbConnection?.closeSilently(true)
        smbShare?.closeSilently()
        smbClient?.closeSilently()
        smbClient = null
        smbSession = null
        smbConnection = null
        smbShare = null
    }
}
