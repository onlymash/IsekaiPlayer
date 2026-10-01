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

package com.fiepi.media.app.ui.screen.browser

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.window.core.layout.WindowSizeClass
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.AppBackHandler
import com.fiepi.media.app.ui.components.SplitPaneLayout
import com.fiepi.media.app.ui.components.rememberSplitPaneState
import com.fiepi.media.app.ui.navigation.AppNavKey
import com.fiepi.media.app.ui.screen.browser.components.BrowserContentSwitcher
import com.fiepi.media.app.ui.screen.browser.components.BrowserDialogHost
import com.fiepi.media.app.ui.screen.browser.components.BrowserFloatingActionButton
import com.fiepi.media.app.ui.screen.browser.components.BrowserSearchBar
import com.fiepi.media.app.ui.screen.browser.components.HistorySourceFilterBar
import com.fiepi.media.app.ui.screen.browser.components.FileSelectionFloatingToolbar
import com.fiepi.media.app.ui.screen.browser.components.MediaBreadcrumbsBar
import com.fiepi.media.app.ui.screen.browser.components.ModalBrowserDrawer
import com.fiepi.media.app.ui.screen.browser.components.PlaylistBreadcrumbsBar
import com.fiepi.media.app.ui.screen.browser.components.SelectionFloatingActionButton
import com.fiepi.media.app.ui.screen.browser.components.SelectionTopAppBar
import com.fiepi.media.app.ui.screen.browser.components.SplitPaneBrowserDrawer
import com.fiepi.media.app.ui.screen.browser.components.rememberBrowserDialogHostState
import com.fiepi.media.app.ui.screen.browser.components.rememberSearchTextFieldState
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserActions
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserIntent
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserMode
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserViewModel
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.playlist.Playlist
import com.fiepi.media.domain.model.playlist.PlaylistItem
import com.fiepi.media.domain.model.source.SourceType
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point for the Browser Screen. 
 * Manages the connection between the UI and the [BrowserViewModel].
 */
