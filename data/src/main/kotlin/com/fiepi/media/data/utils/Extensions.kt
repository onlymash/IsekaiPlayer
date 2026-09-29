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

package com.fiepi.media.data.utils

import android.webkit.MimeTypeMap
import com.fiepi.media.domain.utils.FileUtils
import com.fiepi.media.domain.utils.MimeTypeUtils
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.connection.Connection
import io.ktor.client.HttpClient


/**
 * Provides extension for safe closing of any AutoCloseable object
 */
fun AutoCloseable.closeSilently() {
    runCatching { close() }
}

/**
 * Provides safe closing for SMBClient (which does not inherit standard AutoCloseable)
 */
fun SMBClient.closeSilently() {
    runCatching { close() }
}

/**
 * Provides safe closing for Connection (which does not inherit standard AutoCloseable)
 */
fun Connection.closeSilently(force: Boolean = false) {
    runCatching { close(force) }
}

/**
 * Provides safe closing for HttpClient
 */
fun HttpClient.closeSilently() {
    runCatching { close() }
}

/**
 * Resolves MIME type using domain MimeTypeUtils first, with Android MimeTypeMap as fallback.
 */
fun resolveMimeType(fileNameOrExtension: String?): String? {
    if (fileNameOrExtension.isNullOrBlank()) return null
    return MimeTypeUtils.getMimeType(fileNameOrExtension)
        ?: run {
            val ext = FileUtils.extractExtension(fileNameOrExtension)
                ?: fileNameOrExtension.trim().lowercase().removePrefix(".")
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        }
}