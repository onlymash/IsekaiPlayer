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

package com.fiepi.media.data.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.fiepi.media.data.database.dao.NetworkStreamHistoryDao
import com.fiepi.media.data.database.dao.PlaybackHistoryDao
import com.fiepi.media.data.database.dao.PlaylistDao
import com.fiepi.media.data.database.dao.RemoteSourceDao
import com.fiepi.media.data.database.entity.NetworkStreamHistoryEntity
import com.fiepi.media.data.database.entity.PlaybackHistoryEntity
import com.fiepi.media.data.database.entity.PlaylistEntity
import com.fiepi.media.data.database.entity.PlaylistItemEntity
import com.fiepi.media.data.database.entity.RemoteSourceEntity

@Database(
    entities = [
        PlaybackHistoryEntity::class,
        RemoteSourceEntity::class,
        NetworkStreamHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun remoteSourceDao(): RemoteSourceDao
    abstract fun networkStreamHistoryDao(): NetworkStreamHistoryDao
    abstract fun playlistDao(): PlaylistDao
}
