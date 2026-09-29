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

package com.fiepi.media.domain.usecases

import com.fiepi.media.domain.usecase.history.GetHistoryUseCase
import com.fiepi.media.domain.usecase.history.SaveHistoryUseCase
import com.fiepi.media.domain.usecase.media.GetMediaFilesUseCase
import com.fiepi.media.domain.usecase.preferences.GetBrightnessUseCase
import com.fiepi.media.domain.usecase.preferences.GetPlaybackOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetPlayerUiOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetSortOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetSubtitleOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.SaveBrightnessUseCase
import com.fiepi.media.domain.usecase.source.GetSourceUseCase
import com.fiepi.media.domain.usecases.playlist.PlaylistUseCases
import com.fiepi.media.domain.usecases.preferences.AudioPrefsUseCases
import com.fiepi.media.domain.usecases.preferences.GesturePrefsUseCases
import com.fiepi.media.domain.usecases.preferences.PlayerPrefsUseCases
import com.fiepi.media.domain.usecases.preferences.SubtitlePrefsUseCases

data class PlayerUseCases(
    val getMediaSource: GetSourceUseCase,
    val getMediaFiles: GetMediaFilesUseCase,
    val getSortOptions: GetSortOptionsUseCase,
    val getHistory: GetHistoryUseCase,
    val saveHistory: SaveHistoryUseCase,
    val getBrightness: GetBrightnessUseCase,
    val saveBrightness: SaveBrightnessUseCase,
    val getPlaybackOptions: GetPlaybackOptionsUseCase,
    val getPlayerUiOptions: GetPlayerUiOptionsUseCase,
    val getSubtitleOptions: GetSubtitleOptionsUseCase,
    val playerPrefs: PlayerPrefsUseCases,
    val gesturePrefs: GesturePrefsUseCases,
    val audioPrefs: AudioPrefsUseCases,
    val subtitlePrefs: SubtitlePrefsUseCases,
    val playlist: PlaylistUseCases
)
