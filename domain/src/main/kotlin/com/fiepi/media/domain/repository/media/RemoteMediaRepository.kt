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
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface RemoteMediaRepository {
    val isConnected: Boolean

    data class SpaceInfo(val totalSpace: Long, val freeSpace: Long)

    /**
     * Connects to the remote source.
     * @throws Exception if connection fails with detailed error message.
     */
    suspend fun connect()
    suspend fun disconnect()

    suspend fun getMediaFiles(path: String): List<MediaFile>
    fun searchMediaFiles(query: String, path: String): Flow<MediaFile.Video>
    suspend fun openFile(path: String, offset: Long = 0): InputStream
    suspend fun getFileSize(path: String): Long
    suspend fun readRange(path: String, range: LongRange): ByteArray

    /**
     * Retrieves the storage space information of the remote source.
     * Returns null if the protocol does not support this operation.
     */
    suspend fun getSpaceInfo(): SpaceInfo?
}
