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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SelectablePreferencesItem
import com.fiepi.media.domain.model.preferences.SeekBarStyle


@Composable
fun SeekBarPage(
    currentStyle: SeekBarStyle,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    onStyleSelected: (SeekBarStyle) -> Unit,
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_player_seek_bar_style),
                items = SeekBarStyle.entries.map { style ->
                    val content: @Composable (ListItemShapes) -> Unit = { shapes ->
                        SelectablePreferencesItem(
                            title = when (style) {
                                SeekBarStyle.Wavy -> stringResource(R.string.settings_player_seek_bar_style_wavy)
                                SeekBarStyle.Straight -> stringResource(R.string.settings_player_seek_bar_style_straight)
                            },
                            shapes = shapes,
                            onClick = { onStyleSelected(style) },
                            selected = style == currentStyle,
                            bottomContent = {
                                // Preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .height(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (style == SeekBarStyle.Wavy) {
                                        LinearWavyProgressIndicator(
                                            progress = { 0.6f },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.24f
                                            )
                                        )
                                    } else {
                                        LinearProgressIndicator(
                                            progress = { 0.6f },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.24f
                                            ),
                                            strokeCap = StrokeCap.Round,
                                            gapSize = 0.dp,
                                            drawStopIndicator = {}
                                        )
                                    }
                                }
                            }
                        )
                    }
                    content
                }.toTypedArray()
            )
        }
    }
}