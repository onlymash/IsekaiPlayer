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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsSearchViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val allItems = listOf(
        // Appearance
        SearchablePreference(
            R.string.settings_appearance_theme_mode,
            null,
            SettingsCategory.Appearance,
            SettingsNavKey.Category(SettingsCategory.Appearance.ordinal)
        ),
        SearchablePreference(
            R.string.settings_appearance_theme_high_contrast,
            R.string.settings_appearance_theme_high_contrast_summary,
            SettingsCategory.Appearance,
            SettingsNavKey.Category(SettingsCategory.Appearance.ordinal)
        ),

        // Player
        SearchablePreference(
            R.string.settings_player_auto_hide_duration,
            null,
            SettingsCategory.Player,
            SettingsNavKey.Category(SettingsCategory.Player.ordinal)
        ),
        SearchablePreference(
            R.string.settings_player_enable_pip,
            R.string.settings_player_enable_pip_summary,
            SettingsCategory.Player,
            SettingsNavKey.Category(SettingsCategory.Player.ordinal)
        ),
        SearchablePreference(
            R.string.player_control_playback_speed,
            null,
            SettingsCategory.Player,
            SettingsNavKey.Category(SettingsCategory.Player.ordinal)
        ),
        SearchablePreference(
            R.string.settings_player_seek_bar_style,
            null,
            SettingsCategory.Player,
            SettingsNavKey.PlayerSubPage(SettingsSubPage.Player.SEEK_BAR_STYLE)
        ),
        SearchablePreference(
            R.string.settings_player_orientation,
            null,
            SettingsCategory.Player,
            SettingsNavKey.PlayerSubPage(SettingsSubPage.Player.ORIENTATION)
        ),
        SearchablePreference(
            R.string.settings_player_resume_playback,
            R.string.settings_player_resume_playback_summary,
            SettingsCategory.Player,
            SettingsNavKey.Category(SettingsCategory.Player.ordinal)
        ),
        SearchablePreference(
            R.string.settings_player_loop_mode,
            null,
            SettingsCategory.Player,
            SettingsNavKey.PlayerSubPage(SettingsSubPage.Player.LOOP_MODE)
        ),
        SearchablePreference(
            R.string.settings_player_remember_brightness,
            R.string.settings_player_remember_brightness_summary,
            SettingsCategory.Player,
            SettingsNavKey.Category(SettingsCategory.Player.ordinal)
        ),
        SearchablePreference(
            R.string.settings_player_screenshot_format,
            null,
            SettingsCategory.Player,
            SettingsNavKey.PlayerSubPage(SettingsSubPage.Player.SCREENSHOT_FORMAT)
        ),

        // Gestures
        SearchablePreference(
            R.string.settings_gestures_horizontal_drag,
            R.string.settings_gestures_horizontal_drag_summary,
            SettingsCategory.Gestures,
            SettingsNavKey.Category(SettingsCategory.Gestures.ordinal)
        ),
        SearchablePreference(
            R.string.settings_gestures_vertical_drag_volume,
            R.string.settings_gestures_vertical_drag_volume_summary,
            SettingsCategory.Gestures,
            SettingsNavKey.Category(SettingsCategory.Gestures.ordinal)
        ),
        SearchablePreference(
            R.string.settings_gestures_vertical_drag_brightness,
            R.string.settings_gestures_vertical_drag_brightness_summary,
            SettingsCategory.Gestures,
            SettingsNavKey.Category(SettingsCategory.Gestures.ordinal)
        ),
        SearchablePreference(
            R.string.settings_gestures_double_tap_seek,
            R.string.settings_gestures_double_tap_seek_summary,
            SettingsCategory.Gestures,
            SettingsNavKey.Category(SettingsCategory.Gestures.ordinal)
        ),
        SearchablePreference(
            R.string.settings_gestures_double_tap_play_pause,
            R.string.settings_gestures_double_tap_play_pause_summary,
            SettingsCategory.Gestures,
            SettingsNavKey.Category(SettingsCategory.Gestures.ordinal)
        ),
        SearchablePreference(
            R.string.settings_gestures_long_press,
            R.string.settings_gestures_long_press_summary,
            SettingsCategory.Gestures,
            SettingsNavKey.Category(SettingsCategory.Gestures.ordinal)
        ),
        SearchablePreference(
            R.string.settings_gestures_long_press_speed,
            R.string.settings_gestures_long_press_speed_summary,
            SettingsCategory.Gestures,
            SettingsNavKey.Category(SettingsCategory.Gestures.ordinal)
        ),

        // Decoder
        SearchablePreference(
            R.string.settings_decoder_profile,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.PROFILE)
        ),
        SearchablePreference(
            R.string.settings_decoder_vo,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.VO)
        ),
        SearchablePreference(
            R.string.settings_decoder_gpu_api,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.GPU_API)
        ),
        SearchablePreference(
            R.string.settings_decoder_hwdec,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.HWDEC)
        ),
        SearchablePreference(
            R.string.settings_decoder_scale,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.SCALE)
        ),
        SearchablePreference(
            R.string.settings_decoder_dscale,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.DSCALE)
        ),
        SearchablePreference(
            R.string.settings_decoder_cscale,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.CSCALE)
        ),
        SearchablePreference(
            R.string.settings_decoder_hdr_settings,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.HDR)
        ),
        SearchablePreference(
            R.string.settings_decoder_framedrop,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.FRAMEDROP)
        ),
        SearchablePreference(
            R.string.settings_decoder_video_sync,
            null,
            SettingsCategory.Decoder,
            SettingsNavKey.DecoderSubPage(SettingsSubPage.Decoder.VIDEO_SYNC)
        ),

        // Audio
        SearchablePreference(
            R.string.settings_audio_preferred_language,
            null,
            SettingsCategory.Audio,
            SettingsNavKey.AudioSubPage(SettingsSubPage.Audio.PREFERRED_LANGUAGE)
        ),

        // Subtitle
        SearchablePreference(
            R.string.settings_subtitle_preferred_language,
            null,
            SettingsCategory.Subtitle,
            SettingsNavKey.SubtitleSubPage(SettingsSubPage.Subtitle.PREFERRED_LANGUAGE)
        ),

        // Advanced
        SearchablePreference(
            R.string.settings_advanced_user_agent,
            null,
            SettingsCategory.Advanced,
            SettingsNavKey.AdvancedSubPage(SettingsSubPage.Advanced.USER_AGENT)
        ),

        // General
        SearchablePreference(
            R.string.settings_general_haptics,
            R.string.settings_general_haptics_summary,
            SettingsCategory.General,
            SettingsNavKey.Category(SettingsCategory.General.ordinal)
        ),
        SearchablePreference(
            R.string.settings_general_intercept_back,
            R.string.settings_general_intercept_back_summary,
            SettingsCategory.General,
            SettingsNavKey.Category(SettingsCategory.General.ordinal)
        ),
        SearchablePreference(
            R.string.settings_general_app_language,
            R.string.settings_general_app_language_summary,
            SettingsCategory.General,
            SettingsNavKey.Category(SettingsCategory.General.ordinal)
        ),

        // About
        SearchablePreference(
            R.string.settings_about_copyright,
            R.string.settings_about_copyright_summary,
            SettingsCategory.About,
            SettingsNavKey.AboutSubPage(SettingsSubPage.About.COPYRIGHT)
        ),
        SearchablePreference(
            R.string.settings_about_github,
            R.string.settings_about_github_summary,
            SettingsCategory.About,
            SettingsNavKey.Category(SettingsCategory.About.ordinal)
        ),
        SearchablePreference(
            R.string.settings_about_telegram,
            R.string.settings_about_telegram_summary,
            SettingsCategory.About,
            SettingsNavKey.Category(SettingsCategory.About.ordinal)
        ),
    )

    val searchResults = _searchQuery.map { query ->
        if (query.isBlank()) emptyList()
        else {
            allItems.filter { item ->
                appContext.getString(item.titleRes).contains(query, ignoreCase = true) ||
                        (item.summaryRes?.let {
                            appContext.getString(it).contains(query, ignoreCase = true)
                        } ?: false)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
