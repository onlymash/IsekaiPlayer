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

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.fiepi.media.data.database.entity.NetworkStreamHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkStreamHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stream: NetworkStreamHistoryEntity)

    @Query("SELECT * FROM network_stream_history ORDER BY last_played_at DESC")
    fun getAllHistoryFlow(): Flow<List<NetworkStreamHistoryEntity>>

    @Query("DELETE FROM network_stream_history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM network_stream_history")
    suspend fun clearAll()
}
