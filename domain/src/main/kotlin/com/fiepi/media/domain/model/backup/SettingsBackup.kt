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

package com.fiepi.media.domain.model.backup

import com.fiepi.media.domain.model.preferences.AdvancedOptions
import com.fiepi.media.domain.model.preferences.AudioOptions
import com.fiepi.media.domain.model.preferences.DecoderOptions
import com.fiepi.media.domain.model.preferences.GeneralOptions
import com.fiepi.media.domain.model.preferences.GestureOptions
import com.fiepi.media.domain.model.preferences.MediaPreferences
import com.fiepi.media.domain.model.preferences.PlayerOptions
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import com.fiepi.media.domain.model.preferences.ThemeOptions
import com.fiepi.media.domain.model.source.RemoteSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing a complete serializable backup of all app settings and sources.
 */
@Serializable
data class SettingsBackup(
    @SerialName("version")
    val version: Int = 1,
    @SerialName("audio_options")
    val audioOptions: AudioOptions = AudioOptions(),
    @SerialName("decoder_options")
    val decoderOptions: DecoderOptions = DecoderOptions(),
    @SerialName("theme_options")
    val themeOptions: ThemeOptions = ThemeOptions(),
    @SerialName("subtitle_options")
    val subtitleOptions: SubtitleOptions = SubtitleOptions(),
    @SerialName("player_options")
    val playerOptions: PlayerOptions = PlayerOptions(),
    @SerialName("media_preferences")
    val mediaPreferences: MediaPreferences = MediaPreferences(),
    @SerialName("gesture_options")
    val gestureOptions: GestureOptions = GestureOptions(),
    @SerialName("advanced_options")
    val advancedOptions: AdvancedOptions = AdvancedOptions(),
    @SerialName("general_options")
    val generalOptions: GeneralOptions = GeneralOptions(),
    @SerialName("remote_sources")
    val remoteSources: List<RemoteSource> = emptyList()
)
