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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.components.VideoItem
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.source.SourceType

/**
 * List for search results shown in the expanded search bar.
 */
@Composable
fun MediaSearchList(
    modifier: Modifier = Modifier,
    results: List<MediaFile.Video>,
    searchQuery: String,
    localDisplayFields: List<MediaField> = emptyList(),
    remoteDisplayFields: List<MediaField> = emptyList(),
    onVideoClick: (MediaFile.Video) -> Unit = {}
) {
    val highlightColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(results, key = { "search_${it.id}" }) { video ->
            val itemModifier = Modifier.animateItem()

            val isRemote = video.sourceType.isRemote
            val itemFields = if (isRemote) remoteDisplayFields else localDisplayFields

            val highlightedTitle = remember(video.name, searchQuery) {
                createHighlightedText(video.name, searchQuery, highlightColor)
            }

            VideoItem(
                item = video,
                displayFields = itemFields,
                onClick = onVideoClick,
                modifier = itemModifier,
                title = highlightedTitle,
                supportingContent = {
                    Text(
                        text = video.path.substringBeforeLast('/', ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

private fun createHighlightedText(
    text: String,
    query: String,
    highlightColor: androidx.compose.ui.graphics.Color
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
fun MediaSearchListPreview() {
    val mockResults = listOf(
        MediaFile.Video(
            path = "/root/Movies/action1.mp4",
            name = "action1",
            sourceType = SourceType.Local,
            id = "1",
            duration = 120000,
            size = 1024 * 1024 * 10,
            lastModified = 0,
            mimeType = "video/mp4",
            extension = "mp4",
            width = 1920,
            height = 1080
        ),
        MediaFile.Video(
            path = "/root/Downloads/comedy.mkv",
            name = "comedy",
            sourceType = SourceType.Local,
            id = "2",
            duration = 3600000,
            size = 1024 * 1024 * 500,
            lastModified = 0,
            mimeType = "video/x-matroska",
            extension = "mkv",
            width = 1280,
            height = 720
        )
    )
    MaterialTheme {
        MediaSearchList(
            results = mockResults,
            searchQuery = "action",
            localDisplayFields = MediaField.entries,
            remoteDisplayFields = MediaField.entries,
            onVideoClick = {})
    }
}
