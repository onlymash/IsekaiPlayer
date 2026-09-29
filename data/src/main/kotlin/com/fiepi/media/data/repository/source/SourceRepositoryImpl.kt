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

package com.fiepi.media.data.repository.source

import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.repository.source.LocalSourceRepository
import com.fiepi.media.domain.repository.source.RemoteSourceRepository
import com.fiepi.media.domain.repository.source.SourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SourceRepositoryImpl(
    private val localSourceRepo: LocalSourceRepository,
    private val remoteSourceRepo: RemoteSourceRepository
) : SourceRepository {

    override fun getSources(): Flow<List<MediaSource>> = combine(
        localSourceRepo.getSources(),
        remoteSourceRepo.getSources()
    ) { local, remote ->
        local + remote.map { MediaSource.Remote(it) }
    }

    override fun getLocalSources(): Flow<List<MediaSource.Local>> {
        return localSourceRepo.getSources()
    }

    override fun getRemoteSources(): Flow<List<RemoteSource>> {
        return remoteSourceRepo.getSources()
    }

    override suspend fun getRemoteSourceById(id: String): RemoteSource? {
        return remoteSourceRepo.getSourceById(id)
    }

    override suspend fun saveRemoteSource(source: RemoteSource) {
        remoteSourceRepo.saveSource(source)
    }

    override suspend fun updateRemoteSources(sources: List<RemoteSource>) {
        remoteSourceRepo.updateSources(sources)
    }

    override suspend fun deleteRemoteSource(source: RemoteSource) {
        remoteSourceRepo.deleteSource(source)
    }

    override suspend fun getMaxSortOrder(): Int {
        return remoteSourceRepo.getMaxSortOrder()
    }
}
