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

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.fiepi.media.app.service.MediaSessionManager
import com.fiepi.media.app.service.NotificationController
import com.fiepi.media.app.service.PlaybackNotificationReceiver
import com.fiepi.media.domain.player.model.MediaPlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware delegate responsible for managing foreground media notifications for PlayerActivity.
 * Integrates with the application's unified PlayerMediaSessionManager to ensure stable MediaSession token sharing.
 */
class PlayerMediaSessionDelegate(
    private val activity: Activity,
    private val notificationController: NotificationController,
    private val mediaSessionManager: MediaSessionManager,
    private val playbackState: Flow<MediaPlaybackState>,
    private val isBackgroundAudioEnabled: () -> Boolean
) : DefaultLifecycleObserver {

    private var observeJob: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        startObserving(owner)
    }

    override fun onStop(owner: LifecycleOwner) {
        stopObserving()
        if (activity.isFinishing && !isBackgroundAudioEnabled()) {
            notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopObserving()
        if (activity.isFinishing && !isBackgroundAudioEnabled()) {
            notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
        }
    }

    private fun startObserving(owner: LifecycleOwner) {
        observeJob?.cancel()
        observeJob = owner.lifecycleScope.launch {
            playbackState.collectLatest { state ->
                if (!state.isLoaded && state.mediaTitle.isNullOrEmpty()) {
                    notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
                    return@collectLatest
                }

                updateNotification(state)
            }
        }
    }

    private fun updateNotification(state: MediaPlaybackState) {
        val notification = notificationController.buildPlaybackNotification(
            context = activity,
            state = state,
            sessionToken = mediaSessionManager.sessionToken,
            artworkBitmap = mediaSessionManager.currentArtwork,
            artworkUri = mediaSessionManager.currentArtworkContentUri,
            playPendingIntent = getPendingIntent(PlaybackNotificationReceiver.ACTION_PLAY),
            pausePendingIntent = getPendingIntent(PlaybackNotificationReceiver.ACTION_PAUSE),
            prevPendingIntent = getPendingIntent(PlaybackNotificationReceiver.ACTION_PREVIOUS),
            nextPendingIntent = getPendingIntent(PlaybackNotificationReceiver.ACTION_NEXT)
        )
        notificationController.showNotification(
            NotificationController.NOTIFICATION_ID_PLAYBACK,
            notification
        )
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent =
            Intent(activity, PlaybackNotificationReceiver::class.java).apply {
                this.action = action
            }
        return PendingIntent.getBroadcast(
            activity,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
    }
}
