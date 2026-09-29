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
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.fiepi.media.data.database.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface PlaybackHistoryDao {
    /**
     * Inserts or replaces/updates playback history record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: PlaybackHistoryEntity)

    /**
     * Queries a single record by URL (used to check resume position on video start)
     */
    @Query("SELECT * FROM playback_history WHERE source_id = :sourceId AND path = :path LIMIT 1")
    suspend fun getByPath(sourceId: String, path: String): PlaybackHistoryEntity?

    /**
     * Gets playback records as a PagingSource with optional sourceId filter and search query.
     */
    @Query(
        """
        SELECT * FROM playback_history 
        WHERE (:sourceId IS NULL OR source_id = :sourceId)
        AND (:query IS NULL OR :query = '' OR title LIKE '%' || :query || '%' OR path LIKE '%' || :query || '%')
        ORDER BY last_played_at DESC
    """
    )
    fun getHistoryPagingSource(
        sourceId: String?,
        query: String?
    ): PagingSource<Int, PlaybackHistoryEntity>

    /**
     * Gets recent uncompleted playback records (e.g. for "Continue Watching" section, default limit 10)
     */
    @Query("SELECT * FROM playback_history WHERE is_completed = 0 AND position_ms > 5000 ORDER BY last_played_at DESC LIMIT :limit")
    fun getRecentUncompletedFlow(limit: Int = 10): Flow<List<PlaybackHistoryEntity>>

    /**
     * Deletes a specified single playback record
     */
    @Query("DELETE FROM playback_history WHERE source_id = :sourceId AND path = :path")
    suspend fun deleteByPath(sourceId: String, path: String)

    /**
     * Cleans up expired records older than specified timestamp (e.g. auto-cleanup records older than 30 days)
     */
    @Query("DELETE FROM playback_history WHERE last_played_at < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    /**
     * Gets the total count of playback history records
     */
    @Query("SELECT COUNT(*) FROM playback_history")
    fun getCountFlow(): Flow<Int>

    /**
     * Clears all playback records
     */
    @Query("DELETE FROM playback_history")
    suspend fun clearAll()

    /**
     * Observes the last played video in a specific directory
     */
    @Query(
        """
        SELECT path FROM playback_history 
        WHERE source_id = :sourceId AND path LIKE :directoryPath || '%' 
        ORDER BY last_played_at DESC 
        LIMIT 1
    """
    )
    fun observeLastPlayedInDirectory(sourceId: String, directoryPath: String): Flow<String?>
}
