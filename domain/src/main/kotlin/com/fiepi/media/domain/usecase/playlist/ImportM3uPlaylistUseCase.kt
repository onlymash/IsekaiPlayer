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

package com.fiepi.media.domain.usecase.playlist

import com.fiepi.media.domain.repository.playlist.PlaylistRepository

class ImportM3uPlaylistUseCase(
    private val repository: PlaylistRepository
) {
    suspend fun fromUrl(title: String, url: String): String =
        repository.importM3uFromUrl(title, url)

    suspend fun fromUri(title: String, uriString: String): String =
        repository.importM3uFromUri(title, uriString)
}
