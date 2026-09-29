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

package com.fiepi.media.app.ui.screen.source.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.domain.usecases.SourceManagerUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SourceManagerViewModel(
    private val useCases: SourceManagerUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(SourceManagerState())
    val state: StateFlow<SourceManagerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            useCases.getRemoteSources().collect { sources ->
                _state.update { it.copy(remoteSources = sources, isLoading = false) }
            }
        }

        viewModelScope.launch {
            useCases.getSelectedSourceId().collect { id ->
                _state.update { it.copy(selectedSourceId = id) }
            }
        }
    }

    fun onIntent(intent: SourceManagerIntent) {
        when (intent) {
            is SourceManagerIntent.DeleteSource -> {
                viewModelScope.launch {
                    useCases.deleteRemoteSource(intent.source)
                    // If deleted source was selected, revert to internal storage
                    if (_state.value.selectedSourceId == intent.source.id) {
                        useCases.saveSelectedSourceId(null)
                    }
                }
            }

            is SourceManagerIntent.SelectSource -> {
                viewModelScope.launch {
                    useCases.saveSelectedSourceId(intent.sourceId)
                }
            }

            is SourceManagerIntent.ReorderSources -> {
                val currentList = _state.value.remoteSources.toMutableList()
                if (intent.fromIndex in currentList.indices && intent.toIndex in currentList.indices) {
                    val item = currentList.removeAt(intent.fromIndex)
                    currentList.add(intent.toIndex, item)

                    // Update sortOrder for all items to persist the new order
                    val updatedList = currentList.mapIndexed { index, source ->
                        source.copy(sortOrder = index)
                    }

                    viewModelScope.launch {
                        useCases.updateRemoteSources(updatedList)
                    }
                }
            }

            is SourceManagerIntent.SaveReorderedSources -> {
                viewModelScope.launch {
                    val updatedList = intent.sources.mapIndexed { index, source ->
                        source.copy(sortOrder = index)
                    }
                    useCases.updateRemoteSources(updatedList)
                }
            }
        }
    }
}
