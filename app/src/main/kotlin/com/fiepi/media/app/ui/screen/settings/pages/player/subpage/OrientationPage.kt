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
import com.fiepi.media.domain.model.preferences.PlayerOrientation


@Composable
fun OrientationPage(
    currentOrientation: PlayerOrientation,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    onOrientationSelected: (PlayerOrientation) -> Unit,
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_player_orientation),
                items = PlayerOrientation.entries.map { orientation ->
                    val content: @Composable (ListItemShapes) -> Unit = { shapes ->
                        SelectablePreferencesItem(
                            title = when (orientation) {
                                PlayerOrientation.Default -> stringResource(R.string.settings_player_orientation_default)
                                PlayerOrientation.Sensor -> stringResource(R.string.settings_player_orientation_sensor)
                                PlayerOrientation.Landscape -> stringResource(R.string.settings_player_orientation_landscape)
                                PlayerOrientation.Portrait -> stringResource(R.string.settings_player_orientation_portrait)
                                PlayerOrientation.Video -> stringResource(R.string.settings_player_orientation_video)
                            },
                            shapes = shapes,
                            onClick = { onOrientationSelected(orientation) },
                            selected = orientation == currentOrientation,
                            summary = when (orientation) {
                                PlayerOrientation.Default -> stringResource(R.string.settings_player_orientation_default_summary)
                                PlayerOrientation.Sensor -> stringResource(R.string.settings_player_orientation_sensor_summary)
                                PlayerOrientation.Landscape -> stringResource(R.string.settings_player_orientation_landscape_summary)
                                PlayerOrientation.Portrait -> stringResource(R.string.settings_player_orientation_portrait_summary)
                                PlayerOrientation.Video -> stringResource(R.string.settings_player_orientation_video_summary)
                            }
                        )
                    }
                    content
                }.toTypedArray()
            )
        }
    }
}