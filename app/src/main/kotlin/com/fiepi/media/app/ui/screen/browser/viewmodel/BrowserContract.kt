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

import android.net.Uri
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.media.ConflictDecision
import com.fiepi.media.domain.model.media.FileOperationType
import com.fiepi.media.domain.model.media.FileTask
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.playlist.Playlist
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceCapabilities
import com.fiepi.media.domain.model.source.SourceType

/**
 * Represents a single segment in the navigation path.
 */
data class MediaBreadcrumb(
    val name: String,
    val path: String,
    val sourceType: SourceType = SourceType.Local
) {
    companion object {
        fun build(path: String, source: MediaSource): List<MediaBreadcrumb> {
            val breadcrumbs = mutableListOf<MediaBreadcrumb>()
            val rootPath = source.rootPath
            breadcrumbs.add(MediaBreadcrumb(source.name, rootPath, source.type))

            val normalizedRoot = if (rootPath == "/") "/" else rootPath.removeSuffix("/")
            val normalizedPath = if (path == "/") "/" else path.removeSuffix("/")

            if (normalizedPath != normalizedRoot && normalizedPath.startsWith(normalizedRoot)) {
                val relativePath = normalizedPath.removePrefix(normalizedRoot).removePrefix("/")
                val parts = relativePath.split('/').filter { it.isNotEmpty() }

                var currentAccumulatedPath = normalizedRoot
                parts.forEach { part ->
                    currentAccumulatedPath = if (currentAccumulatedPath == "/") {
                        "/$part"
                    } else {
                        "$currentAccumulatedPath/$part"
                    }
                    breadcrumbs.add(MediaBreadcrumb(part, currentAccumulatedPath, source.type))
                }
            }
            return breadcrumbs
        }
    }
}

/**
 * Stores the scroll position of a specific directory to enable state restoration.
 */
data class ScrollPosition(
    val index: Int = 0,
    val offset: Int = 0
)

/**
 * Encapsulates the state related to media sources and permissions.
 */
data class BrowserSourceState(
    val current: MediaSource = MediaSource.Local(
        id = MediaSource.INTERNAL_STORAGE_ID,
        name = "",
        rootPath = "",
        totalSpace = 0,
        freeSpace = 0
    ),
    val available: List<MediaSource> = emptyList(),
    val permissionGranted: Boolean = false
)

/**
 * Encapsulates the state related to directory navigation and path history.
 */
data class MediaBrowserNavigationState(
    val currentPath: String = "",
    val breadcrumbs: List<MediaBreadcrumb> = emptyList()
)

/**
 * Encapsulates the state for search functionality within the current directory or history.
 */
