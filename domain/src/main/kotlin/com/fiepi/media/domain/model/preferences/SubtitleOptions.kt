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
 * Customization settings for ExoPlayer Compose subtitle overlay
 */
@Serializable
data class ExoSubtitleOptions(
    @SerialName("font_size_sp")
    val fontSizeSp: Int = 18,
    @SerialName("text_color_hex")
    val textColorHex: String = "#FFFFFF",
    @SerialName("bg_color_hex")
    val bgColorHex: String = "#000000",
    @SerialName("bg_opacity")
    val bgOpacity: Float = 0.1f,
    @SerialName("bottom_padding_dp")
    val bottomPaddingDp: Int = 12,
    @SerialName("font_weight")
    val fontWeight: Int = 500,
    @SerialName("font_family")
    val fontFamily: String = "sans-serif",
    @SerialName("fonts_dir")
    val fontsDir: String = "",
    @SerialName("outline_color_hex")
    val outlineColorHex: String = "#212121",
    @SerialName("outline_width_dp")
    val outlineWidthDp: Int = 2,
)

/**
 * Customization settings for mpv subtitle style and ASS override behavior
 */
@Serializable
data class MpvSubtitleOptions(
    @SerialName("sub_font_size")
    val subFontSize: Int = 50,
    @SerialName("font_color_hex")
    val fontColorHex: String = "#FFFFFF",
    @SerialName("outline_color_hex")
    val outlineColorHex: String = "#212121",
    @SerialName("outline_width")
    val outlineWidth: Int = 2,
    @SerialName("shadow_color_hex")
    val shadowColorHex: String = "#000000",
    @SerialName("shadow_offset")
    val shadowOffset: Int = 0,
    @SerialName("sub_bold")
    val subBold: Boolean = false,
    @SerialName("sub_italic")
    val subItalic: Boolean = false,
    @SerialName("sub_font")
    val subFont: String = "sans-serif",
    @SerialName("sub_fonts_dir")
    val subFontsDir: String = "",
    @SerialName("bottom_margin")
    val bottomMargin: Int = 12,
    @SerialName("sub_ass_override")
    val subAssOverride: String = "scale",
)

/**
 * Subtitle-related preference settings
 */
@Serializable
data class SubtitleOptions(
    @SerialName("preferred_subtitle_languages")
    val preferredSubtitleLanguages: List<String> = emptyList(),
    @SerialName("subtitle_fallback")
    val subtitleFallback: Boolean = true,
    @SerialName("sub_codepage")
    val subCodepage: String = "auto",
    @SerialName("exo_subtitle")
    val exoSubtitle: ExoSubtitleOptions = ExoSubtitleOptions(),
    @SerialName("mpv_subtitle")
    val mpvSubtitle: MpvSubtitleOptions = MpvSubtitleOptions(),
)
