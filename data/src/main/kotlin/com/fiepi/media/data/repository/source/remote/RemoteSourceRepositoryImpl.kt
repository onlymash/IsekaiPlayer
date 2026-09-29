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

package com.fiepi.media.data.repository.source.remote

import com.fiepi.media.data.database.dao.RemoteSourceDao
import com.fiepi.media.data.database.entity.RemoteSourceEntity
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.source.RemoteSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteSourceRepositoryImpl(
    private val dao: RemoteSourceDao
) : RemoteSourceRepository {

    override fun getSources(): Flow<List<RemoteSource>> = dao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun getSourceById(id: String): RemoteSource? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun saveSource(source: RemoteSource) {
        dao.insert(source.toEntity())
    }

    override suspend fun updateSources(sources: List<RemoteSource>) {
        dao.updateAll(sources.map { it.toEntity() })
    }

    override suspend fun deleteSource(source: RemoteSource) {
        dao.delete(source.toEntity())
    }

    override suspend fun getMaxSortOrder(): Int {
        return dao.getMaxSortOrder() ?: -1
    }

    private fun RemoteSourceEntity.toDomain() = RemoteSource(
        id = id,
        name = name,
        type = SourceType.fromValue(type),
        host = host,
        port = port,
        username = username,
        password = password,
        path = path,
        useHttps = useHttps,
        sortOrder = sortOrder,
        totalSpace = totalSpace,
        freeSpace = freeSpace
    )

    private fun RemoteSource.toEntity() = RemoteSourceEntity(
        id = id,
        name = name,
        type = type.value,
        host = host,
        port = port,
        username = username,
        password = password,
        path = path,
        useHttps = useHttps,
        sortOrder = sortOrder,
        totalSpace = totalSpace,
        freeSpace = freeSpace
    )
}