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
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "playlist_item",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["playlist_id", "source_id", "path"], unique = true)
    ]
)
data class PlaylistItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "playlist_id")
    val playlistId: String,

    @ColumnInfo(name = "path")
    val path: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "source_id")
    val sourceId: String,

    @ColumnInfo(name = "source_type")
    val sourceType: String = "local",

    @ColumnInfo(name = "size")
    val size: Long? = null,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long? = null,

    @ColumnInfo(name = "extension")
    val extension: String? = null,

    @ColumnInfo(name = "media_id")
    val mediaId: String,

    @ColumnInfo(name = "duration")
    val duration: Long? = null,

    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,

    @ColumnInfo(name = "width")
    val width: Int? = null,

    @ColumnInfo(name = "height")
    val height: Int? = null,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @ColumnInfo(name = "extra_headers")
    val extraHeaders: String? = null,

    @ColumnInfo(name = "external_subtitles")
    val externalSubtitles: String? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
