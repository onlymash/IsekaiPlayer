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

package com.fiepi.media.app.ui.screen.browser.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType

/**
 * A horizontal scrollable ButtonGroup for filtering playback history by media source.
 * The first button represents "All Sources" (selectedSourceId == null).
 */
@Composable
fun HistorySourceFilterBar(
    sources: List<MediaSource>,
    selectedSourceId: String?,
    onSourceSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val lastIndex = sources.size

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Button 0: All Sources
        val isAllSelected = selectedSourceId == null
        OutlinedToggleButton(
            checked = isAllSelected,
            onCheckedChange = { if (!isAllSelected) onSourceSelected(null) },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            colors = OutlinedToggleButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(text = stringResource(R.string.common_all))
        }

        // Subsequent buttons: Media sources
        sources.forEachIndexed { index, source ->
            val buttonIndex = index + 1
            val isSelected = selectedSourceId == source.id
            val shapes = when (buttonIndex) {
                lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }

            OutlinedToggleButton(
                checked = isSelected,
                onCheckedChange = { if (!isSelected) onSourceSelected(source.id) },
                shapes = shapes,
                colors = OutlinedToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(text = source.name)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistorySourceFilterBarPreview() {
    val mockSources = listOf(
        MediaSource.Local(
            MediaSource.INTERNAL_STORAGE_ID,
            "Internal Storage",
            "/root"
        ),
        MediaSource.Remote(
            RemoteSource(
                id = "smb1",
                name = "NAS Share",
                type = SourceType.Smb,
                host = "192.168.1.100",
                path = "share"
            )
        )
    )
    AppTheme {
        HistorySourceFilterBar(
            sources = mockSources,
            selectedSourceId = null,
            onSourceSelected = {}
        )
    }
}
