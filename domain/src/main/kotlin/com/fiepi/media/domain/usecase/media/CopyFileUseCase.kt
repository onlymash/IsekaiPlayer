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

import com.fiepi.media.domain.repository.media.LocalMediaRepository

class CopyFileUseCase(
    private val repository: LocalMediaRepository
) {
    suspend operator fun invoke(
        sourcePath: String,
        targetDirectory: String,
        overwrite: Boolean,
        onProgress: ((bytesWritten: Long) -> Unit)? = null
    ): Result<Unit> {
        return repository.copyFile(sourcePath, targetDirectory, overwrite, onProgress)
    }
}
