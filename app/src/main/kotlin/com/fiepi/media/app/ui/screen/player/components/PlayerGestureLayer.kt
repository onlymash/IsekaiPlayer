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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.hooks.LocalAppHaptics
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerControlMode
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState


@Composable
fun PlayerGestureLayer(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val appHaptics = LocalAppHaptics.current

    val gestureOptions = state.uiOptions.gestureOptions

    var width by remember { mutableIntStateOf(0) }
    val viewConfiguration = LocalViewConfiguration.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { 8.dp.toPx() }

    // Unified physical drag distance required for full-range adjustment (480dp ~ half screen height)
    // Guarantees visually consistent ratio changes for volume and brightness when dragging same distance
    val fullSweepPx = with(density) { 480.dp.toPx() }

    // Capture latest business state to prevent stale data in gesture callbacks
    val currentDisplayPosition by rememberUpdatedState(state.displayPosition)
    val currentDuration by rememberUpdatedState(state.playback.duration)
    val currentBrightness by rememberUpdatedState(state.ui.brightness)
    val currentSystemVolume by rememberUpdatedState(state.playback.systemVolume)
    val currentSystemMaxVolume by rememberUpdatedState(
        state.playback.systemMaxVolume.coerceAtLeast(
            1
        )
    )

    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .onSizeChanged { width = it.width }
            .pointerInput(width, state.ui.isLocked, state.ui.controlMode, gestureOptions) {
                if (width <= 0 || state.ui.isLocked || state.ui.controlMode != PlayerControlMode.Normal) return@pointerInput
                var dragTotalX = 0f
                var dragTotalY = 0f
                var isHorizontalDrag = false
                var isVerticalDrag = false
                var dragStarted = false
                var dragAnchorTime = 0L
                var lastTargetTime = 0L
                var detectionDragOffset = 0f
                var dragAnchorVolume = 0 // Records starting system volume of swipe
                var dragAnchorBrightness = 0f // Records starting brightness position of swipe

                detectDragGestures(
                    onDragStart = { _ ->
                        dragTotalX = 0f
                        dragTotalY = 0f
                        isHorizontalDrag = false
                        isVerticalDrag = false
                        dragStarted = false
                        dragAnchorTime = currentDisplayPosition
                        lastTargetTime = dragAnchorTime
                        detectionDragOffset = 0f
                        dragAnchorVolume = currentSystemVolume
                        dragAnchorBrightness =
                            if (currentBrightness < 0f) 0.5f else currentBrightness
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragTotalX += dragAmount.x
                        dragTotalY += dragAmount.y

                        if (!dragStarted) {
                            val absX = kotlin.math.abs(dragTotalX)
                            val absY = kotlin.math.abs(dragTotalY)
                            val slopeThreshold = 2.75f // ~20 degree angle (1/tan(20°))

                            if (absX > thresholdPx && absX > absY * slopeThreshold) {
                                // Enter horizontal drag detection zone
                                isHorizontalDrag = true
                                dragStarted = true
                                detectionDragOffset = dragTotalX
                                if (gestureOptions.horizontalDragEnabled) {
                                    onIntent(PlayerIntent.OnScrubbing(dragAnchorTime))
                                }
                            } else if (absY > thresholdPx && absY > absX * slopeThreshold) {
                                // Enter vertical drag detection zone
                                isVerticalDrag = true
                                dragStarted = true
                            }
                        }

                        if (isHorizontalDrag && currentDuration > 0) {
                            if (gestureOptions.horizontalDragEnabled) {
                                // Subtract detection threshold to achieve smooth perception
                                val effectiveDragX = dragTotalX - detectionDragOffset
                                val sensitivity = 1.0f
                                val deltaMs =
                                    (effectiveDragX / width * currentDuration.toFloat() * sensitivity).toLong()
                                lastTargetTime =
                                    (dragAnchorTime + deltaMs).coerceIn(0, currentDuration)
                                onIntent(PlayerIntent.OnScrubbing(lastTargetTime))
                            }
                        } else if (isVerticalDrag) {
                            val isLeftSide = change.previousPosition.x < width * 0.5f
                            val isVolumeControl = if (gestureOptions.swapVolumeBrightness) !isLeftSide else isLeftSide

                            if (isVolumeControl) {
                                // Volume adjustment
                                if (gestureOptions.verticalDragVolumeEnabled) {
                                    val deltaVol =
                                        ((dragTotalY / -fullSweepPx) * currentSystemMaxVolume).toInt()
                                    val newSystemVol = (dragAnchorVolume + deltaVol).coerceIn(
                                        0,
                                        currentSystemMaxVolume
                                    )
                                    onIntent(PlayerIntent.SetSystemVolume(newSystemVol))
                                }
                            } else {
                                // Brightness adjustment (0..1f)
                                if (gestureOptions.verticalDragBrightnessEnabled) {
                                    val newBrightness =
                                        (dragAnchorBrightness + (dragTotalY / -fullSweepPx))
                                            .coerceIn(0f, 1f)
                                    onIntent(PlayerIntent.SetBrightness(newBrightness))
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        if (isHorizontalDrag && gestureOptions.horizontalDragEnabled) {
                            onIntent(PlayerIntent.OnScrubbingFinished(lastTargetTime))
                        }
                        dragStarted = false
                    }
                )
            }
            .pointerInput(width, state.ui.isLocked, state.ui.controlMode, gestureOptions) {
                if (width <= 0 || state.ui.controlMode != PlayerControlMode.Normal) return@pointerInput
                if (state.ui.isLocked) {
                    detectTapGestures(onTap = { onIntent(PlayerIntent.ToggleControls) })
                } else {
                    detectTapGestures(
                        onTap = { onIntent(PlayerIntent.ToggleControls) },
                        onDoubleTap = { offset ->
                            val x = offset.x
                            val isSeek = x < width * 0.33f || x > width * 0.66f
                            if (isSeek) {
                                if (gestureOptions.doubleTapSeekEnabled) {
                                    val seekDurationMs =
                                        gestureOptions.doubleTapSeekDurationSeconds * 1000L
                                    if (x < width * 0.33f) {
                                        onIntent(PlayerIntent.SeekBy(-seekDurationMs))
                                    } else {
                                        onIntent(PlayerIntent.SeekBy(seekDurationMs))
                                    }
                                }
                            } else {
                                if (gestureOptions.doubleTapPlayPauseEnabled) {
                                    appHaptics.performClick()
                                    onIntent(PlayerIntent.TogglePlay)
                                }
                            }
                        }
                    )
                }
            }
            .pointerInput(width, state.ui.isLocked, state.ui.controlMode, gestureOptions) {
                if (width <= 0 || state.ui.isLocked || state.ui.controlMode != PlayerControlMode.Normal) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()

                    // 400ms long-press detection
                    val result = withTimeoutOrNull(400) {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()

                            // Critical: if movement exceeds slop, treat as drag and cancel long press
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                return@withTimeoutOrNull false
                            }

                            if (event.changes.any { it.changedToUp() }) {
                                return@withTimeoutOrNull false
                            }
                        }
                    }

                    if (result == null && gestureOptions.longPressEnabled) {
                        // Trigger long press logic: use configured playback speed
                        onIntent(
                            PlayerIntent.SetPlaybackRate(
                                speed = gestureOptions.longPressSpeed,
                                isTemporary = true
                            )
                        )

                        // Wait until user completely releases finger
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.all { !it.pressed }) {
                                onIntent(
                                    PlayerIntent.SetPlaybackRate(
                                        speed = state.uiOptions.playbackSpeed,
                                        isTemporary = false
                                    )
                                )
                                break
                            }
                        }
                    }
                }
            }
    )
}
