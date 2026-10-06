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

package com.fiepi.media.app.ui.screen.player.components.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SelectablePreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.app.ui.theme.AppTheme

@Composable
fun OsdDrawerPage(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = contentPadding
    ) {

        item {
            PreferencesGroup(
                items = listOf(
                    { shapes: ListItemShapes ->
                        SwitchPreferencesItem(
                            title = stringResource(R.string.player_control_osd),
                            shapes = shapes,
                            checked = state.ui.isOsdVisible,
                            onCheckedChange = { onIntent(PlayerIntent.SetOsdVisible(it)) }
                        )
                    }
                )
            )
        }

        item {
            val pages = listOf(
                1 to R.string.player_osd_stats_general,
                2 to R.string.player_osd_stats_timings,
                3 to R.string.player_osd_stats_cache,
                4 to R.string.player_osd_stats_key_bindings,
                5 to R.string.player_osd_stats_tracks
            )

            PreferencesGroup(
                title = stringResource(R.string.player_osd_stats_page_header),
                items = pages.map { (page, labelRes) ->
                    val content: @Composable (ListItemShapes) -> Unit =
                        { shapes: ListItemShapes ->
                            SelectablePreferencesItem(
                                title = stringResource(labelRes),
                                selected = state.ui.currentOsdPage == page,
                                onClick = { onIntent(PlayerIntent.SetOsdPage(page)) },
                                shapes = shapes,
                                enabled = state.ui.isOsdVisible
                            )
                        }
                    content
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "OSD Page")
@Composable
fun OsdDrawerPagePreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f))
                .width(400.dp)
                .fillMaxHeight()
        ) {
            OsdDrawerPage(
                state = PlayerState(),
                onIntent = {},
                contentPadding = PaddingValues(16.dp)
            )
        }
    }
}
