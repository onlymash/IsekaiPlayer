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

package com.fiepi.media.app.ui.screen.settings.pages.subtitle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.domain.model.assets.Language
import com.fiepi.media.domain.model.assets.LanguageType
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import com.fiepi.media.domain.usecases.preferences.SubtitlePrefsUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubtitlePrefsViewModel(
    private val useCases: SubtitlePrefsUseCases,
) : ViewModel() {

    private val _languages = MutableStateFlow<List<Language>>(emptyList())
    val languages = _languages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            _languages.value = useCases.getLanguages(LanguageType.Subtitle)
        }
    }

    val subtitleOptions = useCases.getSubtitleOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubtitleOptions())

    fun onIntent(intent: SubtitlePrefsIntent) {
        viewModelScope.launch {
            val current = subtitleOptions.value
            when (intent) {
                is SubtitlePrefsIntent.SetSearchQuery -> {
                    _searchQuery.value = intent.query
                }

                is SubtitlePrefsIntent.UpdatePreferredSubtitleLanguages -> {
                    useCases.updateSubtitleOptions(current.copy(preferredSubtitleLanguages = intent.languages))
                }

                is SubtitlePrefsIntent.UpdateSubtitleFallback -> {
                    useCases.updateSubtitleOptions(current.copy(subtitleFallback = intent.enabled))
                }

                is SubtitlePrefsIntent.UpdateSubCodepage -> {
                    useCases.updateSubtitleOptions(current.copy(subCodepage = intent.codepage))
                }

                is SubtitlePrefsIntent.UpdateExoSubtitleOptions -> {
                    useCases.updateSubtitleOptions(current.copy(exoSubtitle = intent.options))
                }

                is SubtitlePrefsIntent.UpdateMpvSubtitleOptions -> {
                    useCases.updateSubtitleOptions(current.copy(mpvSubtitle = intent.options))
                }
            }
        }
    }
}
