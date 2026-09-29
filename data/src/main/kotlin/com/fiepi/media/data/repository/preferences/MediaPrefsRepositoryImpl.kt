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

package com.fiepi.media.data.repository.preferences

import androidx.datastore.core.DataStore
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.preferences.MediaPreferences
import com.fiepi.media.domain.repository.preferences.MediaPrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaPrefsRepositoryImpl(
    private val dataStore: DataStore<MediaPreferences>
) : MediaPrefsRepository {

    override fun getSortOptions(isRemote: Boolean): Flow<MediaOptions> =
        dataStore.data.map { prefs ->
            if (isRemote) prefs.remoteOptions else prefs.localOptions
        }

    override suspend fun updateSortOptions(options: MediaOptions, isRemote: Boolean) {
        dataStore.updateData { current ->
            if (isRemote) {
                current.copy(remoteOptions = options)
            } else {
                current.copy(localOptions = options)
            }
        }
    }

    override fun getDisplayFields(isRemote: Boolean): Flow<List<MediaField>> =
        dataStore.data.map { prefs ->
            if (isRemote) prefs.remoteDisplayFields else prefs.localDisplayFields
        }

    override suspend fun updateDisplayFields(fields: List<MediaField>, isRemote: Boolean) {
        dataStore.updateData { current ->
            if (isRemote) {
                current.copy(remoteDisplayFields = fields)
            } else {
                current.copy(localDisplayFields = fields)
            }
        }
    }

    override val selectedSourceId: Flow<String?> = dataStore.data.map { it.selectedSourceId }

    override suspend fun saveSelectedSourceId(id: String?) {
        dataStore.updateData { it.copy(selectedSourceId = id) }
    }
}
