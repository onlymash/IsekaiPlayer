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

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fiepi.media.app.service.PlaybackService
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent

/**
 * Lifecycle-aware delegate responsible for coordinating playback service lifecycle and background audio policies.
 * Ensures PlaybackService remains continuously running for seamless foreground and background audio playback.
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
        // Ensure PlaybackService is active whenever playback is started
        if (isPlaying()) {
            PlaybackService.start(activity)
        }
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
        val bgEnabled = isBackgroundAudioEnabled()
        val manualBg = isEnteringBackgroundAudioManually()

        if (activity.isFinishing) {
            // User explicitly closed page (e.g. back button); stop playback and service
            onIntent(PlayerIntent.SetPlay(false))
            PlaybackService.stop(activity)
        } else {
            // Switching to background or locking screen
            if (!activity.isInPictureInPictureMode && !bgEnabled && !manualBg) {
                onIntent(PlayerIntent.SetPlay(false))
                PlaybackService.stop(activity)
            }
            onIntent(PlayerIntent.EnterBackground)
        }
    }
}
