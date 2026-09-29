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

import com.fiepi.media.domain.model.preferences.PlayerUiOptions
import com.fiepi.media.domain.repository.preferences.AudioPrefsRepository
import com.fiepi.media.domain.repository.preferences.GesturePrefsRepository
import com.fiepi.media.domain.repository.preferences.PlayerPrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class GetPlayerUiOptionsUseCase(
    private val playerPrefs: PlayerPrefsRepository,
    private val audioPrefs: AudioPrefsRepository,
    private val gesturePrefs: GesturePrefsRepository
) {
    operator fun invoke(): Flow<PlayerUiOptions> = combine(
        playerPrefs.getPlayerOptions(),
        audioPrefs.getAudioOptions(),
        gesturePrefs.getGestureOptions()
    ) { player, audio, gesture ->
        PlayerUiOptions(
            seekBarStyle = player.seekBarStyle,
            autoHideDurationSeconds = player.autoHideDurationSeconds,
            seekDurationSeconds = player.seekDurationSeconds,
            doubleTapSeekDurationSeconds = gesture.doubleTapSeekDurationSeconds,
            enablePiP = player.enablePiP,
            orientation = player.orientation,
            rememberBrightness = player.rememberBrightness,
            enableBackgroundAudio = audio.enableBackgroundAudio,
            buttonEffect = player.buttonEffect,
            alwaysFullScreen = player.alwaysFullScreen,
            videoScaleMode = player.videoScaleMode,
            playbackSpeed = player.playbackSpeed,
            isOsdVisible = player.isOsdVisible,
            osdPage = player.osdPage,
            gestureOptions = gesture,
            layoutOptions = player.layoutOptions
        )
    }.distinctUntilChanged()
}
