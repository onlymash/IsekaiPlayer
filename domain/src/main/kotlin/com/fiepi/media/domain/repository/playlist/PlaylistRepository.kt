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

package com.fiepi.media.domain.repository.playlist

import androidx.paging.PagingData
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.playlist.Playlist
import com.fiepi.media.domain.model.playlist.PlaylistItem
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    /**
     * Observes all playlists ordered by recent update time.
     */
    fun getPlaylistsFlow(): Flow<List<Playlist>>

    /**
     * Observes a single playlist by ID.
     */
    fun getPlaylistFlow(playlistId: String): Flow<Playlist?>

    /**
     * Observes all media items for a specific playlist sorted by custom sort order.
     */
    fun getPlaylistItemsFlow(playlistId: String): Flow<List<PlaylistItem>>

    /**
     * Observes media items for a specific playlist (or all playlists when playlistId is null) using Paging 3 with optional search query and initial key.
     */
    fun getPlaylistItemsPagingFlow(
        playlistId: String?,
        query: String? = null,
        initialKey: Int? = null
    ): Flow<PagingData<PlaylistItem>>

    /**
     * Gets a single playlist item on-demand by index.
     */
    suspend fun getPlaylistItemByIndex(playlistId: String, index: Int): PlaylistItem?

    /**
     * Gets the total count of items in a playlist.
     */
    suspend fun getPlaylistItemCount(playlistId: String): Int

    /**
     * Finds the index of a media item in a playlist by media/item ID.
     */
    suspend fun getPlaylistItemIndexByMediaId(playlistId: String, mediaId: String): Int?

    /**
     * Creates a new playlist and returns its generated UUID.
     */
    suspend fun createPlaylist(title: String, coverUrl: String? = null): String

    /**
     * Updates the title of an existing playlist.
     */
    suspend fun updatePlaylistTitle(playlistId: String, newTitle: String)

    /**
     * Reorders playlists by providing the new list of playlist IDs in desired order.
     */
    suspend fun reorderPlaylists(playlistIds: List<String>)

    /**
     * Deletes a playlist and all its items via cascade delete.
     */
    suspend fun deletePlaylist(playlistId: String)

    /**
     * Swaps the sort order of two playlist items within a playlist.
     */
    suspend fun swapPlaylistItems(playlistId: String, itemId1: String, itemId2: String)

    /**
     * Adds a single video to the playlist and returns the generated item UUID.
     */
    suspend fun addMediaToPlaylist(playlistId: String, media: MediaFile.Video): String

    /**
     * Adds multiple videos to the playlist in batch.
     */
    suspend fun addMediaListToPlaylist(playlistId: String, mediaList: List<MediaFile.Video>)

    /**
     * Removes a specific item from a playlist by playlist_item UUID.
     */
    suspend fun removePlaylistItem(itemId: String)

    /**
     * Removes a video from a playlist by sourceId and path.
     */
    suspend fun removeMediaFromPlaylist(playlistId: String, sourceId: String, path: String)

    /**
     * Reorders playlist items according to the provided ordered list of item UUIDs.
     */
    suspend fun reorderPlaylistItems(playlistId: String, itemIds: List<String>)

    /**
     * Clears all items in a playlist while keeping the playlist itself.
     */
    suspend fun clearPlaylistItems(playlistId: String)

    /**
     * Imports an M3U playlist from a network URL string and returns the generated playlist ID.
     */
    suspend fun importM3uFromUrl(title: String, url: String): String

    /**
     * Imports an M3U playlist from a local stream or Uri string and returns the generated playlist ID.
     */
    suspend fun importM3uFromUri(title: String, uriString: String): String

    /**
     * Refreshes / re-syncs an M3U playlist from its original source URL.
     */
    suspend fun refreshPlaylist(playlistId: String)

    /**
     * Refreshes an M3U file playlist using a newly selected file Uri string.
     */
    suspend fun refreshM3uFilePlaylist(playlistId: String, uriString: String)
}
