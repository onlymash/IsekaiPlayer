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

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.SeekBarStyle

/**
 * Player progress bar supporting display of current playback position and preloaded/buffered progress.
 */
@Composable
fun PlayerProgressBar(
    modifier: Modifier = Modifier,
    displayPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    seekBarStyle: SeekBarStyle,
    onScrubbing: (Long) -> Unit,
    onScrubbingFinished: (Long) -> Unit,
    onSeekTo: (Long) -> Unit
) {
    val sliderInteractionSource = remember { MutableInteractionSource() }
    var sliderWidth by remember { mutableIntStateOf(0) }

    val safeDuration = maxOf(duration, 1L)
    val floatDuration = safeDuration.toFloat()
    val floatPosition = displayPosition.toFloat().coerceIn(0f, floatDuration)

    val currentFraction = (floatPosition / floatDuration).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedPosition.toFloat() / floatDuration).coerceIn(0f, 1f)
    val coercedBufferedFraction = maxOf(bufferedFraction, currentFraction)

    val sliderState = remember(safeDuration) {
        SliderState(
            value = floatPosition,
            trackRange = 0f..floatDuration
        )
    }

    // Sync external displayPosition to sliderState when not user dragging
    LaunchedEffect(displayPosition, safeDuration) {
        if (!sliderState.isDragging) {
            sliderState.value = floatPosition
        }
    }

    Slider(
        state = sliderState,
        onValueChange = { newValue ->
            onScrubbing(newValue.toLong())
            if (sliderState.isDragging) {
                sliderState.value = newValue
            }
        },
        onValueChangeFinished = { onScrubbingFinished(sliderState.value.toLong()) },
        interactionSource = sliderInteractionSource,
        track = { stateTrack ->
            val range = stateTrack.trackRange.endInclusive - stateTrack.trackRange.start
            val activeFraction = if (range > 0f) {
                ((stateTrack.value - stateTrack.trackRange.start) / range).coerceIn(0f, 1f)
            } else 0f
            val preloadFraction = maxOf(coercedBufferedFraction, activeFraction)

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (seekBarStyle == SeekBarStyle.Wavy) {
                    // Preloaded / Buffered Progress (Track Background Layer, semi-transparent white)
                    LinearWavyProgressIndicator(
                        progress = { preloadFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.6f),
                        trackColor = Color.White.copy(alpha = 0.4f)
                    )
                    // Active Playback Progress (Top Layer, solid white)
                    LinearWavyProgressIndicator(
                        progress = { activeFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Color.Transparent
                    )
                } else {
                    // Preloaded / Buffered Progress (Track Background Layer, semi-transparent white)
                    LinearProgressIndicator(
                        progress = { preloadFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.6f),
                        trackColor = Color.White.copy(alpha = 0.4f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 2.dp,
                        drawStopIndicator = {}
                    )
                    // Active Playback Progress (Top Layer, solid white)
                    LinearProgressIndicator(
                        progress = { activeFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Color.Transparent,
                        strokeCap = StrokeCap.Round,
                        gapSize = 2.dp,
                        drawStopIndicator = {}
                    )
                }
            }
        },
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = sliderInteractionSource,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                thumbSize = DpSize(16.dp, 16.dp)
            )
        },
        modifier = modifier
            .height(48.dp)
            .onSizeChanged { sliderWidth = it.width }
            .pointerInput(safeDuration) {
                // Intercept Down event to achieve instant seeking on press
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (sliderWidth > 0) {
                        val ratio = (down.position.x / sliderWidth).coerceIn(0f, 1f)
                        val targetMs = (ratio * safeDuration).toLong()
                        onSeekTo(targetMs)
                    }
                }
            }
    )
}

/**
 * Overload accepting PlayerIntent for convenience
 */
@Composable
fun PlayerProgressBar(
    modifier: Modifier = Modifier,
    displayPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    seekBarStyle: SeekBarStyle,
    onIntent: (PlayerIntent) -> Unit
) {
    PlayerProgressBar(
        modifier = modifier,
        displayPosition = displayPosition,
        bufferedPosition = bufferedPosition,
        duration = duration,
        seekBarStyle = seekBarStyle,
        onScrubbing = { onIntent(PlayerIntent.OnScrubbing(it)) },
        onScrubbingFinished = { onIntent(PlayerIntent.OnScrubbingFinished(it)) },
        onSeekTo = { onIntent(PlayerIntent.SeekTo(it)) }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayerProgressBarStraightPreview() {
    AppTheme {
        PlayerProgressBar(
            modifier = Modifier.fillMaxWidth(),
            displayPosition = 300000L,
            bufferedPosition = 600000L,
            duration = 1000000L,
            seekBarStyle = SeekBarStyle.Straight,
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayerProgressBarWavyPreview() {
    AppTheme {
        PlayerProgressBar(
            modifier = Modifier.fillMaxWidth(),
            displayPosition = 300000L,
            bufferedPosition = 600000L,
            duration = 1000000L,
            seekBarStyle = SeekBarStyle.Wavy,
            onIntent = {}
        )
    }
}
