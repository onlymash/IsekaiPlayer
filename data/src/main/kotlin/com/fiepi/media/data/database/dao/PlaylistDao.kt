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

package com.fiepi.media.data.database.dao

import androidx.paging.PagingSource
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.fiepi.media.data.database.entity.PlaylistEntity
import com.fiepi.media.data.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    @ColumnInfo(name = "item_count") val itemCount: Int
)

data class PlaylistItemOrderInfo(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "added_at") val addedAt: Long
)

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface PlaylistDao {

    @Query(
        """
        SELECT p.*, COUNT(i.id) AS item_count 
        FROM playlist p 
        LEFT JOIN playlist_item i ON p.id = i.playlist_id 
        GROUP BY p.id 
        ORDER BY p.sort_order ASC, p.updated_at DESC
        """
    )
    fun getPlaylistsWithCountFlow(): Flow<List<PlaylistWithCount>>

    @Query("SELECT MAX(sort_order) FROM playlist")
    suspend fun getPlaylistMaxSortOrder(): Int?

    @Query("UPDATE playlist SET sort_order = :sortOrder WHERE id = :playlistId")
    suspend fun updatePlaylistSortOrder(playlistId: String, sortOrder: Int)

    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    fun getPlaylistByIdFlow(playlistId: String): Flow<PlaylistEntity?>

    @Query(
        """
        SELECT p.*, COUNT(i.id) AS item_count 
        FROM playlist p 
        LEFT JOIN playlist_item i ON p.id = i.playlist_id 
        WHERE p.id = :playlistId 
        GROUP BY p.id
        """
    )
    fun getPlaylistWithCountFlow(playlistId: String): Flow<PlaylistWithCount?>

    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    suspend fun getPlaylistByIdSync(playlistId: String): PlaylistEntity?

    @Query("SELECT * FROM playlist_item WHERE playlist_id = :playlistId ORDER BY sort_order ASC, added_at ASC, id ASC LIMIT 1 OFFSET :index")
    suspend fun getPlaylistItemByIndex(playlistId: String, index: Int): PlaylistItemEntity?

    @Query("SELECT COUNT(*) FROM playlist_item WHERE playlist_id = :playlistId")
    suspend fun getPlaylistItemCount(playlistId: String): Int

    @Query("SELECT id, sort_order, added_at FROM playlist_item WHERE playlist_id = :playlistId AND (id = :mediaId OR media_id = :mediaId) LIMIT 1")
    suspend fun getItemOrder(playlistId: String, mediaId: String): PlaylistItemOrderInfo?

    @Query("SELECT COUNT(*) FROM playlist_item WHERE playlist_id = :playlistId AND (sort_order < :sortOrder OR (sort_order = :sortOrder AND added_at < :addedAt) OR (sort_order = :sortOrder AND added_at = :addedAt AND id < :id))")
    suspend fun getCountBefore(playlistId: String, sortOrder: Int, addedAt: Long, id: String): Int

    suspend fun getPlaylistItemIndexByMediaId(playlistId: String, mediaId: String): Int? {
        val target = getItemOrder(playlistId, mediaId) ?: return null
        return getCountBefore(playlistId, target.sortOrder, target.addedAt, target.id)
    }

    @Query("SELECT * FROM playlist_item WHERE playlist_id = :playlistId ORDER BY sort_order ASC, added_at ASC, id ASC")
    fun getPlaylistItemsFlow(playlistId: String): Flow<List<PlaylistItemEntity>>

    @Query(
        """
        SELECT * FROM playlist_item 
        WHERE (:playlistId IS NULL OR playlist_id = :playlistId) 
          AND (:query IS NULL OR :query = '' OR name LIKE '%' || :query || '%') 
        ORDER BY sort_order ASC, added_at ASC, id ASC
        """
    )
    fun getPlaylistItemsPagingSource(
        playlistId: String?,
        query: String?
    ): PagingSource<Int, PlaylistItemEntity>

    @Query("SELECT * FROM playlist_item WHERE playlist_id = :playlistId ORDER BY sort_order ASC, added_at ASC, id ASC")
    suspend fun getPlaylistItemsSync(playlistId: String): List<PlaylistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlist SET title = :newTitle, updated_at = :updatedAt WHERE id = :playlistId")
    suspend fun updatePlaylistTitle(
        playlistId: String,
        newTitle: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE playlist SET updated_at = :updatedAt WHERE id = :playlistId")
    suspend fun updatePlaylistTimestamp(
        playlistId: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Query("SELECT MAX(sort_order) FROM playlist_item WHERE playlist_id = :playlistId")
    suspend fun getMaxSortOrder(playlistId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItems(items: List<PlaylistItemEntity>)

    @Query("SELECT sort_order FROM playlist_item WHERE id = :itemId")
    suspend fun getPlaylistItemSortOrder(itemId: String): Int?

    @Query("UPDATE playlist_item SET sort_order = :sortOrder WHERE id = :itemId")
    suspend fun updatePlaylistItemSortOrder(itemId: String, sortOrder: Int)

    @Transaction
    suspend fun swapPlaylistItems(itemId1: String, itemId2: String) {
        val sort1 = getPlaylistItemSortOrder(itemId1) ?: return
        val sort2 = getPlaylistItemSortOrder(itemId2) ?: return
        updatePlaylistItemSortOrder(itemId1, sort2)
        updatePlaylistItemSortOrder(itemId2, sort1)
    }

    @Query("DELETE FROM playlist_item WHERE id = :itemId")
    suspend fun deletePlaylistItemById(itemId: String)

    @Query("DELETE FROM playlist_item WHERE playlist_id = :playlistId AND source_id = :sourceId AND path = :path")
    suspend fun deletePlaylistItemByPath(playlistId: String, sourceId: String, path: String)

    @Query("DELETE FROM playlist_item WHERE playlist_id = :playlistId")
    suspend fun clearPlaylistItems(playlistId: String)

    @Update
    suspend fun updatePlaylistItems(items: List<PlaylistItemEntity>)
}
