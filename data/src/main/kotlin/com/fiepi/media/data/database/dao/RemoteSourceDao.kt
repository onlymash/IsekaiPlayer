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
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.fiepi.media.data.database.entity.RemoteSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteSourceDao {
    @Query("SELECT * FROM remote_sources ORDER BY sort_order ASC, created_at ASC")
    fun getAll(): Flow<List<RemoteSourceEntity>>

    @Query("SELECT * FROM remote_sources WHERE id = :id")
    suspend fun getById(id: String): RemoteSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: RemoteSourceEntity)

    @Delete
    suspend fun delete(source: RemoteSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAll(sources: List<RemoteSourceEntity>)

    @Query("SELECT MAX(sort_order) FROM remote_sources")
    suspend fun getMaxSortOrder(): Int?
}
