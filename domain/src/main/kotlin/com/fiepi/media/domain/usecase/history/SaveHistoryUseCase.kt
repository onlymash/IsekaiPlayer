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

package com.fiepi.media.domain.usecase.history

import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.history.HistoryRepository

class SaveHistoryUseCase(
    private val repository: HistoryRepository
) {
    suspend operator fun invoke(
        sourceId: String,
        path: String,
        positionMs: Long,
        durationMs: Long,
        title: String = "",
        thumbnailUrl: String? = null,
        sourceType: SourceType = SourceType.Local,
        isCompleted: Boolean? = null
    ) = repository.saveHistory(
        sourceId = sourceId,
        path = path,
        positionMs = positionMs,
        durationMs = durationMs,
        title = title,
        thumbnailUrl = thumbnailUrl,
        sourceType = sourceType,
        isCompleted = isCompleted
    )
}
