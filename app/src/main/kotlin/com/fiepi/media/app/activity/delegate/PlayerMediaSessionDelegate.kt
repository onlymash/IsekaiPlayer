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
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.domain.player.model.MediaPlaybackState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware delegate responsible for managing the foreground MediaSession of PlayerActivity.
 * Ensures bluetooth media buttons and headset controls work properly while in the foreground,
 * without creating ongoing notification bar items.
 */
class PlayerMediaSessionDelegate(
    private val activity: Activity,
    private val playbackState: Flow<MediaPlaybackState>,
    private val onIntent: (PlayerIntent) -> Unit
) : DefaultLifecycleObserver {

    private var mediaSession: MediaSession? = null
    private var observeJob: Job? = null

    override fun onCreate(owner: LifecycleOwner) {
        setupMediaSession()
    }

    override fun onStart(owner: LifecycleOwner) {
        startObserving(owner)
    }

    override fun onStop(owner: LifecycleOwner) {
        stopObserving()
    }

    override fun onDestroy(owner: LifecycleOwner) {
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
                if (!state.isLoaded) return@collectLatest

                val metadata = MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, state.mediaTitle)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, state.mediaSubtitle)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, state.duration)
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
            }
        }
    }

    private fun stopObserving() {
        observeJob?.cancel()
        mediaSession?.isActive = false
    }
}
