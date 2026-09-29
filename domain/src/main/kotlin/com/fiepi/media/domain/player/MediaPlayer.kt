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

package com.fiepi.media.domain.player

import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.player.model.EngineEvent
import com.fiepi.media.domain.player.model.ExternalSubtitle
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.player.model.PlayerClientType
import com.fiepi.media.domain.player.model.TrackType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Business-level player interface, the sole object for ViewModel interaction
 */
interface MediaPlayer {
    val state: StateFlow<MediaPlaybackState>
    val isReady: StateFlow<Boolean>
    val events: Flow<EngineEvent>

    // Core controls
    // Play external (e.g., from file descriptor)
    fun load(url: String)

    // Called within app
    fun load(video: MediaFile.Video, source: MediaSource)

    // Playlist management
    fun setPlaylist(playlist: List<MediaFile.Video>, initialIndex: Int, source: MediaSource)
    fun playPlaylist(playlistId: String, initialMediaId: String? = null, initialIndex: Int = 0)
    fun playNext()
    fun playPrevious()
    fun playAtIndex(index: Int)

    fun play()
    fun pause()
    fun togglePlay()
    fun seekTo(positionMs: Long)
    fun seekBy(offsetMs: Long): Boolean

    // Progress bar scrubbing interaction (dedicated to avoid UI jitter)
    fun onScrubbing(positionMs: Long)
    fun onScrubbingFinished(positionMs: Long)

    // Volume and brightness
    fun setSystemVolume(volume: Int)
    fun setGainVolume(gain: Int)
    fun setMute(muted: Boolean)
    fun adjustVolume(up: Boolean)

    // Playback speed
    fun setPlaybackRate(speed: Float)
    fun setAudioDelay(delayMs: Long)
    fun setSubtitleDelay(delayMs: Long)

    // Track selection
    fun selectTrack(type: TrackType, id: Int)
    fun addSubtitle(url: String)
    fun addSubtitles(subtitles: List<ExternalSubtitle>)

    // Low-level key event pass-through
    fun sendKey(action: String, key: String)
    fun stepFrame(forward: Boolean)
    fun command(vararg args: String)

    // Screenshot functionality
    suspend fun takeScreenshot(): File?

    // View binding
    fun setSurface(surface: Any)
    fun detachSurface(surface: Any)
    fun setSurfaceSize(width: Int, height: Int)

    // Lifecycle reference counting
    fun incrementClientCount(type: PlayerClientType)
    fun decrementClientCount(type: PlayerClientType)

    // Complete release
    fun release()
}
