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

package com.fiepi.media.data.repository.history

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.fiepi.media.data.database.dao.PlaybackHistoryDao
import com.fiepi.media.data.database.entity.PlaybackHistoryEntity
import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.history.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext


class HistoryRepositoryImpl(
    private val dao: PlaybackHistoryDao
) : HistoryRepository {

    override suspend fun saveHistory(
        sourceId: String,
        path: String,
        positionMs: Long,
        durationMs: Long,
        title: String,
        thumbnailUrl: String?,
        sourceType: SourceType,
        isCompleted: Boolean?
    ) = withContext(Dispatchers.IO) {
        if (path.isBlank() || durationMs <= 0L) return@withContext
        // If isCompleted is explicitly passed, use it; otherwise automatically determine by playback ratio (> 95% considered completed)
        val finalIsCompleted =
            isCompleted ?: ((positionMs.toDouble() / durationMs.toDouble()) >= 0.95)

        val existingEntity = dao.getByPath(sourceId, path)

        // Preserve title from existing record (prevents background periodic updates without video title from overwriting with "")
        val finalTitle = title.ifBlank {
            existingEntity?.title ?: ""
        }

        // Preserve thumbnailUrl from existing record if current parameter is null or blank
        val finalThumbnailUrl = thumbnailUrl.takeIf { !it.isNullOrBlank() }
            ?: existingEntity?.thumbnailUrl

        // Preserve non-default sourceType if existing record has it and caller passed Local by default
        val finalSourceType = if (sourceType != SourceType.Local) {
            sourceType
        } else {
            existingEntity?.sourceType?.let { SourceType.fromValue(it) } ?: sourceType
        }

        val historyEntity = PlaybackHistoryEntity(
            id = existingEntity?.id ?: 0L,
            sourceId = sourceId,
            path = path,
            title = finalTitle,
            thumbnailUrl = finalThumbnailUrl,
            sourceType = finalSourceType.value,
            positionMs = if (finalIsCompleted) 0L else positionMs,
            durationMs = durationMs,
            lastPlayedAt = System.currentTimeMillis(),
            isCompleted = finalIsCompleted
        )
        dao.upsert(historyEntity)
    }


    override suspend fun getHistory(sourceId: String, path: String): PlaybackHistory? =
        withContext(Dispatchers.IO) {
            if (path.isBlank()) return@withContext null
            val history = dao.getByPath(sourceId, path)

            // If a record exists but was marked as completed, return null so it starts from the beginning
            if (history != null && history.isCompleted) {
                return@withContext null
            }
            return@withContext history?.toDomain()
        }

    override fun getRecentHistoryFlow(limit: Int): Flow<List<PlaybackHistory>> {
        return dao.getRecentUncompletedFlow(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getHistoryPagingFlow(
        sourceId: String?,
        query: String?
    ): Flow<PagingData<PlaybackHistory>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                dao.getHistoryPagingSource(
                    sourceId = sourceId,
                    query = query?.ifBlank { null }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDomain() }
        }
    }

    override suspend fun markCompleted(sourceId: String, path: String) =
        withContext(Dispatchers.IO) {
            val existing = dao.getByPath(sourceId, path) ?: return@withContext
            val updated = existing.copy(
                positionMs = 0L,
                isCompleted = true,
                lastPlayedAt = System.currentTimeMillis()
            )
            dao.upsert(updated)
        }

    override suspend fun deleteHistory(sourceId: String, path: String) =
        withContext(Dispatchers.IO) {
            dao.deleteByPath(sourceId, path)
        }

    override fun observeLastPlayedInDirectory(
        sourceId: String,
        directoryPath: String
    ): Flow<String?> {
        return dao.observeLastPlayedInDirectory(sourceId, directoryPath)
    }

    override fun getHistoryCountFlow(): Flow<Int> = dao.getCountFlow()

    override suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    private fun PlaybackHistory.toEntity() = PlaybackHistoryEntity(
        sourceId = sourceId,
        path = path,
        title = title,
        thumbnailUrl = thumbnailUrl,
        sourceType = sourceType.value,
        positionMs = positionMs,
        durationMs = durationMs,
        lastPlayedAt = lastPlayedAt,
        isCompleted = isCompleted
    )

    private fun PlaybackHistoryEntity.toDomain() = PlaybackHistory(
        sourceId = sourceId,
        path = path,
        title = title,
        thumbnailUrl = thumbnailUrl,
        sourceType = SourceType.fromValue(sourceType),
        positionMs = positionMs,
        durationMs = durationMs,
        lastPlayedAt = lastPlayedAt,
        isCompleted = isCompleted
    )

}