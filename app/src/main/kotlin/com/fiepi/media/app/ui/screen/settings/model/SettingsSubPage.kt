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

import com.fiepi.media.app.R

/**
 * Defines type constants and corresponding title resources for settings sub-pages.
 */
object SettingsSubPage {

    // Player
    object Player {
        const val SEEK_BAR_STYLE = "seek_bar_style"
        const val ORIENTATION = "orientation"
        const val LOOP_MODE = "loop_mode"
        const val BUTTON_EFFECTS = "button_effects"
        const val PLAYER_LAYOUT = "player_layout"
        const val SCREENSHOT_FORMAT = "screenshot_format"

        fun getTitleResId(type: String): Int = when (type) {
            SEEK_BAR_STYLE -> R.string.settings_player_seek_bar_style
            ORIENTATION -> R.string.settings_player_orientation
            LOOP_MODE -> R.string.settings_player_loop_mode
            BUTTON_EFFECTS -> R.string.settings_player_button_effects
            PLAYER_LAYOUT -> R.string.settings_player_layout
            SCREENSHOT_FORMAT -> R.string.settings_player_screenshot_format
            else -> R.string.common_settings
        }
    }

    // Audio
    object Audio {
        const val PREFERRED_LANGUAGE = "preferred_language"
        const val SELECT_LANGUAGE = "select_language"
        const val AUDIO_OUTPUT_ORDER = "audio_output_order"

        fun getTitleResId(type: String): Int = when (type) {
            PREFERRED_LANGUAGE -> R.string.settings_audio_preferred_language
            SELECT_LANGUAGE -> R.string.settings_lang_select_language
            AUDIO_OUTPUT_ORDER -> R.string.settings_audio_output_order
            else -> R.string.common_settings
        }
    }

    // Subtitle
    object Subtitle {
        const val PREFERRED_LANGUAGE = "preferred_language"
        const val SELECT_LANGUAGE = "select_language"
        const val SUB_CODEPAGE = "sub_codepage"
        const val SUB_ASS_OVERRIDE = "sub_ass_override"
        const val EXO_SUBTITLE_STYLE = "exo_subtitle_style"
        const val EXO_SUBTITLE_FONT = "exo_subtitle_font"
        const val MPV_SUBTITLE_STYLE = "mpv_subtitle_style"
        const val MPV_SUBTITLE_FONT = "mpv_subtitle_font"

        fun getTitleResId(type: String): Int = when (type) {
            PREFERRED_LANGUAGE -> R.string.settings_subtitle_preferred_language
            SELECT_LANGUAGE -> R.string.settings_lang_select_language
            SUB_CODEPAGE -> R.string.settings_subtitle_codepage
            SUB_ASS_OVERRIDE -> R.string.settings_subtitle_mpv_ass_override
            EXO_SUBTITLE_STYLE -> R.string.settings_subtitle_exo_style
            EXO_SUBTITLE_FONT, MPV_SUBTITLE_FONT -> R.string.settings_subtitle_font
            MPV_SUBTITLE_STYLE -> R.string.settings_subtitle_mpv_style
            else -> R.string.common_settings
        }
    }

    // Decoder
    object Decoder {
        const val CODEC_INFO = "decoder_codec_info"
        const val ENGINE_TYPE = "engine_type"
        const val PROFILE = "decoder_profile"
        const val VO = "decoder_vo"
        const val GPU_API = "decoder_gpu_api"
        const val HWDEC = "decoder_hwdec"
        const val VIDEO_SYNC = "decoder_video_sync"
        const val SCALE = "decoder_scale"
        const val DSCALE = "decoder_dscale"
        const val CSCALE = "decoder_cscale"
        const val HDR = "decoder_hdr"
        const val FRAMEDROP = "decoder_framedrop"
        const val DEBAND = "decoder_deband"

        fun getTitleResId(type: String): Int = when (type) {
            CODEC_INFO -> R.string.settings_decoder_codec_info
            ENGINE_TYPE -> R.string.settings_decoder_player_engine_type
            PROFILE -> R.string.settings_decoder_profile
            VO -> R.string.settings_decoder_vo
            GPU_API -> R.string.settings_decoder_gpu_api
            HWDEC -> R.string.settings_decoder_hwdec
            VIDEO_SYNC -> R.string.settings_decoder_video_sync
            SCALE -> R.string.settings_decoder_scale
            DSCALE -> R.string.settings_decoder_dscale
            CSCALE -> R.string.settings_decoder_cscale
            HDR -> R.string.settings_decoder_hdr_settings
            FRAMEDROP -> R.string.settings_decoder_framedrop
            DEBAND -> R.string.settings_decoder_deband
            else -> R.string.common_settings
        }
    }

    // Advanced
    object Advanced {
        const val USER_AGENT = "advanced_user_agent"

        fun getTitleResId(type: String): Int = when (type) {
            USER_AGENT -> R.string.settings_advanced_user_agent
            else -> R.string.common_settings
        }
    }

    // About
    object About {
        const val LIBRARIES = "libraries"
        const val COPYRIGHT = "copyright"

        fun getTitleResId(type: String): Int = when (type) {
            LIBRARIES -> R.string.settings_about_libraries
            COPYRIGHT -> R.string.settings_about_copyright
            else -> R.string.common_settings
        }
    }
}
