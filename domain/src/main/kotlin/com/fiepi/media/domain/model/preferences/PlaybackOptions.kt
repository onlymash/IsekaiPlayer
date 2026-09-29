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

import com.fiepi.media.domain.player.model.PlayerEngineType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LoopMode(val value: String) {
    @SerialName("none")
    None("none"),

    @SerialName("repeat_current")
    RepeatCurrent("repeat_current"),

    @SerialName("sequential")
    Sequential("sequential"),

    @SerialName("repeat_list")
    RepeatList("repeat_list"),

    @SerialName("shuffle")
    Shuffle("shuffle"),

    @SerialName("shuffle_repeat")
    ShuffleRepeat("shuffle_repeat");

    companion object {
        fun fromValue(value: String): LoopMode =
            entries.firstOrNull { it.value == value } ?: None
    }
}

/**
 * Core playback options used by the player engine and business logic.
 */
@Serializable
data class PlaybackOptions(
    @SerialName("resume_playback")
    val resumePlayback: Boolean = true,
    @SerialName("loop_mode")
    val loopMode: LoopMode = LoopMode.None,
    @SerialName("request_audio_focus")
    val requestAudioFocus: Boolean = true,
    @SerialName("preferred_audio_languages")
    val preferredAudioLanguages: List<String> = emptyList(),
    @SerialName("preferred_subtitle_languages")
    val preferredSubtitleLanguages: List<String> = emptyList(),
    @SerialName("subtitle_fallback")
    val subtitleFallback: Boolean = true,
    @SerialName("engine_type")
    val engineType: PlayerEngineType = PlayerEngineType.MPV
)
