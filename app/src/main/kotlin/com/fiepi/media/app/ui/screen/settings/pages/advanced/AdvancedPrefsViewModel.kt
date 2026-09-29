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

package com.fiepi.media.app.ui.screen.settings.pages.advanced

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.app.R
import com.fiepi.media.domain.model.cache.CacheInfo
import com.fiepi.media.domain.model.preferences.AdvancedOptions
import com.fiepi.media.domain.usecases.cache.CacheUseCases
import com.fiepi.media.domain.usecases.preferences.AdvancedPrefsUseCases
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdvancedPrefsViewModel(
    private val application: Application,
    private val useCases: AdvancedPrefsUseCases,
    private val cacheUseCases: CacheUseCases
) : ViewModel() {

    val advancedOptions: StateFlow<AdvancedOptions> = useCases.getAdvancedOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdvancedOptions())

    val cacheInfo: StateFlow<CacheInfo> = cacheUseCases.getCacheInfo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CacheInfo())

    val historyCount: StateFlow<Int> = useCases.getHistoryCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _effect = Channel<AdvancedEffect>(Channel.BUFFERED)
    val effect: Flow<AdvancedEffect> = _effect.receiveAsFlow()

    fun onIntent(intent: AdvancedPrefsIntent) {
        when (intent) {
            is AdvancedPrefsIntent.ClearThumbnailCache -> {
                viewModelScope.launch {
                    cacheUseCases.clearThumbnailCache()
                    _effect.send(AdvancedEffect.ShowToast(application.getString(R.string.settings_cache_cleared)))
                }
            }

            is AdvancedPrefsIntent.ClearVideoCache -> {
                viewModelScope.launch {
                    cacheUseCases.clearVideoCache()
                    _effect.send(AdvancedEffect.ShowToast(application.getString(R.string.settings_cache_cleared)))
                }
            }

            is AdvancedPrefsIntent.ClearPlaybackHistory -> {
                viewModelScope.launch {
                    useCases.clearAllHistory()
                    _effect.send(AdvancedEffect.ShowToast(application.getString(R.string.settings_advanced_history_cleared)))
                }
            }

            else -> {
                val current = advancedOptions.value
                val newOptions = when (intent) {
                    is AdvancedPrefsIntent.UpdateTlsVerify -> current.copy(tlsVerify = intent.enabled)
                    is AdvancedPrefsIntent.UpdateUserAgent -> current.copy(userAgent = intent.ua)
                    is AdvancedPrefsIntent.UpdateNetworkTimeout -> current.copy(networkTimeout = intent.timeout)
                    is AdvancedPrefsIntent.UpdateCacheSecs -> current.copy(cacheSecs = intent.secs)
                    is AdvancedPrefsIntent.UpdateCacheBackSecs -> current.copy(cacheBackSecs = intent.secs)
                    is AdvancedPrefsIntent.UpdateDemuxerMaxBytes -> current.copy(demuxerMaxBytes = intent.bytes)
                    is AdvancedPrefsIntent.UpdateDemuxerMaxBackBytes -> current.copy(
                        demuxerMaxBackBytes = intent.bytes
                    )
                }
                viewModelScope.launch {
                    useCases.updateAdvancedOptions(newOptions)
                }
            }
        }
    }
}
