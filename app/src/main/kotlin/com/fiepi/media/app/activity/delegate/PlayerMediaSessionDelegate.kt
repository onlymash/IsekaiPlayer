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
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.fiepi.media.app.service.NotificationController
import com.fiepi.media.app.service.PlaybackService
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.domain.player.model.MediaPlaybackState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware delegate responsible for managing the foreground MediaSession and notification of PlayerActivity.
 * Ensures bluetooth media buttons, headset controls, and foreground media notifications work properly while in foreground.
 */
class PlayerMediaSessionDelegate(
    private val activity: Activity,
    private val notificationController: NotificationController,
    private val playbackState: Flow<MediaPlaybackState>,
    private val isBackgroundAudioEnabled: () -> Boolean,
    private val onIntent: (PlayerIntent) -> Unit
) : DefaultLifecycleObserver {

    private var mediaSession: MediaSession? = null
    private var observeJob: Job? = null
    private var artworkJob: Job? = null

    private var currentArtwork: android.graphics.Bitmap? = null
    private var currentArtworkContentUri: android.net.Uri? = null

    override fun onCreate(owner: LifecycleOwner) {
        setupMediaSession()
    }

    override fun onStart(owner: LifecycleOwner) {
        startObserving(owner)
        observeArtwork(owner)
    }

    override fun onStop(owner: LifecycleOwner) {
        stopObserving()
        if (!isBackgroundAudioEnabled() || activity.isFinishing) {
            notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopObserving()
        if (!isBackgroundAudioEnabled() || activity.isFinishing) {
            notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
        }
        mediaSession?.release()
        mediaSession = null
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(activity, "MediaActivityPlaybackSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    onIntent(PlayerIntent.SetPlay(true))
                }

                override fun onPause() {
                    onIntent(PlayerIntent.SetPlay(false))
                }

                override fun onSkipToNext() {
                    onIntent(PlayerIntent.PlayNext)
                }

                override fun onSkipToPrevious() {
                    onIntent(PlayerIntent.PlayPrevious)
                }

                override fun onSeekTo(pos: Long) {
                    onIntent(PlayerIntent.SeekTo(pos))
                }
            })
        }
    }

    private fun startObserving(owner: LifecycleOwner) {
        mediaSession?.isActive = true
        observeJob?.cancel()
        observeJob = owner.lifecycleScope.launch {
            playbackState.collectLatest { state ->
                if (!state.isLoaded) {
                    notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_PLAYBACK)
                    mediaSession?.isActive = false
                    return@collectLatest
                }

                val metadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, state.mediaTitle)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, state.mediaSubtitle)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, state.duration)
                    .apply {
                        currentArtwork?.let {
                            putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                            putBitmap(MediaMetadata.METADATA_KEY_ART, it)
                        }
                    }
                    .build()
                mediaSession?.setMetadata(metadata)

                val pbState =
                    if (state.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
                val pbStateObj = PlaybackState.Builder()
                    .setState(
                        pbState,
                        state.currentPosition,
                        state.playbackSpeed,
                        SystemClock.elapsedRealtime()
                    )
                    .setActions(
                        PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                                PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackState.ACTION_SEEK_TO
                    )
                    .build()
                mediaSession?.setPlaybackState(pbStateObj)

                updateNotification(state)
            }
        }
    }

    private fun observeArtwork(owner: LifecycleOwner) {
        artworkJob?.cancel()
        artworkJob = owner.lifecycleScope.launch {
            playbackState
                .map { it.mediaArtworkUri }
                .distinctUntilChanged()
                .collectLatest { uriString ->
                    currentArtwork = null
                    currentArtworkContentUri = null

                    if (uriString != null) {
                        val uri = uriString.toUri()
                        currentArtworkContentUri = if (uri.scheme == "content") uri else null
                        loadArtworkViaCoil(uriString)
                    }
                }
        }
    }

    private suspend fun loadArtworkViaCoil(uri: String) {
        try {
            val request = ImageRequest.Builder(activity)
                .data(uri)
                .size(320)
                .build()
            val result = SingletonImageLoader.get(activity).execute(request)
            currentArtwork = if (result is SuccessResult) {
                result.image.asDrawable(activity.resources).toBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            currentArtwork = null
        }
    }

    private fun updateNotification(state: MediaPlaybackState) {
        val notification = notificationController.buildPlaybackNotification(
            context = activity,
            state = state,
            sessionToken = mediaSession?.sessionToken,
            artworkBitmap = currentArtwork,
            artworkUri = currentArtworkContentUri,
            playPendingIntent = getPendingIntent(PlaybackService.ACTION_PLAY),
            pausePendingIntent = getPendingIntent(PlaybackService.ACTION_PAUSE),
            prevPendingIntent = getPendingIntent(PlaybackService.ACTION_PREVIOUS),
            nextPendingIntent = getPendingIntent(PlaybackService.ACTION_NEXT)
        )
        notificationController.showNotification(
            NotificationController.NOTIFICATION_ID_PLAYBACK,
            notification
        )
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(activity, PlaybackService::class.java).apply { this.action = action }
        return PendingIntent.getService(activity, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun stopObserving() {
        observeJob?.cancel()
        artworkJob?.cancel()
        mediaSession?.isActive = false
    }
}
