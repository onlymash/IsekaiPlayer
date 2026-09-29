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

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserActions
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserMode
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserSourceState
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.HistoryBrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.MediaBreadcrumb
import com.fiepi.media.app.ui.screen.browser.viewmodel.MediaBrowserNavigationState
import com.fiepi.media.app.ui.screen.browser.viewmodel.MediaBrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.PlaylistBrowserState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.playlist.Playlist
import com.fiepi.media.domain.model.playlist.PlaylistItem
import com.fiepi.media.domain.model.preferences.ThemeMode
import com.fiepi.media.domain.model.preferences.ThemeOptions
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType
import kotlinx.coroutines.flow.flowOf

@Preview(showBackground = true, name = "Skeleton State")
@Composable
fun BrowserSkeletonPreview() {
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(mediaState = MediaBrowserState.Skeleton),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Skeleton State - Dark")
@Composable
fun BrowserSkeletonDarkPreview() {
    AppTheme(themeOptions = ThemeOptions(themeMode = ThemeMode.Dark)) {
        BrowserScreenContent(
            state = BrowserState(mediaState = MediaBrowserState.Skeleton),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Permission Required")
@Composable
fun BrowserPermissionPreview() {
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mediaState = MediaBrowserState.Permission,
                mediaNavigationState = MediaBrowserNavigationState(currentPath = "/root")
            ),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Root Folder List")
@Composable
fun BrowserRootPreview() {
    val mockFiles = listOf(
        MediaFile.Folder(
            path = "/root/Movies",
            name = "Movies",
            sourceType = SourceType.Local,
            itemCount = 5
        ),
        MediaFile.Folder(
            path = "/root/Downloads",
            name = "Downloads",
            sourceType = SourceType.Local,
            itemCount = 10
        ),
        MediaFile.Video(
            path = "/root/video1.mp4",
            name = "video1",
            sourceType = SourceType.Local,
            id = "1",
            duration = 1000 * 60 * 5, // 5 mins
            size = 1024 * 1024 * 50,
            lastModified = System.currentTimeMillis(),
            mimeType = "video/mp4",
            extension = "mp4",
            width = 1920,
            height = 1080
        ),
        MediaFile.Subtitle(
            path = "/root/video1.srt",
            name = "video1",
            sourceType = SourceType.Local,
            size = 1024 * 10,
            extension = "srt",
            lastModified = System.currentTimeMillis()
        )
    )
    val rootSource = MediaSource.Local(
        MediaSource.INTERNAL_STORAGE_ID,
        name = "Internal Storage",
        rootPath = "/root",
        totalSpace = 128L * 1024 * 1024 * 1024,
        freeSpace = 50L * 1024 * 1024 * 1024
    )
    val breadcrumbs = listOf(MediaBreadcrumb("Internal Storage", "/root", SourceType.Local))
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mediaState = MediaBrowserState.Content("/root", mockFiles),
                mediaNavigationState = MediaBrowserNavigationState(
                    currentPath = "/root",
                    breadcrumbs = breadcrumbs
                ),
                source = BrowserSourceState(
                    current = rootSource,
                    available = listOf(rootSource),
                    permissionGranted = true
                )
            ),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Subfolder List")
@Composable
fun BrowserSubfolderPreview() {
    val mockFiles = listOf(
        MediaFile.Video(
            path = "/root/Movies/action.mp4",
            name = "action",
            sourceType = SourceType.Local,
            id = "2",
            duration = 1000 * 60 * 120, // 2 hours
            size = 1024 * 1024 * 200,
            lastModified = System.currentTimeMillis(),
            mimeType = "video/mp4",
            extension = "mp4",
            width = 1280,
            height = 720
        ),
        MediaFile.Subtitle(
            "/root/Movies/action.ass",
            "action",
            SourceType.Local,
            size = 1024 * 50,
            extension = "ass",
            lastModified = System.currentTimeMillis()
        )
    )
    val rootSource = MediaSource.Local(
        MediaSource.INTERNAL_STORAGE_ID,
        name = "Internal Storage",
        rootPath = "/root",
        totalSpace = 128L * 1024 * 1024 * 1024,
        freeSpace = 50L * 1024 * 1024 * 1024
    )
    val breadcrumbs = listOf(
        MediaBreadcrumb("Internal Storage", "/root", SourceType.Local),
        MediaBreadcrumb("Movies", "/root/Movies", SourceType.Local)
    )
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mediaState = MediaBrowserState.Content("/root/Movies", mockFiles),
                mediaNavigationState = MediaBrowserNavigationState(
                    currentPath = "/root/Movies",
                    breadcrumbs = breadcrumbs
                ),
                source = BrowserSourceState(
                    current = rootSource,
                    available = listOf(rootSource),
                    permissionGranted = true
                )
            ),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Tablet - Folder List", device = Devices.TABLET)
@Composable
fun BrowserTabletPreview() {
    val mockFiles = listOf(
        MediaFile.Folder(
            path = "/root/Movies",
            name = "Movies",
            sourceType = SourceType.Local,
            itemCount = 5
        ),
        MediaFile.Folder(
            path = "/root/Downloads",
            name = "Downloads",
            sourceType = SourceType.Local,
            itemCount = 10
        ),
        MediaFile.Video(
            path = "/root/video1.mp4",
            name = "video1",
            sourceType = SourceType.Local,
            id = "1",
            duration = 1000 * 60 * 5,
            size = 1024 * 1024 * 50,
            lastModified = System.currentTimeMillis(),
            mimeType = "video/mp4",
            extension = "mp4",
            width = 1920,
            height = 1080
        )
    )
    val rootSource = MediaSource.Local(
        id = MediaSource.INTERNAL_STORAGE_ID,
        name = "Internal Storage",
        rootPath = "/root",
        totalSpace = 128L * 1024 * 1024 * 1024,
        freeSpace = 50L * 1024 * 1024 * 1024
    )
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mediaState = MediaBrowserState.Content("/root", mockFiles),
                mediaNavigationState = MediaBrowserNavigationState(
                    currentPath = "/root",
                    breadcrumbs = listOf(
                        MediaBreadcrumb(
                            name = "Internal Storage",
                            path = "/root",
                            sourceType = SourceType.Local
                        )
                    )
                ),
                source = BrowserSourceState(
                    current = rootSource,
                    available = listOf(rootSource),
                    permissionGranted = true
                )
            ),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Playback History Content")
@Composable
fun BrowserHistoryContentPreview() {
    val mockHistory = listOf(
        PlaybackHistory(
            sourceId = MediaSource.INTERNAL_STORAGE_ID,
            path = "/storage/emulated/0/Movies/Inception.mp4",
            title = "Inception",
            positionMs = 3600000L,
            durationMs = 8880000L,
            lastPlayedAt = System.currentTimeMillis() - 3600000L
        ),
        PlaybackHistory(
            sourceId = "smb_1",
            path = "smb://192.168.1.100/share/Videos/Anime_EP01.mkv",
            title = "Anime EP01",
            sourceType = SourceType.Smb,
            positionMs = 1200000L,
            durationMs = 1440000L,
            lastPlayedAt = System.currentTimeMillis() - 86400000L
        ),
        PlaybackHistory(
            sourceId = MediaSource.INTERNAL_STORAGE_ID,
            path = "/storage/emulated/0/Downloads/Short_Clip.mp4",
            title = "Short Clip",
            positionMs = 180000L,
            durationMs = 180000L,
            lastPlayedAt = System.currentTimeMillis() - 172800000L,
            isCompleted = true
        )
    )
    val historyItems = flowOf(PagingData.from(mockHistory)).collectAsLazyPagingItems()
    val localSource = MediaSource.Local(
        id = MediaSource.INTERNAL_STORAGE_ID,
        name = "Internal Storage",
        rootPath = "/root",
        totalSpace = 128L * 1024 * 1024 * 1024,
        freeSpace = 50L * 1024 * 1024 * 1024
    )
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mode = BrowserMode.History,
                historyState = HistoryBrowserState.Content(),
                source = BrowserSourceState(
                    current = localSource,
                    available = listOf(localSource),
                    permissionGranted = true
                )
            ),
            historyItems = historyItems,
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Playback History Content - Dark")
@Composable
fun BrowserHistoryContentDarkPreview() {
    val mockHistory = listOf(
        PlaybackHistory(
            sourceId = MediaSource.INTERNAL_STORAGE_ID,
            path = "/storage/emulated/0/Movies/Inception.mp4",
            title = "Inception",
            positionMs = 3600000L,
            durationMs = 8880000L,
            lastPlayedAt = System.currentTimeMillis() - 3600000L
        )
    )
    val historyItems = flowOf(PagingData.from(mockHistory)).collectAsLazyPagingItems()
    val localSource = MediaSource.Local(
        id = MediaSource.INTERNAL_STORAGE_ID,
        name = "Internal Storage",
        rootPath = "/root",
        totalSpace = 128L * 1024 * 1024 * 1024,
        freeSpace = 50L * 1024 * 1024 * 1024
    )
    AppTheme(themeOptions = ThemeOptions(themeMode = ThemeMode.Dark)) {
        BrowserScreenContent(
            state = BrowserState(
                mode = BrowserMode.History,
                historyState = HistoryBrowserState.Content(),
                source = BrowserSourceState(
                    current = localSource,
                    available = listOf(localSource),
                    permissionGranted = true
                )
            ),
            historyItems = historyItems,
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Playback History Empty")
@Composable
fun BrowserHistoryEmptyPreview() {
    val historyItems =
        flowOf(PagingData.from(emptyList<PlaybackHistory>())).collectAsLazyPagingItems()
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mode = BrowserMode.History,
                historyState = HistoryBrowserState.Empty
            ),
            historyItems = historyItems,
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Playlist List - Level 1")
@Composable
fun BrowserPlaylistListPreview() {
    val mockPlaylists = listOf(
        Playlist(id = "1", title = "Favorites", itemCount = 12),
        Playlist(id = "2", title = "Movies to Watch", itemCount = 5),
        Playlist(id = "3", title = "Anime Series", itemCount = 24)
    )
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mode = BrowserMode.Playlist,
                playlistState = PlaylistBrowserState.ListContent(mockPlaylists)
            ),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Playlist Empty - Level 1")
@Composable
fun BrowserPlaylistListEmptyPreview() {
    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mode = BrowserMode.Playlist,
                playlistState = PlaylistBrowserState.Empty
            ),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Playlist Detail - Level 2")
@Composable
fun BrowserPlaylistDetailPreview() {
    val mockPlaylist = Playlist(id = "1", title = "Favorites", itemCount = 2)
    val mockItems = listOf(
        PlaylistItem(
            id = "item_1",
            playlistId = "1",
            media = MediaFile.Video(
                path = "/storage/emulated/0/Movies/Inception.mp4",
                name = "Inception.mp4",
                sourceType = SourceType.Local,
                id = "video_1",
                sourceId = MediaSource.INTERNAL_STORAGE_ID,
                duration = 1000 * 60 * 148,
                size = 1024L * 1024 * 1024 * 2,
                extension = "mp4"
            )
        ),
        PlaylistItem(
            id = "item_2",
            playlistId = "1",
            media = MediaFile.Video(
                path = "smb://192.168.1.100/share/Videos/Anime_EP01.mkv",
                name = "Anime_EP01.mkv",
                sourceType = SourceType.Smb,
                id = "video_2",
                sourceId = "smb_1",
                duration = 1000 * 60 * 24,
                size = 1024L * 1024 * 450,
                extension = "mkv"
            )
        )
    )

    AppTheme {
        BrowserScreenContent(
            state = BrowserState(
                mode = BrowserMode.Playlist,
                selectedPlaylistId = "1",
                playlistState = PlaylistBrowserState.DetailContent(mockPlaylist)
            ),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            playlistItems = flowOf(PagingData.from(mockItems)).collectAsLazyPagingItems(),
            actions = BrowserActions()
        )
    }
}

@Preview(showBackground = true, name = "Playlist Detail - Level 2 (Dark)")
@Composable
fun BrowserPlaylistDetailDarkPreview() {
    val mockPlaylist = Playlist(id = "1", title = "Favorites", itemCount = 2)
    val mockItems = listOf(
        PlaylistItem(
            id = "item_1",
            playlistId = "1",
            media = MediaFile.Video(
                path = "/storage/emulated/0/Movies/Inception.mp4",
                name = "Inception.mp4",
                sourceType = SourceType.Local,
                id = "video_1",
                sourceId = MediaSource.INTERNAL_STORAGE_ID,
                duration = 1000 * 60 * 148,
                size = 1024L * 1024 * 1024 * 2,
                extension = "mp4"
            )
        )
    )

    AppTheme(themeOptions = ThemeOptions(themeMode = ThemeMode.Dark)) {
        BrowserScreenContent(
            state = BrowserState(
                mode = BrowserMode.Playlist,
                selectedPlaylistId = "1",
                playlistState = PlaylistBrowserState.DetailContent(mockPlaylist),
            ),
            searchBarState = rememberSearchBarState(),
            textFieldState = rememberTextFieldState(),
            scrollState = rememberLazyListState(),
            playlistItems = flowOf(PagingData.from(mockItems)).collectAsLazyPagingItems(),
            actions = BrowserActions()
        )
    }
}
