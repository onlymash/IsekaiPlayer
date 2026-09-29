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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.source.MediaSource
import kotlinx.coroutines.flow.flowOf

/**
 * List for playback history search results shown in the expanded search bar.
 */
@Composable
fun HistorySearchList(
    modifier: Modifier = Modifier,
    historyItems: LazyPagingItems<PlaybackHistory>,
    searchQuery: String,
    localDisplayFields: List<MediaField> = emptyList(),
    remoteDisplayFields: List<MediaField> = emptyList(),
    onHistoryClick: (PlaybackHistory) -> Unit = {},
    onReplayHistoryClick: (PlaybackHistory) -> Unit = {},
    onDeleteHistoryClick: (PlaybackHistory) -> Unit = {}
) {
    val highlightColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = historyItems.itemCount,
            key = historyItems.itemKey { "history_search_${it.sourceId}:${it.path}" }
        ) { index ->
            val history = historyItems[index]
            if (history != null) {
                val isRemote = history.sourceType.isRemote
                val displayFields = if (isRemote) remoteDisplayFields else localDisplayFields
                val showThumbnail = displayFields.contains(MediaField.Thumbnail)

                val highlightedTitle = remember(history.title, history.path, searchQuery) {
                    val rawTitle = history.title.ifBlank { history.path.substringAfterLast('/') }
                    createHighlightedText(rawTitle, searchQuery, highlightColor)
                }

                val highlightedSubtitle = remember(history.path, searchQuery) {
                    createHighlightedText(history.path, searchQuery, highlightColor)
                }

                PlaybackHistoryItem(
                    history = history,
                    title = highlightedTitle,
                    subtitle = highlightedSubtitle,
                    showThumbnail = showThumbnail,
                    onClick = { onHistoryClick(history) },
                    onReplayClick = { onReplayHistoryClick(history) },
                    onDeleteClick = { onDeleteHistoryClick(history) }
                )
            }
        }
    }
}

private fun createHighlightedText(
    text: String,
    query: String,
    highlightColor: Color
): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = text.indexOf(query, start, ignoreCase = true)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistorySearchListPreview() {
    val mockResults = listOf(
        PlaybackHistory(
            sourceId = MediaSource.INTERNAL_STORAGE_ID,
            path = "/storage/emulated/0/Movies/Inception.mp4",
            title = "Inception",
            positionMs = 3600000L,
            durationMs = 8880000L,
            lastPlayedAt = System.currentTimeMillis() - 3600000L
        )
    )
    val historyItems = flowOf(PagingData.from(mockResults)).collectAsLazyPagingItems()
    AppTheme {
        HistorySearchList(
            historyItems = historyItems,
            searchQuery = "Inception",
            onHistoryClick = {},
            onReplayHistoryClick = {},
            onDeleteHistoryClick = {}
        )
    }
}
