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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.fiepi.media.app.R
import com.fiepi.media.app.activity.PlayerActivity
import com.fiepi.media.domain.player.model.MediaPlaybackState
import java.io.File

class NotificationController(
    private val context: Context
) {

    companion object {
        private const val CHANNEL_ID_PLAYBACK = "playback_channel"
        private const val CHANNEL_ID_SCREENSHOT = "screenshot_channel"

        const val NOTIFICATION_ID_PLAYBACK = 1001
        const val NOTIFICATION_ID_SCREENSHOT = 2001
    }

    private val manager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannels()
    }

    fun showNotification(id: Int, notification: Notification) {
        manager.notify(id, notification)
    }

    private fun createNotificationChannels() {
        // Playback notification channel
        val playbackChannel = NotificationChannel(
            CHANNEL_ID_PLAYBACK,
            context.getString(R.string.notification_playback_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_playback_channel_description)
            setShowBadge(false)
        }
        // Screenshot notification channel
        val screenshotChannel = NotificationChannel(
            CHANNEL_ID_SCREENSHOT,
            context.getString(R.string.notification_screenshot_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_screenshot_channel_description)
        }
        manager.createNotificationChannel(playbackChannel)
        manager.createNotificationChannel(screenshotChannel)
    }

    /**
     * Displays screenshot saved notification
     */
    fun showScreenshotNotification(context: Context, imageFile: File, mediaTitle: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SCREENSHOT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SCREENSHOT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_screenshot_saved))
            .setContentText(mediaTitle)
            .setLargeIcon(bitmap)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID_SCREENSHOT, notification)
    }

    /**
     * Builds playback control notification
     */
    fun buildPlaybackNotification(
        context: Context,
        state: MediaPlaybackState,
        sessionToken: MediaSession.Token?,
        artworkBitmap: Bitmap?,
        artworkUri: Uri?,
        playPendingIntent: PendingIntent,
        pausePendingIntent: PendingIntent,
        prevPendingIntent: PendingIntent,
        nextPendingIntent: PendingIntent
    ): Notification {
        val playPauseAction = if (state.isPlaying) {
            Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_player_pause),
                context.getString(R.string.player_control_pause),
                pausePendingIntent
            ).build()
        } else {
            Notification.Action.Builder(
                Icon.createWithResource(context, R.drawable.ic_player_play),
                context.getString(R.string.player_control_play),
                playPendingIntent
            ).build()
        }

        val style = Notification.MediaStyle()
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(0, 1, 2)

        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, PlayerActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(context, CHANNEL_ID_PLAYBACK)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(state.mediaTitle)
            .setContentText(state.mediaSubtitle)
            .apply {
                when {
                    artworkUri != null -> setLargeIcon(Icon.createWithContentUri(artworkUri))
                    artworkBitmap != null -> setLargeIcon(Icon.createWithBitmap(artworkBitmap))
                }
            }
            .setContentIntent(contentIntent)
            .setOngoing(state.isPlaying)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_player_previous),
                    context.getString(R.string.player_control_previous),
                    prevPendingIntent
                ).build()
            )
            .addAction(playPauseAction)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_player_next),
                    context.getString(R.string.player_control_next),
                    nextPendingIntent
                ).build()
            )
            .setStyle(style)
            .build()
    }
}