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

package com.fiepi.media.app.ui.screen.settings.pages.gestures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.domain.usecases.preferences.GesturePrefsUseCases
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GesturesPrefsViewModel(
    private val useCases: GesturePrefsUseCases
) : ViewModel() {

    val uiState: StateFlow<GestureUiState> = useCases.getGestureOptions()
        .map { options -> GestureUiState(gestureOptions = options) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GestureUiState()
        )

    fun onIntent(intent: GesturesPrefsIntent) {
        viewModelScope.launch {
            when (intent) {
                is GesturesPrefsIntent.UpdateGestureOptions -> {
                    useCases.updateGestureOptions(intent.options)
                }
            }
        }
    }
}
