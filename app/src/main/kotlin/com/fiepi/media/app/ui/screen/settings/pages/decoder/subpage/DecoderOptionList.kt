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

package com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage

import androidx.compose.runtime.Composable
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SelectablePreferencesItem

@Composable
internal fun <T> DecoderOptionList(
    title: String,
    options: List<T>,
    currentValue: T,
    onValueChange: (T) -> Unit,
    titleProvider: @Composable (T) -> String,
    summaryProvider: @Composable (T) -> String?,
    enabled: Boolean = true
) {
    PreferencesGroup(
        title = title,
        items = options.map { option ->
            { shapes ->
                SelectablePreferencesItem(
                    title = titleProvider(option),
                    selected = option == currentValue,
                    onClick = { onValueChange(option) },
                    shapes = shapes,
                    summary = summaryProvider(option),
                    enabled = enabled
                )
            }
        }
    )
}
