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

package com.fiepi.media.domain.player.model

/**
 * Represents a track in a media file (audio, video, or subtitle)
 */
sealed class MediaTrack {
    abstract val id: Int
    abstract val title: String
    abstract val language: String?
    abstract val isSelected: Boolean
    abstract val isExternal: Boolean
    abstract val codec: String?

    data class Video(
        override val id: Int,
        override val title: String,
        override val language: String?,
        override val isSelected: Boolean,
        override val isExternal: Boolean = false,
        override val codec: String? = null,
        val width: Int = 0,
        val height: Int = 0,
        val fps: Double = 0.0,
        val bitrate: Long = 0L,
    ) : MediaTrack()

    data class Audio(
        override val id: Int,
        override val title: String,
        override val language: String?,
        override val isSelected: Boolean,
        override val isExternal: Boolean = false,
        override val codec: String? = null,
        val bitrate: Long = 0L,
        val channels: Int = 0,
        val sampleRate: Int = 0,
    ) : MediaTrack()

    data class Subtitle(
        override val id: Int,
        override val title: String,
        override val language: String?,
        override val isSelected: Boolean,
        override val isExternal: Boolean = false,
        override val codec: String? = null,
    ) : MediaTrack()
}

enum class TrackType {
    Audio, Subtitle, Video
}
