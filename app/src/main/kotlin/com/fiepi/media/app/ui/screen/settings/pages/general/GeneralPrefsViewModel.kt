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

package com.fiepi.media.app.ui.screen.settings.pages.general

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.app.R
import com.fiepi.media.domain.usecases.backup.BackupUseCases
import com.fiepi.media.domain.usecases.preferences.GeneralPrefsUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GeneralPrefsViewModel(
    private val application: Application,
    private val useCases: GeneralPrefsUseCases,
    private val backupUseCases: BackupUseCases
) : ViewModel() {

    private val _effect = Channel<GeneralEffect>(Channel.BUFFERED)
    val effect: Flow<GeneralEffect> = _effect.receiveAsFlow()

    val uiState: StateFlow<GeneralUiState> = combine(
        useCases.getInterceptBackNavigation(),
        useCases.getHapticsEnabled()
    ) { interceptBackNavigation, hapticsEnabled ->
        GeneralUiState(interceptBackNavigation, hapticsEnabled)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GeneralUiState()
    )

    fun onIntent(intent: GeneralPrefsIntent) {
        viewModelScope.launch {
            when (intent) {
                is GeneralPrefsIntent.SetInterceptBackNavigation -> {
                    useCases.setInterceptBackNavigation(intent.enabled)
                }

                is GeneralPrefsIntent.SetHapticsEnabled -> {
                    useCases.setHapticsEnabled(intent.enabled)
                }

                is GeneralPrefsIntent.ExportBackup -> {
                    val result = backupUseCases.exportSettings(intent.uriString)
                    val msg = if (result.isSuccess) {
                        application.getString(R.string.settings_backup_export_success)
                    } else {
                        application.getString(R.string.settings_backup_export_failed)
                    }
                    _effect.send(GeneralEffect.ShowToast(msg))
                }

                is GeneralPrefsIntent.ImportBackup -> {
                    val result = backupUseCases.importSettings(intent.uriString)
                    val msg = if (result.isSuccess) {
                        application.getString(R.string.settings_backup_import_success)
                    } else {
                        application.getString(R.string.settings_backup_import_failed)
                    }
                    _effect.send(GeneralEffect.ShowToast(msg))
                }
            }
        }
    }
}
