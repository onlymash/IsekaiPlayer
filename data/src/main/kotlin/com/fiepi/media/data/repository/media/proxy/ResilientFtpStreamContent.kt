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

import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.time.Duration.Companion.milliseconds

/**
 * Resilient FTP streaming content response.
 */
class ResilientFtpStreamContent(
    private val session: FtpStreamSession,
    private val startOffset: Long,
    private val endOffset: Long,
    contentSize: Long
) : OutgoingContent.WriteChannelContent() {

    override val contentLength: Long = contentSize

    override suspend fun writeTo(channel: ByteWriteChannel) {
        withContext(Dispatchers.IO) {
            val bufferSize = 1024 * 1024
            val buffer = ByteArray(bufferSize)
            var currentOffset = startOffset
            val bytesToRead = endOffset - startOffset + 1
            var totalRead = 0L

            var retryCount = 0
            val maxRetries = 3

            while (totalRead < bytesToRead) {
                var stream: InputStream? = null
                try {
                    stream = session.getStream(currentOffset)
                    while (totalRead < bytesToRead) {
                        val readChunk = minOf(bufferSize.toLong(), bytesToRead - totalRead).toInt()
                        val actualRead = stream.read(buffer, 0, readChunk)
                        if (actualRead <= 0) break

                        channel.writeFully(buffer, 0, actualRead)
                        currentOffset += actualRead
                        totalRead += actualRead
                        retryCount = 0
                    }
                    break
                } catch (_: Exception) {
                    runCatching { stream?.close() }
                    retryCount++
                    if (retryCount > maxRetries) break
                    delay((500L * retryCount).milliseconds)
                } finally {
                    runCatching { stream?.close() }
                }
            }
        }
    }
}
