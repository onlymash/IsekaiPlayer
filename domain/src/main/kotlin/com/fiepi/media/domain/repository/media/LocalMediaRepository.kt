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

package com.fiepi.media.domain.repository.media

import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.MediaOptions
import kotlinx.coroutines.flow.Flow

interface LocalMediaRepository {
    suspend fun getMediaFiles(
        path: String?,
        options: MediaOptions,
        forceRefresh: Boolean = false
    ): List<MediaFile>

    fun observeMediaFiles(
        path: String?,
        options: MediaOptions,
        forceRefresh: Boolean = false
    ): Flow<List<MediaFile>>

    fun searchMediaFiles(query: String, path: String?): Flow<MediaFile.Video>

    fun isCached(): Boolean

    suspend fun renameFile(path: String, newName: String): Result<Unit>

    suspend fun deleteFiles(paths: List<String>): Result<Unit>

    suspend fun copyFile(
        sourcePath: String,
        targetDirectory: String,
        overwrite: Boolean,
        onProgress: ((bytesWritten: Long) -> Unit)? = null
    ): Result<Unit>

    suspend fun moveFile(
        sourcePath: String,
        targetDirectory: String,
        overwrite: Boolean,
        onProgress: ((bytesWritten: Long) -> Unit)? = null
    ): Result<Unit>

    suspend fun createDirectory(parentPath: String, folderName: String): Result<Unit>
}
