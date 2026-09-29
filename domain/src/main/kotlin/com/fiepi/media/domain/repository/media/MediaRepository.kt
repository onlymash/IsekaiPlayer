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
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.RemoteSource
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface MediaRepository {

    /**
     * Generic method to get files at a specific path for a given source.
     * @param path The directory path to list files from.
     * @param source The media source to use.
     * @param options Sorting and filtering options.
     * @param forceRefresh Whether to bypass cache and reload from the storage provider.
     * @throws Exception if the source is connection-related and fails.
     */
    suspend fun getMediaFiles(
        path: String?,
        source: MediaSource,
        options: MediaOptions,
        forceRefresh: Boolean = false
    ): List<MediaFile>

    /**
     * Checks if the data for the given path is already cached in memory.
     */
    fun hasCachedData(path: String, source: MediaSource): Boolean

    /**
     * Searches for media files matching the query under the specified path.
     * @param query The search query string.
     * @param path The root directory to start searching from.
     * @param source The media source to search in.
     */
    fun searchMediaFiles(query: String, path: String?, source: MediaSource): Flow<MediaFile.Video>

    /**
     * Opens a remote file and returns an InputStream.
     * @throws Exception if the source is connection-related and fails.
     */
    suspend fun openRemoteFile(
        path: String,
        source: MediaSource.Remote,
        offset: Long = 0
    ): InputStream

    /**
     * Gets the size of a remote file.
     * @throws Exception if the source is connection-related and fails.
     */
    suspend fun getRemoteFileSize(path: String, source: MediaSource.Remote): Long

    /**
     * Reads a specific range of bytes from a remote file.
     * @throws Exception if the source is connection-related and fails.
     */
    suspend fun readRemoteRange(
        path: String,
        source: MediaSource.Remote,
        range: LongRange
    ): ByteArray

    /**
     * Connect any closed remote source connection.
     * @throws Exception if connection fails with detailed error message.
     */
    fun connectRemote()

    /**
     * Closes any active remote source connection immediately.
     */
    fun disconnectRemote()

    /**
     * Validates a remote source by attempting to connect to it.
     * @throws Exception if connection fails.
     */
    suspend fun validateRemoteSource(source: RemoteSource)
}
