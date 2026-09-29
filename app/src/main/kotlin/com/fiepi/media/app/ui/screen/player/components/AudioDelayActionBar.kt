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

package com.fiepi.media.app.ui.screen.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerControlMode
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.player.model.MediaPlaybackState
import java.util.Locale

/**
 * Audio delay adjustment panel.
 */
@Composable
fun AudioDelayActionBar(
    state: PlayerState,
    modifier: Modifier = Modifier,
    onIntent: (PlayerIntent) -> Unit
) {
    PlayerAdjustBar(
        modifier = modifier,
        value = state.playback.audioDelay.toFloat(),
        onValueChange = { newValue, persist ->
            onIntent(PlayerIntent.SetAudioDelay(newValue.toLong(), persist = persist))
        },
        onClose = { onIntent(PlayerIntent.SetControlMode(PlayerControlMode.Normal)) },
        valueText = String.format(Locale.US, "%.2fs", state.playback.audioDelay / 1000f),
        label = stringResource(R.string.player_audio_delay),
        range = -5000f..5000f,
        steps = 99, // 100ms per step
        step = 10f
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun AudioDelayActionBarPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AudioDelayActionBar(
                state = PlayerState(
                    playback = MediaPlaybackState(
                        audioDelay = 250L
                    )
                ),
                onIntent = {}
            )
        }
    }
}
