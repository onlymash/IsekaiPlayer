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

package com.fiepi.media.app.ui.screen.player.viewmodel

import android.net.Uri
import com.fiepi.media.domain.model.preferences.ExoSubtitleOptions
import com.fiepi.media.domain.model.preferences.PlaybackOptions
import com.fiepi.media.domain.model.preferences.PlayerUiOptions
import com.fiepi.media.domain.model.preferences.VideoScaleMode
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.player.model.TrackType
import com.fiepi.media.player.model.PlayerSurfaceEvent

/**
 * 1. Single source of truth for UI rendering (StateFlow)
 */
data class PlayerState(
    val playback: MediaPlaybackState = MediaPlaybackState(),
    val ui: UIState = UIState(),
    val uiOptions: PlayerUiOptions = PlayerUiOptions(),
    val playbackOptions: PlaybackOptions = PlaybackOptions(),
    val exoSubtitleOptions: ExoSubtitleOptions = ExoSubtitleOptions()
) {
    /**
     * Precise millisecond position for UI progress bar display
     */
    val displayPosition: Long
        get() = if (playback.duration <= 0L) 0L else {
            playback.scrubbingPosition ?: if (playback.isSeeking) {
                playback.targetSeekPosition ?: playback.currentPosition
            } else {
                playback.currentPosition
            }
        }
}

data class UIState(
    val isControlsVisible: Boolean = false,
    val isLocked: Boolean = false,
    val isPiP: Boolean = false,
    val isSurfaceReady: Boolean = false,
    val isFastForwarding: Boolean = false,
    val activeNotification: PlayerNotification? = null,
    val brightness: Float = -1f, // -1 means use system default
    val quickBarMode: QuickBarMode = QuickBarMode.None,
    val activeDrawer: PlayerDrawerType = PlayerDrawerType.Tracks,
    val controlMode: PlayerControlMode = PlayerControlMode.Normal,
    val isOsdVisible: Boolean = false,
    val currentOsdPage: Int = 1,
    val isEnteringBackgroundAudioManually: Boolean = false
)

enum class QuickBarMode {
    None, Volume, Brightness
}

enum class PlayerDrawerType {
    Tracks, Playlist, Osd, Decoder, Player, Gestures, Audio, Subtitle
}

enum class PlayerControlMode {
    Normal, Screenshot, PlaybackSpeed, AudioDelay, SubtitleDelay
}

sealed class PlayerNotification {
    data class SeekForward(val seconds: Int) : PlayerNotification()
    data class SeekBackward(val seconds: Int) : PlayerNotification()
}

/**
 * 2. All user interaction events for player and control panel (Intent)
 */
sealed interface PlayerIntent {
    // Video loading and screen lifecycle
    // Called in video list
    data class LoadMedia(val sourceId: String, val videoPath: String) : PlayerIntent

    // Called for custom playlist
    data class LoadPlaylist(
        val playlistId: String,
        val initialMediaId: String? = null,
        val initialIndex: Int = 0
    ) : PlayerIntent

    // Called in playlist
    data class PlayAtIndex(val index: Int) : PlayerIntent

    // Called by external apps to play single video
    data class LoadVideo(val uri: Uri, val title: String = "") : PlayerIntent
    data class HandleSurfaceEvent(val event: PlayerSurfaceEvent) : PlayerIntent
    object EnterBackground : PlayerIntent

    // Basic playback controls
    object TogglePlay : PlayerIntent
    data class SetPlay(val play: Boolean) : PlayerIntent
    data class SetPlaybackRate(
        val speed: Float,
        val persist: Boolean = false,
        val isTemporary: Boolean = false
    ) : PlayerIntent

    data class SetSystemVolume(val volume: Int) : PlayerIntent
    data class SetGainVolume(val gain: Int) : PlayerIntent
    data class AdjustVolume(val up: Boolean) : PlayerIntent
    data class SetBrightness(val brightness: Float) : PlayerIntent
    data class SetVideoScaleMode(val mode: VideoScaleMode) : PlayerIntent
    data class SeekTo(val positionMs: Long) : PlayerIntent
    data class SeekBy(val offsetMs: Long) : PlayerIntent
    data class SetAudioDelay(val delayMs: Long, val persist: Boolean = false) : PlayerIntent
    data class SetSubtitleDelay(val delayMs: Long) : PlayerIntent
    data class SelectTrack(val type: TrackType, val id: Int) : PlayerIntent
    data class AddSubtitle(val uri: Uri) : PlayerIntent

    // Progress bar scrubbing interaction (dedicated to avoid UI jitter)
    data class OnScrubbing(val positionMs: Long) : PlayerIntent
    data class OnScrubbingFinished(val positionMs: Long) : PlayerIntent

    // Compose UI control panel management
    object ToggleControls : PlayerIntent
    data class SetControlsVisible(val visible: Boolean) : PlayerIntent
    data class SetPiPMode(val active: Boolean) : PlayerIntent
    data class SetBackgroundAudio(val enabled: Boolean) : PlayerIntent
    data class SetActiveDrawer(val type: PlayerDrawerType) : PlayerIntent
    object ToggleLock : PlayerIntent
    object ToggleLoopMode : PlayerIntent
    object NotifyUserInteraction : PlayerIntent
    object ExitPlayer : PlayerIntent
    object EnterBackgroundAudio : PlayerIntent

    object PlayNext : PlayerIntent
    object PlayPrevious : PlayerIntent

    object ClearStatus : PlayerIntent

    data class SetControlMode(val mode: PlayerControlMode) : PlayerIntent
    object TakeScreenshot : PlayerIntent
    data class StepFrame(val forward: Boolean) : PlayerIntent

    data class SetOsdVisible(val visible: Boolean) : PlayerIntent
    data class SetOsdPage(val page: Int) : PlayerIntent

    // System functions
    object TogglePiP : PlayerIntent
    data class OnKeyEvent(val action: String, val mpvKey: String) : PlayerIntent
    data class OnScrollEvent(val commands: List<Pair<String, String>>) : PlayerIntent
    data class ExecuteCommand(val args: String) : PlayerIntent
}

/**
 * 3. One-off behavior feedback (Side Effects)
 */
sealed interface PlayerEffect {
    object TriggerEnterPiP : PlayerEffect
    object TriggerEnterBackgroundAudio : PlayerEffect
    object FinishPlayer : PlayerEffect
    data class ShowToast(val message: String) : PlayerEffect
    data class KeepScreenOn(val keep: Boolean) : PlayerEffect
    data class SetWindowBrightness(val brightness: Float) : PlayerEffect
}
