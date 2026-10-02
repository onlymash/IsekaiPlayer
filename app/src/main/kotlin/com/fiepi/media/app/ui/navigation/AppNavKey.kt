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

package com.fiepi.media.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AppNavKey : NavKey {

    @Serializable
    data object Browser : AppNavKey()

    @Serializable
    data object Settings : AppNavKey()

    @Serializable
    data class SourceEditor(
        @SerialName("id")
        val id: String? = null
    ) : AppNavKey()

    @Serializable
    data object SourceManager : AppNavKey()

    @Serializable
    data object NetworkStream : AppNavKey()

    @Serializable
    data object FileTaskQueue : AppNavKey()

    @Serializable
    data class Player(
        @SerialName("source_id")
        val sourceId: String,
        @SerialName("video_path")
        val videoPath: String
    ) : AppNavKey()

    @Serializable
    data class PlaylistPlayer(
        @SerialName("playlist_id")
        val playlistId: String,
        @SerialName("initial_media_id")
        val initialMediaId: String? = null
    ) : AppNavKey()
}
