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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.hooks.LocalAppHaptics

/**
 * Generic player adjustment bar with liquid glass styling.
 */
@Composable
fun PlayerAdjustBar(
    value: Float,
    onValueChange: (newValue: Float, persist: Boolean) -> Unit,
    onClose: () -> Unit,
    valueText: String,
    label: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    step: Float,
    modifier: Modifier = Modifier,
    showTemporaryToggle: Boolean = true
) {
    var isTemporary by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(bottom = 24.dp)
            .width(320.dp)
            .playerButtonSurface(interactionSource, RoundedCornerShape(28.dp), scaleTarget = 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { /* Intercept clicks */ }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top control row (Decrease - Value - Increase)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AdjustIconButton(
                    onClick = {
                        val newValue = (value - step).coerceIn(range.start, range.endInclusive)
                        onValueChange(newValue, !isTemporary || !showTemporaryToggle)
                    },
                    icon = Icons.Outlined.Remove,
                    enabled = value > range.start
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 100.dp)
                ) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                AdjustIconButton(
                    onClick = {
                        val newValue = (value + step).coerceIn(range.start, range.endInclusive)
                        onValueChange(newValue, !isTemporary || !showTemporaryToggle)
                    },
                    icon = Icons.Outlined.Add,
                    enabled = value < range.endInclusive
                )
            }

            val sliderState = remember(range, steps) {
                SliderState(
                    value = value,
                    steps = steps,
                    trackRange = range
                )
            }

            LaunchedEffect(value) {
                if (!sliderState.isDragging) {
                    sliderState.value = value.coerceIn(range.start, range.endInclusive)
                }
            }

            // Progress bar
            Slider(
                state = sliderState,
                onValueChange = { newValue ->
                    onValueChange(newValue, !isTemporary || !showTemporaryToggle)
                    if (sliderState.isDragging) {
                        sliderState.value = newValue
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                    activeTickColor = Color.White,
                    inactiveTickColor = Color.White.copy(alpha = 0.24f),
                )
            )

            // Temporary adjustment toggle row
            if (showTemporaryToggle) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .alpha(0.8f)
                ) {
                    Text(
                        text = stringResource(R.string.player_control_adjust_temporary),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                    Switch(
                        checked = isTemporary,
                        onCheckedChange = { isTemporary = it },
                        modifier = Modifier.scale(0.7f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // Close button
            AdjustCloseButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.common_close),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AdjustIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val appHaptics = LocalAppHaptics.current

    Box(
        modifier = Modifier
            .size(48.dp)
            .playerButtonSurface(interactionSource, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    appHaptics.performClick()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.38f)
        )
    }
}

@Composable
private fun AdjustCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val appHaptics = LocalAppHaptics.current

    Box(
        modifier = modifier
            .playerButtonSurface(interactionSource, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    appHaptics.performClick()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            content()
        }
    }
}
