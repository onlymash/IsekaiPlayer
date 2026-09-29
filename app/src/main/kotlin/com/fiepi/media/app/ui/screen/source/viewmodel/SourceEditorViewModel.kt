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
import com.fiepi.media.app.R
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.usecases.SourceEditorUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class SourceEditorViewModel(
    private val sourceId: String?,
    private val useCases: SourceEditorUseCases
) : ViewModel() {

    private val _state = MutableStateFlow(SourceEditorState(id = sourceId))
    val state: StateFlow<SourceEditorState> = _state.asStateFlow()

    init {
        if (sourceId != null) {
            viewModelScope.launch {
                val source = useCases.getRemoteSource(sourceId)
                if (source != null) {
                    _state.update {
                        it.copy(
                            name = source.name,
                            sourceType = source.type,
                            host = source.host,
                            port = source.port?.toString() ?: "",
                            username = source.username ?: "",
                            password = source.password ?: "",
                            path = source.path ?: "",
                            useHttps = source.useHttps,
                            sortOrder = source.sortOrder
                        )
                    }
                }
            }
        }
    }

    fun onIntent(intent: SourceEditorIntent) {
        when (intent) {
            is SourceEditorIntent.UpdateName -> _state.update { it.copy(name = intent.name) }
            is SourceEditorIntent.UpdateProtocol -> _state.update {
                it.copy(
                    sourceType = intent.sourceType,
                    errorResId = null,
                    detailedError = null
                )
            }

            is SourceEditorIntent.UpdateHost -> _state.update { it.copy(host = intent.host) }
            is SourceEditorIntent.UpdatePort -> _state.update { it.copy(port = intent.port) }
            is SourceEditorIntent.UpdateUsername -> _state.update { it.copy(username = intent.username) }
            is SourceEditorIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is SourceEditorIntent.UpdatePath -> _state.update { it.copy(path = intent.path) }
            is SourceEditorIntent.UpdateUseHttps -> _state.update { it.copy(useHttps = intent.useHttps) }
            is SourceEditorIntent.Save -> saveSource()
        }
    }

    private fun saveSource() {
        val s = _state.value
        if (s.name.isBlank() || s.host.isBlank()) {
            _state.update { it.copy(errorResId = R.string.source_editor_error_required) }
            return
        }

        if (s.sourceType == SourceType.Smb && s.path.isBlank()) {
            _state.update { it.copy(errorResId = R.string.source_editor_error_smb_share_required) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorResId = null, detailedError = null) }
            try {
                // Calculate next sortOrder if adding a new source
                val finalSortOrder = if (s.id == null) {
                    useCases.getMaxSortOrder() + 1
                } else {
                    s.sortOrder
                }

                val source = RemoteSource(
                    id = s.id ?: UUID.randomUUID().toString(),
                    name = s.name.trim(),
                    type = s.sourceType,
                    host = s.host.trim(),
                    port = s.port.trim().toIntOrNull(),
                    username = s.username.trim().takeIf { it.isNotEmpty() },
                    password = s.password.trim().takeIf { it.isNotEmpty() },
                    path = s.path.trim().takeIf { it.isNotEmpty() },
                    useHttps = s.useHttps,
                    sortOrder = finalSortOrder
                )
                // Validate connection before saving
                useCases.validateRemoteSource(source)

                useCases.saveRemoteSource(source)

                // If it's a new source, select it automatically
                if (sourceId == null) {
                    useCases.saveSelectedSourceId(source.id)
                }

                _state.update { it.copy(isSaving = false, isSuccess = true) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        detailedError = e.localizedMessage ?: e.message ?: "Unknown error"
                    )
                }
            }
        }
    }
}
