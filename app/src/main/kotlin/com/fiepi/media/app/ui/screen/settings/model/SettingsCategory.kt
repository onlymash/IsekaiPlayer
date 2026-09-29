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

package com.fiepi.media.app.ui.screen.settings.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AppSettingsAlt
import androidx.compose.material.icons.twotone.Audiotrack
import androidx.compose.material.icons.twotone.DisplaySettings
import androidx.compose.material.icons.twotone.DynamicForm
import androidx.compose.material.icons.twotone.FormatPaint
import androidx.compose.material.icons.twotone.Gesture
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Subtitles
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.fiepi.media.app.R

enum class SettingsCategory(
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector
) {
    Appearance(
        titleResId = R.string.settings_category_appearance,
        descriptionResId = R.string.settings_category_appearance_summary,
        icon = Icons.TwoTone.FormatPaint
    ),
    Player(
        titleResId = R.string.settings_category_player,
        descriptionResId = R.string.settings_category_player_summary,
        icon = Icons.TwoTone.DisplaySettings
    ),
    Gestures(
        titleResId = R.string.settings_category_gestures,
        descriptionResId = R.string.settings_category_gestures_summary,
        icon = Icons.TwoTone.Gesture
    ),
    Decoder(
        titleResId = R.string.settings_category_decoder,
        descriptionResId = R.string.settings_category_decoder_summary,
        icon = Icons.TwoTone.DynamicForm
    ),
    Audio(
        titleResId = R.string.settings_category_audio,
        descriptionResId = R.string.settings_category_audio_summary,
        icon = Icons.TwoTone.Audiotrack
    ),
    Subtitle(
        titleResId = R.string.settings_category_subtitle,
        descriptionResId = R.string.settings_category_subtitle_summary,
        icon = Icons.TwoTone.Subtitles
    ),
    Advanced(
        titleResId = R.string.settings_category_advanced,
        descriptionResId = R.string.settings_category_advanced_summary,
        icon = Icons.TwoTone.Tune
    ),
    General(
        titleResId = R.string.settings_category_general,
        descriptionResId = R.string.settings_category_general_summary,
        icon = Icons.TwoTone.AppSettingsAlt
    ),
    About(
        titleResId = R.string.settings_category_about,
        descriptionResId = R.string.settings_category_about_summary,
        icon = Icons.TwoTone.Info
    )
}