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

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.StatusView
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserActions
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserIntent
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserMode
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.HistoryBrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.MediaBrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.PlaylistBrowserState
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.playlist.PlaylistItem
import com.fiepi.media.domain.model.playlist.PlaylistType

/**
 * Maps the current load state of history Paging 3 items into a [HistoryBrowserState].
 */
@Composable
fun rememberHistoryBrowserState(
    historyItems: LazyPagingItems<PlaybackHistory>,
    selectedSourceId: String?,
    query: String
): HistoryBrowserState {
    val isLocalInspection = LocalInspectionMode.current
    return remember(
        historyItems.loadState.refresh,
        historyItems.itemCount,
        selectedSourceId,
        query,
        isLocalInspection
    ) {
        if (isLocalInspection) {
            if (historyItems.itemCount == 0) HistoryBrowserState.Empty
            else HistoryBrowserState.Content(selectedSourceId, query)
        } else {
            when (val refreshState = historyItems.loadState.refresh) {
                is LoadState.Loading -> if (historyItems.itemCount == 0) HistoryBrowserState.Skeleton else HistoryBrowserState.Content(
                    selectedSourceId,
                    query
                )

                is LoadState.Error -> HistoryBrowserState.Error(
                    refreshState.error.localizedMessage ?: "Error loading history"
                )

                is LoadState.NotLoading -> if (historyItems.itemCount == 0) HistoryBrowserState.Empty else HistoryBrowserState.Content(
                    selectedSourceId,
                    query
                )
            }
        }
    }
}

/**
 * Handles animated transitions across Storage Mode, History Mode, and all Playlist states.
 * Reuses StatusView for all empty/error states and applies smooth transition animations.
 */
