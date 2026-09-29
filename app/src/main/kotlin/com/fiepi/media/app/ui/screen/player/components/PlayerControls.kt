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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerControlMode
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerDrawerType
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.screen.player.viewmodel.UIState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.ThemeMode
import com.fiepi.media.domain.model.preferences.ThemeOptions
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.model.MediaMetadata


@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onBack: () -> Unit,
    onOpenDrawer: (PlayerDrawerType) -> Unit
) {

    val nonSystemBarsInsets = WindowInsets.displayCutout
    val hasSystemBarsInsets = if (state.uiOptions.alwaysFullScreen) {
        WindowInsets.displayCutout
    } else {
        WindowInsets.systemBars.union(WindowInsets.displayCutout)
    }

    Box(
        modifier = modifier
    ) {
        // Fullscreen gesture capture layer (placed at the bottom)
        PlayerGestureLayer(
            state = state,
            onIntent = onIntent
        )

        // UI control bar layer
        AnimatedVisibility(
            visible = state.ui.isControlsVisible && state.ui.controlMode == PlayerControlMode.Normal && !state.ui.isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                PlayerTopBar(
                    modifier = Modifier.align(Alignment.TopCenter),
                    state = state,
                    onIntent = onIntent,
                    onBack = onBack,
                    onOpenDrawer = onOpenDrawer
                )

                PlayerBottomBar(
                    state = state,
                    onIntent = onIntent,
                    onOpenDrawer = onOpenDrawer,
                    onBack = onBack,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(hasSystemBarsInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                )
            }
        }

        // 2.1 Screenshot Action Bar
        AnimatedVisibility(
            visible = state.ui.controlMode == PlayerControlMode.Screenshot,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ScreenshotActionBar(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.windowInsetsPadding(nonSystemBarsInsets.only(WindowInsetsSides.Bottom))
            )
        }

        // 2.3 Playback Speed Action Bar
        AnimatedVisibility(
            visible = state.ui.controlMode == PlayerControlMode.PlaybackSpeed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlaybackSpeedActionBar(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.windowInsetsPadding(nonSystemBarsInsets.only(WindowInsetsSides.Bottom))
            )
        }

        // 2.4 Audio Delay Action Bar
        AnimatedVisibility(
            visible = state.ui.controlMode == PlayerControlMode.AudioDelay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AudioDelayActionBar(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.windowInsetsPadding(nonSystemBarsInsets.only(WindowInsetsSides.Bottom))
            )
        }

        // 2.5 Subtitle Delay Action Bar
        AnimatedVisibility(
            visible = state.ui.controlMode == PlayerControlMode.SubtitleDelay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SubtitleDelayActionBar(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.windowInsetsPadding(nonSystemBarsInsets.only(WindowInsetsSides.Bottom))
            )
        }

        // 2.2 Unlock Slider
        AnimatedVisibility(
            visible = state.ui.isControlsVisible && state.ui.isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PlayerUnlockSlider(
                onUnlock = { onIntent(PlayerIntent.ToggleLock) },
                modifier = Modifier.padding(bottom = 56.dp)
            )
        }

        // Quick Overlays (Brightness/Volume) - Shown independent of control visibility
        PlayerQuickControls(
            state = state,
            onSystemVolumeChange = { onIntent(PlayerIntent.SetSystemVolume(it)) },
            onPlayerVolumeChange = { onIntent(PlayerIntent.SetGainVolume(it)) },
            onBrightnessChange = { onIntent(PlayerIntent.SetBrightness(it)) },
            modifier = Modifier.windowInsetsPadding(hasSystemBarsInsets.only(WindowInsetsSides.Horizontal))
        )

        // Status Indicators (Seek/Speed) - Shown independent of control visibility
        PlayerStatusIndicator(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(nonSystemBarsInsets.only(WindowInsetsSides.Top))
                .padding(top = 120.dp)
        )
    }
}


@Preview(
    name = "Compact - Mobile",
    showBackground = true,
    backgroundColor = 0xff333333,
    widthDp = 400,
    heightDp = 800
)
@Composable
fun PlayerScreenCompactPreview() {
    PlayerPreviewContent()
}

@Preview(
    name = "Expanded - Tablet",
    showBackground = true,
    backgroundColor = 0xff333333,
    widthDp = 900,
    heightDp = 600
)
@Composable
fun PlayerScreenExpandedPreview() {
    PlayerPreviewContent()
}

@Composable
private fun PlayerPreviewContent() {
    AppTheme(
        themeOptions = ThemeOptions(
            themeMode = ThemeMode.Dark,
            highContrastDarkThemeEnabled = true
        )
    ) {
        // Mock a playlist with multiple items to see next/prev buttons
        val mockVideo = MediaFile.Video(
            path = "", name = "Movie", extension = "mkv",
            sourceType = SourceType.Local, id = ""
        )

        PlayerControls(
            state = PlayerState(
                playback = com.fiepi.media.domain.player.model.MediaPlaybackState(
                    mediaMetadata = MediaMetadata(title = "Awesome Movie.mkv"),
                    playlist = listOf(mockVideo, mockVideo, mockVideo),
                    currentIndex = 1, // Middle item to enable both next and prev
                    isPlaying = false,
                    currentPosition = 45000,
                    duration = 120000,
                    isBuffering = false
                ),
                ui = UIState(
                    isControlsVisible = true,
                    isFastForwarding = true
                )
            ),
            onIntent = {},
            onBack = {},
            onOpenDrawer = {}
        )
    }
}
