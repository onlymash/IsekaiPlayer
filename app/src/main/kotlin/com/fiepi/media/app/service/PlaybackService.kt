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
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.fiepi.media.app.activity.PlayerActivity
import com.fiepi.media.domain.player.MediaPlayer
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.player.model.PlayerClientType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Background playback service implemented purely on system native APIs (android.media.*).
 * Optimized for API 33+, featuring an extremely streamlined architecture.
 */
@Suppress("SameParameterValue")
class PlaybackService : Service() {

    private val mediaPlayer: MediaPlayer by inject()
    private val notificationController: NotificationController by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var mediaSession: MediaSession? = null
    private var currentArtwork: android.graphics.Bitmap? = null
    private var currentArtworkContentUri: android.net.Uri? = null
    private var currentArtworkUri: String? = null

    override fun onCreate() {
        super.onCreate()
        mediaPlayer.incrementClientCount(PlayerClientType.BACKGROUND)
        setupMediaSession()
        observePlayerState()
        observeArtworkChanges()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "MediaServicePlaybackSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    mediaPlayer.play()
                }

                override fun onPause() {
                    mediaPlayer.pause()
                }

                override fun onSkipToNext() {
                    mediaPlayer.playNext()
                }

                override fun onSkipToPrevious() {
                    mediaPlayer.playPrevious()
                }

                override fun onStop() {
                    mediaPlayer.release()
                    stopSelf()
                }

                override fun onSeekTo(pos: Long) {
                    mediaPlayer.seekTo(pos)
                }
            })

            val intent = Intent(this@PlaybackService, PlayerActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this@PlaybackService, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            setSessionActivity(pendingIntent)
            isActive = true
        }
    }

    private fun observePlayerState() {
        serviceScope.launch {
            mediaPlayer.state.collectLatest { state ->
                // Maintain foreground state and notification only after video is loaded
                if (!state.isLoaded) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    return@collectLatest
                }

                updateMediaSession(state)

                val notification = createNotification(state)

                // For Android 14+ (API 34), must explicitly specify FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
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
        }
    }

    private fun observeArtworkChanges() {
        serviceScope.launch {
            mediaPlayer.state
                .map { it.mediaArtworkUri }
                .distinctUntilChanged()
                .collectLatest { uriString ->
                    // Whenever URI changes, clear old artwork cache immediately to prevent flashing previous track cover when switching tracks
                    currentArtwork = null
                    currentArtworkContentUri = null
                    currentArtworkUri = uriString

                    if (uriString != null) {
                        val uri = uriString.toUri()
                        currentArtworkContentUri = if (uri.scheme == "content") {
                            uri
                        } else {
                            null
                        }
                        // Asynchronously load artwork via Coil
                        loadArtworkViaCoil(uriString)
                    } else {
                        // If explicitly no artwork, trigger a refresh to update notification bar
                        refreshMetadataAndNotification()
                    }
                }
        }
    }

    private suspend fun loadArtworkViaCoil(uri: String) {
        try {
            val request = ImageRequest.Builder(this)
                .data(uri)
                .size(320)
                .build()
            val result = SingletonImageLoader.get(this).execute(request)
            currentArtwork = if (result is SuccessResult) {
                result.image.asDrawable(resources).toBitmap()
            } else {
                null
            }
            // Manually trigger a state refresh to update notification bar and MediaSession regardless of load success
            refreshMetadataAndNotification()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            currentArtwork = null
            refreshMetadataAndNotification()
        }
    }

    private fun refreshMetadataAndNotification() {
        val state = mediaPlayer.state.value
        updateMediaSession(state)
        val notification = createNotification(state)
        notificationController.showNotification(
            NotificationController.NOTIFICATION_ID_PLAYBACK,
            notification
        )
    }

    private fun updateMediaSession(state: MediaPlaybackState) {
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, state.mediaTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, state.mediaSubtitle)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.duration)
            .apply {
                currentArtwork?.let {
                    putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                    putBitmap(
                        MediaMetadata.METADATA_KEY_ART,
                        it
                    ) // Lock screen artwork compatibility for more devices
                }
            }
            .build()
        mediaSession?.setMetadata(metadata)

        val pbState =
            if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED

        // Native PlaybackState requires 4 arguments; 4th is state update timestamp
        val playbackState = PlaybackState.Builder()
            .setState(
                pbState,
                state.currentPosition,
                state.playbackSpeed,
                SystemClock.elapsedRealtime()
            )
            .setActions(
                PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_STOP
            )
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun createNotification(state: MediaPlaybackState): Notification {
        return notificationController.buildPlaybackNotification(
            context = this,
            state = state,
            sessionToken = mediaSession?.sessionToken,
            artworkBitmap = currentArtwork,
            artworkUri = currentArtworkContentUri,
            playPendingIntent = getPendingIntent(ACTION_PLAY),
            pausePendingIntent = getPendingIntent(ACTION_PAUSE),
            prevPendingIntent = getPendingIntent(ACTION_PREVIOUS),
            nextPendingIntent = getPendingIntent(ACTION_NEXT)
        )
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> mediaPlayer.play()
            ACTION_PAUSE -> mediaPlayer.pause()
            ACTION_PREVIOUS -> mediaPlayer.playPrevious()
            ACTION_NEXT -> mediaPlayer.playNext()
            ACTION_STOP -> {
                mediaPlayer.release()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaPlayer.decrementClientCount(PlayerClientType.BACKGROUND)
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.fiepi.media.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.fiepi.media.app.ACTION_PAUSE"
        const val ACTION_PREVIOUS = "com.fiepi.media.app.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.fiepi.media.app.ACTION_NEXT"
        const val ACTION_STOP = "com.fiepi.media.app.ACTION_STOP"
    }
}
