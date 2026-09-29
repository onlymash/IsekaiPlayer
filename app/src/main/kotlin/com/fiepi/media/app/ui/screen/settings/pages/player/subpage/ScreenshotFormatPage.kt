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

package com.fiepi.media.app.ui.screen.settings.pages.player.subpage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SelectablePreferencesItem
import com.fiepi.media.domain.model.preferences.ScreenshotFormat

@Composable
fun ScreenshotFormatPage(
    currentFormat: ScreenshotFormat,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    onFormatSelected: (ScreenshotFormat) -> Unit,
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_player_screenshot_format),
                items = ScreenshotFormat.entries.map { format ->
                    val content: @Composable (ListItemShapes) -> Unit = { shapes ->
                        SelectablePreferencesItem(
                            title = when (format) {
                                ScreenshotFormat.JPG -> stringResource(R.string.settings_player_screenshot_format_jpg)
                                ScreenshotFormat.PNG -> stringResource(R.string.settings_player_screenshot_format_png)
                                ScreenshotFormat.WEBP -> stringResource(R.string.settings_player_screenshot_format_webp)
                            },
                            shapes = shapes,
                            onClick = { onFormatSelected(format) },
                            selected = format == currentFormat
                        )
                    }
                    content
                }.toTypedArray()
            )
        }
    }
}