data class BrowserSearchState(
    val query: String = "",
    val results: List<MediaFile.Video> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * Manages the visibility state of various dialogs in the browser screen.
 */
data class BrowserDialogState(
    val isSortVisible: Boolean = false,
    val isAddToPlaylistVisible: Boolean = false,
    val isCreatePlaylistVisible: Boolean = false,
    val isCreateBlankPlaylistVisible: Boolean = false,
    val pendingRestoreAddToPlaylist: Boolean = false,
    val playlistToRename: Playlist? = null,
    val playlistToDelete: Playlist? = null,
    val pendingRefreshFilePlaylistId: String? = null,
    val fileToRename: MediaFile? = null,
    val isDeleteConfirmVisible: Boolean = false,
    val folderPickerOperation: FileOperationType? = null,
    val folderPickerCurrentPath: String? = null,
    val createFolderParentPath: String? = null,
    val pendingRestoreFolderPickerOperation: FileOperationType? = null,
    val conflictFileName: String? = null
)

/**
 * Encapsulates the state for item multi-selection in the browser.
 */
data class BrowserSelectionState(
    val isActive: Boolean = false,
    val selectedPaths: Set<String> = emptySet()
)

/**
 * Represents a single segment in the playlist navigation path.
 */
data class PlaylistBreadcrumb(
    val name: String,
    val playlistId: String? = null
)

/**
 * Encapsulates the distinct UI states for playlist browsing and details.
 */
sealed interface PlaylistBrowserState {
    /** Initial loading state showing skeleton placeholders. */
    data object Skeleton : PlaylistBrowserState

    /** Error state with a descriptive message. */
    data class Error(val message: String) : PlaylistBrowserState

    /** State indicating that no playlists exist yet (Level 1). */
    data object Empty : PlaylistBrowserState

    /** Content state displaying all playlists (Level 1). */
    data class ListContent(
        val playlists: List<Playlist>
    ) : PlaylistBrowserState

    /** State indicating that the selected playlist contains no video items (Level 2). */
    data class DetailEmpty(
        val playlistId: String,
        val playlistTitle: String
    ) : PlaylistBrowserState

    /** Content state displaying videos in a selected playlist (Level 2). */
    data class DetailContent(
        val playlist: Playlist
    ) : PlaylistBrowserState
}

/**
 * Represents the current main view section in the browser.
 */
enum class BrowserMode {
    Storage,
    History,
    Playlist
}

/**
 * The root state for the Browser Screen, aggregating all sub-states.
 */
data class BrowserState(
    val mode: BrowserMode = BrowserMode.Storage,
    val isRefreshing: Boolean = false,
    val interceptBackNavigation: Boolean = true,
    val activeFileTask: FileTask? = null,

    //source
    val source: BrowserSourceState = BrowserSourceState(),

    //media
    val lastPlayedUri: String? = null,
    val mediaState: MediaBrowserState = MediaBrowserState.Skeleton,
    val options: MediaOptions = MediaOptions(),
    val localDisplayFields: List<MediaField> = emptyList(),
    val remoteDisplayFields: List<MediaField> = emptyList(),
    val mediaNavigationState: MediaBrowserNavigationState = MediaBrowserNavigationState(),

    //history
    val selectedHistorySourceId: String? = null,
    val historyState: HistoryBrowserState = HistoryBrowserState.Skeleton,

    //playlist
    val selectedPlaylistId: String? = null,
    val playlists: List<Playlist> = emptyList(),
    val playlistState: PlaylistBrowserState = PlaylistBrowserState.Skeleton,
    val refreshingPlaylistIds: Set<String> = emptySet(),

    //selection
    val selection: BrowserSelectionState = BrowserSelectionState(),

    //search
    val search: BrowserSearchState = BrowserSearchState(),
    //dialog
    val dialogs: BrowserDialogState = BrowserDialogState(),
) {
    /** Whether the browser is currently at the root directory of the selected source. */
    val isAtRoot: Boolean get() = mediaNavigationState.currentPath == source.current.rootPath

    /** The SourceType (Local, SMB, etc.) of the current media source. */
    val sourceType: SourceType get() = source.current.type

    /** Feature capabilities supported by the active media source. */
    val capabilities: SourceCapabilities
        get() = source.current.capabilities

    /** Display fields corresponding to the current media source type (Local vs Remote). */
    val displayFields: List<MediaField>
        get() = if (sourceType.isRemote) remoteDisplayFields else localDisplayFields

    /** Selected media file objects in current content. */
    val selectedMediaFiles: List<MediaFile>
        get() = (mediaState as? MediaBrowserState.Content)?.files
            ?.filter { it.path in selection.selectedPaths } ?: emptyList()

    /** Total number of valid selected items. */
    val selectedCount: Int get() = selectedMediaFiles.size

    /** Selected video files in current content. */
    val selectedVideos: List<MediaFile.Video>
        get() = selectedMediaFiles.filterIsInstance<MediaFile.Video>()

    /** Whether add to playlist action is enabled (all selected items are videos). */
    val isAddToPlaylistEnabled: Boolean
        get() = selectedMediaFiles.isNotEmpty() && selectedMediaFiles.all { it is MediaFile.Video }

    /** Calculates the playlist breadcrumb list for current detail state. */
    fun playlistBreadcrumbs(rootTitle: String): List<PlaylistBreadcrumb> {
        val list = mutableListOf(PlaylistBreadcrumb(rootTitle, null))
        when (val pState = playlistState) {
            is PlaylistBrowserState.DetailContent -> {
                list.add(PlaylistBreadcrumb(pState.playlist.title, pState.playlist.id))
            }

            is PlaylistBrowserState.DetailEmpty -> {
                list.add(PlaylistBreadcrumb(pState.playlistTitle, pState.playlistId))
            }

            else -> {}
        }
        return list
    }
}

/**
 * Represents the distinct UI states of the media storage browser.
 */
sealed interface MediaBrowserState {
    /** Initial loading state showing skeleton placeholders. */
    data object Skeleton : MediaBrowserState

    /** State indicating that system permissions are required to browse. */
    data object Permission : MediaBrowserState

    /** Error state with a descriptive message. */
    data class Error(val message: String) : MediaBrowserState

    /** Content state displaying the list of media files. */
    data class Content(
        val path: String,
        val files: List<MediaFile>,
        val scrollPosition: ScrollPosition? = null
    ) : MediaBrowserState

    /** State indicating that the current directory has no displayable media files. */
    data object Empty : MediaBrowserState
}

/**
 * Represents the distinct UI states of the playback history browser.
 */
sealed interface HistoryBrowserState {
    /** Initial loading state showing skeleton placeholders. */
    data object Skeleton : HistoryBrowserState

    /** Error state with a descriptive message. */
    data class Error(val message: String) : HistoryBrowserState

    /** State indicating that there are no playback history items. */
    data object Empty : HistoryBrowserState

    /** Content state displaying the playback history list. */
    data class Content(
        val sourceId: String? = null,
        val query: String = ""
    ) : HistoryBrowserState
}

/**
 * Groups all user interaction callbacks for the Browser Screen.
 */
data class BrowserActions(
    val onIntent: (BrowserIntent) -> Unit = {},
    val onRequestPermission: () -> Unit = {},
    val onCreatePlaylist: () -> Unit = {},
    val onFolderClick: (MediaFile.Folder) -> Unit = {},
    val onMediaBreadcrumbClick: (MediaBreadcrumb) -> Unit = {},
    val onVideoClick: (MediaFile.Video) -> Unit = {},
    val onSubtitleClick: (MediaFile.Subtitle) -> Unit = {},
    val onPlayHistory: (PlaybackHistory) -> Unit = {},
    val onPlayPlaylist: (playlistId: String, initialMediaId: String?) -> Unit = { _, _ -> },
    val onManageSources: () -> Unit = {},
    val onSettingsClick: () -> Unit = {},
    val onAddRemoteSource: () -> Unit = {},
    val onPlayNetworkStream: () -> Unit = {},
    val onSourcesClick: () -> Unit = {},
    val onOpenTaskQueue: () -> Unit = {}
)

/**
 * User intentions/actions that can be performed on the Browser Screen.
 */
sealed interface BrowserIntent {
    /** Intents related to storage browsing and directory navigation. */
    sealed interface Storage : BrowserIntent {
        /** Triggers a reload of the current file list. */
        data object LoadFiles : Storage

        /** Triggers a manual refresh (e.g., via pull-to-refresh). */
        data object Refresh : Storage

        /** Navigates to a specific folder path. */
        data class NavigateToFolder(
            val path: String,
            val sourceType: SourceType = SourceType.Local
        ) : Storage

        /** Navigates back to the parent folder. */
        data object NavigateBack : Storage

        /** Handles the result of a system permission request. */
        data class OnPermissionResult(val granted: Boolean) : Storage

        /** Persists the current scroll position for the active directory. */
        data class SaveScrollPosition(val index: Int, val offset: Int) : Storage
    }

    /** Intents related to playback history browsing and management. */
    sealed interface History : BrowserIntent {
        /** Switches view to Playback History. */
        data object SwitchToHistory : History

        /** Filters playback history by a specific source ID (null for all sources). */
        data class FilterBySource(val sourceId: String?) : History

        /** Resets playback position to 0 and executes completion callback for replay. */
        data class Replay(val history: PlaybackHistory, val onComplete: () -> Unit) : History

        /** Deletes a single playback history item. */
        data class Delete(val sourceId: String, val path: String) : History

        /** Clears all playback history items. */
        data object ClearAll : History
    }

    /** Intents related to playlist creation, import, and management. */
    sealed interface Playlist : BrowserIntent {
        /** Switches view to Custom Playlists. */
        data object SwitchToPlaylist : Playlist

        /** Selects a specific playlist or null for root playlist list. */
        data class Select(val playlistId: String?) : Playlist

        /** Creates a new playlist. */
        data class Create(val title: String) : Playlist

        /** Imports an M3U playlist from a URL. */
        data class ImportFromUrl(val title: String, val url: String) : Playlist

        /** Imports an M3U playlist from a local file Uri. */
        data class ImportFromUri(val title: String, val uri: Uri) : Playlist

        /** Refreshes an M3U link playlist. */
        data class Refresh(val playlistId: String) : Playlist

        /** Refreshes an M3U file playlist with a new file Uri. */
        data class RefreshM3uFile(val playlistId: String, val uri: Uri) : Playlist

        /** Renames an existing playlist. */
        data class Rename(val playlistId: String, val newTitle: String) : Playlist

        /** Reorders playlists according to a new list of playlist IDs. */
        data class Reorder(val playlistIds: List<String>) : Playlist

        /** Deletes a playlist. */
        data class Delete(val playlistId: String) : Playlist

        /** Removes an item from a playlist. */
        data class RemoveItem(val itemId: String) : Playlist

        /** Swaps the sort order of two playlist items within a playlist. */
        data class SwapItems(val playlistId: String, val itemId1: String, val itemId2: String) :
            Playlist
    }

    /** Intents related to active media source switching and management. */
    sealed interface Source : BrowserIntent {
        /** Switches the active media source. */
        data class Switch(val source: MediaSource) : Source

        /** Deletes a remote media source from the saved list. */
        data class Delete(val source: RemoteSource) : Source
    }

    /** Intents related to multi-selection in file browser. */
    sealed interface Selection : BrowserIntent {
        /** Toggles or sets selection mode active status. */
        data class SetActive(val active: Boolean) : Selection

        /** Toggles selection of a specific item path. */
        data class ToggleSelect(val path: String) : Selection

        /** Selects all items in current directory. */
        data object SelectAll : Selection

        /** Inverts current item selection in directory. */
        data object InvertSelection : Selection

        /** Clears all selected items. */
        data object ClearSelection : Selection

        /** Shows dialog to select a target playlist for adding selected files. */
        data object OpenAddToPlaylist : Selection

        /** Dismisses the add to playlist dialog. */
        data object DismissAddToPlaylist : Selection

        /** Adds selected video files to specified playlist. */
        data class AddToPlaylist(val playlistId: String) : Selection
    }

    /** Intents related to dialog visibility and host state. */
    sealed interface Dialog : BrowserIntent {
        data object ShowCreatePlaylist : Dialog
        data object DismissCreatePlaylist : Dialog
        data class ShowCreateBlankPlaylist(val fromAddToPlaylist: Boolean = false) : Dialog
        data object DismissCreateBlankPlaylist : Dialog
        data class ShowRenamePlaylist(val playlist: com.fiepi.media.domain.model.playlist.Playlist) :
            Dialog

        data object DismissRenamePlaylist : Dialog
        data class ShowDeletePlaylist(val playlist: com.fiepi.media.domain.model.playlist.Playlist) :
            Dialog

        data object DismissDeletePlaylist : Dialog
        data class LaunchM3uFilePicker(val playlistId: String) : Dialog
        data class HandleM3uFileResult(val uri: Uri?) : Dialog
    }

    /** Intents related to UI options, search query, and dialog states. */
    sealed interface Config : BrowserIntent {
        /** Shows the sorting and view options dialog. */
        data object ShowSortDialog : Config

        /** Dismisses the sorting and view options dialog. */
        data object DismissSortDialog : Config

        /** Updates the global media sort settings. */
        data class UpdateSortOptions(val options: MediaOptions) : Config

        /** Updates the global media display fields. */
        data class UpdateDisplayFields(val fields: List<MediaField>) : Config

        /** Updates the search query and filters the file list. */
        data class SearchQueryChange(val query: String) : Config
    }

    /** Intents related to file management (Copy, Move, Rename, Delete, Create Folder). */
    sealed interface FileAction : BrowserIntent {
        data class ShowRename(val file: MediaFile) : FileAction
        data class ConfirmRename(val file: MediaFile, val newName: String) : FileAction
        data object DismissRename : FileAction

        data object ShowDelete : FileAction
        data object ConfirmDelete : FileAction
        data object DismissDelete : FileAction

        data class StartCopy(val files: List<MediaFile>) : FileAction
        data class StartMove(val files: List<MediaFile>) : FileAction
        data class ConfirmFolderPicker(val targetPath: String) : FileAction
        data object DismissFolderPicker : FileAction

        data class ShowCreateFolder(
            val parentPath: String,
            val currentFolderPickerPath: String? = null
        ) : FileAction

        data object DismissCreateFolder : FileAction
        data class CreateFolder(val parentPath: String, val name: String) : FileAction
        data class ResolveConflict(val decision: ConflictDecision) : FileAction
        data object DismissConflict : FileAction
    }
}
