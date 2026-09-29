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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerControlMode
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerNotification
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.screen.player.viewmodel.UIState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.app.ui.utils.formatTime


@Composable
fun PlayerStatusIndicator(
    state: PlayerState,
    modifier: Modifier = Modifier
) {
    val notification = state.ui.activeNotification

    val message = when (notification) {
        is PlayerNotification.SeekForward -> stringResource(
            R.string.player_status_seek_forward,
            notification.seconds
        )

        is PlayerNotification.SeekBackward -> stringResource(
            R.string.player_status_seek_backward,
            notification.seconds
        )

        null -> {
            val scrubbingPosition = state.playback.scrubbingPosition
            when {
                state.ui.controlMode == PlayerControlMode.Screenshot -> {
                    if (state.playback.capabilities.supportsFrameStep) {
                        val current = state.playback.currentFrame
                        val total = state.playback.totalFrames
                        if (total > 0) "$current / $total" else current.toString()
                    } else {
                        val current = formatTime(state.displayPosition, state.playback.duration)
                        val duration = formatTime(state.playback.duration)
                        "$current / $duration"
                    }
                }

                scrubbingPosition != null -> {
                    val scrubbing = formatTime(scrubbingPosition, state.playback.duration)
                    val duration = formatTime(state.playback.duration)
                    "$scrubbing / $duration"
                }

                state.ui.isFastForwarding -> stringResource(
                    R.string.player_status_fast_forwarding,
                    state.playback.playbackSpeed
                )

                else -> null
            }
        }
    }

    val icon = when (notification) {
        is PlayerNotification.SeekForward -> Icons.Rounded.FastForward
        is PlayerNotification.SeekBackward -> Icons.Rounded.FastRewind
        null -> when {
            state.ui.controlMode == PlayerControlMode.Screenshot -> Icons.Rounded.AccessTime
            state.playback.scrubbingPosition != null -> Icons.Rounded.AccessTime
            state.ui.isFastForwarding -> Icons.Rounded.FastForward
            else -> null
        }
    }

    if (message != null && icon != null) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = CircleShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFeatureSettings = "tnum" // Tabular numbers
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun PlayerStatusIndicatorForwardSeekPreview() {
    PlayerStatusIndicatorPreviewWrapper(
        state = PlayerState(
            ui = UIState(activeNotification = PlayerNotification.SeekForward(10))
        )
    )
}

@Preview
@Composable
private fun PlayerStatusIndicatorBackwardSeekPreview() {
    PlayerStatusIndicatorPreviewWrapper(
        state = PlayerState(
            ui = UIState(activeNotification = PlayerNotification.SeekBackward(10))
        )
    )
}

@Preview
@Composable
private fun PlayerStatusIndicatorScrubbingPreview() {
    PlayerStatusIndicatorPreviewWrapper(
        state = PlayerState(
            playback = com.fiepi.media.domain.player.model.MediaPlaybackState(
                scrubbingPosition = 45000L,
                duration = 120000L
            )
        )
    )
}

@Preview
@Composable
private fun PlayerStatusIndicatorFastForwardPreview() {
    PlayerStatusIndicatorPreviewWrapper(
        state = PlayerState(
            playback = com.fiepi.media.domain.player.model.MediaPlaybackState(playbackSpeed = 3f),
            ui = UIState(isFastForwarding = true)
        )
    )
}

@Composable
private fun PlayerStatusIndicatorPreviewWrapper(state: PlayerState) {
    AppTheme {
        Box(
            modifier = Modifier
                .size(300.dp, 100.dp)
                .background(Color.Gray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            PlayerStatusIndicator(state = state)
        }
    }
}
