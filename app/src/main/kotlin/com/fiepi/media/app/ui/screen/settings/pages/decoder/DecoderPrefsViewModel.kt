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

package com.fiepi.media.app.ui.screen.settings.pages.decoder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.domain.model.preferences.DecoderOptions
import com.fiepi.media.domain.usecases.preferences.DecoderPrefsUseCases
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DecoderPrefsViewModel(
    private val useCases: DecoderPrefsUseCases
) : ViewModel() {

    val decoderOptions = useCases.getDecoderOptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DecoderOptions())

    fun onIntent(intent: DecoderPrefsIntent) {
        viewModelScope.launch {
            val current = decoderOptions.value
            val newOptions = when (intent) {
                is DecoderPrefsIntent.UpdateEngineType -> current.copy(engineType = intent.engineType)
                is DecoderPrefsIntent.UpdateProfile -> current.copy(profile = intent.profile)
                is DecoderPrefsIntent.UpdateVo -> current.copy(vo = intent.vo)
                is DecoderPrefsIntent.UpdateGpuApi -> current.copy(gpuApi = intent.gpuApi)
                is DecoderPrefsIntent.UpdateHwdec -> current.copy(hwdec = intent.hwdec)
                is DecoderPrefsIntent.UpdateScale -> current.copy(scale = intent.scale)
                is DecoderPrefsIntent.UpdateDscale -> current.copy(dscale = intent.dscale)
                is DecoderPrefsIntent.UpdateCscale -> current.copy(cscale = intent.cscale)
                is DecoderPrefsIntent.UpdateScaleAntiring -> current.copy(scaleAntiring = intent.antiring)
                is DecoderPrefsIntent.UpdateDeband -> current.copy(deband = intent.enabled)
                is DecoderPrefsIntent.UpdateDebandIterations -> current.copy(debandIterations = intent.iterations)
                is DecoderPrefsIntent.UpdateDebandThreshold -> current.copy(debandThreshold = intent.threshold)
                is DecoderPrefsIntent.UpdateDebandRange -> current.copy(debandRange = intent.range)
                is DecoderPrefsIntent.UpdateDebandGrain -> current.copy(debandGrain = intent.grain)
                is DecoderPrefsIntent.UpdateToneMapping -> current.copy(toneMapping = intent.toneMapping)
                is DecoderPrefsIntent.UpdateToneMappingMaxBoost -> current.copy(toneMappingMaxBoost = intent.maxBoost)
                is DecoderPrefsIntent.UpdateGamutMappingMode -> current.copy(gamutMappingMode = intent.mode)
                is DecoderPrefsIntent.UpdateHdrPassthrough -> current.copy(hdrPassthrough = intent.enabled)
                is DecoderPrefsIntent.UpdateFramedrop -> current.copy(framedrop = intent.framedrop)
                is DecoderPrefsIntent.UpdateVideoSync -> current.copy(videoSync = intent.videoSync)
                is DecoderPrefsIntent.UpdateOverrideProfile -> current.copy(overrideProfile = intent.enabled)
            }
            useCases.updateDecoderOptions(newOptions)
        }
    }
}
