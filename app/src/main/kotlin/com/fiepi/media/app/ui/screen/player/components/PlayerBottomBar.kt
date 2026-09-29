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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerDrawerType
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.app.ui.utils.formatTime
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.PlayerAction
import com.fiepi.media.domain.model.preferences.PlayerLayoutOptions
import com.fiepi.media.domain.model.preferences.PlayerUiOptions
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.model.MediaMetadata
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow


@Composable
fun PlayerBottomBar(
    modifier: Modifier = Modifier,
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onOpenDrawer: (PlayerDrawerType) -> Unit,
    onBack: () -> Unit = {}
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isWideScreen =
        adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    val backdrop = LocalBackdrop.current
    val options = LocalButtonEffectOptions.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Intercept clicks to prevent passing through to gesture layer
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* No-op to block background gestures */ }
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                PlayerCornerActions(
                    modifier = Modifier.weight(1f),
                    actions = state.uiOptions.layoutOptions.bottomLeftActions,
                    state = state,
                    onIntent = onIntent,
                    onOpenDrawer = onOpenDrawer,
                    onBack = onBack,
                    horizontalAlignment = Alignment.Start,
                    verticalAlignment = Alignment.Bottom
                )
                PlayerCornerActions(
                    modifier = Modifier.weight(1f),
                    actions = state.uiOptions.layoutOptions.bottomRightActions,
                    state = state,
                    onIntent = onIntent,
                    onOpenDrawer = onOpenDrawer,
                    onBack = onBack,
                    horizontalAlignment = Alignment.End,
                    verticalAlignment = Alignment.Bottom
                )
            }
        }
        // Time Info & Slider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(8.dp) },
                    effects = {
                        if (options.blur || options.lens) {
                            vibrancy()
                        }
                    },
                    highlight = {
                        Highlight(
                            width = 0.dp
                        )
                    },
                    shadow = { Shadow.Default },
                    innerShadow = { InnerShadow.Default },
                    onDrawSurface = { drawRect(Color.Black.copy(alpha = options.alpha)) }
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PlayerTimeText(
                text = formatTime(
                    millis = state.displayPosition,
                    referenceMillis = state.playback.duration
                )
            )

            PlayerProgressBar(
                modifier = Modifier.weight(1f),
                displayPosition = state.displayPosition,
                bufferedPosition = state.playback.bufferedPosition,
                duration = state.playback.duration,
                seekBarStyle = state.uiOptions.seekBarStyle,
                onIntent = onIntent
            )

            PlayerTimeText(text = formatTime(state.playback.duration))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (isWideScreen) {
                    PlayerCornerActions(
                        modifier = Modifier.fillMaxWidth(),
                        actions = state.uiOptions.layoutOptions.bottomLeftActions,
                        state = state,
                        onIntent = onIntent,
                        onOpenDrawer = onOpenDrawer,
                        onBack = onBack,
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.Bottom
                    )
                }
            }
            PlayerIconButtons(
                state = state,
                isWideScreen = isWideScreen,
                onIntent = onIntent
            )
            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (isWideScreen) {
                    PlayerCornerActions(
                        modifier = Modifier.fillMaxWidth(),
                        actions = state.uiOptions.layoutOptions.bottomRightActions,
                        state = state,
                        onIntent = onIntent,
                        onOpenDrawer = onOpenDrawer,
                        onBack = onBack,
                        horizontalAlignment = Alignment.End,
                        verticalAlignment = Alignment.Bottom
                    )
                }
            }
        }
    }
}


