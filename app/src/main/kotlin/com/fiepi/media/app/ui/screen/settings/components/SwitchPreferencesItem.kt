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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun SwitchPreferencesItem(
    title: String,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    PreferencesItem(
        title = title,
        summary = summary,
        icon = icon,
        shapes = shapes,
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 0.dp, bottom = 8.dp),
        onClick = onSecondaryClick ?: { onCheckedChange(!checked) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxHeight()
            ) {
                if (onSecondaryClick != null) {
                    VerticalDivider(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxHeight()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(
                            if (onSecondaryClick != null && enabled) {
                                Modifier.clickable { onCheckedChange(!checked) }
                            } else Modifier
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Switch(
                        checked = checked,
                        onCheckedChange = null,
                        enabled = enabled,
                        thumbContent = {
                            if (checked) Icon(
                                Icons.Default.Check,
                                null,
                                Modifier.size(SwitchDefaults.IconSize)
                            )
                            else Icon(
                                Icons.Default.Close,
                                null,
                                Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }
    )
}
