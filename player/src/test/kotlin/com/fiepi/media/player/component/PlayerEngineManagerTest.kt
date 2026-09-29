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

import com.fiepi.media.domain.player.PlayerEngine
import com.fiepi.media.domain.player.model.PlayerEngineType
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerEngineManagerTest {

    private val mockEngineMPV = mockk<PlayerEngine>(relaxed = true)
    private val mockEngineExo = mockk<PlayerEngine>(relaxed = true)
    private val testScope = TestScope(UnconfinedTestDispatcher())

    private lateinit var engineManager: PlayerEngineManager

    @Before
    fun setup() {
        engineManager = PlayerEngineManager(
            playerScope = testScope,
            engineFactory = { type ->
                if (type == PlayerEngineType.EXO_PLAYER) mockEngineExo else mockEngineMPV
            },
            onEngineEvent = {},
            onCapabilitiesChanged = {},
            bindSurfaceToNewEngine = {},
            unbindSurfaceFromOldEngine = {}
        )
    }

    @Test
    fun `getOrInitEngine creates and sets active engine`() {
        val engine = engineManager.getOrInitEngine(PlayerEngineType.MPV)

        assertEquals(mockEngineMPV, engine)
        assertEquals(mockEngineMPV, engineManager.activeEngine)
        assertEquals(PlayerEngineType.MPV, engineManager.currentEngineType)
    }

    @Test
    fun `switchEngineIfNeeded switches to target engine type and notifies capabilities`() = runTest {
        var capabilitiesNotified = false
        val manager = PlayerEngineManager(
            playerScope = testScope,
            engineFactory = { type ->
                if (type == PlayerEngineType.EXO_PLAYER) mockEngineExo else mockEngineMPV
            },
            onEngineEvent = {},
            onCapabilitiesChanged = { capabilitiesNotified = true },
            bindSurfaceToNewEngine = {},
            unbindSurfaceFromOldEngine = {}
        )

        manager.getOrInitEngine(PlayerEngineType.MPV)

        var prepareCalled = false
        var loadNewEngineCalled = false

        manager.switchEngineIfNeeded(
            targetType = PlayerEngineType.EXO_PLAYER,
            hasCurrentVideo = true,
            onPrepareSwitchState = { prepareCalled = true },
            onLoadNewEngineVideo = { loadNewEngineCalled = true }
        )

        assertEquals(mockEngineExo, manager.activeEngine)
        assertEquals(PlayerEngineType.EXO_PLAYER, manager.currentEngineType)
        assertEquals(true, prepareCalled)
        assertEquals(true, loadNewEngineCalled)
        assertEquals(true, capabilitiesNotified)
        verify { mockEngineMPV.release() }
    }

    @Test
    fun `releaseEngine clears active engine reference`() {
        engineManager.getOrInitEngine(PlayerEngineType.MPV)

        var unbindCalled = false
        engineManager.releaseEngine {
            unbindCalled = true
        }

        assertNull(engineManager.activeEngine)
        assertEquals(true, unbindCalled)
    }
}
