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

package com.fiepi.media.app.ui.screen.settings.pages.player

import com.fiepi.media.domain.model.preferences.ButtonEffectOptions
import com.fiepi.media.domain.model.preferences.LoopMode
import com.fiepi.media.domain.model.preferences.PlayerLayoutOptions
import com.fiepi.media.domain.model.preferences.PlayerOrientation
import com.fiepi.media.domain.model.preferences.ScreenshotFormat
import com.fiepi.media.domain.model.preferences.SeekBarStyle

sealed class PlayerPrefsIntent {
    data class UpdateAutoHideDuration(val seconds: Int) : PlayerPrefsIntent()
    data class UpdateSeekDuration(val seconds: Int) : PlayerPrefsIntent()
    data class UpdatePlaybackSpeed(val speed: Float) : PlayerPrefsIntent()
    data class UpdateSeekBarStyle(val style: SeekBarStyle) : PlayerPrefsIntent()
    data class UpdateResumePlayback(val resume: Boolean) : PlayerPrefsIntent()
    data class UpdateLoopMode(val mode: LoopMode) : PlayerPrefsIntent()
    data class UpdateEnablePiP(val enable: Boolean) : PlayerPrefsIntent()
    data class UpdateRememberBrightness(val remember: Boolean) : PlayerPrefsIntent()
    data class UpdateOrientation(val orientation: PlayerOrientation) : PlayerPrefsIntent()
    data class UpdateButtonEffectOptions(val options: ButtonEffectOptions) : PlayerPrefsIntent()
    data class UpdateAlwaysFullScreen(val always: Boolean) : PlayerPrefsIntent()
    data class UpdateLayoutOptions(val options: PlayerLayoutOptions) : PlayerPrefsIntent()
    data class UpdateScreenshotFormat(val format: ScreenshotFormat) : PlayerPrefsIntent()
}
