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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.PlaylistPlay
import androidx.compose.material.icons.twotone.ArrowDownward
import androidx.compose.material.icons.twotone.ArrowUpward
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BrowserItem
import com.fiepi.media.app.ui.components.MetadataTag
import com.fiepi.media.app.ui.components.StyledDropdownMenu
import com.fiepi.media.app.ui.components.StyledDropdownMenuItem
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.domain.model.playlist.Playlist
import com.fiepi.media.domain.model.playlist.PlaylistType

@Composable
fun PlaylistList(
    modifier: Modifier = Modifier,
    playlists: List<Playlist>,
    onSelectPlaylist: (String) -> Unit,
    onRenameClick: (Playlist) -> Unit,
    onDeleteClick: (Playlist) -> Unit,
    bottomContentPadding: Dp,
    onReorderPlaylists: (List<String>) -> Unit = {},
    refreshingPlaylistIds: Set<String> = emptySet(),
    onRefreshClick: (Playlist) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 8.dp + bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(
            items = playlists,
            key = { _, playlist -> playlist.id }
        ) { index, playlist ->
            PlaylistItem(
                modifier = Modifier.animateItem(),
                playlist = playlist,
                isRefreshing = playlist.id in refreshingPlaylistIds,
                isFirst = index == 0,
                isLast = index == playlists.size - 1,
                onClick = { onSelectPlaylist(playlist.id) },
                onRenameClick = { onRenameClick(playlist) },
                onDeleteClick = { onDeleteClick(playlist) },
                onRefreshClick = { onRefreshClick(playlist) },
                onMoveUpClick = {
                    if (index > 0) {
                        val mutable = playlists.toMutableList()
                        val temp = mutable[index]
                        mutable[index] = mutable[index - 1]
                        mutable[index - 1] = temp
                        onReorderPlaylists(mutable.map { it.id })
                    }
                },
                onMoveDownClick = {
                    if (index < playlists.size - 1) {
                        val mutable = playlists.toMutableList()
                        val temp = mutable[index]
                        mutable[index] = mutable[index + 1]
                        mutable[index + 1] = temp
                        onReorderPlaylists(mutable.map { it.id })
                    }
                }
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    isRefreshing: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onMoveUpClick: () -> Unit,
    onMoveDownClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    BrowserItem(
        modifier = modifier,
        title = playlist.title,
        onClick = rememberHapticClickHandler(onClick),
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(width = 80.dp, height = 60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.TwoTone.PlaylistPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        tags = {
            val typeTagRes = when (playlist.type) {
                PlaylistType.M3U_LINK -> R.string.playlist_type_m3u_link
                PlaylistType.M3U_FILE -> R.string.playlist_type_m3u_file
                PlaylistType.NORMAL -> null
            }
            typeTagRes?.let { resId ->
                MetadataTag(text = stringResource(resId))
            }
            MetadataTag(
                text = stringResource(R.string.playlist_item_count, playlist.itemCount)
            )
        },
        tailingContent = {
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
            } else {
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
                        if (playlist.isRefreshable) {
                            StyledDropdownMenuItem(
                                text = stringResource(R.string.common_refresh),
                                leadingIcon = Icons.TwoTone.Refresh,
                                onClick = rememberHapticClickHandler {
                                    showMenu = false
                                    onRefreshClick()
                                }
                            )
                        }
                        StyledDropdownMenuItem(
                            text = stringResource(R.string.common_edit),
                            leadingIcon = Icons.TwoTone.Edit,
                            onClick = rememberHapticClickHandler {
                                showMenu = false
                                onRenameClick()
                            }
                        )
                        StyledDropdownMenuItem(
                            text = stringResource(R.string.common_delete),
                            leadingIcon = Icons.TwoTone.Delete,
                            isDanger = true,
                            onClick = rememberHapticClickHandler {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }
        }
    )
}