@Composable
fun BrowserScreen(
    onNavigate: (AppNavKey) -> Unit,
    viewModel: BrowserViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val historyPagingItems = viewModel.historyPagingFlow.collectAsLazyPagingItems()
    val playlistPagingItems = viewModel.playlistItemPagingFlow.collectAsLazyPagingItems()

    // UI & Navigation states
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val searchBarState = rememberSearchBarState()
    val scrollState = rememberLazyListState()

    // Synced search text field state
    val textFieldState = rememberSearchTextFieldState(
        query = state.search.query,
        onQueryChange = { query ->
            viewModel.onIntent(BrowserIntent.Config.SearchQueryChange(query))
        }
    )

    // Encapsulated Dialog Host State & Activity Launchers
    val dialogHostState = rememberBrowserDialogHostState(
        onIntent = viewModel::onIntent
    )

    LaunchedEffect(Unit) {
        viewModel.onIntent(BrowserIntent.Storage.OnPermissionResult(Environment.isExternalStorageManager()))
    }

    // System back button handler
    AppBackHandler(
        enabled = state.selection.isActive || (state.interceptBackNavigation && (!state.isAtRoot || state.mode == BrowserMode.History || state.mode == BrowserMode.Playlist) && drawerState.isClosed && searchBarState.currentValue == SearchBarValue.Collapsed)
    ) {
        if (state.selection.isActive) {
            viewModel.onIntent(BrowserIntent.Selection.SetActive(false))
        } else {
            viewModel.onIntent(BrowserIntent.Storage.NavigateBack)
        }
    }

    // --- Actions ---

    val actions =
        remember(viewModel, onNavigate, scope, drawerState, context, dialogHostState) {
            BrowserActions(
                onIntent = { intent ->
                    when (intent) {
                        is BrowserIntent.Playlist.RefreshM3uFile if intent.uri == Uri.EMPTY -> {
                            dialogHostState.launchM3uFilePicker(intent.playlistId)
                        }

                        is BrowserIntent.Playlist.Rename -> {
                            val targetPlaylist =
                                viewModel.state.value.playlists.find { it.id == intent.playlistId }
                                    ?: Playlist(
                                        id = intent.playlistId,
                                        title = intent.newTitle
                                    )
                            viewModel.onIntent(
                                BrowserIntent.Dialog.ShowRenamePlaylist(
                                    targetPlaylist
                                )
                            )
                        }

                        is BrowserIntent.Playlist.Delete -> {
                            viewModel.state.value.playlists.find { it.id == intent.playlistId }
                                ?.let {
                                    viewModel.onIntent(
                                        BrowserIntent.Dialog.ShowDeletePlaylist(
                                            it
                                        )
                                    )
                                }
                        }

                        else -> {
                            viewModel.onIntent(intent)
                        }
                    }
                },
                onRequestPermission = { dialogHostState.requestAllFilesPermission() },
                onCreatePlaylist = { viewModel.onIntent(BrowserIntent.Dialog.ShowCreatePlaylist) },
                onFolderClick = { folder ->
                    viewModel.onIntent(
                        BrowserIntent.Storage.NavigateToFolder(
                            folder.path,
                            folder.sourceType
                        )
                    )
                },
                onMediaBreadcrumbClick = { crumb ->
                    if (crumb.path == viewModel.state.value.mediaNavigationState.currentPath) {
                        scope.launch { scrollState.animateScrollToItem(0) }
                    } else {
                        viewModel.onIntent(
                            BrowserIntent.Storage.NavigateToFolder(
                                crumb.path,
                                crumb.sourceType
                            )
                        )
                    }
                },
                onVideoClick = { video ->
                    onNavigate(
                        AppNavKey.Player(
                            sourceId = state.source.current.id,
                            videoPath = video.path
                        )
                    )
                },
                onSubtitleClick = { subtitle ->
                    val matchingVideo = viewModel.findMatchingVideoForSubtitle(subtitle)
                    if (matchingVideo != null) {
                        onNavigate(
                            AppNavKey.Player(
                                sourceId = state.source.current.id,
                                videoPath = matchingVideo.path
                            )
                        )
                    } else {
                        Toast.makeText(
                            context,
                            context.applicationContext.getString(R.string.browser_subtitle_no_matching_video),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onPlayHistory = { history ->
                    onNavigate(
                        AppNavKey.Player(
                            sourceId = history.sourceId,
                            videoPath = history.path
                        )
                    )
                },
                onPlayPlaylist = { playlistId, initialMediaId ->
                    onNavigate(
                        AppNavKey.PlaylistPlayer(
                            playlistId = playlistId,
                            initialMediaId = initialMediaId
                        )
                    )
                },
                onManageSources = {
                    onNavigate(AppNavKey.SourceManager)
                },
                onSettingsClick = {
                    onNavigate(AppNavKey.Settings)
                },
                onAddRemoteSource = {
                    onNavigate(AppNavKey.SourceEditor(id = null))
                },
                onPlayNetworkStream = {
                    onNavigate(AppNavKey.NetworkStream)
                },
                onSourcesClick = { scope.launch { drawerState.open() } }
            )
        }

    // --- Content ---

    BrowserScreenContent(
        state = state,
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        drawerState = drawerState,
        scrollState = scrollState,
        historyItems = historyPagingItems,
        playlistItems = playlistPagingItems,
        actions = actions
    )

    // --- Dialogs Host ---

    BrowserDialogHost(
        state = state,
        viewModel = viewModel
    )
}

/**
 * Stateless layout for the Browser Screen. 
 * Supports both Modal and Permanent Navigation Drawers based on adaptive layout.
 */
@Composable
fun BrowserScreenContent(
    state: BrowserState,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    actions: BrowserActions,
    scrollState: LazyListState,
    historyItems: LazyPagingItems<PlaybackHistory> = flowOf(PagingData.from(emptyList<PlaybackHistory>())).collectAsLazyPagingItems(),
    playlistItems: LazyPagingItems<PlaylistItem> = flowOf(PagingData.from(emptyList<PlaylistItem>())).collectAsLazyPagingItems(),
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val isExpanded =
        adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val isFabVisible by remember {
        derivedStateOf { scrollBehavior.scrollState.scrollOffset == 0f }
    }

    val splitPaneState = rememberSplitPaneState()

    val screenContent = @Composable {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = if (isExpanded) {
                ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.Right)
            } else {
                ScaffoldDefaults.contentWindowInsets
            },
            floatingActionButtonPosition = if (state.selection.isActive && state.capabilities.canModifyFiles) {
                FabPosition.Center
            } else {
                FabPosition.End
            },
            topBar = {
                AnimatedContent(
                    targetState = state.selection.isActive,
                    label = "TopAppBarSelectionTransition"
                ) { isSelecting ->
                    if (isSelecting) {
                        SelectionTopAppBar(
                            selectedCount = state.selectedCount,
                            onExitSelection = {
                                actions.onIntent(
                                    BrowserIntent.Selection.SetActive(
                                        false
                                    )
                                )
                            },
                            onSelectAll = { actions.onIntent(BrowserIntent.Selection.SelectAll) },
                            onInvertSelection = { actions.onIntent(BrowserIntent.Selection.InvertSelection) }
                        )
                    } else {
                        BrowserSearchBar(
                            state = state,
                            searchBarState = searchBarState,
                            scrollBehavior = scrollBehavior,
                            textFieldState = textFieldState,
                            actions = actions,
                            historyItems = historyItems,
                            playlistItems = playlistItems,
                            hasPermanentDrawer = isExpanded
                        )
                    }
                }
            },
            floatingActionButton = {
                AnimatedContent(
                    targetState = state.selection.isActive to state.capabilities.canModifyFiles,
                    label = "FloatingActionButtonSelectionTransition"
                ) { (isSelectionActive, canModify) ->
                    if (isSelectionActive) {
                        if (canModify) {
                            FileSelectionFloatingToolbar(
                                modifier = Modifier.padding(16.dp),
                                selectedCount = state.selectedCount,
                                isAddToPlaylistEnabled = state.isAddToPlaylistEnabled,
                                onCopyClick = {
                                    actions.onIntent(BrowserIntent.FileAction.StartCopy(state.selectedMediaFiles))
                                },
                                onMoveClick = {
                                    actions.onIntent(BrowserIntent.FileAction.StartMove(state.selectedMediaFiles))
                                },
                                onRenameClick = {
                                    state.selectedMediaFiles.firstOrNull()?.let { file ->
                                        actions.onIntent(BrowserIntent.FileAction.ShowRename(file))
                                    }
                                },
                                onDeleteClick = {
                                    actions.onIntent(BrowserIntent.FileAction.ShowDelete)
                                },
                                onAddToPlaylistClick = {
                                    actions.onIntent(BrowserIntent.Selection.OpenAddToPlaylist)
                                }
                            )
                        } else {
                            SelectionFloatingActionButton(
                                modifier = Modifier.padding(16.dp),
                                isVisible = true,
                                isAddToPlaylistEnabled = state.isAddToPlaylistEnabled,
                                onAddToPlaylistClick = {
                                    actions.onIntent(BrowserIntent.Selection.OpenAddToPlaylist)
                                }
                            )
                        }
                    } else {
                        BrowserFloatingActionButton(
                            isVisible = isFabVisible,
                            onAddRemoteSource = actions.onAddRemoteSource,
                            onPlayNetworkStream = actions.onPlayNetworkStream,
                            onCreatePlaylist = actions.onCreatePlaylist
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) { contentPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val bottomContentPadding = contentPadding.calculateBottomPadding()
            val contentShape = MaterialTheme.shapes.large.copy(
                bottomStart = CornerSize(0f),
                bottomEnd = CornerSize(0f)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        top = contentPadding.calculateTopPadding()
                    )
            ) {
                // Animated header filter / breadcrumbs bar
                AnimatedVisibility(
                    visible = state.mode == BrowserMode.Playlist || state.mode == BrowserMode.History || state.source.permissionGranted || !state.isAtRoot || state.sourceType != SourceType.Local,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    when (state.mode) {
                        BrowserMode.Playlist -> {
                            val rootTitle = stringResource(R.string.playlist_title)
                            val playlistBreadcrumbs = remember(
                                rootTitle,
                                state.selectedPlaylistId,
                                state.playlistState
                            ) {
                                state.playlistBreadcrumbs(rootTitle)
                            }
                            PlaylistBreadcrumbsBar(
                                breadcrumbs = playlistBreadcrumbs,
                                selectedPlaylistId = state.selectedPlaylistId,
                                onBreadcrumbClick = { crumb ->
                                    actions.onIntent(
                                        BrowserIntent.Playlist.Select(
                                            crumb.playlistId
                                        )
                                    )
                                }
                            )
                        }

                        BrowserMode.History -> {
                            HistorySourceFilterBar(
                                sources = state.source.available,
                                selectedSourceId = state.selectedHistorySourceId,
                                onSourceSelected = { sourceId ->
                                    actions.onIntent(
                                        BrowserIntent.History.FilterBySource(
                                            sourceId
                                        )
                                    )
                                }
                            )
                        }

                        else -> {
                            MediaBreadcrumbsBar(
                                breadcrumbs = state.mediaNavigationState.breadcrumbs,
                                currentPath = state.mediaNavigationState.currentPath,
                                onBreadcrumbClick = actions.onMediaBreadcrumbClick
                            )
                        }
                    }
                }

                // Main content area
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { actions.onIntent(BrowserIntent.Storage.Refresh) },
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(contentShape)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = contentShape
                            ),
                    ) {
                        BrowserContentSwitcher(
                            state = state,
                            historyItems = historyItems,
                            playlistItems = playlistItems,
                            actions = actions,
                            scrollState = scrollState,
                            bottomContentPadding = bottomContentPadding
                        )
                    }
                }
            }
        }
    }

    if (isExpanded) {
        SplitPaneLayout(
            state = splitPaneState,
            leftContent = {
                SplitPaneBrowserDrawer(
                    modifier = Modifier.fillMaxSize(),
                    availableSources = state.source.available,
                    currentSource = state.source.current,
                    currentMode = state.mode,
                    onSourceSelected = { source ->
                        actions.onIntent(BrowserIntent.Source.Switch(source))
                    },
                    onHistoryClick = {
                        actions.onIntent(BrowserIntent.History.SwitchToHistory)
                    },
                    onPlaylistClick = {
                        actions.onIntent(BrowserIntent.Playlist.SwitchToPlaylist)
                    },
                    onManageSources = actions.onManageSources,
                    onSettingsClick = actions.onSettingsClick
                )
            },
            rightContent = {
                screenContent()
            }
        )
    } else {
        val scope = rememberCoroutineScope()
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalBrowserDrawer(
                    drawerState = drawerState,
                    availableSources = state.source.available,
                    currentSource = state.source.current,
                    currentMode = state.mode,
                    onSourceSelected = { source ->
                        scope.launch { drawerState.close() }
                        actions.onIntent(BrowserIntent.Source.Switch(source))
                    },
                    onHistoryClick = {
                        scope.launch { drawerState.close() }
                        actions.onIntent(BrowserIntent.History.SwitchToHistory)
                    },
                    onPlaylistClick = {
                        scope.launch { drawerState.close() }
                        actions.onIntent(BrowserIntent.Playlist.SwitchToPlaylist)
                    },
                    onManageSources = actions.onManageSources,
                    onSettingsClick = actions.onSettingsClick
                )
            },
            content = screenContent
        )
    }
}
