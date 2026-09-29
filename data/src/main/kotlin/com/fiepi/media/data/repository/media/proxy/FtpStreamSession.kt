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

import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.source.RemoteSource
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Session context for each FTP playback item, managing FTP connection and stream lifecycles.
 */
class FtpStreamSession(
    val source: RemoteSource,
    val filePath: String,
    var fileSize: Long = -1L
) : AutoCloseable {

    private var ftpClient: FTPClient? = null

    @Volatile
    private var isDisposed = false

    @Synchronized
    fun getStream(offset: Long): InputStream {
        if (isDisposed) throw IllegalStateException("Session already closed")

        internalClose()

        if (isDisposed) throw IllegalStateException("Session already closed during init")

        val client = FTPClient().apply {
            controlEncoding = "UTF-8"
            connectTimeout = AppConstants.REMOTE_TIMEOUT_MS.toInt()
            defaultTimeout = AppConstants.REMOTE_TIMEOUT_MS.toInt()
            bufferSize = 4 * 1024 * 1024
        }

        client.connect(source.host, source.port ?: 21)
        if (!FTPReply.isPositiveCompletion(client.replyCode)) {
            client.disconnect()
            throw IOException("FTP server refused connection")
        }
        val loginSuccess = if (source.username != null) {
            client.login(source.username, source.password ?: "")
        } else {
            client.login("anonymous", "")
        }
        if (!loginSuccess) {
            client.disconnect()
            throw IOException("FTP login failed")
        }
        client.setFileType(FTP.BINARY_FILE_TYPE)
        client.enterLocalPassiveMode()
        runCatching { client.sendCommand("OPTS UTF8 ON") }

        if (offset > 0) {
            client.restartOffset = offset
        }

        val rawStream = client.retrieveFileStream(filePath) ?: run {
            runCatching { client.disconnect() }
            throw IOException("Failed to open FTP stream for $filePath")
        }

        this.ftpClient = client
        return object : FilterInputStream(BufferedInputStream(rawStream, 4 * 1024 * 1024)) {
            override fun close() {
                runCatching { super.close() }
                runCatching {
                    if (client.isConnected) {
                        client.completePendingCommand()
                        client.logout()
                        client.disconnect()
                    }
                }
                ftpClient = null
            }
        }
    }

    override fun close() {
        synchronized(this) {
            isDisposed = true
            internalClose()
        }
    }

    private fun internalClose() {
        runCatching {
            ftpClient?.let {
                if (it.isConnected) {
                    it.logout()
                    it.disconnect()
                }
            }
        }
        ftpClient = null
    }
}
