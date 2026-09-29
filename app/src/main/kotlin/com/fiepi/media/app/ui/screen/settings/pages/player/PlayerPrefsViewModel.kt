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

package com.fiepi.media.app.ui.screen.settings.pages.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiepi.media.domain.model.preferences.PlayerOptions
import com.fiepi.media.domain.usecases.preferences.PlayerPrefsUseCases
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerPrefsViewModel(
    private val useCases: PlayerPrefsUseCases
) : ViewModel() {

    val options = useCases.getPlayerOptions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerOptions()
    )

    fun onIntent(intent: PlayerPrefsIntent) {
        viewModelScope.launch {
            val current = options.value
            val newOptions = when (intent) {
                is PlayerPrefsIntent.UpdateAutoHideDuration -> current.copy(autoHideDurationSeconds = intent.seconds)
                is PlayerPrefsIntent.UpdateSeekDuration -> current.copy(seekDurationSeconds = intent.seconds)
                is PlayerPrefsIntent.UpdatePlaybackSpeed -> current.copy(playbackSpeed = intent.speed)
                is PlayerPrefsIntent.UpdateSeekBarStyle -> current.copy(seekBarStyle = intent.style)
                is PlayerPrefsIntent.UpdateResumePlayback -> current.copy(resumePlayback = intent.resume)
                is PlayerPrefsIntent.UpdateLoopMode -> current.copy(loopMode = intent.mode)
                is PlayerPrefsIntent.UpdateEnablePiP -> current.copy(enablePiP = intent.enable)
                is PlayerPrefsIntent.UpdateRememberBrightness -> current.copy(rememberBrightness = intent.remember)
                is PlayerPrefsIntent.UpdateOrientation -> current.copy(orientation = intent.orientation)
                is PlayerPrefsIntent.UpdateButtonEffectOptions -> current.copy(buttonEffect = intent.options)
                is PlayerPrefsIntent.UpdateAlwaysFullScreen -> current.copy(alwaysFullScreen = intent.always)
                is PlayerPrefsIntent.UpdateLayoutOptions -> current.copy(layoutOptions = intent.options)
                is PlayerPrefsIntent.UpdateScreenshotFormat -> current.copy(screenshotFormat = intent.format)
            }
            useCases.updatePlayerOptions(newOptions)
        }
    }
}
