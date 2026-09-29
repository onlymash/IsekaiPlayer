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

import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.assets.LanguageType
import com.fiepi.media.domain.repository.preferences.AdvancedPrefsRepository
import com.fiepi.media.domain.repository.preferences.AudioPrefsRepository
import com.fiepi.media.domain.repository.preferences.PlayerPrefsRepository
import com.fiepi.media.domain.repository.preferences.SubtitlePrefsRepository
import com.fiepi.media.domain.usecase.assets.GetLanguageCodesUseCase
import com.fiepi.media.player.model.ExoPlayerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetExoPlayerConfigUseCase(
    private val audioPrefs: AudioPrefsRepository,
    private val subtitlePrefs: SubtitlePrefsRepository,
    private val playerPrefs: PlayerPrefsRepository,
    private val advancedPrefs: AdvancedPrefsRepository,
    private val getLanguageCodes: GetLanguageCodesUseCase
) {
    operator fun invoke(): Flow<ExoPlayerConfig> = combine(
        audioPrefs.getAudioOptions(),
        subtitlePrefs.getSubtitleOptions(),
        playerPrefs.getPlayerOptions(),
        advancedPrefs.getAdvancedOptions()
    ) { audio, subtitle, player, advanced ->
        val audioCodes = getLanguageCodes(LanguageType.Audio, audio.preferredAudioLanguages)
            .filter { it != "none" }

        val subCodes = getLanguageCodes(LanguageType.Subtitle, subtitle.preferredSubtitleLanguages)
            .filter { it != "none" && it.isNotBlank() }

        ExoPlayerConfig(
            preferredAudioLanguages = audioCodes,
            preferredSubtitleLanguages = subCodes,
            audioPitchCorrection = audio.audioPitchCorrection,
            volume = audio.exoVolume.toDouble(),
            videoScaleMode = player.videoScaleMode,
            playbackSpeed = player.playbackSpeed,
            networkTimeout = advanced.networkTimeout * 1000L,
            userAgent = advanced.userAgent.ifBlank { AppConstants.DEFAULT_USER_AGENT }
        )
    }.distinctUntilChanged()
}
