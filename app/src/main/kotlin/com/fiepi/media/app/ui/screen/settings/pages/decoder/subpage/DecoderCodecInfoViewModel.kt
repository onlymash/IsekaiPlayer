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

package com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.domain.model.decoder.CodecInfo
import com.fiepi.media.domain.usecase.decoder.GetSystemCodecsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DecoderCodecInfoViewModel(
    private val getSystemCodecs: GetSystemCodecsUseCase
) : ViewModel() {

    private val _codecs = MutableStateFlow<List<CodecInfo>>(emptyList())
    val codecs = _codecs.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _codecs.value = getSystemCodecs()
        }
    }
}
