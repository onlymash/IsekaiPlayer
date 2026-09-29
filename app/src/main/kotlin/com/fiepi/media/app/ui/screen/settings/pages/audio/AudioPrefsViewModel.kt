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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.domain.model.assets.Language
import com.fiepi.media.domain.model.assets.LanguageType
import com.fiepi.media.domain.model.preferences.AudioOptions
import com.fiepi.media.domain.usecases.preferences.AudioPrefsUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioPrefsViewModel(
    private val useCases: AudioPrefsUseCases,
) : ViewModel() {

    private val _languages = MutableStateFlow<List<Language>>(emptyList())
    val languages = _languages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            _languages.value = useCases.getLanguages(LanguageType.Audio)
        }
    }

    val audioOptions = useCases.getAudioOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioOptions())

    fun onIntent(intent: AudioPrefsIntent) {
        viewModelScope.launch {
            val current = audioOptions.value
            when (intent) {
                is AudioPrefsIntent.SetSearchQuery -> {
                    _searchQuery.value = intent.query
                }

                is AudioPrefsIntent.ToggleRequestAudioFocus -> {
                    useCases.updateAudioOptions(current.copy(requestAudioFocus = intent.enabled))
                }

                is AudioPrefsIntent.ToggleBackgroundAudio -> {
                    useCases.updateAudioOptions(current.copy(enableBackgroundAudio = intent.enabled))
                }

                is AudioPrefsIntent.UpdatePreferredAudioLanguages -> {
                    useCases.updateAudioOptions(current.copy(preferredAudioLanguages = intent.languages))
                }

                is AudioPrefsIntent.UpdateAudioOutputBackends -> {
                    useCases.updateAudioOptions(current.copy(audioOutputBackends = intent.backends))
                }

                is AudioPrefsIntent.UpdateDynamicAudioNormalize -> {
                    useCases.updateAudioOptions(current.copy(dynamicAudioNormalize = intent.enabled))
                }

                is AudioPrefsIntent.UpdateAudioPitchCorrection -> {
                    useCases.updateAudioOptions(current.copy(audioPitchCorrection = intent.enabled))
                }

                is AudioPrefsIntent.UpdateAudioDelay -> {
                    useCases.updateAudioOptions(current.copy(audioDelay = intent.delayMs))
                }

                is AudioPrefsIntent.UpdateMpvVolume -> {
                    useCases.updateAudioOptions(current.copy(mpvVolume = intent.volume))
                }

                is AudioPrefsIntent.UpdateExoVolume -> {
                    useCases.updateAudioOptions(current.copy(exoVolume = intent.volume))
                }
            }
        }
    }
}
