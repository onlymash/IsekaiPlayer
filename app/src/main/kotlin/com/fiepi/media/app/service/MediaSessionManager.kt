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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.fiepi.media.app.R
import com.fiepi.media.app.activity.PlayerActivity
import com.fiepi.media.domain.player.MediaPlayer
import com.fiepi.media.domain.player.model.MediaPlaybackState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Centralized, singleton manager for the application's unified MediaSession.
 * Ensures a single stable MediaSession token is shared across foreground Activity and background Service,
 * preventing system UI media controls, ColorOS Fluid Cloud, and Bluetooth headsets from losing connection during track switches.
 */
class MediaSessionManager(
    private val context: Context,
    private val mediaPlayer: MediaPlayer
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val placeholderArtwork: Bitmap by lazy {
        val width = 256
        val height = 256
        val bitmap = createBitmap(width, height)
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_playback_cover_placeholder)
            ?: return@lazy bitmap
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        bitmap
    }

    val mediaSession: MediaSession = MediaSession(context, "IsekaiPlayerMediaSession").apply {
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
            }

            override fun onSeekTo(pos: Long) {
                mediaPlayer.seekTo(pos)
            }
        })

        val intent = Intent(context, PlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        setSessionActivity(pendingIntent)
    }

    val sessionToken: MediaSession.Token get() = mediaSession.sessionToken

    var currentArtwork: Bitmap? = null
        private set
    var currentArtworkContentUri: android.net.Uri? = null
        private set

    init {
        observePlayerState()
        observeArtworkChanges()
    }

    private fun observePlayerState() {
        managerScope.launch {
            mediaPlayer.state.collectLatest { state ->
                updateMediaSession(state)
            }
        }
    }

    private fun observeArtworkChanges() {
        managerScope.launch {
            mediaPlayer.state
                .map { it.mediaArtworkUri }
                .distinctUntilChanged()
                .collectLatest { uriString ->
                    currentArtwork = null
                    currentArtworkContentUri = null

                    if (uriString != null) {
                        val uri = uriString.toUri()
                        currentArtworkContentUri = if (uri.scheme == "content") uri else null
                        loadArtworkViaCoil(uriString)
                    } else {
                        updateMediaSession(mediaPlayer.state.value)
                    }
                }
        }
    }

    private suspend fun loadArtworkViaCoil(uri: String) {
        try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(320)
                .build()
            val result = SingletonImageLoader.get(context).execute(request)
            currentArtwork = if (result is SuccessResult) {
                result.image.asDrawable(context.resources).toBitmap()
            } else {
                null
            }
            updateMediaSession(mediaPlayer.state.value)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            currentArtwork = null
            updateMediaSession(mediaPlayer.state.value)
        }
    }

    fun updateMediaSession(state: MediaPlaybackState) {
        if (!state.isLoaded && state.mediaTitle.isNullOrEmpty()) {
            mediaSession.isActive = false
            return
        }

        val artworkToUse = currentArtwork ?: placeholderArtwork

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, state.mediaTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, state.mediaSubtitle)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, state.duration)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artworkToUse)
            .putBitmap(MediaMetadata.METADATA_KEY_ART, artworkToUse)
            .build()
        mediaSession.setMetadata(metadata)

        val pbStateCode = when {
            state.isBuffering || (!state.isLoaded && !state.mediaTitle.isNullOrEmpty()) -> PlaybackState.STATE_BUFFERING
            state.isPlaying -> PlaybackState.STATE_PLAYING
            else -> PlaybackState.STATE_PAUSED
        }

        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_STOP

        val playbackState = PlaybackState.Builder()
            .setState(
                pbStateCode,
                state.currentPosition,
                state.playbackSpeed,
                SystemClock.elapsedRealtime()
            )
            .setActions(actions)
            .build()

        mediaSession.setPlaybackState(playbackState)
        mediaSession.isActive = true
    }
}
