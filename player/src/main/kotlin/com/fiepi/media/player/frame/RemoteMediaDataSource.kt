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

package com.fiepi.media.player.frame

import android.media.MediaDataSource
import android.util.Log
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.repository.media.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * A seekable bridge between Android's synchronous MediaMetadataRetriever and 
 * our asynchronous MediaRepository.
 */
class RemoteMediaDataSource(
    private val path: String,
    private val mediaSource: MediaSource.Remote,
    private val mediaRepository: MediaRepository
) : MediaDataSource() {

    // Size is retrieved once and cached to avoid redundant network calls
    private var cachedSize: Long = -2L

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        val fileSize = getSize()
        if (position >= fileSize || fileSize <= 0) return -1
        
        val end = (position + size - 1).coerceAtMost(fileSize - 1)
        val range = position..end
        
        return try {
            // MediaDataSource is a synchronous API, so we must use runBlocking here.
            // We use a timeout to prevent hanging the retriever forever if the network fails.
            val data = runBlocking {
                withTimeoutOrNull(15.seconds) {
                    mediaRepository.readRemoteRange(path, mediaSource, range)
                }
            }
            
            if (data == null) {
                Log.w("RemoteDataSource", "Read timed out for $path at $position")
                return -1
            }
            
            data.copyInto(buffer, offset)
            data.size
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("RemoteDataSource", "Read error for $path at $position: ${e.message}")
            -1
        }
    }

    override fun getSize(): Long {
        if (cachedSize == -2L) {
            cachedSize = try {
                runBlocking {
                    withTimeoutOrNull(10.seconds) {
                        mediaRepository.getRemoteFileSize(path, mediaSource)
                    }
                } ?: -1L
            } catch (e: Exception) {
                Log.w("RemoteDataSource", "Failed to get size for $path: ${e.message}")
                -1L
            }
        }
        return cachedSize
    }

    override fun close() {
        // No-op, managed by repository or caller
    }
}
