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

import com.fiepi.media.data.utils.closeSilently
import com.fiepi.media.domain.model.source.RemoteSource
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import java.util.EnumSet

/**
 * Session context for each playback item, managing SMB connection and file handle lifecycles.
 */
class SmbStreamSession(
    val source: RemoteSource,
    val filePath: String,
    var fileSize: Long = -1L
) : AutoCloseable {

    private var smbClient: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null
    private var fileHandle: File? = null

    @Volatile
    private var isDisposed = false

    /**
     * Gets or establishes DiskShare for the current session.
     */
    @Synchronized
    fun getDiskShare(config: SmbConfig, shareName: String): DiskShare {
        if (isDisposed) throw IllegalStateException("Session already closed")

        val currentConn = connection
        if (diskShare == null || currentConn == null || !currentConn.isConnected) {
            internalClose()

            // Re-check state to prevent release during internalClose
            if (isDisposed) throw IllegalStateException("Session already closed during init")

            val client = SMBClient(config)
            val conn = client.connect(source.host, source.port ?: 445)
            val authContext = if (source.username != null) {
                AuthenticationContext(source.username, source.password?.toCharArray(), null)
            } else {
                AuthenticationContext.anonymous()
            }
            val sess = conn.authenticate(authContext)
            val share = sess.connectShare(shareName) as DiskShare

            this.smbClient = client
            this.connection = conn
            this.session = sess
            this.diskShare = share
        }
        return diskShare!!
    }

    /**
     * Gets or opens the file handle for the current session.
     */
    @Synchronized
    fun getFileHandle(config: SmbConfig, shareName: String, smbPath: String): File {
        // If DiskShare or Connection is invalidated, trigger reconnect
        getDiskShare(config, shareName)

        if (fileHandle == null) {
            fileHandle = diskShare!!.openFile(
                smbPath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ),
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
        }
        return fileHandle!!
    }

    /**
     * Marks current handle invalid (e.g. called on read error) to reopen on next getFileHandle call.
     */
    @Synchronized
    fun invalidateHandle() {
        fileHandle?.closeSilently()
        fileHandle = null
    }

    override fun close() {
        synchronized(this) {
            isDisposed = true
            internalClose()
        }
    }

    private fun internalClose() {
        fileHandle?.closeSilently()
        diskShare?.closeSilently()
        session?.closeSilently()
        connection?.closeSilently()
        smbClient?.closeSilently()

        fileHandle = null
        diskShare = null
        session = null
        connection = null
        smbClient = null
    }
}
