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

package com.fiepi.media.domain.player.model

import com.fiepi.media.domain.model.media.MediaFile

/**
 * Business-layer common playback state
 */
data class MediaPlaybackState(
    val sourceId: String = "",

    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val isLoaded: Boolean = false,

    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val bufferedPosition: Long = 0L,
    val scrubbingPosition: Long? = null,
    val targetSeekPosition: Long? = null, // Internal seek target, not necessarily displayed on UI
    val isSeeking: Boolean = false,

    val currentFrame: Long = 0,
    val totalFrames: Long = 0,

    val hwdec: String? = null,

    val playbackSpeed: Float = 1.0f,
    val audioDelay: Long = 0L,
    val subtitleDelay: Long = 0L,
    val subtitleText: String? = null,

    // Detailed volume state
    val volumeState: PlayerVolumeState = PlayerVolumeState(),

    // Media metadata and playlist
    val mediaMetadata: MediaMetadata = MediaMetadata(),
    val playlist: List<MediaFile.Video> = emptyList(),
    val playlistId: String? = null,
    val totalPlaylistItems: Int = 0,
    val currentIndex: Int = -1,

    // Track information
    val audioTracks: List<MediaTrack.Audio> = emptyList(),
    val subtitleTracks: List<MediaTrack.Subtitle> = emptyList(),
    val videoTracks: List<MediaTrack.Video> = emptyList(),

    // Engine capabilities
    val capabilities: EngineCapabilities = EngineCapabilities()
) {
    /**
     * Convenient property accessors for volume state
     */
    val systemVolume: Int get() = volumeState.systemVolume
    val systemMaxVolume: Int get() = volumeState.systemMaxVolume
    val gainVolume: Int get() = volumeState.gainVolume

    /**
     * Convenient property accessors for current media metadata
     */
    val mediaId: String get() = mediaMetadata.mediaId
    val mediaTitle: String? get() = mediaMetadata.title
    val mediaSubtitle: String? get() = mediaMetadata.subtitle
    val mediaArtworkUri: String? get() = mediaMetadata.artworkUri

    /**
     * Convenient property accessor for currently selected video track
     */
    val selectedVideoTrack: MediaTrack.Video?
        get() = videoTracks.firstOrNull { it.isSelected } ?: videoTracks.firstOrNull()

    /**
     * Total number of videos in current playback context (folder or playlist)
     */
    val totalVideosCount: Int
        get() = if (playlistId != null) totalPlaylistItems else playlist.size

    /**
     * Whether playlist navigation (prev/next track) is applicable
     */
    val hasPlaylistNavigation: Boolean
        get() = totalVideosCount > 1

    /**
     * Whether progress is being scrubbed or the engine is seeking
     */
    val isSeekingOrScrubbing: Boolean
        get() = isSeeking || scrubbingPosition != null
}
