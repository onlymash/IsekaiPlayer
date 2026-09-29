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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiepi.media.app.ui.hooks.LocalAppHaptics
import com.fiepi.media.app.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Stateful wrapper for FastScrollBar connecting LazyListState to FastScrollBarContent.
 *
 * @param modifier Layout modifier applied as the first parameter.
 * @param state The [LazyListState] bound to the target list.
 * @param itemCount Total number of items in the list.
 * @param radius Radius of the circular handle.
 * @param labelProvider Callback returning text for the single-line adaptive indicator pill.
 */
@Composable
fun FastScrollBar(
    modifier: Modifier = Modifier,
    state: LazyListState,
    itemCount: Int,
    radius: Dp = 24.dp,
    labelProvider: ((index: Int) -> String) = { index -> "${index + 1} / $itemCount" }
) {
    if (itemCount <= 0) return

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val appHaptics = LocalAppHaptics.current

    var isDragging by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var lastHapticIndex by remember { mutableIntStateOf(-1) }

    // Auto-fade visibility logic matching Google Photos behavior
    LaunchedEffect(state.isScrollInProgress, isDragging) {
        if (state.isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            delay(1500.milliseconds)
            isVisible = false
        }
    }

    val scrollProgress by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || itemCount <= 1) return@derivedStateOf 0f

            val firstVisibleIndex = state.firstVisibleItemIndex
            val firstVisibleOffset = state.firstVisibleItemScrollOffset
            val firstItemSize = visibleItems.first().size.toFloat().coerceAtLeast(1f)

            val fractionalIndex = firstVisibleIndex + (firstVisibleOffset / firstItemSize)
            val visibleCount = visibleItems.size
            val maxScrollIndex = (itemCount - visibleCount).coerceAtLeast(1)
            (fractionalIndex / maxScrollIndex).coerceIn(0f, 1f)
        }
    }

    val handleHeight = radius * 2f
    val handleHeightPx = with(density) { handleHeight.toPx() }

    val currentTargetIndex by remember {
        derivedStateOf {
            val visibleCount = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val maxScrollIndex = (itemCount - visibleCount).coerceAtLeast(1)
            val currentProgress = if (isDragging) {
                val viewportHeightPx = state.layoutInfo.viewportSize.height.toFloat()
                val availableTrackPx = (viewportHeightPx - handleHeightPx).coerceAtLeast(1f)
                (dragOffsetY / availableTrackPx).coerceIn(0f, 1f)
            } else {
                scrollProgress
            }
            (currentProgress * maxScrollIndex).roundToInt().coerceIn(0, maxScrollIndex)
        }
    }

    if (isDragging && currentTargetIndex != lastHapticIndex) {
        lastHapticIndex = currentTargetIndex
        appHaptics.performFrequentTick()
    }

    FastScrollBarContent(
        modifier = modifier,
        scrollProgress = scrollProgress,
        isDragging = isDragging,
        isVisible = isVisible,
        labelText = if (isDragging) labelProvider(currentTargetIndex) else null,
        dragOffsetY = if (isDragging) dragOffsetY else null,
        radius = radius,
        onDragStart = { touchY ->
            isDragging = true
            val viewportHeightPx = state.layoutInfo.viewportSize.height.toFloat()
            val availableTrackPx = (viewportHeightPx - handleHeightPx).coerceAtLeast(1f)
            dragOffsetY = (touchY - handleHeightPx / 2f).coerceIn(0f, availableTrackPx)
            val visibleCount = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val maxScrollIndex = (itemCount - visibleCount).coerceAtLeast(1)
            val targetFraction = dragOffsetY / availableTrackPx
            val targetIndex =
                (targetFraction * maxScrollIndex).roundToInt().coerceIn(0, maxScrollIndex)
            coroutineScope.launch {
                state.scrollToItem(targetIndex)
            }
        },
        onDragEnd = {
            isDragging = false
        },
        onDrag = { dragAmountY ->
            val viewportHeightPx = state.layoutInfo.viewportSize.height.toFloat()
            val availableTrackPx = (viewportHeightPx - handleHeightPx).coerceAtLeast(1f)
            dragOffsetY = (dragOffsetY + dragAmountY).coerceIn(0f, availableTrackPx)
            val visibleCount = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val maxScrollIndex = (itemCount - visibleCount).coerceAtLeast(1)
            val targetFraction = dragOffsetY / availableTrackPx
            val targetIndex =
                (targetFraction * maxScrollIndex).roundToInt().coerceIn(0, maxScrollIndex)
            coroutineScope.launch {
                state.scrollToItem(targetIndex)
            }
        }
    )
}

