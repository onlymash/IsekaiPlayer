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

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.twotone.ArrowDownward
import androidx.compose.material.icons.twotone.ArrowUpward
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material.icons.twotone.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BrowserItem
import com.fiepi.media.app.ui.components.BrowserItemPlaceholder
import com.fiepi.media.app.ui.components.DurationTag
import com.fiepi.media.app.ui.components.FastScrollBar
import com.fiepi.media.app.ui.components.MediaFileThumbnail
import com.fiepi.media.app.ui.components.MetadataTag
import com.fiepi.media.app.ui.components.StyledDropdownMenu
import com.fiepi.media.app.ui.components.StyledDropdownMenuItem
import com.fiepi.media.app.ui.components.rememberShimmerBrush
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.playlist.Playlist
import com.fiepi.media.domain.model.playlist.PlaylistItem
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.source.SourceType

@Composable
fun PlaylistDetails(
    modifier: Modifier = Modifier,
    scrollState: LazyListState = rememberLazyListState(),
    playlist: Playlist,
    playlistItems: LazyPagingItems<PlaylistItem>,
    localDisplayFields: List<MediaField> = emptyList(),
    remoteDisplayFields: List<MediaField> = emptyList(),
    onPlayAll: () -> Unit = {},
    onItemClick: (MediaFile.Video) -> Unit = {},
    onRemoveItem: (PlaylistItem) -> Unit = {},
    onSwapItems: (itemId1: String, itemId2: String) -> Unit = { _, _ -> },
    isRefreshing: Boolean = false,
    bottomContentPadding: Dp = 0.dp
) {
    val shimmerBrush = rememberShimmerBrush()

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
            // Header Banner with "Play All" and "Refresh" buttons
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.TwoTone.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.playlist_item_count,
                                    playlist.itemCount
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isRefreshing) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Button(
                            onClick = rememberHapticClickHandler(onPlayAll),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.PlayArrow,
                                contentDescription = stringResource(R.string.playlist_play_all)
                            )
                        }
                    }
                }
            }

            // List of videos using reusable BrowserItem layout
            items(
                count = playlistItems.itemCount,
                key = playlistItems.itemKey { it.id }
            ) { index ->
                val item = playlistItems[index]
                val prevItem = if (index > 0) playlistItems[index - 1] else null
                val nextItem =
                    if (index < playlistItems.itemCount - 1) playlistItems[index + 1] else null

                if (item != null) {
                    PlaylistMediaItem(
                        modifier = Modifier.animateItem(),
                        item = item,
                        localDisplayFields = localDisplayFields,
                        remoteDisplayFields = remoteDisplayFields,
                        isEditable = playlist.isEditable,
                        isFirst = index == 0,
                        isLast = index == playlistItems.itemCount - 1,
                        onClick = { onItemClick(item.media) },
                        onRemoveClick = { onRemoveItem(item) },
                        onMoveUpClick = {
                            val prevId = prevItem?.id
                            if (prevId != null) {
                                onSwapItems(item.id, prevId)
                            }
                        },
                        onMoveDownClick = {
                            val nextId = nextItem?.id
                            if (nextId != null) {
                                onSwapItems(item.id, nextId)
                            }
                        }
                    )
                } else {
                    BrowserItemPlaceholder(
                        modifier = Modifier.animateItem(),
                        brush = shimmerBrush
                    )
                }
            }
        }

        if (playlistItems.itemCount > 30) {
            FastScrollBar(
                modifier = Modifier.align(Alignment.CenterEnd),
                state = scrollState,
                itemCount = playlistItems.itemCount
            )
        }
    }
}

@Composable
private fun PlaylistMediaItem(
    modifier: Modifier = Modifier,
    item: PlaylistItem,
    localDisplayFields: List<MediaField> = emptyList(),
    remoteDisplayFields: List<MediaField> = emptyList(),
    isEditable: Boolean = true,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onMoveUpClick: () -> Unit = {},
    onMoveDownClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val video = item.media
    var showMenu by remember { mutableStateOf(false) }

    val isRemote = video.sourceType.isRemote
    val displayFields = if (isRemote) remoteDisplayFields else localDisplayFields

    BrowserItem(
        modifier = modifier,
        title = video.name,
        onClick = onClick,
        leadingContent = {
            MediaFileThumbnail(
                thumbnailUri = video.thumbnailUrl?.toUri(),
                enableThumbnail = displayFields.contains(MediaField.Thumbnail),
                icon = Icons.Default.Movie,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                contentScale = if (video.thumbnailUrl?.startsWith("http") == true) ContentScale.Fit else ContentScale.Crop,
                overlay = {
                    // Duration badge at bottom-right of thumbnail
                    video.duration?.let { duration ->
                        if (duration > 0) {
                            DurationTag(
                                duration = duration,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }
                }
            )
        },
        tailingContent = if (isEditable) {
            {
                Box {
                    IconButton(onClick = rememberHapticClickHandler { showMenu = true }) {
                        Icon(
                            imageVector = Icons.TwoTone.MoreVert,
                            contentDescription = stringResource(R.string.common_more)
                        )
                    }
                    StyledDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!isFirst) {
                            StyledDropdownMenuItem(
                                text = stringResource(R.string.playlist_move_up),
                                leadingIcon = Icons.TwoTone.ArrowUpward,
                                onClick = rememberHapticClickHandler {
                                    showMenu = false
                                    onMoveUpClick()
                                }
                            )
                        }
                        if (!isLast) {
                            StyledDropdownMenuItem(
                                text = stringResource(R.string.playlist_move_down),
                                leadingIcon = Icons.TwoTone.ArrowDownward,
                                onClick = rememberHapticClickHandler {
                                    showMenu = false
                                    onMoveDownClick()
                                }
                            )
                        }
                        StyledDropdownMenuItem(
                            text = stringResource(R.string.common_delete),
                            leadingIcon = Icons.TwoTone.Delete,
                            isDanger = true,
                            onClick = rememberHapticClickHandler {
                                showMenu = false
                                onRemoveClick()
                            }
                        )
                    }
                }
            }
        } else null,
        tags = {
            // Primary focus for playlists: Display Source / Protocol Origin
            val sourceText = when (video.sourceType) {
                SourceType.Local -> stringResource(R.string.source_internal_storage_name)
                SourceType.External -> stringResource(R.string.source_local_external_desc)
                SourceType.Smb -> "SMB"
                SourceType.Ftp -> "FTP"
                SourceType.WebDav -> "WebDAV"
                SourceType.Stream -> "Stream"
            }
            MetadataTag(text = sourceText)

            video.extension?.let { ext ->
                MetadataTag(text = ext.uppercase())
            }
            video.size?.let { size ->
                if (size > 0) {
                    MetadataTag(text = Formatter.formatFileSize(context, size))
                }
            }
        }
    )
}
