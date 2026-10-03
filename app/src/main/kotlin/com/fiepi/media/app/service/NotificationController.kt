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
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.fiepi.media.app.R
import com.fiepi.media.app.activity.MainActivity
import com.fiepi.media.app.activity.PlayerActivity
import com.fiepi.media.domain.model.media.FileOperationType
import com.fiepi.media.domain.player.model.MediaPlaybackState
import java.io.File

class NotificationController(
    private val context: Context
) {

    companion object {
        private const val CHANNEL_ID_PLAYBACK = "playback_channel"
        private const val CHANNEL_ID_SCREENSHOT = "screenshot_channel"
        const val CHANNEL_ID_FILE_OPERATION = "file_operation_channel"

        const val NOTIFICATION_ID_PLAYBACK = 1001
        const val NOTIFICATION_ID_SCREENSHOT = 2001
        const val NOTIFICATION_ID_FILE_OPERATION = 3001
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

    fun cancelNotification(id: Int) {
        manager.cancel(id)
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
        // File operation notification channel
        val fileOperationChannel = NotificationChannel(
            CHANNEL_ID_FILE_OPERATION,
            context.getString(R.string.file_operation_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.file_operation_notification_channel_desc)
        }
        manager.createNotificationChannel(playbackChannel)
        manager.createNotificationChannel(screenshotChannel)
        manager.createNotificationChannel(fileOperationChannel)
    }

    /**
     * Builds notification for file operations (Copy, Move, Delete) with byte progress and speed.
     */
    fun buildFileOperationNotification(
        context: Context,
        type: FileOperationType,
        processedBytes: Long,
        totalBytes: Long,
        bytesPerSecond: Long,
        fileName: String,
        cancelPendingIntent: PendingIntent
    ): Notification {
        val titleRes = when (type) {
            FileOperationType.Copy -> R.string.file_operation_copying
            FileOperationType.Move -> R.string.file_operation_moving
            FileOperationType.Delete -> R.string.file_operation_deleting
        }
        val title = context.getString(titleRes)

        val formattedSpeed = if (bytesPerSecond > 0) {
            "${Formatter.formatFileSize(context, bytesPerSecond)}/s"
        } else ""

        val formattedProcessed = Formatter.formatFileSize(context, processedBytes)
        val formattedTotal = Formatter.formatFileSize(context, totalBytes)

        val contentText = buildString {
            if (fileName.isNotEmpty()) {
                append(fileName)
            }
            if (totalBytes > 0) {
                if (isNotEmpty()) append(" • ")
                append("$formattedProcessed / $formattedTotal")
            }
            if (formattedSpeed.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(formattedSpeed)
            }
        }

        val progressPercent = if (totalBytes > 0) {
            ((processedBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
        } else 0

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_FILE_OPERATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setProgress(100, progressPercent, totalBytes == 0L && type != FileOperationType.Delete)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.file_operation_cancel),
                cancelPendingIntent
            )
            .build()
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
