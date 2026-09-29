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

package com.fiepi.media.domain.model.media

/**
 * Represents standard URI schemes and protocols used by media resolvers and engines.
 */
enum class UriScheme(val scheme: String) {
    File("file"),
    Content("content"),
    Http("http"),
    Https("https"),
    Smb("smb"),
    Ftp("ftp"),
    FdClose("fdclose");

    companion object {
        fun fromScheme(scheme: String?): UriScheme {
            if (scheme.isNullOrBlank()) return File
            return entries.find { it.scheme.equals(scheme, ignoreCase = true) } ?: File
        }

        fun fromPath(path: String?): UriScheme {
            if (path.isNullOrBlank()) return File
            val schemeStr = if (path.contains("://")) path.substringBefore("://") else "file"
            return fromScheme(schemeStr)
        }
    }
}
