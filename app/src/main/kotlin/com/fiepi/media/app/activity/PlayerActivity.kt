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

package com.fiepi.media.app.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.fiepi.media.app.activity.delegate.PlayerBackgroundPlaybackDelegate
import com.fiepi.media.app.activity.delegate.PlayerMediaSessionDelegate
import com.fiepi.media.app.activity.delegate.PlayerPipDelegate
import com.fiepi.media.app.ui.app.Player
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerViewModel
import com.fiepi.media.domain.utils.FileUtils
import com.fiepi.media.player.input.PlayerKeyHandler
import kotlinx.coroutines.flow.map
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlayerActivity : ComponentActivity() {

    companion object {

        const val EXTRA_SOURCE_ID = "extra_source_id"
        const val EXTRA_VIDEO_PATH = "extra_video_path"
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
        const val EXTRA_INITIAL_MEDIA_ID = "extra_initial_media_id"

        const val ACTION_PIP_CONTROL = "com.fiepi.media.app.ACTION_PIP_CONTROL"
        const val EXTRA_CONTROL_TYPE = "control_type"
        const val CONTROL_TYPE_PREV = 1
        const val CONTROL_TYPE_PLAY_PAUSE = 2
        const val CONTROL_TYPE_NEXT = 3

        fun newIntent(context: Context, sourceId: String, videoPath: String): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_SOURCE_ID, sourceId)
                putExtra(EXTRA_VIDEO_PATH, videoPath)
                // Ensure Activity can be restored from background/PiP when reused
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        }

        fun newPlaylistIntent(
            context: Context,
            playlistId: String,
            initialMediaId: String? = null
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_PLAYLIST_ID, playlistId)
                if (initialMediaId != null) {
                    putExtra(EXTRA_INITIAL_MEDIA_ID, initialMediaId)
                }
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        }

        fun newIntent(context: Context, url: String): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                data = url.toUri()
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        }
    }

    private val viewModel: PlayerViewModel by viewModel()

    // Lifecycle-aware MediaSession delegate for handling bluetooth headset media controls in foreground
    private val mediaSessionDelegate by lazy {
        PlayerMediaSessionDelegate(
            activity = this,
            playbackState = viewModel.state.map { it.playback },
            onIntent = viewModel::sendIntent
        )
    }

    // Lifecycle-aware PiP delegate for handling Picture-in-Picture controls and state sync
    private val pipDelegate by lazy {
        PlayerPipDelegate(
            activity = this,
            onIntent = viewModel::sendIntent,
            isBackgroundAudioEnabled = { viewModel.playerUiOptions.enableBackgroundAudio }
        )
    }

    // Lifecycle-aware background playback delegate for coordinating foreground and background service transitions
    private val backgroundPlaybackDelegate by lazy {
        PlayerBackgroundPlaybackDelegate(
            activity = this,
            isPlaying = { viewModel.state.value.playback.isPlaying },
            isBackgroundAudioEnabled = { viewModel.playerUiOptions.enableBackgroundAudio },
            isEnteringBackgroundAudioManually = { viewModel.uiState.isEnteringBackgroundAudioManually },
            onIntent = viewModel::sendIntent
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.enableEdgeToEdge(window)
        super.onCreate(savedInstanceState)

        // Ensure volume buttons directly adjust media stream volume
        volumeControlStream = AudioManager.STREAM_MUSIC

        // Register lifecycle observers
        lifecycle.addObserver(mediaSessionDelegate)
        lifecycle.addObserver(pipDelegate)
        lifecycle.addObserver(backgroundPlaybackDelegate)

        // Request notification permission for showing media controls
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        // Process launch Intent only when not recreating to prevent Activity recreation from reloading video and auto-playing
        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            Player(
                viewModel = viewModel,
                onBack = { finish() },
                onEnterPiP = {
                    pipDelegate.enterPiP()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        backgroundPlaybackDelegate.markEntering()
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Core Intent handler. Responsible for dispatching media loading requests.
     */
    private fun handleIntent(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null) {
            // Attempt to retrieve file name as title
            val rawTitle = try {
                contentResolver.query(data, null, null, null, null)?.use { cursor ->
                    val nameIndex =
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)
                    } else null
                } ?: FileUtils.extractFileName(data.toString())
            } catch (_: Exception) {
                FileUtils.extractFileName(data.toString())
            }.ifBlank { "External Media" }

            val title = FileUtils.extractNameWithoutExtension(rawTitle)

            viewModel.sendIntent(PlayerIntent.LoadVideo(data, title))
        } else if (intent.hasExtra(EXTRA_PLAYLIST_ID)) {
            val playlistId = intent.getStringExtra(EXTRA_PLAYLIST_ID) ?: ""
            val initialMediaId = intent.getStringExtra(EXTRA_INITIAL_MEDIA_ID)
            if (playlistId.isNotBlank()) {
                viewModel.sendIntent(PlayerIntent.LoadPlaylist(playlistId, initialMediaId))
            }
        } else if (intent.hasExtra(EXTRA_SOURCE_ID) && intent.hasExtra(EXTRA_VIDEO_PATH)) {
            // Trigger loading only when Intent explicitly contains source info.
            val sid = intent.getStringExtra(EXTRA_SOURCE_ID) ?: ""
            val path = intent.getStringExtra(EXTRA_VIDEO_PATH) ?: ""
            if (sid.isNotBlank() && path.isNotBlank()) {
                viewModel.sendIntent(PlayerIntent.LoadMedia(sid, path))
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Do not consume volume keys in PiP mode; retain default system behavior
        if (isInPictureInPictureMode) return true

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                viewModel.sendIntent(PlayerIntent.AdjustVolume(true))
                return true
            }

            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                viewModel.sendIntent(PlayerIntent.AdjustVolume(false))
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipDelegate.onPictureInPictureModeChanged(isInPictureInPictureMode)
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Disable all regular key dispatching in PiP mode
        if (isInPictureInPictureMode) return true

        // Parse keyboard, remote d-pad, and media keys via PlayerKeyHandler and pass through to engine
        val mapped = PlayerKeyHandler.mapKeyEvent(event)
        if (mapped != null) {
            viewModel.sendIntent(PlayerIntent.OnKeyEvent(mapped.first, mapped.second))
            return true
        }

        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        // Disable all gesture/scroll events in PiP mode
        if (isInPictureInPictureMode) return true

        val scrollCommands = PlayerKeyHandler.mapScrollEvent(event)
        if (scrollCommands.isNotEmpty()) {
            viewModel.sendIntent(PlayerIntent.OnScrollEvent(scrollCommands))
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}