@Composable
private fun PlayerIconButtons(
    modifier: Modifier = Modifier,
    state: PlayerState,
    isWideScreen: Boolean,
    onIntent: (PlayerIntent) -> Unit
) {
    // Playback Control Row
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val controllerButtonSize = 48.dp
        val controllerButtonIconSize = 30.dp
        val hasPlaylistNavigation = state.playback.hasPlaylistNavigation
        val showSeekButtons = isWideScreen || !hasPlaylistNavigation

        if (showSeekButtons) {
            val seekDurationMs = state.uiOptions.seekDurationSeconds * 1000L
            PlayerIconButton(
                onClick = { onIntent(PlayerIntent.SeekBy(-seekDurationMs)) },
                modifier = Modifier.size(controllerButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastRewind,
                    contentDescription = stringResource(
                        R.string.player_control_rewind,
                        state.uiOptions.seekDurationSeconds
                    ),
                    modifier = Modifier.size(controllerButtonIconSize)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
        }

        if (hasPlaylistNavigation) {
            PlayerIconButton(
                onClick = { onIntent(PlayerIntent.PlayPrevious) },
                modifier = Modifier.size(controllerButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.player_control_previous),
                    modifier = Modifier.size(controllerButtonIconSize)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))
        }

        PlayerIconButton(
            onClick = rememberHapticClickHandler {
                onIntent(PlayerIntent.TogglePlay)
            },
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (state.playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.player_control_play_pause),
                modifier = Modifier.size(36.dp)
            )
        }

        if (hasPlaylistNavigation) {
            Spacer(modifier = Modifier.width(16.dp))

            PlayerIconButton(
                onClick = { onIntent(PlayerIntent.PlayNext) },
                modifier = Modifier.size(controllerButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.player_control_next),
                    modifier = Modifier.size(controllerButtonIconSize)
                )
            }
        }

        if (showSeekButtons) {
            val seekDurationMs = state.uiOptions.seekDurationSeconds * 1000L
            Spacer(modifier = Modifier.width(12.dp))

            PlayerIconButton(
                onClick = { onIntent(PlayerIntent.SeekBy(seekDurationMs)) },
                modifier = Modifier.size(controllerButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FastForward,
                    contentDescription = stringResource(
                        R.string.player_control_forward,
                        state.uiOptions.seekDurationSeconds
                    ),
                    modifier = Modifier.size(controllerButtonIconSize)
                )
            }
        }
    }
}

@Composable
private fun PlayerTimeText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontFeatureSettings = "tnum", // Tabular numbers
        ),
        color = Color.White,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayerBottomBarPreview() {
    val mockPlaylist = listOf(
        MediaFile.Video(
            path = "local/video1.mp4",
            name = "Big Buck Bunny",
            sourceType = SourceType.Local,
            id = "1",
            extension = "mp4",
            duration = 3600000L
        ),
        MediaFile.Video(
            path = "local/video2.mp4",
            name = "Elephants Dream",
            sourceType = SourceType.Local,
            id = "2",
            extension = "mp4",
            duration = 1800000L
        )
    )

    AppTheme {
        PlayerBottomBar(
            state = PlayerState(
                playback = MediaPlaybackState(
                    isPlaying = true,
                    duration = 3600000L,
                    currentPosition = 1200000L,
                    mediaMetadata = MediaMetadata(title = "Big Buck Bunny"),
                    playlist = mockPlaylist,
                    currentIndex = 0
                ),
                uiOptions = PlayerUiOptions(
                    layoutOptions = PlayerLayoutOptions(
                        bottomLeftActions = listOf(
                            PlayerAction.Screenshot,
                            PlayerAction.PiP,
                            PlayerAction.Osd,
                            PlayerAction.PlayerSettings
                        ),
                        bottomRightActions = listOf(
                            PlayerAction.Lock,
                            PlayerAction.PlaybackSpeed
                        )
                    ),
                )
            ),
            onIntent = {},
            onOpenDrawer = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, device = Devices.TABLET)
@Composable
private fun PlayerBottomBarPausedPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            PlayerBottomBar(
                state = PlayerState(
                    playback = MediaPlaybackState(
                        isPlaying = false,
                        duration = 3600000L,
                        currentPosition = 600000L,
                        mediaMetadata = MediaMetadata(title = "Big Buck Bunny"),
                        playlist = emptyList()
                    ),
                    uiOptions = PlayerUiOptions(
                        layoutOptions = PlayerLayoutOptions(
                            bottomLeftActions = listOf(
                                PlayerAction.Screenshot,
                                PlayerAction.PiP,
                                PlayerAction.Osd,
                                PlayerAction.PlayerSettings
                            ),
                            bottomRightActions = listOf(
                                PlayerAction.Lock,
                                PlayerAction.PlaybackSpeed
                            )
                        ),
                    )
                ),
                onIntent = {},
                onOpenDrawer = {}
            )
        }
    }
}
