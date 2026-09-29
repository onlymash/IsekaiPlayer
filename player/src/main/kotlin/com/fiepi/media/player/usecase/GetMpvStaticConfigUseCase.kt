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

package com.fiepi.media.player.usecase

import com.fiepi.media.domain.repository.preferences.AdvancedPrefsRepository
import com.fiepi.media.domain.repository.preferences.AudioPrefsRepository
import com.fiepi.media.domain.repository.preferences.DecoderPrefsRepository
import com.fiepi.mpv.model.MpvStaticConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMpvStaticConfigUseCase(
    private val decoderPrefs: DecoderPrefsRepository,
    private val audioPrefs: AudioPrefsRepository,
    private val advancedPrefs: AdvancedPrefsRepository
) {
    operator fun invoke(): Flow<MpvStaticConfig> = combine(
        decoderPrefs.getDecoderOptions(),
        audioPrefs.getAudioOptions(),
        advancedPrefs.getAdvancedOptions()
    ) { decoder, audio, advanced ->
        MpvStaticConfig(
            vo = decoder.vo,
            gpuApi = decoder.gpuApi,
            profile = decoder.profile,
            videoSync = decoder.videoSync,
            audioOutput = audio.audioOutputBackends.joinToString(","),
            tlsVerify = advanced.tlsVerify,
            overrideProfile = decoder.overrideProfile
        )
    }
}
