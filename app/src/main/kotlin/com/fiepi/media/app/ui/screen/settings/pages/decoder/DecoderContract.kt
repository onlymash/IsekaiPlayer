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

import com.fiepi.media.domain.player.model.PlayerEngineType

sealed class DecoderPrefsIntent {
    data class UpdateEngineType(val engineType: PlayerEngineType) : DecoderPrefsIntent()
    data class UpdateProfile(val profile: String) : DecoderPrefsIntent()
    data class UpdateVo(val vo: String) : DecoderPrefsIntent()
    data class UpdateGpuApi(val gpuApi: String) : DecoderPrefsIntent()
    data class UpdateHwdec(val hwdec: List<String>) : DecoderPrefsIntent()
    data class UpdateScale(val scale: String) : DecoderPrefsIntent()
    data class UpdateDscale(val dscale: String) : DecoderPrefsIntent()
    data class UpdateCscale(val cscale: String) : DecoderPrefsIntent()
    data class UpdateScaleAntiring(val antiring: Float) : DecoderPrefsIntent()
    data class UpdateDeband(val enabled: Boolean) : DecoderPrefsIntent()
    data class UpdateDebandIterations(val iterations: Int) : DecoderPrefsIntent()
    data class UpdateDebandThreshold(val threshold: Int) : DecoderPrefsIntent()
    data class UpdateDebandRange(val range: Int) : DecoderPrefsIntent()
    data class UpdateDebandGrain(val grain: Int) : DecoderPrefsIntent()
    data class UpdateToneMapping(val toneMapping: String) : DecoderPrefsIntent()
    data class UpdateToneMappingMaxBoost(val maxBoost: Float) : DecoderPrefsIntent()
    data class UpdateGamutMappingMode(val mode: String) : DecoderPrefsIntent()
    data class UpdateHdrPassthrough(val enabled: Boolean) : DecoderPrefsIntent()
    data class UpdateFramedrop(val framedrop: String) : DecoderPrefsIntent()
    data class UpdateVideoSync(val videoSync: String) : DecoderPrefsIntent()
    data class UpdateOverrideProfile(val enabled: Boolean) : DecoderPrefsIntent()
}
