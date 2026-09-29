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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ArtTrack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.DynamicForm
import androidx.compose.material.icons.outlined.FilterCenterFocus
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerControlMode
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerDrawerType
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.domain.model.preferences.LoopMode
import com.fiepi.media.domain.model.preferences.PlayerAction
import com.fiepi.media.domain.model.preferences.VideoScaleMode


@Composable
fun PlayerCornerActions(
    modifier: Modifier = Modifier,
    actions: List<PlayerAction>,
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onOpenDrawer: (PlayerDrawerType) -> Unit,
    onBack: () -> Unit = {},
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    space: Dp = 8.dp,
) {
    // Filter out None actions and ensure non-empty list for FlowRow
    val displayActions = remember(actions) {
        actions.filter { it != PlayerAction.None }
    }

    val isRightAligned = horizontalAlignment == Alignment.End
    val isBottomAligned = verticalAlignment == Alignment.Bottom

    FlowRow(
        modifier = modifier.graphicsLayer {
            scaleX = if (isRightAligned) -1f else 1f
            scaleY = if (isBottomAligned) -1f else 1f
        },
        horizontalArrangement = Arrangement.spacedBy(space, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(space, Alignment.Top),
    ) {
        displayActions.forEach { action ->
            PlayerActionButton(
                modifier = Modifier.graphicsLayer {
                    // Double flip to keep content oriented correctly
                    scaleX = if (isRightAligned) -1f else 1f
                    scaleY = if (isBottomAligned) -1f else 1f
                },
                action = action,
                state = state,
                onIntent = onIntent,
                onOpenDrawer = onOpenDrawer,
                onBack = onBack
            )
        }
    }
}

@Composable
fun PlayerActionButton(
    modifier: Modifier = Modifier,
    action: PlayerAction,
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onOpenDrawer: (PlayerDrawerType) -> Unit,
    onBack: () -> Unit = {}
) {
    Box(modifier = modifier) {
        when (action) {
            PlayerAction.None -> {}
            PlayerAction.Screenshot -> {
                PlayerIconButton(onClick = { onIntent(PlayerIntent.SetControlMode(PlayerControlMode.Screenshot)) }) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = stringResource(R.string.player_control_screenshot)
                    )
                }
            }

            PlayerAction.PiP -> {
                PlayerIconButton(onClick = { onIntent(PlayerIntent.TogglePiP) }) {
                    Icon(
                        imageVector = Icons.Outlined.PictureInPicture,
                        contentDescription = stringResource(R.string.settings_player_enable_pip)
                    )
                }
            }

            PlayerAction.BackgroundAudio -> {
                PlayerIconButton(onClick = { onIntent(PlayerIntent.EnterBackgroundAudio) }) {
                    Icon(
                        imageVector = Icons.Outlined.Headset,
                        contentDescription = stringResource(R.string.settings_audio_enable_background_audio)
                    )
                }
            }

            PlayerAction.Osd -> {
                PlayerIconButton(
                    enabled = state.playback.capabilities.supportsOsdStats,
                    onClick = { onOpenDrawer(PlayerDrawerType.Osd) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.player_control_osd)
                    )
                }
            }

            PlayerAction.PlayerSettings -> {
                PlayerIconButton(onClick = { onOpenDrawer(PlayerDrawerType.Player) }) {
                    Icon(
                        imageVector = Icons.Outlined.DisplaySettings,
                        contentDescription = stringResource(R.string.player_settings_player)
                    )
                }
            }

            PlayerAction.Playlist -> {
                PlayerIconButton(onClick = { onOpenDrawer(PlayerDrawerType.Playlist) }) {
                    Icon(
                        imageVector = Icons.Outlined.VideoLibrary,
                        contentDescription = stringResource(R.string.player_drawer_playlist_button)
                    )
                }
            }

            PlayerAction.Tracks -> {
                PlayerIconButton(onClick = { onOpenDrawer(PlayerDrawerType.Tracks) }) {
                    Icon(
                        imageVector = Icons.Outlined.ArtTrack,
                        contentDescription = stringResource(R.string.player_drawer_tracks_button)
                    )
                }
            }

            PlayerAction.LoopModeToggle -> {
                val loopMode = state.playbackOptions.loopMode
                val (baseIcon, isShuffle) = when (loopMode) {
                    LoopMode.None -> Icons.Outlined.Repeat to false
                    LoopMode.RepeatCurrent -> Icons.Outlined.RepeatOne to false
                    LoopMode.Sequential -> Icons.AutoMirrored.Outlined.PlaylistPlay to false
                    LoopMode.RepeatList -> Icons.Outlined.Repeat to false
                    LoopMode.Shuffle -> Icons.AutoMirrored.Outlined.PlaylistPlay to true
                    LoopMode.ShuffleRepeat -> Icons.Outlined.Repeat to true
                }

                PlayerIconButton(onClick = { onIntent(PlayerIntent.ToggleLoopMode) }) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = baseIcon,
                            tint = if (loopMode == LoopMode.None) Color.White.copy(alpha = 0.5f) else Color.White,
                            contentDescription = stringResource(R.string.settings_player_loop_mode),
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isShuffle) {
                            Icon(
                                imageVector = Icons.Outlined.Shuffle,
                                tint = Color.White,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(11.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 3.dp, y = (-3).dp)
                            )
                        }
                    }
                }
            }

            PlayerAction.ScaleModeToggle -> {
                val scaleMode = state.uiOptions.videoScaleMode
                PlayerIconButton(onClick = {
                    val nextMode = when (scaleMode) {
                        VideoScaleMode.Fit -> VideoScaleMode.Fill
                        VideoScaleMode.Fill -> VideoScaleMode.Original
                        VideoScaleMode.Original -> VideoScaleMode.Fit
                    }
                    onIntent(PlayerIntent.SetVideoScaleMode(nextMode))
                }) {
                    Icon(
                        imageVector = when (scaleMode) {
                            VideoScaleMode.Fit -> Icons.Outlined.AspectRatio
                            VideoScaleMode.Fill -> Icons.Outlined.Fullscreen
                            VideoScaleMode.Original -> Icons.Outlined.FilterCenterFocus
                        },
                        contentDescription = stringResource(
                            when (scaleMode) {
                                VideoScaleMode.Fit -> R.string.settings_player_scale_mode_fit
                                VideoScaleMode.Fill -> R.string.settings_player_scale_mode_fill
                                VideoScaleMode.Original -> R.string.settings_player_scale_mode_original
                            }
                        )
                    )
                }
            }

            PlayerAction.Lock -> {
                PlayerIconButton(onClick = { onIntent(PlayerIntent.ToggleLock) }) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.player_control_lock)
                    )
                }
            }

            PlayerAction.PlaybackSpeed -> {
                PlayerIconButton(onClick = { onIntent(PlayerIntent.SetControlMode(PlayerControlMode.PlaybackSpeed)) }) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = stringResource(R.string.player_control_playback_speed)
                    )
                }
            }

            PlayerAction.Back -> {
                PlayerIconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
            }

            PlayerAction.DecoderSettings -> {
                val decoderText = when (state.playback.hwdec) {
                    "no" -> "SW"
                    "mediacodec" -> "HW+"
                    "mediacodec-copy" -> "HW"
                    null -> ""
                    else -> "HW+"
                }
                if (decoderText.isNotEmpty()) {
                    PlayerButton(
                        onClick = { onOpenDrawer(PlayerDrawerType.Decoder) },
                    ) {
                        Text(
                            text = decoderText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                } else {
                    PlayerIconButton(onClick = { onOpenDrawer(PlayerDrawerType.Decoder) }) {
                        Icon(
                            imageVector = Icons.Outlined.DynamicForm,
                            contentDescription = stringResource(R.string.player_settings_decoder)
                        )
                    }
                }
            }

            PlayerAction.AudioSettings -> {
                PlayerIconButton(onClick = { onOpenDrawer(PlayerDrawerType.Audio) }) {
                    Icon(
                        imageVector = Icons.Outlined.Audiotrack,
                        contentDescription = stringResource(R.string.player_settings_audio)
                    )
                }
            }

            PlayerAction.SubtitleSettings -> {
                PlayerIconButton(onClick = { onOpenDrawer(PlayerDrawerType.Subtitle) }) {
                    Icon(
                        imageVector = Icons.Outlined.Subtitles,
                        contentDescription = stringResource(R.string.player_settings_subtitle)
                    )
                }
            }

            PlayerAction.GestureSettings -> {
                PlayerIconButton(onClick = { onOpenDrawer(PlayerDrawerType.Gestures) }) {
                    Icon(
                        imageVector = Icons.Outlined.Gesture,
                        contentDescription = stringResource(R.string.player_settings_gestures)
                    )
                }
            }

            PlayerAction.AudioDelay -> {
                PlayerIconButton(
                    enabled = state.playback.capabilities.supportsAudioDelay,
                    onClick = { onIntent(PlayerIntent.SetControlMode(PlayerControlMode.AudioDelay)) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AvTimer,
                        contentDescription = stringResource(R.string.player_audio_delay)
                    )
                }
            }

            PlayerAction.SubtitleDelay -> {
                PlayerIconButton(
                    enabled = state.playback.capabilities.supportsSubtitleDelay,
                    onClick = { onIntent(PlayerIntent.SetControlMode(PlayerControlMode.SubtitleDelay)) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = stringResource(R.string.player_subtitle_delay)
                    )
                }
            }
        }
    }
}
