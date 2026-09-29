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

package com.fiepi.media.app.ui.screen.settings.pages.audio

sealed class AudioPrefsIntent {
    data class SetSearchQuery(val query: String) : AudioPrefsIntent()
    data class ToggleRequestAudioFocus(val enabled: Boolean) : AudioPrefsIntent()
    data class ToggleBackgroundAudio(val enabled: Boolean) : AudioPrefsIntent()
    data class UpdatePreferredAudioLanguages(val languages: List<String>) : AudioPrefsIntent()
    data class UpdateAudioOutputBackends(val backends: List<String>) : AudioPrefsIntent()
    data class UpdateDynamicAudioNormalize(val enabled: Boolean) : AudioPrefsIntent()
    data class UpdateAudioPitchCorrection(val enabled: Boolean) : AudioPrefsIntent()
    data class UpdateAudioDelay(val delayMs: Long) : AudioPrefsIntent()
    data class UpdateMpvVolume(val volume: Int) : AudioPrefsIntent()
    data class UpdateExoVolume(val volume: Int) : AudioPrefsIntent()
}
