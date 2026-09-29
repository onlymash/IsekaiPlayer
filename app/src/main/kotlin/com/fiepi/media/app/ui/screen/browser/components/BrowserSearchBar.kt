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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.twotone.DeleteSweep
import androidx.compose.material.icons.twotone.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BackIconButton
import com.fiepi.media.app.ui.components.StatusView
import com.fiepi.media.app.ui.components.StyledDropdownMenu
import com.fiepi.media.app.ui.components.StyledDropdownMenuItem
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserActions
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserIntent
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserMode
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.PlaylistBrowserState
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.playlist.PlaylistItem
import kotlinx.coroutines.launch

/**
 * A dedicated search bar component for the Browser Screen.
 * Encapsulates both the collapsed [AppBarWithSearch] and the [ExpandedFullScreenSearchBar].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSearchBar(
    state: BrowserState,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    scrollBehavior: SearchBarScrollBehavior,
    actions: BrowserActions,
    modifier: Modifier = Modifier,
    historyItems: LazyPagingItems<PlaybackHistory>? = null,
    playlistItems: LazyPagingItems<PlaylistItem>? = null,
    hasPermanentDrawer: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val collapsedTextFieldState = rememberTextFieldState()

    val horizontalInsets = if (hasPermanentDrawer) {
        SearchBarDefaults.windowInsets.only(WindowInsetsSides.Right)
    } else {
        SearchBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal)
    }

    val inputFieldColors = SearchBarDefaults.inputFieldColors(
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface,
    )

    val collapsedInputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = collapsedTextFieldState,
            searchBarState = searchBarState,
            onSearch = { },
            readOnly = true,
            placeholder = {
                Text(stringResource(R.string.browser_search_placeholder_collapsed))
            },
            leadingIcon = {
                if (hasPermanentDrawer) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.browser_menu_search)
                    )
                } else {
                    IconButton(onClick = actions.onSourcesClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.browser_menu_sources)
                        )
                    }
                }
            },
            trailingIcon = {
                var isHistoryMenuExpanded by remember { mutableStateOf(false) }
                var showClearAllDialog by remember { mutableStateOf(false) }

                Row {
                    if (state.mode == BrowserMode.Storage) {
                        IconButton(onClick = { actions.onIntent(BrowserIntent.Config.ShowSortDialog) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.browser_menu_sort)
                            )
                        }
                    } else if (state.mode == BrowserMode.History) {
                        Box {
                            IconButton(onClick = { isHistoryMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.common_more)
                                )
                            }

                            StyledDropdownMenu(
                                expanded = isHistoryMenuExpanded,
                                onDismissRequest = { isHistoryMenuExpanded = false }
                            ) {
                                StyledDropdownMenuItem(
                                    text = stringResource(R.string.playback_history_clear_all),
                                    onClick = {
                                        isHistoryMenuExpanded = false
                                        showClearAllDialog = true
                                    },
                                    leadingIcon = Icons.TwoTone.DeleteSweep,
                                    isDanger = true
                                )
                            }
                        }

                        if (showClearAllDialog) {
                            AlertDialog(
                                onDismissRequest = { showClearAllDialog = false },
                                title = { Text(stringResource(R.string.playback_history_clear_dialog_title)) },
                                text = { Text(stringResource(R.string.playback_history_clear_dialog_message)) },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            actions.onIntent(BrowserIntent.History.ClearAll)
                                            showClearAllDialog = false
                                        }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.playback_history_clear_all),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearAllDialog = false }) {
                                        Text(stringResource(R.string.common_cancel))
                                    }
                                }
                            )
                        }
                    }
                }
            },
            colors = inputFieldColors
        )
    }

    val expandedInputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            readOnly = false,
            placeholder = {
                val placeholderRes = when (state.mode) {
                    BrowserMode.History -> R.string.playback_history_search_placeholder_expanded
                    BrowserMode.Playlist -> R.string.playlist_search_placeholder_expanded
                    BrowserMode.Storage -> R.string.browser_search_placeholder_expanded
                }
                Text(stringResource(placeholderRes))
            },
            leadingIcon = {
                BackIconButton(
                    onClick = { scope.launch { searchBarState.animateToCollapsed() } },
                    enableTonal = false
                )
            },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            colors = inputFieldColors
        )
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.windowInsetsPadding(
                SearchBarDefaults.windowInsets.only(
                    WindowInsetsSides.Top
                )
            )
        ) {
            AppBarWithSearch(
                state = searchBarState,
                inputField = collapsedInputField,
                scrollBehavior = scrollBehavior,
                windowInsets = horizontalInsets,
                colors = SearchBarDefaults.appBarWithSearchColors(
                    scrolledSearchBarContainerColor = Color.Transparent,
                    scrolledAppBarContainerColor = Color.Transparent,
                    appBarContainerColor = Color.Transparent,
                )
            )
        }
        // Full-screen search mode
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = expandedInputField,
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            val search = state.search
            val isHistoryMode = state.mode == BrowserMode.History
            val isPlaylistMode = state.mode == BrowserMode.Playlist
            val playlistDetail =
                (state.playlistState as? PlaylistBrowserState.DetailContent)?.playlist

            val isQueryBlank = search.query.isBlank()

            val hasResults = when {
                isQueryBlank -> false
                isHistoryMode -> historyItems != null && historyItems.itemCount > 0
                isPlaylistMode -> playlistItems != null && playlistItems.itemCount > 0
                else -> search.results.isNotEmpty()
            }
            val isHistoryLoading =
                isHistoryMode && !isQueryBlank && historyItems?.loadState?.refresh is LoadState.Loading
            val isPlaylistLoading =
                isPlaylistMode && !isQueryBlank && playlistItems?.loadState?.refresh is LoadState.Loading
            val isSearchLoading = when {
                isQueryBlank -> false
                isHistoryMode -> isHistoryLoading
                isPlaylistMode -> isPlaylistLoading
                else -> search.isLoading
            }

            AnimatedContent(
                targetState = when {
                    isQueryBlank -> "idle"
                    isSearchLoading && !hasResults -> "loading"
                    !isSearchLoading && !hasResults -> "empty"
                    else -> "results"
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SearchContentTransition"
            ) { targetState ->
                when (targetState) {
                    "idle" -> {
                        StatusView(
                            icon = Icons.Default.Search,
                            message = stringResource(R.string.browser_search_prompt),
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    "loading" -> MediaFileSkeletonList()

                    "empty" -> {
                        StatusView(
                            icon = Icons.TwoTone.SearchOff,
                            message = if (isHistoryMode) stringResource(R.string.playback_history_empty) else stringResource(
                                R.string.browser_media_list_empty
                            ),
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    "results" -> {
                        if (isHistoryMode && historyItems != null) {
                            HistorySearchList(
                                historyItems = historyItems,
                                searchQuery = search.query,
                                localDisplayFields = state.localDisplayFields,
                                remoteDisplayFields = state.remoteDisplayFields,
                                onHistoryClick = { history ->
                                    scope.launch { searchBarState.animateToCollapsed() }
                                    actions.onPlayHistory(history)
                                },
                                onReplayHistoryClick = { history ->
                                    scope.launch { searchBarState.animateToCollapsed() }
                                    actions.onIntent(BrowserIntent.History.Replay(history) {})
                                },
                                onDeleteHistoryClick = { history ->
                                    actions.onIntent(
                                        BrowserIntent.History.Delete(
                                            history.sourceId,
                                            history.path
                                        )
                                    )
                                }
                            )
                        } else if (isPlaylistMode && playlistItems != null) {
                            val itemsList =
                                (0 until playlistItems.itemCount).mapNotNull { playlistItems[it] }
                            MediaSearchList(
                                results = itemsList.map { it.media },
                                searchQuery = search.query,
                                localDisplayFields = state.localDisplayFields,
                                remoteDisplayFields = state.remoteDisplayFields,
                                onVideoClick = { video ->
                                    scope.launch { searchBarState.animateToCollapsed() }
                                    val targetItem = itemsList.find { it.media.id == video.id }
                                    val targetPlaylistId =
                                        targetItem?.playlistId ?: playlistDetail?.id ?: ""
                                    actions.onPlayPlaylist(
                                        targetPlaylistId,
                                        video.id
                                    )
                                }
                            )
                        } else {
                            MediaSearchList(
                                results = search.results,
                                searchQuery = search.query,
                                localDisplayFields = state.localDisplayFields,
                                remoteDisplayFields = state.remoteDisplayFields,
                                onVideoClick = { video ->
                                    scope.launch { searchBarState.animateToCollapsed() }
                                    actions.onVideoClick(video)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Remembers a [TextFieldState] synchronized with an external search query state.
 */
@Composable
fun rememberSearchTextFieldState(
    query: String,
    onQueryChange: (String) -> Unit
): TextFieldState {
    val textFieldState = rememberTextFieldState(initialText = query)

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { text ->
                if (text != query) {
                    onQueryChange(text)
                }
            }
    }

    LaunchedEffect(query) {
        if (query.isEmpty() && textFieldState.text.isNotEmpty()) {
            textFieldState.setTextAndPlaceCursorAtEnd("")
        }
    }

    return textFieldState
}
