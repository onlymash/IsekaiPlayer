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
enum class PlayerAction(val value: String) {
    @SerialName("none")
    None("none"),

    @SerialName("lock")
    Lock("lock"),

    @SerialName("back")
    Back("back"),

    @SerialName("pip")
    PiP("pip"),

    @SerialName("background_audio")
    BackgroundAudio("background_audio"),

    @SerialName("screenshot")
    Screenshot("screenshot"),

    @SerialName("playback_speed")
    PlaybackSpeed("playback_speed"),

    @SerialName("audio_delay")
    AudioDelay("audio_delay"),

    @SerialName("subtitle_delay")
    SubtitleDelay("subtitle_delay"),

    @SerialName("osd")
    Osd("osd"),

    @SerialName("playlist")
    Playlist("playlist"),

    @SerialName("tracks")
    Tracks("tracks"),

    @SerialName("loop_mode")
    LoopModeToggle("loop_mode"),

    @SerialName("scale_mode")
    ScaleModeToggle("scale_mode"),

    @SerialName("decoder_settings")
    DecoderSettings("decoder_settings"),

    @SerialName("player_settings")
    PlayerSettings("player_settings"),

    @SerialName("gesture_settings")
    GestureSettings("gesture_settings"),

    @SerialName("audio_settings")
    AudioSettings("audio_settings"),

    @SerialName("subtitle_settings")
    SubtitleSettings("subtitle_settings");

    companion object {
        fun fromValue(value: String): PlayerAction =
            entries.firstOrNull { it.value == value } ?: None
    }
}
