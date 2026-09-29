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

package com.fiepi.media.data.repository.stream

import com.fiepi.media.data.database.dao.NetworkStreamHistoryDao
import com.fiepi.media.data.database.entity.NetworkStreamHistoryEntity
import com.fiepi.media.domain.model.stream.NetworkStreamHistory
import com.fiepi.media.domain.repository.stream.NetworkStreamHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NetworkStreamHistoryRepositoryImpl(
    private val dao: NetworkStreamHistoryDao
) : NetworkStreamHistoryRepository {

    override fun getStreamHistoryFlow(): Flow<List<NetworkStreamHistory>> {
        return dao.getAllHistoryFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveStreamUrl(url: String, title: String) =
        withContext(Dispatchers.IO) {
            if (url.isBlank()) return@withContext
            val entity = NetworkStreamHistoryEntity(
                url = url.trim(),
                title = title.ifBlank { url.trim() },
                lastPlayedAt = System.currentTimeMillis()
            )
            dao.upsert(entity)
        }

    override suspend fun deleteStreamUrl(url: String) =
        withContext(Dispatchers.IO) {
            dao.deleteByUrl(url)
        }

    override suspend fun clearStreamHistory() =
        withContext(Dispatchers.IO) {
            dao.clearAll()
        }

    private fun NetworkStreamHistoryEntity.toDomain() = NetworkStreamHistory(
        url = url,
        title = title,
        lastPlayedAt = lastPlayedAt
    )
}
