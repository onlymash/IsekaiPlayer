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

package com.fiepi.media.player.component

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.fiepi.media.domain.player.model.PlayerVolumeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Handles Audio Focus requests/abandonment and precise System Volume + Software Gain mapping algorithms.
 * Uses callback [onSetEngineVolume] to adjust engine volume without directly referencing engine instances.
 */
internal class PlayerAudioManager(
    context: Context,
    private val onAudioFocusLost: () -> Unit,
    private val onSetEngineVolume: (volume: Double) -> Unit
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val systemMaxVolume: Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                onAudioFocusLost()
            }
        }
    }

    private val audioFocusRequest by lazy {
        val attrBuilder = AudioAttributes.Builder()
        attrBuilder.setUsage(AudioAttributes.USAGE_MEDIA)
        attrBuilder.setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)

        val reqBuilder = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        reqBuilder.setAudioAttributes(attrBuilder.build())
        reqBuilder.setOnAudioFocusChangeListener(audioFocusChangeListener)
        reqBuilder.build()
    }

    fun requestAudioFocus(enabledPreference: Boolean): Boolean {
        if (!enabledPreference) return true
        val result = audioManager.requestAudioFocus(audioFocusRequest)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    /**
     * Dynamically handles preference change for Audio Focus request while playing.
     */
    fun handleAudioFocusPreferenceChanged(
        enabled: Boolean,
        isPlaying: Boolean,
        onFocusDenied: () -> Unit
    ) {
        if (isPlaying) {
            if (enabled) {
                val granted = requestAudioFocus(enabledPreference = true)
                if (!granted) {
                    onFocusDenied()
                }
            } else {
                abandonAudioFocus()
            }
        }
    }

    fun getInitialVolumeState(maxGain: Int = 200): PlayerVolumeState {
        return PlayerVolumeState(
            systemVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
            systemMaxVolume = systemMaxVolume,
            gainVolume = 100,
            maxGainVolume = maxGain
        )
    }

    fun setSystemVolume(
        systemVol: Int,
        playerScope: CoroutineScope,
        onVolumeUpdated: (systemVol: Int) -> Unit
    ) {
        val cappedSystemVol = systemVol.coerceIn(0, systemMaxVolume)
        onVolumeUpdated(cappedSystemVol)
        playerScope.launch {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, cappedSystemVol, 0)
        }
    }

    fun setGainVolume(
        gainVol: Int,
        maxGain: Int,
        playerScope: CoroutineScope,
        onVolumeUpdated: (gainVol: Int) -> Unit
    ) {
        val cappedGainVol = gainVol.coerceIn(0, maxGain)
        onVolumeUpdated(cappedGainVol)
        playerScope.launch {
            onSetEngineVolume(cappedGainVol.toDouble())
        }
    }

    fun adjustVolume(
        up: Boolean,
        currentSystemVol: Int,
        playerScope: CoroutineScope,
        onVolumeUpdated: (systemVol: Int) -> Unit
    ) {
        val step = (systemMaxVolume * 0.05f).roundToInt().coerceAtLeast(1)
        val newVol = if (up) {
            (currentSystemVol + step).coerceAtMost(systemMaxVolume)
        } else {
            (currentSystemVol - step).coerceAtLeast(0)
        }
        setSystemVolume(newVol, playerScope, onVolumeUpdated)
    }
}
