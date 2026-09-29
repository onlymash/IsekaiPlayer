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

package com.fiepi.media.app.ui.screen.settings.pages.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.domain.usecases.preferences.AppearancePrefsUseCases
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearancePrefsViewModel(
    private val useCases: AppearancePrefsUseCases
) : ViewModel() {

    val uiState: StateFlow<AppearanceUiState> = useCases.getThemeOptions()
        .map { themeOptions -> AppearanceUiState(themeOptions = themeOptions) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppearanceUiState()
        )

    fun onIntent(intent: AppearancePrefsIntent) {
        viewModelScope.launch {
            val current = uiState.value.themeOptions
            when (intent) {
                is AppearancePrefsIntent.SetThemeMode -> {
                    useCases.updateThemeOptions(current.copy(themeMode = intent.mode))
                }

                is AppearancePrefsIntent.SetHighContrastEnabled -> {
                    useCases.updateThemeOptions(current.copy(highContrastDarkThemeEnabled = intent.enabled))
                }
            }
        }
    }
}
