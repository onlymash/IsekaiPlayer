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

package com.fiepi.media.domain.player

import com.fiepi.media.domain.model.source.RemoteSource

/**
 * Embedded streaming proxy server interface
 */
interface MediaStreamServer {
    /**
     * Starts the server
     * @param port Specified port, 0 means automatically search for an available port
     * @return The final bound port
     */
    fun start(port: Int = 0): Int

    /**
     * Stops the server
     */
    fun stop()

    /**
     * Whether the server is running
     */
    fun isRunning(): Boolean

    /**
     * Registers a new media streaming session
     * @param sessionId Unique session ID
     * @param source Remote source configuration
     * @param filePath Remote file path
     * @param knownFileSize Known file size, -1 for unknown
     * @return Accessible URL of the proxy server
     */
    fun registerMediaSession(
        sessionId: String,
        source: RemoteSource,
        filePath: String,
        knownFileSize: Long = -1L
    ): String

    /**
     * Unregisters the specified session
     */
    fun unregisterSession(sessionId: String)

    /**
     * Clears all registered sessions
     */
    fun clearAllSessions()
}
