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

package com.fiepi.media.domain.model.playlist

import java.util.UUID

enum class PlaylistType(val value: String) {
    NORMAL("normal"),       // Normal user-created blank playlist (allows manual video additions)
    M3U_LINK("m3u_link"),   // Imported via M3U URL link (readonly items, supports online refresh)
    M3U_FILE("m3u_file");   // Imported via local M3U file (readonly items, supports re-syncing by picking file)

    companion object {
        fun fromValue(value: String): PlaylistType {
            return entries.find { it.value == value } ?: NORMAL
        }
    }
}

data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val coverUrl: String? = null,
    val itemCount: Int = 0,
    val type: PlaylistType = PlaylistType.NORMAL,
    val sourceUrl: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * True if user can manually add arbitrary videos to this playlist.
     * M3U imported playlists are managed automatically by M3U source and cannot be manually edited.
     */
    val isEditable: Boolean get() = type == PlaylistType.NORMAL

    /**
     * True if playlist can be refreshed from its online link or re-selected local file.
     */
    val isRefreshable: Boolean get() = type == PlaylistType.M3U_LINK || type == PlaylistType.M3U_FILE
}
