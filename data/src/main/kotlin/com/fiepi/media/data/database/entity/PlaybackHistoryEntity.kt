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

package com.fiepi.media.data.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["source_id", "path"], unique = true)
    ]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "source_id")
    val sourceId: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @ColumnInfo(name = "source_type")
    val sourceType: String = "local",

    // Playback progress (in milliseconds)
    @ColumnInfo(name = "position_ms")
    val positionMs: Long = 0L,

    // Total duration (in milliseconds)
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,

    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
) {
    /**
     * Playback progress percentage for card progress bar UI
     */
    val progressPercentage: Float
        get() = if (durationMs > 0L) {
            ((positionMs.toDouble() / durationMs.toDouble()) * 100).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }
}
