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
import android.media.AudioManager
import com.fiepi.media.domain.player.PlayerEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerAudioManagerTest {

    private val mockContext = mockk<Context>()
    private val mockAudioManager = mockk<AudioManager>(relaxed = true)
    private val mockEngine = mockk<PlayerEngine>(relaxed = true)
    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var audioManagerComponent: PlayerAudioManager

    @Before
    fun setup() {
        every { mockContext.getSystemService(Context.AUDIO_SERVICE) } returns mockAudioManager
        every { mockAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) } returns 15
        every { mockAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) } returns 10

        audioManagerComponent = PlayerAudioManager(
            context = mockContext,
            onAudioFocusLost = {},
            onSetEngineVolume = { mockEngine.setVolume(it) }
        )
    }

    @Test
    fun `setSystemVolume updates system volume correctly`() {
        var updatedSystem = 0

        audioManagerComponent.setSystemVolume(
            systemVol = 8,
            playerScope = testScope,
            onVolumeUpdated = { sys ->
                updatedSystem = sys
            }
        )

        assertEquals(8, updatedSystem)
    }

    @Test
    fun `setGainVolume updates gain volume correctly`() {
        var updatedGain = 0

        audioManagerComponent.setGainVolume(
            gainVol = 120,
            maxGain = 200,
            playerScope = testScope,
            onVolumeUpdated = { gain ->
                updatedGain = gain
            }
        )

        assertEquals(120, updatedGain)
    }

    @Test
    fun `adjustVolume up increases system volume when below max`() {
        var updatedSystem = 0

        audioManagerComponent.adjustVolume(
            up = true,
            currentSystemVol = 5,
            playerScope = testScope,
            onVolumeUpdated = { sys ->
                updatedSystem = sys
            }
        )

        assertEquals(6, updatedSystem)
    }

    @Test
    fun `adjustVolume up caps system volume at max`() {
        var updatedSystem = 0

        audioManagerComponent.adjustVolume(
            up = true,
            currentSystemVol = 15,
            playerScope = testScope,
            onVolumeUpdated = { sys ->
                updatedSystem = sys
            }
        )

        assertEquals(15, updatedSystem)
    }

    @Test
    fun `handleAudioFocusPreferenceChanged requests focus when enabled during playback`() {
        var focusDeniedCalled = false
        audioManagerComponent.handleAudioFocusPreferenceChanged(
            enabled = false,
            isPlaying = true,
            onFocusDenied = { focusDeniedCalled = true }
        )

        assertEquals(false, focusDeniedCalled)
    }
}
