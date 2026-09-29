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

/**
 * Audio-related preference settings
 */
@Serializable
data class AudioOptions(
    @SerialName("request_audio_focus")
    val requestAudioFocus: Boolean = true,
    @SerialName("enable_background_audio")
    val enableBackgroundAudio: Boolean = false,
    @SerialName("preferred_audio_languages")
    val preferredAudioLanguages: List<String> = emptyList(),
    @SerialName("audio_output_backends")
    val audioOutputBackends: List<String> = listOf("aaudio", "audiotrack", "opensles"),
    @SerialName("dynamic_audio_normalize")
    val dynamicAudioNormalize: Boolean = false,
    @SerialName("audio_pitch_correction")
    val audioPitchCorrection: Boolean = true,
    @SerialName("audio_delay")
    val audioDelay: Long = 0L,
    @SerialName("mpv_volume")
    val mpvVolume: Int = 100,
    @SerialName("exo_volume")
    val exoVolume: Int = 100
)
