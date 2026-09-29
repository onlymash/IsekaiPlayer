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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.hooks.LocalAppHaptics
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerControlMode
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ScreenshotActionBar(
    state: PlayerState,
    modifier: Modifier = Modifier,
    onIntent: (PlayerIntent) -> Unit
) {
    val canStepBackward = if (state.playback.capabilities.supportsFrameStep) {
        state.playback.totalFrames > 0 || state.playback.currentFrame > 0
    } else {
        state.playback.currentPosition >= 33L
    }
    val canStepForward = if (state.playback.capabilities.supportsFrameStep) {
        state.playback.totalFrames <= 0 || state.playback.currentFrame < state.playback.totalFrames - 1
    } else {
        state.playback.currentPosition < state.playback.duration - 200L
    }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(bottom = 24.dp)
            .playerButtonSurface(interactionSource, RoundedCornerShape(28.dp), scaleTarget = 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* Block clicks */ }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Step Backward
                ScreenshotButton(
                    onClick = { onIntent(PlayerIntent.StepFrame(forward = false)) },
                    enableRepeat = true,
                    useTickHaptic = true,
                    enabled = canStepBackward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                        contentDescription = stringResource(R.string.player_control_frame_step_backward),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Take Screenshot
                ScreenshotButton(
                    onClick = { onIntent(PlayerIntent.TakeScreenshot) },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = stringResource(R.string.player_control_screenshot),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Step Forward
                ScreenshotButton(
                    onClick = { onIntent(PlayerIntent.StepFrame(forward = true)) },
                    enableRepeat = true,
                    useTickHaptic = true,
                    enabled = canStepForward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                        contentDescription = stringResource(R.string.player_control_frame_step_forward),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Close Button
            ScreenshotButton(
                onClick = { onIntent(PlayerIntent.SetControlMode(PlayerControlMode.Normal)) },
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.common_close),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ScreenshotButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enableRepeat: Boolean = false,
    useTickHaptic: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val appHaptics = LocalAppHaptics.current

    if (enabled && enableRepeat && isPressed) {
        LaunchedEffect(interactionSource) {
            delay(400.milliseconds) // Long-press threshold time
            while (true) {
                // Use weaker FrequentTick during long-press repeating
                appHaptics.performFrequentTick()
                onClick()
                delay(150.milliseconds) // Step interval
            }
        }
    }

    val contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.38f)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .size(48.dp)
                .playerButtonSurface(interactionSource, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = {
                        // Single click: use Tick or standard Click based on setting
                        if (useTickHaptic) {
                            appHaptics.performTick()
                        } else {
                            appHaptics.performClick()
                        }
                        onClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ScreenshotActionBarPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            ScreenshotActionBar(
                state = PlayerState(),
                onIntent = {}
            )
        }
    }
}
