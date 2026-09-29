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

package com.fiepi.media.domain.model.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Categorizes the origin / storage type of media source.
 */
@Serializable
enum class SourceType(val value: String) {
    @SerialName("local")
    Local("local"),      // Internal / External storage

    @SerialName("smb")
    Smb("smb"),        // SMB Network Share

    @SerialName("ftp")
    Ftp("ftp"),        // FTP / FTPS Server

    @SerialName("dav")
    WebDav("dav"),     // WebDAV Cloud Storage

    @SerialName("external")
    External("external"),    // Externally loaded (SAF picked files, Intent data)

    @SerialName("stream")
    Stream("stream");        // Direct network streams (HTTP/HTTPS/RTSP/RTMP URLs, M3U streams)

    val isRemote: Boolean get() = this != Local && this != External

    companion object {
        fun fromValue(value: String): SourceType {
            return entries.find { it.value == value } ?: Local
        }
    }
}
