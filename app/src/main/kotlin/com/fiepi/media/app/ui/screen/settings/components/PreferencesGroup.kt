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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PreferencesGroup(
    title: String? = null,
    vararg items: @Composable (shapes: ListItemShapes) -> Unit
) {
    PreferencesGroup(
        title = title,
        items = items.toList(),
        horizontalPadding = 16.dp
    )
}

@Composable
fun PreferencesGroup(
    title: String? = null,
    horizontalPadding: Dp = 16.dp,
    items: List<@Composable (shapes: ListItemShapes) -> Unit>
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        if (title != null) {
            PreferencesLabel(
                title = title,
                horizontalPadding = horizontalPadding
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 4.dp),
            color = Color.Transparent
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val count = items.size
                items.forEachIndexed { index, item ->
                    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
                    item(shapes)
                }
            }
        }
    }
}

@Composable
fun PreferencesLabel(
    title: String,
    horizontalPadding: Dp = 16.dp,
) {
    val realHorizontalPadding = horizontalPadding + 8.dp
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 16.dp,
                bottom = 4.dp,
                start = realHorizontalPadding,
                end = realHorizontalPadding
            )
    )
}
