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

package com.fiepi.media.domain.repository.preferences

import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.preferences.MediaOptions
import kotlinx.coroutines.flow.Flow

interface MediaPrefsRepository {

    fun getSortOptions(isRemote: Boolean): Flow<MediaOptions>

    suspend fun updateSortOptions(options: MediaOptions, isRemote: Boolean)

    fun getDisplayFields(isRemote: Boolean): Flow<List<MediaField>>

    suspend fun updateDisplayFields(fields: List<MediaField>, isRemote: Boolean)

    val selectedSourceId: Flow<String?>
    suspend fun saveSelectedSourceId(id: String?)
}
