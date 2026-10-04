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

package com.fiepi.media.domain.usecase.media

import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.repository.media.MediaRepository
import kotlinx.coroutines.flow.Flow

class ObserveMediaFilesUseCase(
    private val repository: MediaRepository
) {
    operator fun invoke(
        currentPath: String?,
        source: MediaSource,
        options: MediaOptions,
        forceRefresh: Boolean = false
    ): Flow<List<MediaFile>> {
        return repository.observeMediaFiles(currentPath, source, options, forceRefresh)
    }
}
