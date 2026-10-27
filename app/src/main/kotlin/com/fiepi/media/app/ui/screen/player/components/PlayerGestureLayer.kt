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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


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
    val currentPlaybackSpeed by rememberUpdatedState(state.uiOptions.playbackSpeed)

    // Gesture disambiguation tracking
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }
    var singleTapJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .onSizeChanged { width = it.width }
            .pointerInput(width, state.ui.isLocked, state.ui.controlMode, gestureOptions) {
                if (width <= 0 || state.ui.controlMode != PlayerControlMode.Normal) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val downPosition = down.position

                    // When locked, only allow single tap to toggle controls
                    if (state.ui.isLocked) {
                        onIntent(PlayerIntent.ToggleControls)
                        return@awaitEachGesture
                    }

                    // Check if this tap is a candidate for double-tap
                    val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis
                    val isDoubleTapCandidate =
                        (downTime - lastTapTime) < doubleTapTimeoutMs &&
                                (downPosition - lastTapPosition).getDistance() < thresholdPx * 3.5f

                    if (isDoubleTapCandidate) {
                        // Cancel pending single tap job so double-tap takes precedence
                        singleTapJob?.cancel()
                        singleTapJob = null

                        // Wait for second tap release without triggering long-press
                        var secondTapLifted = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break

                            if ((change.position - downPosition).getDistance() > viewConfiguration.touchSlop) {
                                break
                            }

                            if (event.changes.any { it.changedToUp() }) {
                                secondTapLifted = true
                                break
                            }
                        }

                        if (secondTapLifted) {
                            lastTapTime = 0L
                            val x = downPosition.x
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
                    } else {
                        // Fresh touch sequence: candidate for Long Press, Drag/Swipe, or Single Tap
                        singleTapJob?.cancel()
                        singleTapJob = null

                        var pointerMovedBeyondSlop = false
                        var pointerUpBeforeTimeout = false
                        var lastChange: PointerInputChange? = null

                        // 400ms long-press detection
                        val longPressResult = withTimeoutOrNull(400) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                lastChange = change

                                val distance = (change.position - downPosition).getDistance()
                                if (distance > viewConfiguration.touchSlop) {
                                    pointerMovedBeyondSlop = true
                                    return@withTimeoutOrNull false
                                }

                                if (event.changes.any { it.changedToUp() }) {
                                    pointerUpBeforeTimeout = true
                                    return@withTimeoutOrNull false
                                }
                            }
                            true
                        }

                        if (longPressResult == null && !pointerMovedBeyondSlop && !pointerUpBeforeTimeout) {
                            // Trigger long press speedup mode
                            if (gestureOptions.longPressEnabled) {
                                try {
                                    onIntent(
                                        PlayerIntent.SetPlaybackRate(
                                            speed = gestureOptions.longPressSpeed,
                                            isTemporary = true
                                        )
                                    )

                                    // Consume all pointer events during long press speedup until finger release
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                        if (event.changes.all { !it.pressed }) {
                                            break
                                        }
                                    }
                                } finally {
                                    onIntent(
                                        PlayerIntent.SetPlaybackRate(
                                            speed = currentPlaybackSpeed,
                                            isTemporary = false
                                        )
                                    )
                                    lastTapTime = 0L
                                }
                            }
                        } else if (pointerMovedBeyondSlop) {
                            // Trigger drag/swipe mode (scrubbing, volume, brightness)
                            var dragStarted = false
                            var isHorizontalDrag = false
                            var isVerticalDrag = false
                            val dragAnchorTime = currentDisplayPosition
                            var lastTargetTime = dragAnchorTime
                            var detectionDragOffset = 0f
                            val dragAnchorVolume = currentSystemVolume
                            val dragAnchorBrightness =
                                if (currentBrightness < 0f) 0.5f else currentBrightness

                            fun processDragPosition(
                                currentPos: Offset,
                                previousPos: Offset
                            ) {
                                val totalX = currentPos.x - downPosition.x
                                val totalY = currentPos.y - downPosition.y

                                if (!dragStarted) {
                                    val absX = kotlin.math.abs(totalX)
                                    val absY = kotlin.math.abs(totalY)
                                    val slopeThreshold = 2.75f // ~20 degree angle (1/tan(20°))

                                    if (absX > thresholdPx && absX > absY * slopeThreshold) {
                                        isHorizontalDrag = true
                                        dragStarted = true
                                        detectionDragOffset = totalX
                                        if (gestureOptions.horizontalDragEnabled) {
                                            onIntent(PlayerIntent.OnScrubbing(dragAnchorTime))
                                        }
                                    } else if (absY > thresholdPx && absY > absX * slopeThreshold) {
                                        isVerticalDrag = true
                                        dragStarted = true
                                    }
                                }

                                if (isHorizontalDrag && currentDuration > 0) {
                                    if (gestureOptions.horizontalDragEnabled) {
                                        val effectiveDragX = totalX - detectionDragOffset
                                        val sensitivity = 1.0f
                                        val deltaMs =
                                            (effectiveDragX / width * currentDuration.toFloat() * sensitivity).toLong()
                                        lastTargetTime =
                                            (dragAnchorTime + deltaMs).coerceIn(0, currentDuration)
                                        onIntent(PlayerIntent.OnScrubbing(lastTargetTime))
                                    }
                                } else if (isVerticalDrag) {
                                    val isLeftSide = previousPos.x < width * 0.5f
                                    val isVolumeControl =
                                        if (gestureOptions.swapVolumeBrightness) !isLeftSide else isLeftSide

                                    if (isVolumeControl) {
                                        if (gestureOptions.verticalDragVolumeEnabled) {
                                            val deltaVol =
                                                ((totalY / -fullSweepPx) * currentSystemMaxVolume).toInt()
                                            val newSystemVol =
                                                (dragAnchorVolume + deltaVol).coerceIn(
                                                    0,
                                                    currentSystemMaxVolume
                                                )
                                            onIntent(PlayerIntent.SetSystemVolume(newSystemVol))
                                        }
                                    } else {
                                        if (gestureOptions.verticalDragBrightnessEnabled) {
                                            val newBrightness =
                                                (dragAnchorBrightness + (totalY / -fullSweepPx))
                                                    .coerceIn(0f, 1f)
                                            onIntent(PlayerIntent.SetBrightness(newBrightness))
                                        }
                                    }
                                }
                            }

                            try {
                                lastChange?.let {
                                    it.consume()
                                    processDragPosition(it.position, it.previousPosition)
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (event.changes.all { !it.pressed }) break

                                    change.consume()
                                    processDragPosition(change.position, change.previousPosition)
                                }
                            } finally {
                                if (isHorizontalDrag && gestureOptions.horizontalDragEnabled) {
                                    onIntent(PlayerIntent.OnScrubbingFinished(lastTargetTime))
                                }
                                lastTapTime = 0L
                            }
                        } else if (pointerUpBeforeTimeout) {
                            // Single tap detected
                            lastTapTime = downTime
                            lastTapPosition = downPosition

                            val isDoubleTapEnabled =
                                gestureOptions.doubleTapSeekEnabled || gestureOptions.doubleTapPlayPauseEnabled

                            if (isDoubleTapEnabled) {
                                singleTapJob = coroutineScope.launch {
                                    delay(doubleTapTimeoutMs.milliseconds)
                                    onIntent(PlayerIntent.ToggleControls)
                                }
                            } else {
                                onIntent(PlayerIntent.ToggleControls)
                            }
                        }
                    }
                }
            }
    )
}
