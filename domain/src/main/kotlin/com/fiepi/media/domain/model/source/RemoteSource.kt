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

@Serializable
data class RemoteSource(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: SourceType,
    @SerialName("host")
    val host: String,
    @SerialName("port")
    val port: Int? = null,
    @SerialName("username")
    val username: String? = null,
    @SerialName("password")
    val password: String? = null,
    @SerialName("path")
    val path: String? = null,
    @SerialName("use_https")
    val useHttps: Boolean = false,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("total_space")
    val totalSpace: Long? = null,
    @SerialName("free_space")
    val freeSpace: Long? = null
) {
    /**
     * Checks if two sources have the same connection configuration.
     * Excludes metadata like name, sortOrder, and storage space info.
     */
    fun isSameConnection(other: RemoteSource): Boolean {
        return id == other.id &&
                type == other.type &&
                host == other.host &&
                port == other.port &&
                username == other.username &&
                password == other.password &&
                path == other.path &&
                useHttps == other.useHttps
    }
}
