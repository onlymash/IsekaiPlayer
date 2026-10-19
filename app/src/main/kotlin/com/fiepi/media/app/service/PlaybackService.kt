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

package com.fiepi.media.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.fiepi.media.domain.player.MediaPlayer
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.player.model.PlayerClientType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Unified foreground playback service managing notification, foreground service status, and media session integration.
 * Acts as the single source of truth for media playback notifications and background service lifecycle.
 * Optimized for API 33+, featuring a streamlined architecture without foreground/background handoff churn.
 */
@Suppress("SameParameterValue")
class PlaybackService : Service() {

    private val mediaPlayer: MediaPlayer by inject()
    private val notificationController: NotificationController by inject()
    private val mediaSessionManager: MediaSessionManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        mediaPlayer.incrementClientCount(PlayerClientType.BACKGROUND)
        observePlayerState()
    }

    private fun observePlayerState() {
        serviceScope.launch {
            mediaPlayer.state.collectLatest { state ->
                // Maintain foreground state and notification as long as a media item is active or loading
                if (!state.isLoaded && state.mediaTitle.isNullOrEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
                    stopSelf()
                    return@collectLatest
                }

                val notification = createNotification(state)
                startForegroundServiceInternal(notification)
            }
        }
    }

    private fun startForegroundServiceInternal(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NotificationController.NOTIFICATION_ID_PLAYBACK,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NotificationController.NOTIFICATION_ID_PLAYBACK, notification)
        }
    }

    private fun createNotification(state: MediaPlaybackState): Notification {
        return notificationController.buildPlaybackNotification(
            context = this,
            state = state,
            sessionToken = mediaSessionManager.sessionToken,
            artworkBitmap = mediaSessionManager.currentArtwork,
            artworkUri = mediaSessionManager.currentArtworkContentUri,
            playPendingIntent = getPendingIntent(PlaybackNotificationReceiver.ACTION_PLAY),
            pausePendingIntent = getPendingIntent(PlaybackNotificationReceiver.ACTION_PAUSE),
            prevPendingIntent = getPendingIntent(PlaybackNotificationReceiver.ACTION_PREVIOUS),
            nextPendingIntent = getPendingIntent(PlaybackNotificationReceiver.ACTION_NEXT)
        )
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent =
            Intent(this, PlaybackNotificationReceiver::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
            mediaPlayer.release()
            stopSelf()
            return START_NOT_STICKY
        }

        // Must call startForeground immediately in onStartCommand to fulfill Android's FGS requirement
        val notification = createNotification(mediaPlayer.state.value)
        startForegroundServiceInternal(notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
        mediaPlayer.decrementClientCount(PlayerClientType.BACKGROUND)
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.fiepi.media.app.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, PlaybackService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}
