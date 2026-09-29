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

package com.fiepi.media.domain.utils

import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments

/**
 * Extension property to generate a full connection URL for a [RemoteSource].
 * Uses Ktor's URLBuilder for safe encoding of special characters.
 */
val RemoteSource.url: String
    get() = URLBuilder().apply {
        protocol = when (this@url.type) {
            SourceType.Smb -> URLProtocol("smb", 445)
            SourceType.Ftp -> URLProtocol("ftp", 21)
            SourceType.WebDav -> if (useHttps) URLProtocol.HTTPS else URLProtocol.HTTP
            else -> URLProtocol.HTTP
        }
        host = this@url.host
        this@url.port?.let { port = it }

        val segments = this@url.path?.split("/")?.filter { it.isNotEmpty() } ?: emptyList()
        appendPathSegments(segments)
    }.buildString()

/**
 * Generates a full URL including credentials for streaming.
 * Automatically handles encoding of characters like spaces or brackets in paths.
 */
fun RemoteSource.getAuthenticatedPlayUrl(filePath: String): String {
    return URLBuilder().apply {
        protocol = when (this@getAuthenticatedPlayUrl.type) {
            SourceType.Smb -> URLProtocol("smb", 445)
            SourceType.Ftp -> URLProtocol("ftp", 21)
            SourceType.WebDav -> if (useHttps) URLProtocol.HTTPS else URLProtocol.HTTP
            else -> URLProtocol.HTTP
        }
        host = this@getAuthenticatedPlayUrl.host
        this@getAuthenticatedPlayUrl.port?.let { port = it }

        user = username
        password = this@getAuthenticatedPlayUrl.password

        // Standardize path: 
        // For WebDAV/FTP, filePath is usually the absolute path on the server.
        // For SMB, it's relative to the share, but we use proxy anyway.
        val cleanPath = filePath.trim('/')
        if (cleanPath.isNotEmpty()) {
            pathSegments = cleanPath.split("/")
        }
    }.buildString()
}

/**
 * Extension property to generate a base URL (protocol + host + port) without the path.
 */
val RemoteSource.baseUrl: String
    get() = URLBuilder().apply {
        protocol = when (this@baseUrl.type) {
            SourceType.Smb -> URLProtocol("smb", 445)
            SourceType.Ftp -> URLProtocol("ftp", 21)
            SourceType.WebDav -> if (useHttps) URLProtocol.HTTPS else URLProtocol.HTTP
            else -> URLProtocol.HTTP
        }
        host = this@baseUrl.host
        this@baseUrl.port?.let { port = it }
    }.buildString().removeSuffix("/")
