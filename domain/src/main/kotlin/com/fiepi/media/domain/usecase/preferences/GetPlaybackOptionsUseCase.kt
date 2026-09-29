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

package com.fiepi.media.domain.usecase.preferences

import com.fiepi.media.domain.model.preferences.PlaybackOptions
import com.fiepi.media.domain.repository.preferences.AudioPrefsRepository
import com.fiepi.media.domain.repository.preferences.DecoderPrefsRepository
import com.fiepi.media.domain.repository.preferences.PlayerPrefsRepository
import com.fiepi.media.domain.repository.preferences.SubtitlePrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetPlaybackOptionsUseCase(
    private val playerPrefs: PlayerPrefsRepository,
    private val audioPrefs: AudioPrefsRepository,
    private val subtitlePrefs: SubtitlePrefsRepository,
    private val decoderPrefs: DecoderPrefsRepository
) {
    operator fun invoke(): Flow<PlaybackOptions> = combine(
        playerPrefs.getPlayerOptions(),
        audioPrefs.getAudioOptions(),
        subtitlePrefs.getSubtitleOptions(),
        decoderPrefs.getDecoderOptions()
    ) { player, audio, subtitle, decoder ->
        PlaybackOptions(
            resumePlayback = player.resumePlayback,
            loopMode = player.loopMode,
            requestAudioFocus = audio.requestAudioFocus,
            preferredAudioLanguages = audio.preferredAudioLanguages,
            preferredSubtitleLanguages = subtitle.preferredSubtitleLanguages,
            subtitleFallback = subtitle.subtitleFallback,
            engineType = decoder.engineType
        )
    }.distinctUntilChanged()
}
