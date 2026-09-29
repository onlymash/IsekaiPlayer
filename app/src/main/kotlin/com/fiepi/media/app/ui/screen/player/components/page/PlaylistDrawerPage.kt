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

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.fiepi.media.app.ui.components.DurationTag
import com.fiepi.media.app.ui.components.FastScrollBar
import com.fiepi.media.app.ui.components.MediaFileThumbnail
import com.fiepi.media.app.ui.components.MetadataTag
import com.fiepi.media.app.ui.components.rememberShimmerBrush
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.app.ui.utils.formatDate
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.playlist.PlaylistItem
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.model.MediaPlaybackState

@Composable
fun PlaylistDrawerPage(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    contentPadding: PaddingValues,
    playlistItems: LazyPagingItems<PlaylistItem>? = null,
    scrollState: LazyListState = rememberLazyListState()
) {
    val currentIndex = state.playback.currentIndex
    val isPlaylistActive = state.playback.playlistId != null
    val itemCount =
        if (isPlaylistActive) (playlistItems?.itemCount ?: 0) else state.playback.playlist.size

    LaunchedEffect(currentIndex, itemCount, playlistItems?.loadState?.refresh) {
        if (currentIndex in 0 until itemCount) {
            val isVisible = scrollState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex }
            if (!isVisible) {
                scrollState.scrollToItem(currentIndex)
            }
        }
    }

    val shimmerBrush = rememberShimmerBrush()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = contentPadding,
        ) {
            if (isPlaylistActive && playlistItems != null) {
                items(
                    count = playlistItems.itemCount,
                    key = playlistItems.itemKey { "drawer_playlist_${it.id}" }
                ) { index ->
                    val item = playlistItems[index]
                    val isSelected = index == currentIndex
                    if (item != null) {
                        DrawerVideoItem(
                            modifier = Modifier.animateItem(),
                            index = index,
                            video = item.media,
                            isSelected = isSelected,
                            totalCount = playlistItems.itemCount,
                            onIntent = onIntent
                        )
                    } else {
                        DrawerVideoPlaceholderItem(
                            modifier = Modifier.animateItem(),
                            index = index,
                            isSelected = isSelected,
                            totalCount = playlistItems.itemCount,
                            brush = shimmerBrush
                        )
                    }
                }
            } else {
                itemsIndexed(state.playback.playlist, key = { _, item -> item.id }) { index, item ->
                    DrawerVideoItem(
                        modifier = Modifier.animateItem(),
                        index = index,
                        video = item,
                        isSelected = index == currentIndex,
                        totalCount = state.playback.playlist.size,
                        onIntent = onIntent
                    )
                }
            }
        }

        if (itemCount > 20) {
            FastScrollBar(
                modifier = Modifier.align(Alignment.CenterEnd),
                state = scrollState,
                itemCount = itemCount
            )
        }
    }
}

@Composable
private fun DrawerVideoItem(
    modifier: Modifier = Modifier,
    index: Int,
    video: MediaFile.Video,
    isSelected: Boolean,
    totalCount: Int,
    onIntent: (PlayerIntent) -> Unit
) {
    val context = LocalContext.current
    SegmentedListItem(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        selected = isSelected,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        onClick = {
            if (!isSelected) {
                onIntent(PlayerIntent.PlayAtIndex(index))
            }
        },
        colors = ListItemDefaults.segmentedColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.6f),
            containerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f),

            selectedLeadingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedSupportingContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                alpha = 0.8f
            ),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        shapes = ListItemDefaults.segmentedShapes(index, totalCount),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .heightIn(min = 72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading: Thumbnail
                MediaFileThumbnail(
                    thumbnailUri = video.thumbnailUrl?.toUri(),
                    enableThumbnail = true,
                    icon = Icons.Default.Movie,
                    iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.6f
                    ),
                    overlay = {
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

                Spacer(modifier = Modifier.width(12.dp))

                // Content: Title and Metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(),
                            text = video.name,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        video.extension?.let {
                            MetadataTag(text = it.uppercase())
                        }
                        val size = video.size
                        if (size != null && size > 0) {
                            MetadataTag(text = Formatter.formatFileSize(context, size))
                        }
                        val lastModified = video.lastModified
                        if (lastModified != null) {
                            MetadataTag(text = formatDate(lastModified))
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun DrawerVideoPlaceholderItem(
    modifier: Modifier = Modifier,
    index: Int,
    isSelected: Boolean,
    totalCount: Int,
    brush: Brush = rememberShimmerBrush()
) {
    SegmentedListItem(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        selected = isSelected,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        onClick = {},
        colors = ListItemDefaults.segmentedColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.6f),
            containerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f),
        ),
        shapes = ListItemDefaults.segmentedShapes(index, totalCount),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .heightIn(min = 72.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading: Thumbnail placeholder
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 56.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(brush)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Content: Title & Metadata placeholders
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(18.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(brush)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(14.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .height(14.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(brush)
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true, name = "Playlist Page")
@Composable
fun PlaylistDrawerPagePreview() {
    val mockPlaylist = listOf(
        MediaFile.Video(
            path = "/path/1",
            name = "Video 1",
            sourceType = SourceType.Local,
            id = "1",
            duration = 3600000L,
            size = 1024L * 1024 * 500,
            extension = "mp4",
            lastModified = System.currentTimeMillis()
        ),
        MediaFile.Video(
            path = "/path/2",
            name = "Video 2",
            sourceType = SourceType.Local,
            id = "2",
            duration = 1800000L,
            size = 1024L * 1024 * 250,
            extension = "webm",
            lastModified = System.currentTimeMillis() - 86400000L
        )
    )

    val state = PlayerState(
        playback = MediaPlaybackState(
            playlist = mockPlaylist,
            currentIndex = 1
        )
    )
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f))
                .width(400.dp)
                .fillMaxHeight()
        ) {
            PlaylistDrawerPage(
                state = state,
                onIntent = {},
                contentPadding = PaddingValues(16.dp)
            )
        }
    }
}
