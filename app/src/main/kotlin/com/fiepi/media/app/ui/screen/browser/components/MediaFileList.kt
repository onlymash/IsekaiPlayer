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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.components.FastScrollBar
import com.fiepi.media.app.ui.components.FolderItem
import com.fiepi.media.app.ui.components.SubtitleItem
import com.fiepi.media.app.ui.components.VideoItem
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserSelectionState
import com.fiepi.media.app.ui.screen.browser.viewmodel.ScrollPosition
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.source.SourceType

/**
 * Main scrollable list of media files with scroll state restoration support.
 */
@Composable
fun MediaFileList(
    mediaFiles: List<MediaFile>,
    displayFields: List<MediaField>,
    initialPosition: ScrollPosition?,
    lastPlayedPath: String?,
    onFolderClick: (MediaFile.Folder) -> Unit,
    onVideoClick: (MediaFile.Video) -> Unit,
    onSubtitleClick: (MediaFile.Subtitle) -> Unit,
    onScrollChanged: (Int, Int) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier,
    selectionState: BrowserSelectionState = BrowserSelectionState(),
    onItemToggleSelect: (MediaFile) -> Unit = {},
    onItemLongClick: (MediaFile) -> Unit = {},
    scrollState: LazyListState = rememberLazyListState()
) {
    // Restore saved scroll position for this directory
    LaunchedEffect(initialPosition) {
        if (initialPosition != null) {
            if (initialPosition.index < mediaFiles.size) {
                scrollState.scrollToItem(initialPosition.index, initialPosition.offset)
            }
        } else {
            // Reset to top for new directories that don't have a saved position
            scrollState.scrollToItem(0, 0)
        }
    }

    // Report scroll position changes to the ViewModel for persistence
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex to scrollState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                onScrollChanged(index, offset)
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp + bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(mediaFiles, key = { it.path }) { item ->
                val isLastPlayed = when (item) {
                    is MediaFile.Video -> lastPlayedPath == item.path
                    is MediaFile.Folder -> isPathInFolder(
                        folderPath = item.path,
                        filePath = lastPlayedPath
                    )

                    is MediaFile.Subtitle -> false
                }
                val isSelected =
                    item is MediaFile.Video && item.path in selectionState.selectedPaths
                val isSelectionMode = selectionState.isActive

                when (item) {
                    is MediaFile.Folder -> FolderItem(
                        item = item,
                        displayFields = displayFields,
                        onClick = { onFolderClick(item) },
                        isLastPlayed = isLastPlayed,
                        enabled = !isSelectionMode,
                        isSelected = false,
                        onLongClick = null
                    )

                    is MediaFile.Video -> VideoItem(
                        item = item,
                        displayFields = displayFields,
                        onClick = {
                            if (isSelectionMode) {
                                onItemToggleSelect(item)
                            } else {
                                onVideoClick(item)
                            }
                        },
                        isLastPlayed = isLastPlayed,
                        enabled = true,
                        isSelected = isSelected,
                        onLongClick = { onItemLongClick(item) }
                    )

                    is MediaFile.Subtitle -> SubtitleItem(
                        item = item,
                        displayFields = displayFields,
                        onClick = { onSubtitleClick(item) },
                        enabled = !isSelectionMode,
                        isSelected = false,
                        onLongClick = null
                    )
                }
            }
        }

        if (mediaFiles.size > 30) {
            FastScrollBar(
                modifier = Modifier.align(Alignment.CenterEnd),
                state = scrollState,
                itemCount = mediaFiles.size
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaFileListPreview() {
    val mockFiles = listOf(
        MediaFile.Folder(
            path = "/root/Movies",
            name = "Movies",
            sourceType = SourceType.Local,
            itemCount = 5
        ),
        MediaFile.Video(
            path = "/root/video1.mp4", name = "video1", sourceType = SourceType.Local, id = "1",
            duration = 300000, size = 1024 * 1024 * 50, lastModified = System.currentTimeMillis(),
            mimeType = "video/mp4", extension = "mp4", width = 1920, height = 1080
        ),
        MediaFile.Subtitle(
            path = "/root/sub.srt", name = "sub", sourceType = SourceType.Local,
            size = 1024 * 10, extension = "srt", lastModified = System.currentTimeMillis()
        )
    )
    AppTheme {
        MediaFileList(
            mediaFiles = mockFiles,
            displayFields = MediaField.entries,
            initialPosition = null,
            lastPlayedPath = "/root/video1.mp4",
            onFolderClick = {},
            onVideoClick = {},
            onSubtitleClick = {},
            bottomContentPadding = 0.dp,
            onScrollChanged = { _, _ -> }
        )
    }
}


private fun isPathInFolder(folderPath: String, filePath: String?): Boolean {
    if (filePath == null) return false
    if (filePath == folderPath) return true
    if (!filePath.startsWith(folderPath)) return false

    if (folderPath.endsWith('/')) return true

    return filePath.getOrNull(folderPath.length) == '/'
}