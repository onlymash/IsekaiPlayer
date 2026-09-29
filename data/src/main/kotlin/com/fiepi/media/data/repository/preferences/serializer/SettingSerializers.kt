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

package com.fiepi.media.data.repository.preferences.serializer

import com.fiepi.media.domain.model.preferences.AdvancedOptions
import com.fiepi.media.domain.model.preferences.AudioOptions
import com.fiepi.media.domain.model.preferences.DecoderOptions
import com.fiepi.media.domain.model.preferences.GeneralOptions
import com.fiepi.media.domain.model.preferences.GestureOptions
import com.fiepi.media.domain.model.preferences.MediaPreferences
import com.fiepi.media.domain.model.preferences.PlayerOptions
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import com.fiepi.media.domain.model.preferences.ThemeOptions
import kotlinx.serialization.json.Json

object SettingSerializers {
    fun audio(json: Json) = JsonSerializer(AudioOptions.serializer(), AudioOptions(), json)
    fun decoder(json: Json) = JsonSerializer(DecoderOptions.serializer(), DecoderOptions(), json)
    fun theme(json: Json) = JsonSerializer(ThemeOptions.serializer(), ThemeOptions(), json)
    fun subtitle(json: Json) = JsonSerializer(SubtitleOptions.serializer(), SubtitleOptions(), json)
    fun player(json: Json) = JsonSerializer(PlayerOptions.serializer(), PlayerOptions(), json)
    fun media(json: Json) = JsonSerializer(MediaPreferences.serializer(), MediaPreferences(), json)
    fun gesture(json: Json) = JsonSerializer(GestureOptions.serializer(), GestureOptions(), json)
    fun advanced(json: Json) = JsonSerializer(AdvancedOptions.serializer(), AdvancedOptions(), json)
    fun general(json: Json) = JsonSerializer(GeneralOptions.serializer(), GeneralOptions(), json)
}
