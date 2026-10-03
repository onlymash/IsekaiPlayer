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

package com.fiepi.media.app.ui.screen.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler


@Composable
fun SliderPreferencesItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    summary: String? = null,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    showTickMarks: Boolean = steps in 1..20,
    unit: String? = null,
    enabled: Boolean = true,
    valueFormat: String = "%.0f",
    icon: @Composable (() -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    PreferencesItem(
        title = title,
        summary = summary,
        icon = icon,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        trailingContent = onReset?.let {
            {
                FilledTonalIconButton(
                    onClick = rememberHapticClickHandler(onClick = it),
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.RestartAlt,
                        contentDescription = stringResource(R.string.common_reset)
                    )
                }
            }
        },
        bottomContent = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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

                Slider(
                    state = sliderState,
                    onValueChange = { newValue ->
                        onValueChange(newValue)
                        if (sliderState.isDragging) {
                            sliderState.value = newValue
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = if (!showTickMarks) SliderDefaults.colors(
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                        disabledActiveTickColor = Color.Transparent,
                        disabledInactiveTickColor = Color.Transparent
                    ) else SliderDefaults.colors(),
                    enabled = enabled
                )
                Text(
                    text = if (unit.isNullOrEmpty()) {
                        valueFormat.format(value)
                    } else {
                        "${valueFormat.format(value)}$unit"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(58.dp),
                    textAlign = TextAlign.End,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.38f
                    )
                )
            }
        }
    )
}
