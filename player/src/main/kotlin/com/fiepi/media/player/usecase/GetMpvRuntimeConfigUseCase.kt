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

import com.fiepi.media.domain.model.assets.LanguageType
import com.fiepi.media.domain.repository.preferences.AdvancedPrefsRepository
import com.fiepi.media.domain.repository.preferences.AudioPrefsRepository
import com.fiepi.media.domain.repository.preferences.DecoderPrefsRepository
import com.fiepi.media.domain.repository.preferences.PlayerPrefsRepository
import com.fiepi.media.domain.repository.preferences.SubtitlePrefsRepository
import com.fiepi.media.domain.usecase.assets.GetLanguageCodesUseCase
import com.fiepi.mpv.model.MpvAudioConfig
import com.fiepi.mpv.model.MpvCacheConfig
import com.fiepi.mpv.model.MpvDecoderConfig
import com.fiepi.mpv.model.MpvDisplayConfig
import com.fiepi.mpv.model.MpvRuntimeConfig
import com.fiepi.mpv.model.MpvSubtitleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetMpvRuntimeConfigUseCase(
    private val decoderPrefs: DecoderPrefsRepository,
    private val audioPrefs: AudioPrefsRepository,
    private val subtitlePrefs: SubtitlePrefsRepository,
    private val playerPrefs: PlayerPrefsRepository,
    private val advancedPrefs: AdvancedPrefsRepository,
    private val getLanguageCodes: GetLanguageCodesUseCase
) {
    operator fun invoke(): Flow<MpvRuntimeConfig> = combine(
        decoderPrefs.getDecoderOptions(),
        audioPrefs.getAudioOptions(),
        subtitlePrefs.getSubtitleOptions(),
        playerPrefs.getPlayerOptions(),
        advancedPrefs.getAdvancedOptions()
    ) { decoder, audio, subtitle, player, advanced ->
        val audioCodes = getLanguageCodes(LanguageType.Audio, audio.preferredAudioLanguages)
            .filter { it != "none" }
        
        val subCodes = getLanguageCodes(LanguageType.Subtitle, subtitle.preferredSubtitleLanguages)
            .filter { it != "none" && it.isNotBlank() }

        val mpvSub = subtitle.mpvSubtitle

        MpvRuntimeConfig(
            decoder = MpvDecoderConfig(
                hwdec = decoder.hwdec.joinToString(","),
                framedrop = decoder.framedrop,
                scale = decoder.scale,
                dscale = decoder.dscale,
                cscale = decoder.cscale,
                scaleAntiring = decoder.scaleAntiring,
                deband = decoder.deband,
                debandIterations = decoder.debandIterations,
                debandThreshold = decoder.debandThreshold,
                debandRange = decoder.debandRange,
                debandGrain = decoder.debandGrain,
                toneMapping = decoder.toneMapping,
                toneMappingMaxBoost = decoder.toneMappingMaxBoost,
                gamutMappingMode = decoder.gamutMappingMode,
                hdrPassthrough = decoder.hdrPassthrough,
            ),
            audio = MpvAudioConfig(
                audioChannels = "auto-safe",
                audioSpdif = "",
                audioPitchCorrection = audio.audioPitchCorrection,
                volume = audio.mpvVolume.toDouble(),
                volumeMax = 200.0,
                dynamicAudioNormalize = audio.dynamicAudioNormalize,
                audioDelay = audio.audioDelay / 1000.0,
                preferredAudioLang = audioCodes.joinToString(",").ifBlank { "auto" },
            ),
            subtitle = MpvSubtitleConfig(
                preferredSubtitleLang = subCodes.joinToString(",").ifBlank { "auto" },
                subAuto = "fuzzy",
                subCodepage = subtitle.subCodepage,
                subFontSize = mpvSub.subFontSize,
                fontColorHex = mpvSub.fontColorHex,
                outlineColorHex = mpvSub.outlineColorHex,
                outlineWidth = mpvSub.outlineWidth,
                shadowColorHex = mpvSub.shadowColorHex,
                shadowOffset = mpvSub.shadowOffset,
                subBold = mpvSub.subBold,
                subItalic = mpvSub.subItalic,
                subFont = mpvSub.subFont,
                subFontsDir = mpvSub.subFontsDir,
                bottomMargin = mpvSub.bottomMargin,
                subAssOverride = mpvSub.subAssOverride,
                blendSubtitles = "yes",
                subsFallback = subtitle.subtitleFallback,
            ),
            cache = MpvCacheConfig(
                demuxerMaxBytes = advanced.demuxerMaxBytes,
                demuxerMaxBackBytes = advanced.demuxerMaxBackBytes,
                cacheSecs = advanced.cacheSecs,
                cacheBackSecs = advanced.cacheBackSecs,
                networkTimeout = advanced.networkTimeout,
                userAgent = advanced.userAgent,
            ),
            display = MpvDisplayConfig(
                videoScaleMode = player.videoScaleMode.value,
                playbackSpeed = player.playbackSpeed,
                isOsdVisible = player.isOsdVisible,
                osdPage = player.osdPage,
            )
        )
    }
}
