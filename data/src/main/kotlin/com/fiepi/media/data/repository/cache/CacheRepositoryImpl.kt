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

package com.fiepi.media.data.repository.cache

import android.content.Context
import com.fiepi.media.domain.model.cache.CacheInfo
import com.fiepi.media.domain.repository.cache.CacheRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class CacheRepositoryImpl(
    private val context: Context
) : CacheRepository {

    private val thumbnailDir: File
        get() = File(context.cacheDir, "thumbnails")

    private val videoCacheDir: File
        get() = File(context.cacheDir, "exoplayer_cache")

    private val _cacheInfo = MutableStateFlow(calculateCacheInfo())

    override fun getCacheInfo(): Flow<CacheInfo> = _cacheInfo.asStateFlow()

    override suspend fun clearThumbnailCache(): Unit = withContext(Dispatchers.IO) {
        deleteDirectoryContents(thumbnailDir)
        refreshCacheInfo()
    }

    override suspend fun clearVideoCache(): Unit = withContext(Dispatchers.IO) {
        deleteDirectoryContents(videoCacheDir)
        refreshCacheInfo()
    }

    private fun calculateCacheInfo(): CacheInfo {
        val thumbSize = getFolderSize(thumbnailDir)
        val videoSize = getFolderSize(videoCacheDir)
        return CacheInfo(
            thumbnailCacheBytes = thumbSize,
            videoCacheBytes = videoSize
        )
    }

    private fun refreshCacheInfo() {
        _cacheInfo.value = calculateCacheInfo()
    }

    private fun getFolderSize(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        val children = file.listFiles() ?: return 0L
        for (child in children) {
            size += getFolderSize(child)
        }
        return size
    }

    @Suppress("SameReturnValue")
    private fun deleteDirectoryContents(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        if (file.isDirectory) {
            val children = file.listFiles() ?: return true
            for (child in children) {
                deleteDirectoryContents(child)
                child.delete()
            }
        } else if (file.isFile) {
            file.delete()
        }
        return true
    }
}
