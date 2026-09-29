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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.hooks.LocalAppHaptics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PlayerUnlockSlider(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalAppHaptics.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var trackWidth by remember { mutableIntStateOf(0) }
    val thumbSize = 56.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }

    val offsetX = remember { Animatable(0f) }
    val interactionSource = remember { MutableInteractionSource() }

    val maxOffset = remember(trackWidth, thumbSizePx) {
        (trackWidth - thumbSizePx).coerceAtLeast(0f)
    }

    Box(
        modifier = modifier
            .width(280.dp)
            .height(thumbSize)
            .onSizeChanged { trackWidth = it.width }
            .playerButtonSurface(interactionSource, CircleShape, scaleTarget = 1f)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Text
        Text(
            text = stringResource(R.string.player_control_unlock),
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(1f - (offsetX.value / maxOffset).coerceIn(0f, 1f))
        )

        // Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbSize - 8.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offsetX.snapTo((offsetX.value + delta).coerceIn(0f, maxOffset))
                        }
                    },
                    onDragStopped = {
                        if (offsetX.value >= maxOffset * 0.9f) {
                            haptics.performClick()
                            onUnlock()
                        } else {
                            scope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LockOpen,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
