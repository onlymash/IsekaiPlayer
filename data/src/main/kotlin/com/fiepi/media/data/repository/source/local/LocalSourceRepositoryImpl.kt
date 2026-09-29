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

package com.fiepi.media.data.repository.source.local

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.getSystemService
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.repository.source.LocalSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

class LocalSourceRepositoryImpl(
    private val context: Context
) : LocalSourceRepository {

    override fun getSources(): Flow<List<MediaSource.Local>> = callbackFlow {
        val storageManager = context.getSystemService<StorageManager>()!!
        val callback = object : StorageManager.StorageVolumeCallback() {
            override fun onStateChanged(volume: StorageVolume) {
                trySend(queryLocalSources(storageManager))
            }
        }

        storageManager.registerStorageVolumeCallback(context.mainExecutor, callback)
        trySend(queryLocalSources(storageManager))

        awaitClose {
            storageManager.unregisterStorageVolumeCallback(callback)
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    private fun queryLocalSources(storageManager: StorageManager): List<MediaSource.Local> {
        val sources = mutableListOf<MediaSource.Local>()
        val volumes = storageManager.storageVolumes

        volumes.forEach { volume ->
            val rootFile = volume.directory
            if (rootFile != null) {
                val isPrimary = volume.isPrimary
                val id =
                    if (isPrimary) MediaSource.INTERNAL_STORAGE_ID else volume.uuid ?: rootFile.name

                sources.add(
                    MediaSource.Local(
                        id = id,
                        name = volume.getDescription(context),
                        rootPath = rootFile.absolutePath,
                        isRemovable = volume.isRemovable,
                        totalSpace = rootFile.totalSpace,
                        freeSpace = rootFile.freeSpace
                    )
                )
            }
        }
        return sources
    }
}