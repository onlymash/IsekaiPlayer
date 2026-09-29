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

package com.fiepi.media.domain.usecase.source

import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.repository.source.SourceRepository
import kotlinx.coroutines.flow.first

class GetSourceUseCase(
    private val repository: SourceRepository
) {
    /**
     * Resolves MediaSource by ID from all available sources (local volumes and remote shares),
     * with optional filePath fallback matching for local storage volumes.
     */
    suspend operator fun invoke(id: String, filePath: String? = null): MediaSource? {
        val allSources = repository.getSources().first()

        // Primary match by exact source ID across all local and remote sources
        val matchedById = allSources.find { it.id == id }
        if (matchedById != null) return matchedById

        // Fallback match for local storage volumes by file path prefix
        if (!filePath.isNullOrBlank()) {
            return allSources.filterIsInstance<MediaSource.Local>()
                .find { filePath.startsWith(it.rootPath) }
        }

        return null
    }
}
