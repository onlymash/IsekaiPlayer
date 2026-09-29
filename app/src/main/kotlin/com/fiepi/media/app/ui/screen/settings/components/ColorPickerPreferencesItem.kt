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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

/**
 * Preferences item for color selection using clickable circular color dots.
 * Shows a primary border and checkmark icon for the selected color.
 * Supports horizontal scrolling for narrow screens.
 */
@Composable
fun ColorPickerPreferencesItem(
    title: String,
    colors: List<String>,
    currentColor: String,
    onColorChange: (String) -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    PreferencesItem(
        title = title,
        summary = summary,
        icon = icon,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        bottomContent = {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(bottom = 12.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { hexColor ->
                    val isSelected = hexColor.equals(currentColor, ignoreCase = true)
                    val parsedColor = try {
                        Color(hexColor.toColorInt())
                    } catch (_: Exception) {
                        Color.White
                    }

                    val checkmarkColor =
                        if (parsedColor.luminance() > 0.5f) Color.Black else Color.White

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 3.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                }
                            )
                            .padding(if (isSelected) 3.dp else 0.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .clickable(enabled = enabled) {
                                onColorChange(hexColor)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = checkmarkColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}
