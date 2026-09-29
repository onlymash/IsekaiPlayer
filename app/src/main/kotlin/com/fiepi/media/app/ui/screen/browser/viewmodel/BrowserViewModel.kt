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

package com.fiepi.media.app.ui.screen.browser.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fiepi.media.app.R
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.playlist.PlaylistItem
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.usecases.BrowserUseCases
import com.fiepi.media.domain.utils.SubtitleUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for the Browser Screen, managing media file listing, navigation, and source selection.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class BrowserViewModel(
    private val application: Application,
    private val useCases: BrowserUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    // Internal reactive triggers
    private val _currentPath = MutableStateFlow<String?>(null)
    private val _permissionGranted = MutableStateFlow(false)
    private val _refreshTrigger = MutableStateFlow(RefreshSignal(0L, false))
    private val _searchQuery = MutableStateFlow("")
    private val _selectedPlaylistId = MutableStateFlow<String?>(null)

    val historyPagingFlow: Flow<PagingData<PlaybackHistory>> = combine(
        state.map { it.mode == BrowserMode.History }.distinctUntilChanged(),
        state.map { it.selectedHistorySourceId }.distinctUntilChanged(),
        _searchQuery.debounce { if (it.isBlank()) 0L else 300L }.distinctUntilChanged()
    ) { isHistoryMode, sourceId, query ->
        HistoryPagingParams(isHistoryMode, sourceId, query)
    }.flatMapLatest { params ->
        if (!params.isHistoryMode) flowOf(PagingData.empty())
        else useCases.getHistoryPaging(
            sourceId = params.sourceId,
            query = params.query.ifBlank { null }
        )
    }.cachedIn(viewModelScope)

    val playlistItemPagingFlow: Flow<PagingData<PlaylistItem>> = combine(
        state.map { it.mode == BrowserMode.Playlist }.distinctUntilChanged(),
        _selectedPlaylistId,
        _searchQuery.debounce { if (it.isBlank()) 0L else 300L }.distinctUntilChanged()
    ) { isPlaylistMode, playlistId, query ->
        PlaylistItemPagingParams(isPlaylistMode, playlistId, query)
    }.flatMapLatest { params ->
        if (!params.isPlaylistMode || (params.playlistId == null && params.query.isBlank())) {
            flowOf(PagingData.empty())
        } else {
            useCases.playlist.getPlaylistItemPaging(
                playlistId = params.playlistId,
                query = params.query.ifBlank { null }
            )
        }
    }.cachedIn(viewModelScope)

    private data class HistoryPagingParams(
        val isHistoryMode: Boolean,
        val sourceId: String?,
        val query: String
    )

    private data class PlaylistItemPagingParams(
        val isPlaylistMode: Boolean,
        val playlistId: String?,
        val query: String
    )

    private data class RefreshSignal(val timestamp: Long, val isManual: Boolean)

    private val scrollPositions = mutableMapOf<String, ScrollPosition>()

    init {
        observeSources()
        observeDisplayFields()
        observeNavigationPreferences()
        observeMediaLoading()
        observeSearchQuery()
        observeLastPlayedInDirectory()
        observePlaylists()
    }

    // --- Reactive State Pipelines ---

    private fun observeSources() {
        viewModelScope.launch {
            combine(
                useCases.getSources(),
                useCases.getSelectedSourceId()
            ) { allSources, selectedId ->
                val currentSource = allSources.find { it.id == selectedId }
                    ?: allSources.firstOrNull()
                    ?: MediaSource.Local(
                        id = MediaSource.INTERNAL_STORAGE_ID,
                        name = application.getString(R.string.source_internal_storage_name),
                        rootPath = "",
                        totalSpace = 0,
                        freeSpace = 0
                    )
                allSources to currentSource
            }.distinctUntilChanged().collect { (allSources, currentSource) ->
                handleSourceChange(allSources, currentSource)
            }
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            combine(
                useCases.playlist.getPlaylists(),
                _selectedPlaylistId.flatMapLatest { id ->
                    if (id == null) flowOf(null)
                    else useCases.playlist.getPlaylist(id)
                }
            ) { playlists, currentPlaylist ->
                val selectedId = _selectedPlaylistId.value
                val playlistState: PlaylistBrowserState = if (selectedId == null) {
                    if (playlists.isEmpty()) PlaylistBrowserState.Empty
                    else PlaylistBrowserState.ListContent(playlists)
                } else {
                    if (currentPlaylist == null) {
                        val matchingPlaylist = playlists.find { it.id == selectedId }
                        if (matchingPlaylist != null) {
                            PlaylistBrowserState.DetailEmpty(selectedId, matchingPlaylist.title)
                        } else {
                            PlaylistBrowserState.Skeleton
                        }
                    } else if (currentPlaylist.itemCount == 0) {
                        PlaylistBrowserState.DetailEmpty(
                            selectedId,
                            currentPlaylist.title
                        )
                    } else {
                        PlaylistBrowserState.DetailContent(currentPlaylist)
                    }
                }
                playlists to playlistState
            }.collect { (playlists, playlistState) ->
                _state.update { currentState ->
                    currentState.copy(
                        playlists = playlists,
                        selectedPlaylistId = _selectedPlaylistId.value,
                        playlistState = playlistState
                    )
                }
            }
        }
    }

    private fun observePlaylistDetail(playlistId: String?) {
        if (_selectedPlaylistId.value != playlistId) {
            _searchQuery.value = ""
            _state.update {
                it.copy(
                    search = it.search.copy(query = "")
                )
            }
            _selectedPlaylistId.value = playlistId
        }
    }

    private fun observeDisplayFields() {
        viewModelScope.launch {
            combine(
                useCases.getDisplayFields(isRemote = false),
                useCases.getDisplayFields(isRemote = true)
            ) { localFields, remoteFields ->
                localFields to remoteFields
            }.collect { (localFields, remoteFields) ->
                _state.update { currentState ->
                    currentState.copy(
                        localDisplayFields = localFields,
                        remoteDisplayFields = remoteFields
                    )
                }
            }
        }
    }

    private fun observeNavigationPreferences() {
        viewModelScope.launch {
            useCases.getInterceptBackNavigation().collect { enabled ->
                _state.update { it.copy(interceptBackNavigation = enabled) }
            }
        }
    }

    private fun observeLastPlayedInDirectory() {
        viewModelScope.launch {
            state.map { it.source.current.id to it.mediaNavigationState.currentPath }
                .distinctUntilChanged()
                .flatMapLatest { (sourceId, path) ->
                    if (path.isBlank()) flow { emit(null) }
                    else useCases.getLastPlayedInDirectory(sourceId, path)
                }
                .collect { lastPlayedUri ->
                    _state.update { it.copy(lastPlayedUri = lastPlayedUri) }
                }
        }
    }

    private fun observeMediaLoading() {
        val optionsFlow = state.map { it.source.current.type != SourceType.Local }
            .distinctUntilChanged()
            .flatMapLatest { isRemote -> useCases.getSortOptions(isRemote) }

        viewModelScope.launch {
            combine(
                state.map { it.source.current }.distinctUntilChanged { old, new ->
                    if (old is MediaSource.Remote && new is MediaSource.Remote) {
                        old.source.isSameConnection(new.source)
                    } else old == new
                },
                _currentPath,
                _permissionGranted,
                _refreshTrigger,
                optionsFlow.distinctUntilChanged()
            ) { source, path, granted, refreshSignal, options ->
                val targetPath = path ?: source.rootPath
                val isLocal = source.type == SourceType.Local
                LoadRequest(
                    source,
                    targetPath,
                    isLocal && !granted,
                    granted,
                    options,
                    refreshSignal
                )
            }
                .debounce(100.milliseconds)
                .flatMapLatest { request ->
                    flow {
                        // A. Prepare UI and Breadcrumbs
                        val currentState = _state.value
                        val isLocal = request.source.type == SourceType.Local
                        val shouldKeepContent =
                            currentState.mediaState is MediaBrowserState.Content && !request.needsPermission
                        val isCached = useCases.hasCachedMedia(request.path, request.source)

                        _state.update {
                            it.copy(
                                mediaNavigationState = it.mediaNavigationState.copy(
                                    currentPath = request.path,
                                    breadcrumbs = calculateNextMediaBreadcrumbs(
                                        request.path,
                                        request.source,
                                        it.mediaNavigationState.breadcrumbs
                                    )
                                ),
                                options = request.options,
                                mediaState = when {
                                    request.needsPermission -> MediaBrowserState.Permission
                                    it.isRefreshing || shouldKeepContent || it.mediaState is MediaBrowserState.Skeleton -> it.mediaState
                                    else -> MediaBrowserState.Skeleton
                                },
                                source = it.source.copy(permissionGranted = request.granted),
                                isRefreshing = if (request.refreshSignal.isManual) true
                                else if (shouldKeepContent && !isLocal && !isCached) true
                                else it.isRefreshing
                            )
                        }

                        if (request.needsPermission) {
                            emit(MediaBrowserState.Permission)
                            return@flow
                        }

                        // B. Data Fetching
                        try {
                            val files = useCases.getMediaFiles(
                                request.path,
                                request.source,
                                request.options,
                                request.refreshSignal.isManual
                            )
                            if (files.isEmpty()) {
                                emit(MediaBrowserState.Empty)
                            } else {
                                val scrollPos = scrollPositions[request.path]
                                emit(MediaBrowserState.Content(request.path, files, scrollPos))
                            }
                        } catch (e: Exception) {
                            emit(MediaBrowserState.Error(e.localizedMessage ?: "Unknown Error"))
                        }
                    }
                }
                .collect { nextUiState ->
                    _state.update { it.copy(mediaState = nextUiState, isRefreshing = false) }
                }
        }
    }

    private data class SearchResultPayload(
        val videoResults: List<MediaFile.Video> = emptyList(),
        val historyResults: List<PlaybackHistory> = emptyList(),
        val isLoading: Boolean = false
    )

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) return@flatMapLatest flow { emit(SearchResultPayload()) }
                    flow {
                        val current = _state.value
                        if (current.mode == BrowserMode.Storage) {
                            emit(SearchResultPayload(isLoading = true))
                            var results = emptyList<MediaFile.Video>()
                            try {
                                useCases.searchMediaFiles(
                                    query,
                                    current.mediaNavigationState.currentPath,
                                    current.source.current
                                )
                                    .collect { video ->
                                        results = results + video
                                        emit(
                                            SearchResultPayload(
                                                videoResults = results,
                                                isLoading = true
                                            )
                                        )
                                    }
                            } catch (_: Exception) {
                            } finally {
                                emit(SearchResultPayload(videoResults = results, isLoading = false))
                            }
                        } else {
                            emit(SearchResultPayload(isLoading = false))
                        }
                    }
                }
                .collect { payload ->
                    _state.update {
                        it.copy(
                            search = it.search.copy(
                                results = payload.videoResults,
                                isLoading = payload.isLoading
                            )
                        )
                    }
                }
        }
    }

    /**
     * Finds a matching video in the current directory for the given external subtitle file.
     */
    fun findMatchingVideoForSubtitle(subtitle: MediaFile.Subtitle): MediaFile.Video? {
        val currentFiles =
            (_state.value.mediaState as? MediaBrowserState.Content)?.files ?: return null
        val videos = currentFiles.filterIsInstance<MediaFile.Video>()
        return videos.firstOrNull { video ->
            video.externalSubtitles.any { it.path == subtitle.path }
        } ?: videos.firstOrNull { video ->
            SubtitleUtils.isAssociatedWithVideo(subtitle.name, video.name)
        }
    }

    // --- Intent Handling ---

    fun onIntent(intent: BrowserIntent) {
        when (intent) {
            is BrowserIntent.Storage -> handleStorageIntent(intent)
            is BrowserIntent.Source -> handleSourceIntent(intent)
            is BrowserIntent.History -> handleHistoryIntent(intent)
            is BrowserIntent.Playlist -> handlePlaylistIntent(intent)
            is BrowserIntent.Selection -> handleSelectionIntent(intent)
            is BrowserIntent.Dialog -> handleDialogIntent(intent)
            is BrowserIntent.Config -> handleConfigIntent(intent)
        }
    }

    private fun handleStorageIntent(intent: BrowserIntent.Storage) {
        when (intent) {
            is BrowserIntent.Storage.LoadFiles -> _refreshTrigger.value =
                RefreshSignal(System.currentTimeMillis(), false)

            is BrowserIntent.Storage.Refresh -> {
                _state.update { it.copy(isRefreshing = true) }
                _refreshTrigger.value = RefreshSignal(System.currentTimeMillis(), true)
            }

            is BrowserIntent.Storage.NavigateToFolder -> {
                _searchQuery.value = ""
                _state.update {
                    it.copy(
                        mode = BrowserMode.Storage,
                        search = it.search.copy(query = ""),
                        selection = BrowserSelectionState(
                            isActive = false,
                            selectedPaths = emptySet()
                        )
                    )
                }
                navigateToValidatedPath(intent.path, intent.sourceType)
            }

            is BrowserIntent.Storage.NavigateBack -> {
                if (_state.value.selection.isActive) {
                    _state.update {
                        it.copy(
                            selection = BrowserSelectionState(
                                isActive = false,
                                selectedPaths = emptySet()
                            )
                        )
                    }
                    return
                }
                if (_state.value.mode == BrowserMode.History) {
                    _searchQuery.value = ""
                    _state.update {
                        it.copy(
                            mode = BrowserMode.Storage,
                            search = it.search.copy(query = "")
                        )
                    }
                    return
                }
                if (_state.value.mode == BrowserMode.Playlist) {
                    _searchQuery.value = ""
                    val currentSelectedId = _selectedPlaylistId.value
                    if (currentSelectedId != null) {
                        // Return from playlist item details (level 2) back to playlist root list (level 1)
                        observePlaylistDetail(null)
                    } else {
                        // Return from playlist root list back to storage mode
                        _state.update {
                            it.copy(
                                mode = BrowserMode.Storage,
                                search = it.search.copy(query = "")
                            )
                        }
                    }
                    return
                }
                val current = _state.value.mediaNavigationState.currentPath
                val source = _state.value.source.current
                if (current != source.rootPath) {
                    _searchQuery.value = ""
                    _state.update { it.copy(search = it.search.copy(query = "")) }
                    val parent = current.substringBeforeLast('/', source.rootPath)
                        .ifEmpty { source.rootPath }
                    navigateToValidatedPath(parent, source.type)
                }
            }

            is BrowserIntent.Storage.OnPermissionResult -> _permissionGranted.value = intent.granted

            is BrowserIntent.Storage.SaveScrollPosition -> scrollPositions[_state.value.mediaNavigationState.currentPath] =
                ScrollPosition(intent.index, intent.offset)
        }
    }

    private fun handleSourceIntent(intent: BrowserIntent.Source) {
        viewModelScope.launch {
            when (intent) {
                is BrowserIntent.Source.Switch -> {
                    _state.update {
                        it.copy(
                            mode = BrowserMode.Storage,
                            selection = BrowserSelectionState(
                                isActive = false,
                                selectedPaths = emptySet()
                            )
                        )
                    }
                    useCases.saveSelectedSourceId(intent.source.id)
                }

                is BrowserIntent.Source.Delete -> {
                    useCases.deleteRemoteSource(intent.source)
                    if (_state.value.source.current.id == intent.source.id) {
                        useCases.saveSelectedSourceId(MediaSource.INTERNAL_STORAGE_ID)
                    }
                }
            }
        }
    }

    private fun handleHistoryIntent(intent: BrowserIntent.History) {
        when (intent) {
            is BrowserIntent.History.SwitchToHistory -> {
                _searchQuery.value = ""
                _state.update {
                    it.copy(
                        mode = BrowserMode.History,
                        search = it.search.copy(query = ""),
                        selection = BrowserSelectionState(
                            isActive = false,
                            selectedPaths = emptySet()
                        )
                    )
                }
            }

            is BrowserIntent.History.FilterBySource -> {
                _state.update { it.copy(selectedHistorySourceId = intent.sourceId) }
                _searchQuery.value = _searchQuery.value
            }

            is BrowserIntent.History.Replay -> {
                viewModelScope.launch {
                    useCases.saveHistory(
                        sourceId = intent.history.sourceId,
                        path = intent.history.path,
                        positionMs = 0L,
                        durationMs = intent.history.durationMs,
                        title = intent.history.title,
                        thumbnailUrl = intent.history.thumbnailUrl,
                        sourceType = intent.history.sourceType,
                        isCompleted = false
                    )
                    intent.onComplete()
                }
            }

            is BrowserIntent.History.Delete -> {
                viewModelScope.launch {
                    useCases.deleteHistory(intent.sourceId, intent.path)
                }
            }

            is BrowserIntent.History.ClearAll -> {
                viewModelScope.launch {
                    useCases.clearAllHistory()
                }
            }
        }
    }

    private fun handlePlaylistIntent(intent: BrowserIntent.Playlist) {
        when (intent) {
            is BrowserIntent.Playlist.SwitchToPlaylist -> {
                _searchQuery.value = ""
                _state.update {
                    it.copy(
                        mode = BrowserMode.Playlist,
                        search = it.search.copy(query = ""),
                        selection = BrowserSelectionState(
                            isActive = false,
                            selectedPaths = emptySet()
                        )
                    )
                }
            }

            is BrowserIntent.Playlist.Select -> {
                observePlaylistDetail(intent.playlistId)
            }

            is BrowserIntent.Playlist.Create -> {
                viewModelScope.launch {
                    useCases.playlist.createPlaylist(intent.title)
                }
            }

            is BrowserIntent.Playlist.ImportFromUrl -> {
                viewModelScope.launch {
                    try {
                        useCases.playlist.importM3uPlaylist.fromUrl(intent.title, intent.url)
                    } catch (e: Exception) {
                        Log.e("BrowserViewModel", "Failed to import M3U from URL: ${intent.url}", e)
                    }
                }
            }

            is BrowserIntent.Playlist.ImportFromUri -> {
                viewModelScope.launch {
                    try {
                        useCases.playlist.importM3uPlaylist.fromUri(
                            intent.title,
                            intent.uri.toString()
                        )
                    } catch (e: Exception) {
                        Log.e("BrowserViewModel", "Failed to import M3U from Uri: ${intent.uri}", e)
                    }
                }
            }

            is BrowserIntent.Playlist.Refresh -> {
                viewModelScope.launch {
                    _state.update { it.copy(refreshingPlaylistIds = it.refreshingPlaylistIds + intent.playlistId) }
                    try {
                        useCases.playlist.refreshPlaylist.fromUrl(intent.playlistId)
                    } catch (e: Exception) {
                        Log.e(
                            "BrowserViewModel",
                            "Failed to refresh playlist: ${intent.playlistId}",
                            e
                        )
                    } finally {
                        _state.update { it.copy(refreshingPlaylistIds = it.refreshingPlaylistIds - intent.playlistId) }
                    }
                }
            }

            is BrowserIntent.Playlist.RefreshM3uFile -> {
                viewModelScope.launch {
                    _state.update { it.copy(refreshingPlaylistIds = it.refreshingPlaylistIds + intent.playlistId) }
                    try {
                        useCases.playlist.refreshPlaylist.fromFileUri(
                            intent.playlistId,
                            intent.uri.toString()
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "BrowserViewModel",
                            "Failed to refresh file playlist: ${intent.playlistId}",
                            e
                        )
                    } finally {
                        _state.update { it.copy(refreshingPlaylistIds = it.refreshingPlaylistIds - intent.playlistId) }
                    }
                }
            }

            is BrowserIntent.Playlist.Rename -> {
                viewModelScope.launch {
                    useCases.playlist.updatePlaylistTitle(intent.playlistId, intent.newTitle)
                }
            }

            is BrowserIntent.Playlist.Reorder -> {
                viewModelScope.launch {
                    useCases.playlist.reorderPlaylists(intent.playlistIds)
                }
            }

            is BrowserIntent.Playlist.Delete -> {
                viewModelScope.launch {
                    useCases.playlist.deletePlaylist(intent.playlistId)
                    if (_selectedPlaylistId.value == intent.playlistId) {
                        observePlaylistDetail(null)
                    }
                }
            }

            is BrowserIntent.Playlist.RemoveItem -> {
                viewModelScope.launch {
                    useCases.playlist.removeMediaFromPlaylist(intent.itemId)
                }
            }

            is BrowserIntent.Playlist.SwapItems -> {
                viewModelScope.launch {
                    useCases.playlist.reorderPlaylistItems.swap(
                        intent.playlistId,
                        intent.itemId1,
                        intent.itemId2
                    )
                }
            }
        }
    }

    private fun handleSelectionIntent(intent: BrowserIntent.Selection) {
        when (intent) {
            is BrowserIntent.Selection.SetActive -> {
                _state.update {
                    it.copy(
                        selection = if (intent.active) {
                            it.selection.copy(isActive = true)
                        } else {
                            BrowserSelectionState(isActive = false, selectedPaths = emptySet())
                        }
                    )
                }
            }

            is BrowserIntent.Selection.ToggleSelect -> {
                val currentFiles =
                    (_state.value.mediaState as? MediaBrowserState.Content)?.files ?: emptyList()
                val isVideo = currentFiles.any { it is MediaFile.Video && it.path == intent.path }
                if (isVideo || intent.path in _state.value.selection.selectedPaths) {
                    _state.update { currentState ->
                        val currentPaths = currentState.selection.selectedPaths
                        val newPaths = if (intent.path in currentPaths) {
                            currentPaths - intent.path
                        } else {
                            currentPaths + intent.path
                        }
                        val isActive = newPaths.isNotEmpty()
                        currentState.copy(
                            selection = currentState.selection.copy(
                                isActive = isActive,
                                selectedPaths = newPaths
                            )
                        )
                    }
                }
            }

            is BrowserIntent.Selection.SelectAll -> {
                val currentFiles =
                    (_state.value.mediaState as? MediaBrowserState.Content)?.files ?: emptyList()
                val videoPaths =
                    currentFiles.filterIsInstance<MediaFile.Video>().map { it.path }.toSet()
                _state.update { currentState ->
                    currentState.copy(
                        selection = currentState.selection.copy(
                            isActive = videoPaths.isNotEmpty(),
                            selectedPaths = videoPaths
                        )
                    )
                }
            }

            is BrowserIntent.Selection.InvertSelection -> {
                val currentFiles =
                    (_state.value.mediaState as? MediaBrowserState.Content)?.files ?: emptyList()
                val videoPaths =
                    currentFiles.filterIsInstance<MediaFile.Video>().map { it.path }.toSet()
                _state.update { currentState ->
                    val currentSelected = currentState.selection.selectedPaths
                    val inverted = videoPaths - currentSelected
                    currentState.copy(
                        selection = currentState.selection.copy(
                            isActive = inverted.isNotEmpty(),
                            selectedPaths = inverted
                        )
                    )
                }
            }

            is BrowserIntent.Selection.ClearSelection -> {
                _state.update { currentState ->
                    currentState.copy(
                        selection = BrowserSelectionState(
                            isActive = false,
                            selectedPaths = emptySet()
                        )
                    )
                }
            }

            is BrowserIntent.Selection.OpenAddToPlaylist -> {
                _state.update { currentState ->
                    currentState.copy(
                        dialogs = currentState.dialogs.copy(isAddToPlaylistVisible = true)
                    )
                }
            }

            is BrowserIntent.Selection.DismissAddToPlaylist -> {
                _state.update { currentState ->
                    currentState.copy(
                        dialogs = currentState.dialogs.copy(isAddToPlaylistVisible = false)
                    )
                }
            }

            is BrowserIntent.Selection.AddToPlaylist -> {
                viewModelScope.launch {
                    val videosToAdd = _state.value.selectedVideos
                    if (videosToAdd.isNotEmpty()) {
                        useCases.playlist.addMediaToPlaylist(intent.playlistId, videosToAdd)
                    }
                    _state.update { currentState ->
                        currentState.copy(
                            selection = BrowserSelectionState(
                                isActive = false,
                                selectedPaths = emptySet()
                            ),
                            dialogs = currentState.dialogs.copy(isAddToPlaylistVisible = false)
                        )
                    }
                }
            }
        }
    }

    private fun handleDialogIntent(intent: BrowserIntent.Dialog) {
        when (intent) {
            is BrowserIntent.Dialog.ShowCreatePlaylist -> {
                _state.update { it.copy(dialogs = it.dialogs.copy(isCreatePlaylistVisible = true)) }
            }

            is BrowserIntent.Dialog.DismissCreatePlaylist -> {
                _state.update { it.copy(dialogs = it.dialogs.copy(isCreatePlaylistVisible = false)) }
            }

            is BrowserIntent.Dialog.ShowCreateBlankPlaylist -> {
                _state.update {
                    it.copy(
                        dialogs = it.dialogs.copy(
                            isCreateBlankPlaylistVisible = true,
                            pendingRestoreAddToPlaylist = intent.fromAddToPlaylist,
                            isAddToPlaylistVisible = if (intent.fromAddToPlaylist) false else it.dialogs.isAddToPlaylistVisible
                        )
                    )
                }
            }

            is BrowserIntent.Dialog.DismissCreateBlankPlaylist -> {
                _state.update { currentState ->
                    val shouldRestore = currentState.dialogs.pendingRestoreAddToPlaylist
                    currentState.copy(
                        dialogs = currentState.dialogs.copy(
                            isCreateBlankPlaylistVisible = false,
                            pendingRestoreAddToPlaylist = false,
                            isAddToPlaylistVisible = if (shouldRestore) true else currentState.dialogs.isAddToPlaylistVisible
                        )
                    )
                }
            }

            is BrowserIntent.Dialog.ShowRenamePlaylist -> {
                _state.update { it.copy(dialogs = it.dialogs.copy(playlistToRename = intent.playlist)) }
            }

            is BrowserIntent.Dialog.DismissRenamePlaylist -> {
                _state.update { it.copy(dialogs = it.dialogs.copy(playlistToRename = null)) }
            }

            is BrowserIntent.Dialog.ShowDeletePlaylist -> {
                _state.update { it.copy(dialogs = it.dialogs.copy(playlistToDelete = intent.playlist)) }
            }

            is BrowserIntent.Dialog.DismissDeletePlaylist -> {
                _state.update { it.copy(dialogs = it.dialogs.copy(playlistToDelete = null)) }
            }

            is BrowserIntent.Dialog.LaunchM3uFilePicker -> {
                _state.update { it.copy(dialogs = it.dialogs.copy(pendingRefreshFilePlaylistId = intent.playlistId)) }
            }

            is BrowserIntent.Dialog.HandleM3uFileResult -> {
                val targetPlaylistId = _state.value.dialogs.pendingRefreshFilePlaylistId
                _state.update { it.copy(dialogs = it.dialogs.copy(pendingRefreshFilePlaylistId = null)) }
                if (intent.uri != null && targetPlaylistId != null) {
                    onIntent(BrowserIntent.Playlist.RefreshM3uFile(targetPlaylistId, intent.uri))
                }
            }
        }
    }

    suspend fun importM3uFromUrl(title: String, url: String): String {
        return useCases.playlist.importM3uPlaylist.fromUrl(title, url)
    }

    suspend fun importM3uFromUri(title: String, uri: Uri): String {
        return useCases.playlist.importM3uPlaylist.fromUri(title, uri.toString())
    }

    private fun handleConfigIntent(intent: BrowserIntent.Config) {
        viewModelScope.launch {
            val isRemote = _state.value.source.current.type != SourceType.Local
            when (intent) {
                is BrowserIntent.Config.UpdateSortOptions -> useCases.updateSortOptions(
                    intent.options,
                    isRemote
                )

                is BrowserIntent.Config.UpdateDisplayFields -> useCases.updateDisplayFields(
                    intent.fields,
                    isRemote
                )

                is BrowserIntent.Config.ShowSortDialog -> _state.update {
                    it.copy(
                        dialogs = it.dialogs.copy(
                            isSortVisible = true
                        )
                    )
                }

                is BrowserIntent.Config.DismissSortDialog -> _state.update {
                    it.copy(
                        dialogs = it.dialogs.copy(
                            isSortVisible = false
                        )
                    )
                }

                is BrowserIntent.Config.SearchQueryChange -> {
                    _searchQuery.value = intent.query
                    _state.update {
                        it.copy(
                            search = it.search.copy(
                                query = intent.query,
                                isLoading = intent.query.isNotBlank()
                            )
                        )
                    }
                }
            }
        }
    }

    // --- Private Helpers ---

    private fun handleSourceChange(allSources: List<MediaSource>, currentSource: MediaSource) {
        val previousSource = _state.value.source.current
        val sourceChanged = previousSource.id.isNotEmpty() && previousSource.id != currentSource.id

        if (sourceChanged) {
            _searchQuery.value = ""
            useCases.closeRemoteSource()
            scrollPositions.clear()
            _currentPath.value = currentSource.rootPath
            _refreshTrigger.value = RefreshSignal(System.currentTimeMillis(), false)
        }

        _state.update {
            it.copy(
                source = it.source.copy(available = allSources, current = currentSource),
                mediaNavigationState = if (sourceChanged) it.mediaNavigationState.copy(
                    breadcrumbs = emptyList(),
                    currentPath = currentSource.rootPath
                ) else it.mediaNavigationState,
                search = if (sourceChanged) it.search.copy(query = "") else it.search,
                isRefreshing = false,
                mediaState = if (sourceChanged) MediaBrowserState.Skeleton else it.mediaState
            )
        }
    }

    private fun navigateToValidatedPath(path: String, sourceType: SourceType) {
        var targetPath = path
        if (sourceType == SourceType.Local) {
            val root = _state.value.source.current.rootPath
            while (targetPath != root && !File(targetPath).exists()) {
                targetPath = targetPath.substringBeforeLast('/', root)
            }
        }
        _currentPath.value = targetPath
        _refreshTrigger.value = RefreshSignal(System.currentTimeMillis(), false)
    }

    private fun calculateNextMediaBreadcrumbs(
        path: String,
        source: MediaSource,
        currentBreadcrumbs: List<MediaBreadcrumb>
    ): List<MediaBreadcrumb> {
        // Build the branch for the TARGET path
        val targetBranch = MediaBreadcrumb.build(path, source)

        // Validate existing breadcrumbs (for local only) to handle deleted folders
        val validatedHistory = currentBreadcrumbs.filter {
            it.sourceType != SourceType.Local || File(it.path).exists()
        }

        // Merge history: If history is a superset of the new branch (meaning we moved "up"), 
        // keep the longer history to allow "Forward" navigation via breadcrumbs.
        return if (validatedHistory.size >= targetBranch.size &&
            validatedHistory.take(targetBranch.size).zip(targetBranch)
                .all { (h, t) -> h.path == t.path && h.sourceType == t.sourceType }
        ) {
            validatedHistory
        } else {
            targetBranch
        }
    }

    private data class LoadRequest(
        val source: MediaSource,
        val path: String,
        val needsPermission: Boolean,
        val granted: Boolean,
        val options: MediaOptions,
        val refreshSignal: RefreshSignal
    )
}
