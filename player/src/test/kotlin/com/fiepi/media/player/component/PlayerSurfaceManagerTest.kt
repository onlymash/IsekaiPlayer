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
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerSurfaceManagerTest {

    private val mockEngine = mockk<PlayerEngine>(relaxed = true)
    private val surfaceManager = PlayerSurfaceManager(
        onSetEngineSurface = { mockEngine.setSurface(it) }
    )
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Test
    fun `setSurface updates lastSurfaceObject and binds active engine dynamically`() {
        val dummySurface = Any()

        surfaceManager.setSurface(
            surface = dummySurface,
            getCurrentFps = { 60.0 },
            playerScope = testScope
        )

        assertEquals(dummySurface, surfaceManager.lastSurfaceObject)
        verify { mockEngine.setSurface(dummySurface) }
    }

    @Test
    fun `detachSurface detaches active engine only if surface instance matches`() {
        val dummySurface1 = Any()
        val dummySurface2 = Any()

        surfaceManager.setSurface(dummySurface1, { 60.0 }, testScope)

        // Detaching with a different surface instance
        surfaceManager.detachSurface(dummySurface2, testScope)
        assertEquals(dummySurface1, surfaceManager.lastSurfaceObject)

        // Detaching with the matching surface instance
        surfaceManager.detachSurface(dummySurface1, testScope)
        assertNull(surfaceManager.lastSurfaceObject)
        verify { mockEngine.setSurface(null) }
    }

    @Test
    fun `clearSurfaceAndDetach clears reference and detaches engine`() = runTest {
        val dummySurface = Any()
        surfaceManager.setSurface(dummySurface, { 60.0 }, testScope)

        surfaceManager.clearSurfaceAndDetach()

        assertNull(surfaceManager.lastSurfaceObject)
        verify { mockEngine.setSurface(null) }
    }
}
