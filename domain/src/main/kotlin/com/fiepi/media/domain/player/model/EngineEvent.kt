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

/**
 * Common event definitions across player engines
 */
sealed interface EngineEvent {
    // Progress and duration
    data class PositionChanged(val positionMs: Long) : EngineEvent
    data class BufferedPositionChanged(val bufferedPositionMs: Long) : EngineEvent
    data class DurationChanged(val durationMs: Long) : EngineEvent

    // Video metadata
    data class FrameInfoChanged(val current: Long, val total: Long) : EngineEvent
    data class HwdecChanged(val hwdec: String) : EngineEvent

    // Playback state
    data class PlayPauseStateChanged(val isPlaying: Boolean) : EngineEvent
    data class BufferingStateChanged(val isBuffering: Boolean) : EngineEvent
    data object PlaybackEnded : EngineEvent

    // Engine properties
    data class PlaybackSpeedChanged(val speed: Float) : EngineEvent
    data class AudioDelayChanged(val delayMs: Long) : EngineEvent
    data class SubtitleDelayChanged(val delayMs: Long) : EngineEvent
    data class SubtitleTextUpdated(val text: String?) : EngineEvent
    data class VolumeChanged(val gain: Int) : EngineEvent

    // Lifecycle
    data object FileLoaded : EngineEvent
    data object FileUnloaded : EngineEvent
    data object EngineRestarted : EngineEvent

    // Track changes
    data class AudioTracksChanged(val tracks: List<MediaTrack.Audio>) : EngineEvent
    data class SubtitleTracksChanged(val tracks: List<MediaTrack.Subtitle>) : EngineEvent
    data class VideoTracksChanged(val tracks: List<MediaTrack.Video>) : EngineEvent
}
