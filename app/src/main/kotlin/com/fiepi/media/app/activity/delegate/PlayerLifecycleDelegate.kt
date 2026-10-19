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

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.fiepi.media.app.activity.PlayerActivity
import com.fiepi.media.app.service.PlaybackService
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent

/**
 * Unified lifecycle observer managing Picture-in-Picture (PiP) controls, background playback policies,
 * and service lifecycle coordination for PlayerActivity.
 */
class PlayerLifecycleDelegate(
    private val activity: ComponentActivity,
    private val isPlaying: () -> Boolean,
    private val isBackgroundAudioEnabled: () -> Boolean,
    private val isEnteringBackgroundAudioManually: () -> Boolean,
    private val onIntent: (PlayerIntent) -> Unit
) : DefaultLifecycleObserver {

    private var isEntering = false

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action != PlayerActivity.ACTION_PIP_CONTROL) return

            val controlType = intent.getIntExtra(PlayerActivity.EXTRA_CONTROL_TYPE, 0)

            when (controlType) {
                PlayerActivity.CONTROL_TYPE_PREV -> onIntent(PlayerIntent.PlayPrevious)
                PlayerActivity.CONTROL_TYPE_PLAY_PAUSE -> onIntent(PlayerIntent.TogglePlay)
                PlayerActivity.CONTROL_TYPE_NEXT -> onIntent(PlayerIntent.PlayNext)
            }
        }
    }

    /**
     * Marks activity as being re-entered via new intent to prevent false pause in onPause.
     */
    fun markEntering() {
        isEntering = true
    }

    override fun onCreate(owner: LifecycleOwner) {
        ContextCompat.registerReceiver(
            activity,
            pipReceiver,
            IntentFilter(PlayerActivity.ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
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

    override fun onDestroy(owner: LifecycleOwner) {
        activity.unregisterReceiver(pipReceiver)
    }

    /**
     * Handles Picture-in-Picture mode change events.
     * Prevents audio leakage when PiP is closed explicitly by user ("X" button) with background audio disabled.
     */
    fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        if (!isInPictureInPictureMode && !isBackgroundAudioEnabled() && activity.lifecycle.currentState < Lifecycle.State.STARTED) {
            onIntent(PlayerIntent.SetPlay(false))
            PlaybackService.stop(activity)
        }
        onIntent(PlayerIntent.SetPiPMode(isInPictureInPictureMode))
    }

    /**
     * Enters Picture-in-Picture mode.
     */
    fun enterPiP() {
        activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
    }
}
