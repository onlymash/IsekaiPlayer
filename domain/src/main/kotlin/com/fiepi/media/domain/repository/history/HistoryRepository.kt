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

package com.fiepi.media.domain.repository.history

import androidx.paging.PagingData
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.source.SourceType
import kotlinx.coroutines.flow.Flow


interface HistoryRepository {
    /**
     * Records/updates playback position
     */
    suspend fun saveHistory(
        sourceId: String,
        path: String,
        positionMs: Long,
        durationMs: Long,
        title: String = "",
        thumbnailUrl: String? = null,
        sourceType: SourceType = SourceType.Local,
        isCompleted: Boolean? = null
    )

    /**
     * Queries the resume position for a video
     */
    suspend fun getHistory(sourceId: String, path: String): PlaybackHistory?

    /**
     * Responsively observes recent playback history
     */
    fun getRecentHistoryFlow(limit: Int = 20): Flow<List<PlaybackHistory>>

    /**
     * Responsively observes paginated playback history records filtered by sourceId and search query.
     */
    fun getHistoryPagingFlow(
        sourceId: String? = null,
        query: String? = null
    ): Flow<PagingData<PlaybackHistory>>

    /**
     * Marks a playback session as completed (e.g., video ended naturally)
     */
    suspend fun markCompleted(sourceId: String, path: String)

    /**
     * Removes playback progress (useful when restarting playback)
     */
    suspend fun deleteHistory(sourceId: String, path: String)

    /**
     * Observes the last played video path in a specific directory
     */
    fun observeLastPlayedInDirectory(sourceId: String, directoryPath: String): Flow<String?>

    /**
     * Observes the total count of playback history records
     */
    fun getHistoryCountFlow(): Flow<Int>

    /**
     * Clears all playback history records
     */
    suspend fun clearAllHistory()
}