/**
 * Stateless UI component for FastScrollBar featuring a true circular handle offset off the right screen edge
 * to form a perfect circular arc with an up/down arrow icon and an unrestricted single-line adaptive text indicator pill.
 *
 * @param modifier Layout modifier applied as the first parameter.
 * @param scrollProgress Current scroll progress from 0.0f to 1.0f.
 * @param isDragging Whether the drag handle is currently being held.
 * @param isVisible Whether the scrollbar layout should be visible.
 * @param labelText Optional single-line text displayed inside the floating indicator badge.
 * @param dragOffsetY Optional explicit pixel Y offset for the handle during drag.
 * @param radius Radius of the full circular handle.
 * @param onDragStart Drag gesture start callback providing touch Y position.
 * @param onDragEnd Drag gesture end callback.
 * @param onDrag Drag gesture movement callback providing Y drag delta.
 */
@Composable
private fun FastScrollBarContent(
    modifier: Modifier = Modifier,
    scrollProgress: Float,
    isDragging: Boolean,
    isVisible: Boolean,
    labelText: String? = null,
    dragOffsetY: Float? = null,
    radius: Dp = 24.dp,
    onDragStart: ((offsetY: Float) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDrag: ((dragAmountY: Float) -> Unit)? = null
) {
    val density = LocalDensity.current
    val diameter = radius * 2f
    val visibleWidth = radius * 1.5f

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            val constraintsHeightPx = with(density) { maxHeight.toPx() }
            val handleHeightPx = with(density) { diameter.toPx() }
            val availableTrackPx = (constraintsHeightPx - handleHeightPx).coerceAtLeast(1f)

            val currentThumbOffsetPx = if (isDragging && dragOffsetY != null) {
                dragOffsetY.coerceIn(0f, availableTrackPx)
            } else {
                (scrollProgress.coerceIn(0f, 1f) * availableTrackPx)
            }
            val thumbOffsetDp = with(density) { currentThumbOffsetPx.toDp() }

            val pointerModifier = if (onDragStart != null || onDragEnd != null || onDrag != null) {
                Modifier.pointerInput(availableTrackPx) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> onDragStart?.invoke(offset.y) },
                        onDragEnd = { onDragEnd?.invoke() },
                        onDragCancel = { onDragEnd?.invoke() },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            onDrag?.invoke(dragAmount)
                        }
                    )
                }
            } else {
                Modifier
            }

            // Single-line adaptive-width floating text indicator pill displayed during drag
            AnimatedVisibility(
                visible = isDragging && !labelText.isNullOrEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = visibleWidth + 8.dp)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (currentThumbOffsetPx + (handleHeightPx - with(density) { 36.dp.toPx() }) / 2f).roundToInt()
                        )
                    }
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = labelText.orEmpty(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Full circular handle offset to the right edge to form a perfect circular arc
            Surface(
                shape = CircleShape,
                color = if (isDragging) MaterialTheme.colorScheme.surfaceBright else MaterialTheme.colorScheme.surfaceBright.copy(
                    alpha = 0.9f
                ),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = radius * 0.67f, y = thumbOffsetDp)
                    .size(diameter)
                    .systemGestureExclusion()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.UnfoldMore,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = radius * 0.5f)
                            .size(24.dp)
                    )
                }
            }

            // Full-height right-edge gesture detector strip
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(visibleWidth + 16.dp)
                    .then(pointerModifier)
            )
        }
    }
}

@Preview(showBackground = true, name = "FastScrollBar Active Drag Preview")
@Composable
private fun FastScrollBarActivePreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .height(400.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            FastScrollBarContent(
                modifier = Modifier.fillMaxSize(),
                scrollProgress = 0.45f,
                isDragging = true,
                isVisible = true,
                labelText = "Video_2026_03_31_long_filename.mp4 (45 / 100)"
            )
        }
    }
}

@Preview(showBackground = true, name = "FastScrollBar Idle Preview")
@Composable
private fun FastScrollBarIdlePreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .height(400.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
        ) {
            FastScrollBarContent(
                modifier = Modifier.fillMaxSize(),
                scrollProgress = 0.2f,
                isDragging = false,
                isVisible = true
            )
        }
    }
}
