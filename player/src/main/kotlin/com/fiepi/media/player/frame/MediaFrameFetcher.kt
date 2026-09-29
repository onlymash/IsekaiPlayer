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

package com.fiepi.media.player.frame

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.core.graphics.get
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Dimension
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.MediaStreamServer
import com.fiepi.media.domain.repository.media.MediaRepository
import com.fiepi.media.domain.repository.source.SourceRepository
import com.fiepi.media.domain.utils.baseUrl
import com.fiepi.media.domain.utils.getAuthenticatedPlayUrl
import com.fiepi.mpv.controller.MpvConfigProvider
import com.fiepi.mpv.controller.MpvController
import com.fiepi.mpv.model.MpvConfig
import com.fiepi.mpv.model.MpvDecoderConfig
import com.fiepi.mpv.model.MpvEventData
import com.fiepi.mpv.model.MpvRuntimeConfig
import com.fiepi.mpv.model.MpvStaticConfig
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Credentials
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds
import coil3.Uri as CoilUri


class MediaFrameFetcher(
    private val context: Context,
    private val data: Uri,
    private val mediaRepository: MediaRepository,
    private val sourceRepository: SourceRepository,
    private val streamServer: MediaStreamServer,
    private val mpvConfigProvider: MpvConfigProvider,
    private val options: Options,
    private val semaphore: Semaphore,
    private val mpvSemaphore: Semaphore
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val sourceId = data.host ?: return null
        val rawPath = data.path ?: return null
        val path = rawPath.replace(Regex("/+"), "/")

        val cacheFile = getCacheFile(sourceId, path)
        if (cacheFile.exists()) {
            return SourceFetchResult(
                source = ImageSource(
                    file = cacheFile.absolutePath.toPath(),
                    fileSystem = FileSystem.SYSTEM
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK
            )
        }

        val remoteSource =
            if (sourceId != "local") sourceRepository.getRemoteSourceById(sourceId) else null
        val mediaSource = remoteSource?.let { MediaSource.Remote(it) }

        // Try MediaMetadataRetriever first
        val result = semaphore.withPermit {
            try {
                val retriever = MediaMetadataRetriever()
                var smbRetrieverSessionId: String? = null
                try {
                    when {
                        sourceId == "local" -> {
                            currentCoroutineContext().ensureActive()
                            retriever.setDataSource(path)
                        }

                        mediaSource?.type == SourceType.WebDav -> {
                            val url = remoteSource.baseUrl + path

                            val headers = mutableMapOf<String, String>()
                            val username = remoteSource.username
                            val password = remoteSource.password
                            if (username != null && password != null) {
                                headers["Authorization"] = Credentials.basic(username, password)
                            }
                            currentCoroutineContext().ensureActive()
                            retriever.setDataSource(url, headers)
                        }

                        mediaSource?.type == SourceType.Smb -> {
                            currentCoroutineContext().ensureActive()
                            if (!streamServer.isRunning()) streamServer.start()
                            val sessionId = "thumb_retriever_${mediaSource.id}_${path.hashCode()}"
                            val proxyUrl =
                                streamServer.registerMediaSession(sessionId, remoteSource, path)
                            smbRetrieverSessionId = sessionId

                            currentCoroutineContext().ensureActive()
                            // Using the resilient proxy for system metadata retrieval
                            retriever.setDataSource(proxyUrl, emptyMap())
                        }

                        mediaSource != null && mediaSource.type == SourceType.Ftp -> {
                            // System retriever is bad for FTP, skip to mpv fallback
                            return@withPermit null
                        }

                        mediaSource != null -> {
                            currentCoroutineContext().ensureActive()
                            retriever.setDataSource(
                                RemoteMediaDataSource(
                                    path,
                                    mediaSource,
                                    mediaRepository
                                )
                            )
                        }

                        else -> return@withPermit null
                    }

                    currentCoroutineContext().ensureActive()
                    // Try embedded picture first (fastest)
                    val embedded = retriever.embeddedPicture
                    val bitmap = if (embedded != null) {
                        BitmapFactory.decodeByteArray(embedded, 0, embedded.size)
                    } else {
                        // Extract scaled frame
                        val targetWidth = (options.size.width as? Dimension.Pixels)?.px ?: 512
                        val targetHeight = (options.size.height as? Dimension.Pixels)?.px ?: 384

                        val durationStr =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = durationStr?.toLongOrNull() ?: 0L
                        val seekTimeUs = when {
                            durationMs >= 20_000L -> 10_000_000L // 10s
                            durationMs > 0L -> (durationMs / 2) * 1000L // Middle
                            else -> 0L
                        }

                        retriever.getScaledFrameAtTime(
                            seekTimeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            targetWidth,
                            targetHeight
                        )
                    }

                    if (bitmap != null && !isMostlyBlack(bitmap)) {
                        saveToCache(bitmap, cacheFile)
                        SourceFetchResult(
                            source = ImageSource(
                                file = cacheFile.absolutePath.toPath(),
                                fileSystem = FileSystem.SYSTEM
                            ),
                            mimeType = "image/jpeg",
                            dataSource = if (sourceId == "local") DataSource.DISK else DataSource.NETWORK
                        )
                    } else {
                        null
                    }
                } finally {
                    retriever.release()
                    smbRetrieverSessionId?.let { streamServer.unregisterSession(it) }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("MediaFrameFetcher", "Retriever failed for $path: ${e.message}")
                null
            }
        }

        // Fallback to mpv if needed
        return result ?: fetchWithMpv(sourceId, path, mediaSource, cacheFile)
    }

    private suspend fun fetchWithMpv(
        sourceId: String,
        path: String,
        mediaSource: MediaSource?,
        cacheFile: File
    ): FetchResult? {
        return mpvSemaphore.withPermit {
            var pfd: android.os.ParcelFileDescriptor? = null
            val playUrl: String
            var sessionId: String? = null

            try {
                if (sourceId == "local") {
                    val file = File(path)
                    if (!file.exists()) return@withPermit null

                    pfd = android.os.ParcelFileDescriptor.open(
                        file,
                        android.os.ParcelFileDescriptor.MODE_READ_ONLY
                    )
                    playUrl = "fd://${pfd.fd}"
                } else {
                    val resolved =
                        resolveMpvUrl(sourceId, path, mediaSource) ?: return@withPermit null
                    playUrl = resolved.first
                    sessionId = resolved.second
                }

                val controller = MpvController(context, mpvConfigProvider)

                try {
                    val config = MpvConfig(
                        static = MpvStaticConfig(
                            vo = "libmpv",
                            profile = "sw-fast",
                            msgLevel = "all=warn"
                        ),
                        runtime = MpvRuntimeConfig(
                            decoder = MpvDecoderConfig(hwdec = "no")
                        )
                    )

                    controller.initialize(config)

                    // Trigger render context creation for vo=libmpv
                    controller.grabThumbnail(16)

                    controller.setOption("aid", "no")
                    controller.setOption("sid", "no")
                    controller.setOption("frames", "1")
                    controller.setOption("demuxer-max-bytes", "64M")
                    controller.setOption("ytdl", "no")

                    controller.playFile(playUrl)

                    val loaded = withTimeoutOrNull(5000.milliseconds) {
                        controller.getEvents().first { event ->
                            event is MpvEventData.Event && (event.eventId == 8 || event.eventId == 7)
                        }
                    }

                    if (loaded !is MpvEventData.Event || loaded.eventId != 8) {
                        return@withPermit null
                    }

                    val dimension = (options.size.width as? Dimension.Pixels)?.px ?: 512
                    val tracks = controller.getTrackList()
                    val albumArtTrack =
                        tracks.find { it["type"] == "video" && it["albumart"] == true }

                    if (albumArtTrack != null) {
                        // Found embedded picture, switch to it
                        val trackId = albumArtTrack["id"]?.toString() ?: "no"
                        controller.selectTrack("vid", trackId)
                        // Give it a tiny moment to switch the track in render context
                        kotlinx.coroutines.delay(100.milliseconds)
                    } else {
                        // No embedded picture, perform duration-based seek
                        val duration = controller.getPropertyDouble("duration") ?: 0.0
                        val seekMs = when {
                            duration >= 20.0 -> 10000L
                            duration > 0.0 -> (duration / 2 * 1000).toLong()
                            else -> 0L
                        }
                        if (seekMs > 0) {
                            controller.seekTo(seekMs)
                        }
                    }

                    var bitmap: Bitmap? = null
                    withTimeoutOrNull(4000.milliseconds) {
                        while (currentCoroutineContext().isActive) {
                            bitmap = controller.grabThumbnail(dimension)
                            if (bitmap != null && !isMostlyBlack(bitmap!!)) {
                                break
                            }
                            kotlinx.coroutines.delay(200.milliseconds)
                        }
                    }

                    if (bitmap != null) {
                        saveToCache(bitmap!!, cacheFile)
                        SourceFetchResult(
                            source = ImageSource(
                                file = cacheFile.absolutePath.toPath(),
                                fileSystem = FileSystem.SYSTEM
                            ),
                            mimeType = "image/jpeg",
                            dataSource = if (sourceId == "local") DataSource.DISK else DataSource.NETWORK
                        )
                    } else {
                        null
                    }
                } finally {
                    controller.destroy()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("MediaFrameFetcher", "Mpv fallback error for $path: ${e.message}")
                null
            } finally {
                sessionId?.let { streamServer.unregisterSession(it) }
                pfd?.close()
            }
        }
    }

    private fun resolveMpvUrl(
        sourceId: String,
        path: String,
        mediaSource: MediaSource?
    ): Pair<String, String?>? {
        return when {
            sourceId == "local" -> path to null
            mediaSource is MediaSource.Remote -> {
                val remote = mediaSource.source
                when (remote.type) {
                    SourceType.Smb -> {
                        if (!streamServer.isRunning()) {
                            streamServer.start()
                        }
                        val sessionId = "thumb_${remote.id}_${path.hashCode()}"
                        val url = streamServer.registerMediaSession(
                            sessionId = sessionId,
                            source = remote,
                            filePath = path
                        )
                        url to sessionId
                    }

                    SourceType.Ftp, SourceType.WebDav -> remote.getAuthenticatedPlayUrl(path) to null
                    else -> null
                }
            }

            else -> null
        }
    }

    private fun getCacheFile(sourceId: String, path: String): File {
        val hash = MessageDigest.getInstance("MD5")
            .digest("$sourceId$path".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val dir = File(context.cacheDir, "thumbnails").apply { if (!exists()) mkdirs() }
        return File(dir, "$hash.jpg")
    }

    private fun saveToCache(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
        }
    }

    private fun isMostlyBlack(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 15 // Sample 15x15 grid
        val stepX = (width / sampleSize).coerceAtLeast(1)
        val stepY = (height / sampleSize).coerceAtLeast(1)

        val threshold = 25 // brightness threshold (0-255)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap[x, y]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Luminance formula: 0.299R + 0.587G + 0.114B
                val brightness = (r * 299 + g * 587 + b * 114) / 1000
                if (brightness > threshold) {
                    return false // Found a bright enough pixel
                }
            }
        }
        return true
    }

    class Factory(
        private val context: Context,
        private val mediaRepository: MediaRepository,
        private val sourceRepository: SourceRepository,
        private val streamServer: MediaStreamServer,
        private val mpvConfigProvider: MpvConfigProvider
    ) : Fetcher.Factory<Any> {
        private val semaphore = Semaphore(permits = 2)
        private val mpvSemaphore = Semaphore(permits = 1)

        override fun create(data: Any, options: Options, imageLoader: ImageLoader): Fetcher? {
            val uri = when (data) {
                is Uri -> data
                is CoilUri -> data.toString().toUri()
                is String -> data.toUri()
                else -> {
                    val dataStr = data.toString()
                    if (dataStr.startsWith("${AppConstants.MEDIA_FRAME_SCHEME}://")) dataStr.toUri() else null
                }
            }

            if (uri?.scheme != AppConstants.MEDIA_FRAME_SCHEME) return null
            return MediaFrameFetcher(
                context = context,
                data = uri,
                mediaRepository = mediaRepository,
                sourceRepository = sourceRepository,
                streamServer = streamServer,
                mpvConfigProvider = mpvConfigProvider,
                options = options,
                semaphore = semaphore,
                mpvSemaphore = mpvSemaphore
            )
        }
    }
}
