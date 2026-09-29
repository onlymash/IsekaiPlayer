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

package com.fiepi.media.app.ui.screen.stream.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.app.R
import com.fiepi.media.domain.usecases.NetworkStreamUseCases
import com.fiepi.media.domain.utils.FileUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkStreamViewModel(
    private val useCases: NetworkStreamUseCases,
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkStreamState())
    val state: StateFlow<NetworkStreamState> = _state.asStateFlow()

    private val _effect = Channel<NetworkStreamEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            useCases.getStreamHistory().collect { history ->
                _state.update { it.copy(historyList = history) }
            }
        }
    }

    fun onIntent(intent: NetworkStreamIntent) {
        when (intent) {
            is NetworkStreamIntent.UpdateInputUrl -> _state.update {
                it.copy(inputUrl = intent.url, errorMessageResId = null)
            }

            is NetworkStreamIntent.PlayStream -> playStream(intent.url)

            is NetworkStreamIntent.DeleteHistory -> {
                viewModelScope.launch {
                    useCases.deleteStreamUrl(intent.url)
                }
            }

            is NetworkStreamIntent.ClearHistory -> {
                viewModelScope.launch {
                    useCases.clearStreamHistory()
                }
            }

            is NetworkStreamIntent.ClearError -> _state.update {
                it.copy(errorMessageResId = null)
            }
        }
    }

    private fun playStream(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank() || (!trimmed.startsWith("http://", ignoreCase = true) &&
                    !trimmed.startsWith("https://", ignoreCase = true) &&
                    !trimmed.startsWith("rtmp://", ignoreCase = true) &&
                    !trimmed.startsWith("rtsp://", ignoreCase = true))
        ) {
            _state.update { it.copy(errorMessageResId = R.string.network_stream_invalid_url) }
            return
        }

        viewModelScope.launch {
            useCases.saveStreamUrl(trimmed, FileUtils.extractFileName(trimmed))
            _effect.send(NetworkStreamEffect.LaunchPlayer(trimmed))
        }
    }
}
