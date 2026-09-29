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

package com.fiepi.media.data.repository.media.proxy

import com.hierynomus.smbj.SmbConfig
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Resilient SMB streaming content response.
 * Optimizations: reuse session handle, use 1MB buffer to increase throughput.
 */
class ResilientSmbStreamContent(
    private val session: SmbStreamSession,
    private val smbConfig: SmbConfig,
    private val startOffset: Long,
    private val endOffset: Long,
    contentSize: Long
) : OutgoingContent.WriteChannelContent() {

    private val shareName: String by lazy {
        session.source.path?.trim('/')?.substringBefore('/') ?: ""
    }

    private val baseSubPath: String by lazy {
        session.source.path?.trim('/')?.substringAfter('/', "") ?: ""
    }

    private fun toSmbPath(path: String): String {
        val cleanPath = path.trim('/')
        val relative = if (cleanPath.isEmpty()) {
            baseSubPath
        } else {
            cleanPath.removePrefix(shareName).trim('/')
        }
        return relative.replace('/', '\\')
    }

    override val contentLength: Long = contentSize

    override suspend fun writeTo(channel: ByteWriteChannel) {
        withContext(Dispatchers.IO) {
            // Increase buffer size to 1MB
            val bufferSize = 1024 * 1024
            val buffer = ByteArray(bufferSize)
            var currentOffset = startOffset
            val bytesToRead = endOffset - startOffset + 1
            var totalRead = 0L

            var retryCount = 0
            val maxRetries = 3
            val smbPath = toSmbPath(session.filePath)

            while (totalRead < bytesToRead) {
                try {
                    // Get reused file handle from session
                    val file = session.getFileHandle(smbConfig, shareName, smbPath)

                    // Read continuously until end or error
                    while (totalRead < bytesToRead) {
                        val readChunk = minOf(bufferSize.toLong(), bytesToRead - totalRead).toInt()

                        // Read directly from the reused handle, saving Open/Close round-trips
                        val actualRead = file.read(buffer, currentOffset, 0, readChunk)
                        if (actualRead <= 0) break

                        channel.writeFully(buffer, 0, actualRead)
                        currentOffset += actualRead
                        totalRead += actualRead
                        retryCount = 0
                    }
                    break
                } catch (_: Exception) {
                    // Read error occurred, mark handle invalid so next loop will reopen
                    session.invalidateHandle()

                    retryCount++
                    if (retryCount > maxRetries) break
                    delay((500L * retryCount).milliseconds)
                }
            }
        }
    }
}
