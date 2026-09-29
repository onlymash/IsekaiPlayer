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

package com.fiepi.media.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


// Defines state holder class
class SplitPaneState(
    initialWidth: Dp,
    val minLeftWidth: Dp = 100.dp,
    val minRightWidth: Dp = 100.dp
) {
    var leftPaneWidth by mutableStateOf(initialWidth)

    companion object {
        // Custom Saver: saves only Float value (Dp value), reinstantiates state object upon restoration
        fun Saver(minLeftWidth: Dp, minRightWidth: Dp): Saver<SplitPaneState, Float> = Saver(
            save = { it.leftPaneWidth.value },
            restore = { savedValue ->
                SplitPaneState(savedValue.dp, minLeftWidth, minRightWidth)
            }
        )
    }
}

// Remember function for creating state
@Composable
fun rememberSplitPaneState(
    initialLeftWidth: Dp = 320.dp,
    minLeftWidth: Dp = 280.dp,
    minRightWidth: Dp = 420.dp
): SplitPaneState {
    return rememberSaveable(
        saver = SplitPaneState.Saver(minLeftWidth, minRightWidth)
    ) {
        SplitPaneState(initialLeftWidth, minLeftWidth, minRightWidth)
    }
}

// Core split pane layout component
@Composable
fun SplitPaneLayout(
    modifier: Modifier = Modifier,
    state: SplitPaneState = rememberSplitPaneState(),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    handle: @Composable (isPressed: Boolean) -> Unit = { DefaultSplitPaneHandle(it) },
    leftContent: @Composable BoxScope.() -> Unit,
    rightContent: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        color = containerColor
    ) {
        BoxWithConstraints(modifier = modifier) {
            val totalWidth = maxWidth
            val maxLeftWidth = totalWidth - state.minRightWidth

            // Core logic: when screen rotates, total width may shrink causing leftPaneWidth overflow; auto-correct here
            LaunchedEffect(totalWidth) {
                state.leftPaneWidth = state.leftPaneWidth.coerceIn(state.minLeftWidth, maxLeftWidth)
            }

            Row(modifier = Modifier.fillMaxSize()) {
                // Left pane
                Box(
                    modifier = Modifier
                        .width(state.leftPaneWidth)
                        .fillMaxHeight()
                ) {
                    leftContent()
                }

                // Drag handle area
                var isPressed by remember { mutableStateOf(false) }
                var isDragging by remember { mutableStateOf(false) }
                val density = LocalDensity.current

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(20.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                // Wait for touch down; requireUnconsumed = false means listen even if consumed by other gestures
                                awaitFirstDown(requireUnconsumed = false)
                                isPressed = true

                                // Suspend and wait for finger up or gesture cancellation
                                waitForUpOrCancellation()
                                isPressed = false
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                // Update isDragging state across drag gesture lifecycle callbacks
                                onDragStart = { isDragging = true },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) { change, dragAmount ->
                                change.consume()
                                val dragAmountDp = with(density) { dragAmount.toDp() }
                                state.leftPaneWidth = (state.leftPaneWidth + dragAmountDp)
                                    .coerceIn(state.minLeftWidth, maxLeftWidth)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    handle(isPressed || isDragging)
                }

                // Right pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    rightContent()
                }
            }
        }
    }
}

// Default drag handle UI (with animation feedback)
@Composable
fun DefaultSplitPaneHandle(isActive: Boolean) {
    // Color darkens (or changes to primary) when active
    val color by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "handleColor"
    )
    // Line gets thicker when active
    val width by animateDpAsState(
        targetValue = if (isActive) 5.dp else 3.dp,
        label = "handleWidth"
    )

    Box(
        modifier = Modifier
            .width(width)
            .fractionalHeightIn(0.2f, minHeight = 64.dp, maxHeight = 120.dp)
            .background(color, shape = CircleShape)
    )
}


// Custom Modifier: dynamically calculates fractional height with min/max bounds
fun Modifier.fractionalHeightIn(
    fraction: Float,
    minHeight: Dp,
    maxHeight: Dp
) = layout { measurable, constraints ->
    // Get available maximum height from parent container
    val parentHeight = constraints.maxHeight

    // Calculate target fractional height and clamp between [minPx, maxPx]
    val targetHeight = (parentHeight * fraction).toInt()
    val minPx = minHeight.roundToPx()
    val maxPx = maxHeight.roundToPx()
    val finalHeight = targetHeight.coerceIn(minPx, maxPx)

    // Force child component to measure using calculated final height
    val placeable = measurable.measure(
        constraints.copy(
            minHeight = finalHeight,
            maxHeight = finalHeight
        )
    )

    // Place component
    layout(placeable.width, placeable.height) {
        placeable.placeRelative(0, 0)
    }
}