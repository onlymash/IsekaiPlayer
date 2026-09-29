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

package com.fiepi.media.domain.model.preferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SortType(val value: String, val supportsRemote: Boolean = true) {
    @SerialName("name")
    Name("name"),

    @SerialName("date")
    Date("date"),

    @SerialName("size")
    Size("size"),

    @SerialName("duration")
    Duration("duration", false);

    companion object {
        fun fromValue(value: String): SortType = entries.firstOrNull { it.value == value } ?: Name
    }
}

@Serializable
enum class SortOrder(val value: String) {
    @SerialName("asc")
    Ascending("asc"),

    @SerialName("desc")
    Descending("desc");

    companion object {
        fun fromValue(value: String): SortOrder =
            entries.firstOrNull { it.value == value } ?: Ascending
    }
}

@Serializable
enum class MediaField(val value: String, val supportsRemote: Boolean = true) {
    @SerialName("thumbnail")
    Thumbnail("thumbnail", true),

    @SerialName("item_count")
    ItemCount("item_count", false),

    @SerialName("size")
    Size("size", true),

    @SerialName("duration")
    Duration("duration", false),

    @SerialName("resolution")
    Resolution("resolution", false),

    @SerialName("date")
    Date("date", true),

    @SerialName("extension")
    Extension("extension", true);

    companion object {
        fun fromValue(value: String): MediaField? = entries.firstOrNull { it.value == value }
    }
}

/**
 * Options that directly affect data fetching and sorting from the source.
 */
@Serializable
data class MediaOptions(
    @SerialName("sort_type")
    val sortType: SortType = SortType.Name,
    @SerialName("sort_order")
    val sortOrder: SortOrder = SortOrder.Ascending,
    @SerialName("folders_first")
    val foldersFirst: Boolean = true,
    @SerialName("show_hidden")
    val showHidden: Boolean = false,
    @SerialName("show_subtitles")
    val showSubtitles: Boolean = true,
)

/**
 * Wrapper for all media-related preferences to be saved in a single DataStore file.
 */
@Serializable
data class MediaPreferences(
    @SerialName("local_options")
    val localOptions: MediaOptions = MediaOptions(),
    @SerialName("remote_options")
    val remoteOptions: MediaOptions = MediaOptions(),
    @SerialName("local_display_fields")
    val localDisplayFields: List<MediaField> = MediaField.entries,
    @SerialName("remote_display_fields")
    val remoteDisplayFields: List<MediaField> = MediaField.entries.filter { it.supportsRemote && it != MediaField.Thumbnail },
    @SerialName("selected_source_id")
    val selectedSourceId: String? = null
)
