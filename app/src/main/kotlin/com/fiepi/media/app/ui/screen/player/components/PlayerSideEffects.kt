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

package com.fiepi.media.app.ui.screen.player.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.Icon
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.fiepi.media.app.R
import com.fiepi.media.app.activity.PlayerActivity
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerEffect
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.domain.model.preferences.PlayerOrientation
import kotlinx.coroutines.flow.Flow

/**
 * Aggregates various side effect components for the player.
 */
@Composable
fun PlayerSideEffects(
    state: PlayerState,
    effectFlow: Flow<PlayerEffect>,
    onEnterPiP: () -> Unit,
    onFinish: () -> Unit
) {
    val uiOptions = state.uiOptions
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    // Calculate matching screen orientation
    val selectedVideoTrack = state.playback.selectedVideoTrack
    val targetOrientation = remember(
        uiOptions.orientation,
        selectedVideoTrack
    ) {
        when (uiOptions.orientation) {
            PlayerOrientation.Default -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            PlayerOrientation.Sensor -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            PlayerOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            PlayerOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            PlayerOrientation.Video -> {
                if (selectedVideoTrack != null && selectedVideoTrack.width > 0 && selectedVideoTrack.height > 0) {
                    if (selectedVideoTrack.width >= selectedVideoTrack.height) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }
            }
        }
    }

    // Dynamically apply screen orientation
    DisposableEffect(targetOrientation) {
        activity.requestedOrientation = targetOrientation
        onDispose { }
    }

    // Dynamically manage Picture-in-Picture configuration
    val isPlaying = state.playback.isPlaying
    val currentIndex = state.playback.currentIndex
    val totalCount = state.playback.totalVideosCount

    LaunchedEffect(
        isPlaying,
        currentIndex,
        totalCount,
        selectedVideoTrack,
        uiOptions.enablePiP
    ) {
        updatePiPParams(
            activity = activity,
            width = selectedVideoTrack?.width ?: 0,
            height = selectedVideoTrack?.height ?: 0,
            isPlaying = isPlaying,
            hasPrev = currentIndex > 0,
            hasNext = currentIndex < totalCount - 1,
            enabled = uiOptions.enablePiP
        )
    }

    // Process one-off side effects
    LaunchedEffect(effectFlow) {
        effectFlow.collect { effect ->
            when (effect) {
                is PlayerEffect.TriggerEnterPiP -> onEnterPiP()
                is PlayerEffect.TriggerEnterBackgroundAudio -> {
                    activity.moveTaskToBack(true)
                }

                is PlayerEffect.FinishPlayer -> onFinish()
                is PlayerEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()

                is PlayerEffect.KeepScreenOn -> {
                    activity.window?.let { window ->
                        if (effect.keep) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                is PlayerEffect.SetWindowBrightness -> {
                    activity.window?.let { window ->
                        val params = window.attributes
                        params.screenBrightness = effect.brightness
                        window.attributes = params
                    }
                }
            }
        }
    }
}

/**
 * Logic for updating Picture-in-Picture parameters.
 */
private fun updatePiPParams(
    activity: Activity,
    width: Int,
    height: Int,
    isPlaying: Boolean,
    hasPrev: Boolean,
    hasNext: Boolean,
    enabled: Boolean
) {
    val rational = if (width > 0 && height > 0) {
        val raw = Rational(width, height)
        when {
            raw.toFloat() > 2.39f -> Rational(239, 100)
            raw.toFloat() < (1f / 2.39f) -> Rational(100, 239)
            else -> raw
        }
    } else {
        Rational(16, 9)
    }

    val actions = listOf(
        createRemoteAction(
            activity,
            R.drawable.ic_player_previous,
            activity.getString(R.string.player_control_previous),
            PlayerActivity.CONTROL_TYPE_PREV,
            hasPrev
        ),
        createRemoteAction(
            activity,
            if (isPlaying) R.drawable.ic_player_pause else R.drawable.ic_player_play,
            activity.getString(if (isPlaying) R.string.player_control_pause else R.string.player_control_play),
            PlayerActivity.CONTROL_TYPE_PLAY_PAUSE,
            enabled = true
        ),
        createRemoteAction(
            activity,
            R.drawable.ic_player_next,
            activity.getString(R.string.player_control_next),
            PlayerActivity.CONTROL_TYPE_NEXT,
            hasNext
        )
    )

    val params = PictureInPictureParams.Builder()
        .setAspectRatio(rational)
        .setAutoEnterEnabled(enabled)
        .setSeamlessResizeEnabled(true)
        .setActions(actions)
        .build()

    activity.setPictureInPictureParams(params)
}

private fun createRemoteAction(
    context: Context,
    iconRes: Int,
    title: String,
    controlType: Int,
    enabled: Boolean
): RemoteAction {
    val intent = Intent(PlayerActivity.ACTION_PIP_CONTROL).apply {
        putExtra(PlayerActivity.EXTRA_CONTROL_TYPE, controlType)
        `package` = context.packageName
    }
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context, controlType, intent,
        android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
    )
    val icon = Icon.createWithResource(context, iconRes)
    return RemoteAction(icon, title, title, pendingIntent).apply { isEnabled = enabled }
}
