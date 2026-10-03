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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.screen.player.viewmodel.QuickBarMode
import com.fiepi.media.app.ui.screen.player.viewmodel.UIState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.ThemeMode
import com.fiepi.media.domain.model.preferences.ThemeOptions
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.player.model.PlayerVolumeState
import kotlin.math.roundToInt

@Composable
fun PlayerQuickControls(
    modifier: Modifier = Modifier,
    state: PlayerState,
    onBrightnessChange: (Float) -> Unit,
    onSystemVolumeChange: (Int) -> Unit,
    onPlayerVolumeChange: (Int) -> Unit
) {
    val swapVolumeBrightness = state.uiOptions.gestureOptions.swapVolumeBrightness

    // The control bar is shown on the OPPOSITE side of the gesture swipe area to avoid finger obstruction
    val brightnessAlignment = if (swapVolumeBrightness) Alignment.CenterEnd else Alignment.CenterStart
    val brightnessPadding = if (swapVolumeBrightness) Modifier.padding(end = 32.dp) else Modifier.padding(start = 32.dp)

    val volumeAlignment = if (swapVolumeBrightness) Alignment.CenterStart else Alignment.CenterEnd
    val volumePadding = if (swapVolumeBrightness) Modifier.padding(start = 32.dp) else Modifier.padding(end = 32.dp)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.ui.quickBarMode == QuickBarMode.Brightness,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(brightnessAlignment)
                .then(brightnessPadding)
        ) {
            val brightnessVal = if (state.ui.brightness < 0f) 0.5f else state.ui.brightness
            QuickControlBar(
                value = brightnessVal,
                onValueChange = onBrightnessChange,
                valueRange = 0f..1f,
                icon = when {
                    state.ui.brightness < 0f -> Icons.Rounded.BrightnessAuto
                    state.ui.brightness < 0.3f -> Icons.Rounded.BrightnessLow
                    state.ui.brightness < 0.7f -> Icons.Rounded.BrightnessMedium
                    else -> Icons.Rounded.BrightnessHigh
                },
                label = stringResource(R.string.player_control_brightness),
                displayText = "${(brightnessVal * 100).toInt()}%"
            )
        }

        AnimatedVisibility(
            visible = state.ui.quickBarMode == QuickBarMode.Volume,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(volumeAlignment)
                .then(volumePadding)
        ) {
            val systemVol = state.playback.systemVolume
            val systemMaxVol = state.playback.systemMaxVolume.coerceAtLeast(1)
            val playerVol = state.playback.gainVolume
            val maxPlayerVol = state.playback.capabilities.maxGainVolume.coerceAtLeast(100)

            val isVolumeOnLeft = volumeAlignment == Alignment.CenterStart

            val playerVolumeBar = @Composable {
                // Player Volume Bar ("播放器音量" - Equalizer/GraphicEq Icon)
                QuickControlBar(
                    value = playerVol.toFloat(),
                    onValueChange = { onPlayerVolumeChange(it.roundToInt()) },
                    valueRange = 0f..maxPlayerVol.toFloat(),
                    icon = if (playerVol <= 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.Rounded.GraphicEq,
                    label = stringResource(R.string.player_control_player_volume),
                    displayText = "$playerVol%",
                    isOverBoost = playerVol > 100
                )
            }

            val systemVolumeBar = @Composable {
                // System Volume Bar (Speaker Icon)
                QuickControlBar(
                    value = systemVol.toFloat(),
                    onValueChange = { onSystemVolumeChange(it.roundToInt()) },
                    valueRange = 0f..systemMaxVol.toFloat(),
                    icon = if (systemVol <= 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                    label = stringResource(R.string.player_control_system_volume),
                    displayText = "$systemVol"
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isVolumeOnLeft) {
                    // System Volume Bar is closest to left screen edge
                    systemVolumeBar()
                    playerVolumeBar()
                } else {
                    // System Volume Bar is closest to right screen edge
                    playerVolumeBar()
                    systemVolumeBar()
                }
            }
        }
    }
}

@Composable
private fun QuickControlBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    icon: ImageVector,
    label: String,
    displayText: String,
    isOverBoost: Boolean = false
) {
    val sliderState = remember(valueRange) {
        SliderState(
            value = value,
            trackRange = valueRange
        )
    }

    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor = if (isOverBoost) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    }

    LaunchedEffect(value) {
        if (!sliderState.isDragging) {
            sliderState.value = value.coerceIn(valueRange.start, valueRange.endInclusive)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block background click */ }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
            .padding(horizontal = 4.dp, vertical = 12.dp)
    ) {
        val colors = if (isOverBoost) {
            SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.tertiary,
                inactiveTrackColor = MaterialTheme.colorScheme.onTertiary,
                thumbColor = MaterialTheme.colorScheme.tertiary
            )
        } else {
            SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onPrimary,
                thumbColor = MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isOverBoost) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isOverBoost) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        VerticalSlider(
            state = sliderState,
            onValueChange = { newValue ->
                onValueChange(newValue)
                if (sliderState.isDragging) {
                    sliderState.value = newValue
                }
            },
            interactionSource = interactionSource,
            colors = colors,
            track = { stateTrack ->
                SliderDefaults.Track(
                    sliderState = stateTrack,
                    modifier = Modifier.width(32.dp),
                    trackCornerSize = 12.dp,
                    drawTick = { _, _ -> },
                    colors = colors
                )
            },
            topToBottom = false,
            modifier = Modifier.height(160.dp),
        )

        Text(
            text = displayText,
            style = MaterialTheme.typography.labelLarge,
            color = if (isOverBoost) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(
    name = "Player Quick Controls Light",
    showBackground = true,
    backgroundColor = 0xFFAAAAAA,
    device = Devices.PIXEL_2
)
@Composable
fun PlayerQuickControlsLightPreview() {
    AppTheme(
        themeOptions = ThemeOptions(
            themeMode = ThemeMode.Light,
            highContrastDarkThemeEnabled = false
        )
    ) {
        PlayerQuickControlsPreviewContent(
            quickBarMode = QuickBarMode.Volume
        )
    }
}

@Preview(
    name = "Player Quick Controls Dark",
    showBackground = true,
    backgroundColor = 0xFF202020,
    device = Devices.PIXEL
)
@Composable
fun PlayerQuickControlsDarkPreview() {
    AppTheme(
        themeOptions = ThemeOptions(
            themeMode = ThemeMode.Dark,
            highContrastDarkThemeEnabled = false
        )
    ) {
        PlayerQuickControlsPreviewContent(
            quickBarMode = QuickBarMode.Brightness
        )
    }
}

@Composable
private fun PlayerQuickControlsPreviewContent(
    quickBarMode: QuickBarMode
) {
    val mockState = PlayerState(
        ui = UIState(
            quickBarMode = quickBarMode,
            brightness = 0.65f
        ),
        playback = MediaPlaybackState(
            volumeState = PlayerVolumeState(
                systemVolume = 5,
                systemMaxVolume = 15,
                gainVolume = 150,
                maxGainVolume = 200
            )
        )
    )
    PlayerQuickControls(
        state = mockState,
        onBrightnessChange = {},
        onSystemVolumeChange = {},
        onPlayerVolumeChange = {}
    )
}
