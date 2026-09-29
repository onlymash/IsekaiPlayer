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

package com.fiepi.media.app.activity.delegate

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fiepi.media.app.service.PlaybackService
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent

/**
 * Lifecycle-aware delegate responsible for coordinating playback service transitions
 * between foreground Activity playback and background audio playback.
 */
class PlayerBackgroundPlaybackDelegate(
    private val activity: ComponentActivity,
    private val isPlaying: () -> Boolean,
    private val isBackgroundAudioEnabled: () -> Boolean,
    private val isEnteringBackgroundAudioManually: () -> Boolean,
    private val onIntent: (PlayerIntent) -> Unit
) : DefaultLifecycleObserver {

    private var isEntering = false

    /**
     * Marks activity as being re-entered via new intent to prevent false pause in onPause.
     */
    fun markEntering() {
        isEntering = true
    }

    override fun onStart(owner: LifecycleOwner) {
        // Stop background service when returning to foreground; Activity UI takes over playback control
        activity.stopService(Intent(activity, PlaybackService::class.java))
    }

    override fun onPause(owner: LifecycleOwner) {
        val entering = isEntering
        isEntering = false

        val manualBg = isEnteringBackgroundAudioManually()
        val bgEnabled = isBackgroundAudioEnabled()

        if (!entering && !activity.isInPictureInPictureMode && !bgEnabled && !manualBg) {
            onIntent(PlayerIntent.SetPlay(false))
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        val playing = isPlaying()
        val bgEnabled = isBackgroundAudioEnabled()
        val manualBg = isEnteringBackgroundAudioManually()

        if (activity.isFinishing) {
            // User explicitly closed page (e.g. back button); stop service and release resources completely
            if (!bgEnabled) {
                activity.stopService(Intent(activity, PlaybackService::class.java))
            }
        } else {
            // Switching to background or locking screen (onStop indicates screen no longer visible)
            // If background playback conditions are met and currently playing, start service to maintain foreground playback state
            if ((bgEnabled || manualBg) && playing) {
                activity.startService(Intent(activity, PlaybackService::class.java))
            }
            onIntent(PlayerIntent.EnterBackground)
        }
    }
}