@Composable
fun BrowserContentSwitcher(
    state: BrowserState,
    historyItems: LazyPagingItems<PlaybackHistory>,
    playlistItems: LazyPagingItems<PlaylistItem>,
    actions: BrowserActions,
    scrollState: LazyListState,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val historyState = rememberHistoryBrowserState(
        historyItems = historyItems,
        selectedSourceId = state.selectedHistorySourceId,
        query = state.search.query
    )

    val currentTarget = when (state.mode) {
        BrowserMode.Storage -> state.mediaState
        BrowserMode.History -> historyState
        BrowserMode.Playlist -> state.playlistState
    }

    AnimatedContent(
        targetState = currentTarget,
        transitionSpec = {
            if (initialState is PlaylistBrowserState.ListContent && (targetState is PlaylistBrowserState.DetailContent || targetState is PlaylistBrowserState.DetailEmpty)) {
                // Level 1 -> Level 2: Slide in from right
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            } else if ((initialState is PlaylistBrowserState.DetailContent || initialState is PlaylistBrowserState.DetailEmpty) && targetState is PlaylistBrowserState.ListContent) {
                // Level 2 -> Level 1: Slide in from left
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            } else {
                // Mode switches & Loading -> Content: Fade + Scale
                (fadeIn(tween()) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween()
                )) togetherWith fadeOut(tween())
            }
        },
        contentKey = {
            when (it) {
                is PlaylistBrowserState.ListContent -> "playlist_list_content"
                is PlaylistBrowserState.DetailContent -> "playlist_detail_${it.playlist.id}"
                is PlaylistBrowserState.DetailEmpty -> "playlist_detail_empty_${it.playlistId}"
                is PlaylistBrowserState.Empty -> "playlist_empty"
                is PlaylistBrowserState.Skeleton -> "playlist_skeleton"
                is HistoryBrowserState.Skeleton, is MediaBrowserState.Skeleton -> "skeleton"
                is HistoryBrowserState.Empty -> "history_empty"
                is MediaBrowserState.Empty, is MediaBrowserState.Error -> "media_status"
                is HistoryBrowserState.Error -> "history_error"
                is MediaBrowserState.Permission -> "permission"
                is MediaBrowserState.Content -> it.path
                is HistoryBrowserState.Content -> "history_content"
                else -> it::class
            }
        },
        label = "BrowserContentSwitcherTransition",
        modifier = modifier.fillMaxSize()
    ) { target ->
        when (target) {
            // --- Playlist States ---
            PlaylistBrowserState.Skeleton -> {
                MediaFileSkeletonList(
                    bottomContentPadding = bottomContentPadding
                )
            }

            PlaylistBrowserState.Empty -> {
                StatusView(
                    modifier = Modifier.padding(bottom = bottomContentPadding),
                    icon = Icons.AutoMirrored.TwoTone.PlaylistPlay,
                    message = stringResource(R.string.playlist_empty),
                    actionText = stringResource(R.string.playlist_create_dialog_title),
                    onAction = actions.onCreatePlaylist,
                    actionIcon = Icons.Default.Add,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            is PlaylistBrowserState.ListContent -> {
                PlaylistList(
                    playlists = target.playlists,
                    onSelectPlaylist = { id -> actions.onIntent(BrowserIntent.Playlist.Select(id)) },
                    onRenameClick = { playlist ->
                        actions.onIntent(
                            BrowserIntent.Playlist.Rename(
                                playlist.id,
                                playlist.title
                            )
                        )
                    },
                    onDeleteClick = { playlist ->
                        actions.onIntent(
                            BrowserIntent.Playlist.Delete(
                                playlist.id
                            )
                        )
                    },
                    onReorderPlaylists = { ids ->
                        actions.onIntent(
                            BrowserIntent.Playlist.Reorder(
                                ids
                            )
                        )
                    },
                    onRefreshClick = { playlist ->
                        if (playlist.type == PlaylistType.M3U_FILE) {
                            actions.onIntent(
                                BrowserIntent.Playlist.RefreshM3uFile(
                                    playlist.id,
                                    Uri.EMPTY
                                )
                            )
                        } else {
                            actions.onIntent(BrowserIntent.Playlist.Refresh(playlist.id))
                        }
                    },
                    refreshingPlaylistIds = state.refreshingPlaylistIds,
                    bottomContentPadding = bottomContentPadding
                )
            }

            is PlaylistBrowserState.DetailEmpty -> {
                StatusView(
                    modifier = Modifier.padding(bottom = bottomContentPadding),
                    icon = Icons.TwoTone.VideoLibrary,
                    message = stringResource(R.string.playlist_items_empty),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            is PlaylistBrowserState.DetailContent -> {
                PlaylistDetails(
                    playlist = target.playlist,
                    playlistItems = playlistItems,
                    localDisplayFields = state.localDisplayFields,
                    remoteDisplayFields = state.remoteDisplayFields,
                    onPlayAll = {
                        actions.onPlayPlaylist(
                            target.playlist.id,
                            null
                        )
                    },
                    onItemClick = { media ->
                        actions.onPlayPlaylist(
                            target.playlist.id,
                            media.id
                        )
                    },
                    onRemoveItem = { item -> actions.onIntent(BrowserIntent.Playlist.RemoveItem(item.id)) },
                    onSwapItems = { itemId1, itemId2 ->
                        actions.onIntent(
                            BrowserIntent.Playlist.SwapItems(
                                playlistId = target.playlist.id,
                                itemId1 = itemId1,
                                itemId2 = itemId2
                            )
                        )
                    },
                    isRefreshing = target.playlist.id in state.refreshingPlaylistIds,
                    bottomContentPadding = bottomContentPadding
                )
            }

            is PlaylistBrowserState.Error -> {
                StatusView(
                    modifier = Modifier.padding(bottom = bottomContentPadding),
                    icon = Icons.Default.ErrorOutline,
                    message = target.message,
                    actionText = stringResource(R.string.common_retry),
                    onAction = { actions.onIntent(BrowserIntent.Storage.Refresh) },
                    actionIcon = Icons.Default.Refresh,
                    iconTint = MaterialTheme.colorScheme.error
                )
            }

            // --- History States ---
            is HistoryBrowserState.Skeleton -> {
                MediaFileSkeletonList(
                    bottomContentPadding = bottomContentPadding
                )
            }

            is HistoryBrowserState.Empty -> {
                StatusView(
                    modifier = Modifier.padding(bottom = bottomContentPadding),
                    icon = Icons.Default.History,
                    message = stringResource(R.string.playback_history_empty),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            is HistoryBrowserState.Error -> {
                StatusView(
                    modifier = Modifier.padding(bottom = bottomContentPadding),
                    icon = Icons.Default.ErrorOutline,
                    message = target.message,
                    actionText = stringResource(R.string.common_retry),
                    onAction = { actions.onIntent(BrowserIntent.Storage.Refresh) },
                    actionIcon = Icons.Default.Refresh,
                    iconTint = MaterialTheme.colorScheme.error
                )
            }

            is HistoryBrowserState.Content -> {
                PlaybackHistoryList(
                    historyItems = historyItems,
                    localDisplayFields = state.localDisplayFields,
                    remoteDisplayFields = state.remoteDisplayFields,
                    onPlayHistory = actions.onPlayHistory,
                    onReplayHistory = { history ->
                        actions.onIntent(BrowserIntent.History.Replay(history) {})
                    },
                    onDeleteHistory = { sourceId, path ->
                        actions.onIntent(BrowserIntent.History.Delete(sourceId, path))
                    },
                    bottomContentPadding = bottomContentPadding,
                    scrollState = scrollState
                )
            }

            // --- Media Storage States ---
            is MediaBrowserState.Error, MediaBrowserState.Empty -> {
                val isError = target is MediaBrowserState.Error
                StatusView(
                    modifier = Modifier.padding(bottom = bottomContentPadding),
                    icon = if (isError) Icons.Default.ErrorOutline else Icons.Default.Search,
                    message = if (target is MediaBrowserState.Error) target.message else stringResource(
                        R.string.browser_media_list_empty
                    ),
                    actionText = if (isError) stringResource(R.string.common_retry) else null,
                    onAction = if (isError) {
                        { actions.onIntent(BrowserIntent.Storage.Refresh) }
                    } else null,
                    actionIcon = if (isError) {
                        Icons.Default.Refresh
                    } else {
                        null
                    },
                    iconTint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            MediaBrowserState.Skeleton -> {
                MediaFileSkeletonList(
                    bottomContentPadding = bottomContentPadding
                )
            }

            MediaBrowserState.Permission -> {
                StatusView(
                    modifier = Modifier.padding(bottom = bottomContentPadding),
                    icon = Icons.TwoTone.Folder,
                    message = stringResource(R.string.browser_permission_required),
                    actionText = stringResource(R.string.browser_permission_grant),
                    onAction = actions.onRequestPermission,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            is MediaBrowserState.Content -> {
                MediaFileList(
                    mediaFiles = target.files,
                    displayFields = state.displayFields,
                    initialPosition = target.scrollPosition,
                    lastPlayedPath = state.lastPlayedUri,
                    selectionState = state.selection,
                    onItemToggleSelect = { media ->
                        actions.onIntent(BrowserIntent.Selection.ToggleSelect(media.path))
                    },
                    onItemLongClick = { media ->
                        if (!state.selection.isActive) {
                            actions.onIntent(BrowserIntent.Selection.SetActive(true))
                        }
                        actions.onIntent(BrowserIntent.Selection.ToggleSelect(media.path))
                    },
                    onFolderClick = actions.onFolderClick,
                    onVideoClick = actions.onVideoClick,
                    onSubtitleClick = actions.onSubtitleClick,
                    onScrollChanged = { index, offset ->
                        actions.onIntent(
                            BrowserIntent.Storage.SaveScrollPosition(
                                index,
                                offset
                            )
                        )
                    },
                    bottomContentPadding = bottomContentPadding,
                    scrollState = scrollState
                )
            }
        }
    }
}
