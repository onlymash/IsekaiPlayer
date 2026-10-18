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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fiepi.media.domain.player.MediaPlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BroadcastReceiver responsible for handling media notification actions during foreground playback,
 * completely decoupled from PlaybackService.
 */
class PlaybackNotificationReceiver : BroadcastReceiver(), KoinComponent {

    private val mediaPlayer: MediaPlayer by inject()

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY -> mediaPlayer.play()
            ACTION_PAUSE -> mediaPlayer.pause()
            ACTION_PREVIOUS -> mediaPlayer.playPrevious()
            ACTION_NEXT -> mediaPlayer.playNext()
        }
    }

    companion object {
        const val ACTION_PLAY = "com.fiepi.media.app.notification.ACTION_PLAY"
        const val ACTION_PAUSE = "com.fiepi.media.app.notification.ACTION_PAUSE"
        const val ACTION_PREVIOUS = "com.fiepi.media.app.notification.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.fiepi.media.app.notification.ACTION_NEXT"
    }
}
