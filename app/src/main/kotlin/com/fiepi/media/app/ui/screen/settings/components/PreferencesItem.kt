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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp


@Composable
fun PreferencesItem(
    title: String,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    summary: String? = null,
    titleFontFamily: FontFamily? = null,
    icon: @Composable (() -> Unit)? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    summaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailingContent: @Composable (() -> Unit)? = null,
    bottomContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
    colors: ListItemColors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceBright
    )
) {
    val finalIconTint = if (enabled) iconTint else iconTint.copy(alpha = 0.38f)

    val contentWrapper: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.padding(end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalContentColor provides finalIconTint) {
                            icon()
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = titleFontFamily,
                        color = if (enabled) {
                            if (selected == true) MaterialTheme.colorScheme.onSecondaryContainer
                            else Color.Unspecified
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                    if (!summary.isNullOrEmpty()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) summaryColor else summaryColor.copy(alpha = 0.38f)
                        )
                    }
                }
                if (trailingContent != null) {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides if (enabled) {
                                LocalContentColor.current
                            } else {
                                LocalContentColor.current.copy(alpha = 0.38f)
                            }
                        ) {
                            trailingContent()
                        }
                    }
                }
            }

            if (bottomContent != null) {
                CompositionLocalProvider(
                    LocalContentColor provides if (enabled) {
                        LocalContentColor.current
                    } else {
                        LocalContentColor.current.copy(alpha = 0.38f)
                    }
                ) {
                    bottomContent()
                }
            }
        }
    }

    when {
        onClick != null && selected != null -> {
            SegmentedListItem(
                selected = selected,
                onClick = onClick,
                shapes = shapes,
                modifier = modifier,
                colors = colors,
                enabled = enabled,
                contentPadding = contentPadding,
                content = contentWrapper
            )
        }

        onClick != null -> {
            SegmentedListItem(
                onClick = onClick,
                shapes = shapes,
                modifier = modifier,
                colors = colors,
                enabled = enabled,
                contentPadding = contentPadding,
                content = contentWrapper
            )
        }

        else -> {
            SegmentedListItem(
                shapes = shapes,
                modifier = modifier,
                colors = colors,
                enabled = enabled,
                contentPadding = contentPadding,
                content = contentWrapper
            )
        }
    }
}
