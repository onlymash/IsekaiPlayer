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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BrowserItemPlaceholder
import com.fiepi.media.app.ui.components.rememberShimmerBrush
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.preferences.ThemeMode
import com.fiepi.media.domain.model.preferences.ThemeOptions
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType
import kotlinx.coroutines.flow.flowOf

@Composable
fun PlaybackHistoryList(
    modifier: Modifier = Modifier,
    historyItems: LazyPagingItems<PlaybackHistory>,
    localDisplayFields: List<MediaField> = emptyList(),
    remoteDisplayFields: List<MediaField> = emptyList(),
    onPlayHistory: (PlaybackHistory) -> Unit = {},
    onReplayHistory: (PlaybackHistory) -> Unit = {},
    onDeleteHistory: (sourceId: String, path: String) -> Unit = { _, _ -> },
    bottomContentPadding: Dp = 0.dp,
    scrollState: LazyListState = rememberLazyListState()
) {
    val shimmerBrush = rememberShimmerBrush()
    var itemToDelete by remember { mutableStateOf<PlaybackHistory?>(null) }

    LazyColumn(
        state = scrollState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 8.dp + bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            count = historyItems.itemCount,
            key = historyItems.itemKey { "${it.sourceId}:${it.path}" }
        ) { index ->
            val item = historyItems[index]
            if (item != null) {
                val isRemote = item.sourceType.isRemote
                val displayFields = if (isRemote) remoteDisplayFields else localDisplayFields
                val showThumbnail = displayFields.contains(MediaField.Thumbnail)

                PlaybackHistoryItem(
                    modifier = Modifier.animateItem(),
                    history = item,
                    showThumbnail = showThumbnail,
                    onClick = { onPlayHistory(item) },
                    onReplayClick = { onReplayHistory(item) },
                    onDeleteClick = { itemToDelete = item }
                )
            } else {
                BrowserItemPlaceholder(
                    modifier = Modifier.animateItem(),
                    brush = shimmerBrush
                )
            }
        }
    }

    // Confirmation Dialog for Single Item Delete
    if (itemToDelete != null) {
        val targetItem = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.playback_history_delete_dialog_title)) },
            text = { Text(stringResource(R.string.playback_history_delete_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteHistory(targetItem.sourceId, targetItem.path)
                        itemToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Playback History List")
@Composable
fun PlaybackHistoryListPreview() {
    val mockHistory = listOf(
        PlaybackHistory(
            sourceId = MediaSource.INTERNAL_STORAGE_ID,
            path = "/storage/emulated/0/Movies/Inception.mp4",
            title = "Inception",
            positionMs = 3600000L,
            durationMs = 8880000L,
            lastPlayedAt = System.currentTimeMillis() - 3600000L
        ),
        PlaybackHistory(
            sourceId = "smb_1",
            path = "smb://192.168.1.100/share/Videos/Anime_EP01.mkv",
            title = "Anime EP01",
            sourceType = SourceType.Smb,
            positionMs = 1200000L,
            durationMs = 1440000L,
            lastPlayedAt = System.currentTimeMillis() - 86400000L
        ),
        PlaybackHistory(
            sourceId = MediaSource.INTERNAL_STORAGE_ID,
            path = "/storage/emulated/0/Downloads/Short_Clip.mp4",
            title = "Short Clip",
            positionMs = 180000L,
            durationMs = 180000L,
            lastPlayedAt = System.currentTimeMillis() - 172800000L,
            isCompleted = true
        )
    )
    val historyItems = flowOf(PagingData.from(mockHistory)).collectAsLazyPagingItems()
    AppTheme {
        PlaybackHistoryList(
            historyItems = historyItems,
            onPlayHistory = {},
            onReplayHistory = {},
            onDeleteHistory = { _, _ -> },
            bottomContentPadding = 0.dp
        )
    }
}

@Preview(showBackground = true, name = "Playback History List - Dark")
@Composable
fun PlaybackHistoryListDarkPreview() {
    val mockHistory = listOf(
        PlaybackHistory(
            sourceId = MediaSource.INTERNAL_STORAGE_ID,
            path = "/storage/emulated/0/Movies/Inception.mp4",
            title = "Inception",
            positionMs = 3600000L,
            durationMs = 8880000L,
            lastPlayedAt = System.currentTimeMillis() - 3600000L
        )
    )
    val historyItems = flowOf(PagingData.from(mockHistory)).collectAsLazyPagingItems()
    AppTheme(themeOptions = ThemeOptions(themeMode = ThemeMode.Dark)) {
        Surface {
            PlaybackHistoryList(
                historyItems = historyItems,
                onPlayHistory = {},
                onReplayHistory = {},
                onDeleteHistory = { _, _ -> },
                bottomContentPadding = 0.dp
            )
        }
    }
}

@Preview(showBackground = true, name = "Playback History Empty")
@Composable
fun PlaybackHistoryEmptyPreview() {
    val historyItems =
        flowOf(PagingData.from(emptyList<PlaybackHistory>())).collectAsLazyPagingItems()
    AppTheme {
        PlaybackHistoryList(
            historyItems = historyItems,
            onPlayHistory = {},
            onReplayHistory = {},
            onDeleteHistory = { _, _ -> },
            bottomContentPadding = 0.dp
        )
    }
}
