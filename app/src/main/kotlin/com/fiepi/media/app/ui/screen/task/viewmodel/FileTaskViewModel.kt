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

package com.fiepi.media.app.ui.screen.task.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.app.service.FileOperationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FileTaskViewModel(
    private val fileOperationManager: FileOperationManager
) : ViewModel() {

    private val _state = MutableStateFlow(FileTaskScreenState())
    val state: StateFlow<FileTaskScreenState> = _state.asStateFlow()

    init {
        observeTasks()
    }

    private fun observeTasks() {
        viewModelScope.launch {
            fileOperationManager.tasks.collect { tasks ->
                _state.update { it.copy(tasks = tasks) }
            }
        }
    }

    fun onIntent(intent: FileTaskIntent) {
        when (intent) {
            is FileTaskIntent.CancelTask -> fileOperationManager.cancelTask(intent.taskId)
            is FileTaskIntent.ClearCompleted -> fileOperationManager.clearCompletedTasks()
        }
    }
}
