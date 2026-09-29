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

package com.fiepi.media.player.component

import android.content.Context
import androidx.core.net.toUri
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.media.UriScheme
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.MediaStreamServer
import com.fiepi.media.domain.player.model.PlayerEngineType
import com.fiepi.media.domain.utils.getAuthenticatedPlayUrl

/**
 * Handles media URL resolution, including local URI file descriptor mapping (fdclose://)
 * and remote protocol proxying (SMB/FTP via MediaStreamServer, WebDAV authentication).
 */
internal class MediaSourceResolver(
    private val context: Context,
    private val streamServer: MediaStreamServer,
    val getEngineType: () -> PlayerEngineType = { PlayerEngineType.MPV }
) {

    /**
     * Dynamically resolves MediaSource for a given video based on its sourceType and sourceId.
     * Supports internal storage, removable SD cards / USB drives (volume UUIDs), and remote protocols.
     */
    suspend fun resolveMediaSource(
        video: MediaFile.Video,
        getMediaSource: suspend (String, String?) -> MediaSource?
    ): MediaSource? {
        val targetSourceId = video.sourceId ?: MediaSource.INTERNAL_STORAGE_ID
        val resolved = getMediaSource(targetSourceId, video.path)
        if (resolved != null) return resolved

        return when (video.sourceType) {
            SourceType.Local -> MediaSource.Local(
                id = MediaSource.INTERNAL_STORAGE_ID,
                name = "Local Storage",
                rootPath = "/"
            )
            SourceType.External -> MediaSource.External()
            SourceType.Stream -> MediaSource.External(
                id = MediaSource.NETWORK_STREAM_ID,
                name = "Network Stream"
            )
            else -> null
        }
    }

    /**
     * Resolves both MediaSource and play URL for a video entry.
     */
    suspend fun resolveSourceAndPlayUrl(
        video: MediaFile.Video,
        currentSource: MediaSource?,
        getMediaSource: suspend (String, String?) -> MediaSource?
    ): Pair<MediaSource, String>? {
        val targetSource = if (currentSource != null && (currentSource.id == video.sourceId || currentSource.type == video.sourceType)) {
            currentSource
        } else {
            resolveMediaSource(video, getMediaSource)
        } ?: return null

        val playUrl = resolveMediaUrl(targetSource, video)
        return Pair(targetSource, playUrl)
    }

    /**
     * Converts domain MediaFile into an engine-recognizable Play URL.
     * Automatically handles local path mapping and remote protocols (SMB proxy, FTP/WebDAV authenticated links).
     */
    fun resolveMediaUrl(mediaSource: MediaSource?, file: MediaFile): String {
        return when (mediaSource) {
            is MediaSource.Local -> file.path
            is MediaSource.Remote -> {
                val remote = mediaSource.source
                when (remote.type) {
                    SourceType.Smb, SourceType.Ftp -> {
                        if (!streamServer.isRunning()) {
                            streamServer.start()
                        }
                        val sessionId = "${remote.id}_${file.path.hashCode()}"
                        val fileSize = when (file) {
                            is MediaFile.Video -> file.size ?: -1L
                            is MediaFile.Subtitle -> file.size ?: -1L
                            else -> -1L
                        }
                        streamServer.registerMediaSession(
                            sessionId = sessionId,
                            source = remote,
                            filePath = file.path,
                            knownFileSize = fileSize
                        )
                    }

                    SourceType.WebDav -> remote.getAuthenticatedPlayUrl(file.path)
                    else -> file.path
                }
            }
            else -> resolveExternalUrl(file)
        }
    }

    /**
     * Resolves external Uri scheme for MPV native handle via openFileDescriptor when applicable.
     */
    fun resolveExternalUrl(file: MediaFile, engineType: PlayerEngineType = getEngineType()): String {
        val scheme = file.scheme
        if (engineType == PlayerEngineType.MPV && (scheme == UriScheme.Content || scheme == UriScheme.File)) {
            try {
                context.contentResolver.openFileDescriptor(file.path.toUri(), "r")?.use { pfd ->
                    return "fdclose://${pfd.detachFd()}"
                }
            } catch (_: Exception) {
            }
        }
        return file.path
    }
}
