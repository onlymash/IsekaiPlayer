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

import com.fiepi.media.domain.model.preferences.LoopMode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlayerPlaylistManagerTest {

    private val playlistManager = PlayerPlaylistManager()

    @Test
    fun `updateShuffleQueueIfNeeded generates queue containing current index first`() {
        playlistManager.updateShuffleQueueIfNeeded(playlistSize = 5, currentIndex = 2)

        val shuffled = playlistManager.shuffledIndices
        assertEquals(5, shuffled.size)
        assertEquals(2, shuffled.first())
        assertEquals(0, playlistManager.shufflePointer)
        assertTrue(shuffled.containsAll(listOf(0, 1, 2, 3, 4)))
    }

    @Test
    fun `getNextIndex returns next sequential index`() {
        val nextIdx = playlistManager.getNextIndex(
            loopMode = LoopMode.Sequential,
            playlistSize = 3,
            currentIndex = 0
        )
        assertEquals(1, nextIdx)

        val wrapIdx = playlistManager.getNextIndex(
            loopMode = LoopMode.Sequential,
            playlistSize = 3,
            currentIndex = 2
        )
        assertEquals(0, wrapIdx)
    }

    @Test
    fun `getNextIndex in Shuffle mode returns valid index`() {
        val nextIdx = playlistManager.getNextIndex(
            loopMode = LoopMode.Shuffle,
            playlistSize = 4,
            currentIndex = 0
        )
        assertNotNull(nextIdx)
        assertTrue(nextIdx in 0 until 4)
    }

    @Test
    fun `getPreviousIndex returns previous sequential index`() {
        val prevIdx = playlistManager.getPreviousIndex(
            loopMode = LoopMode.Sequential,
            playlistSize = 3,
            currentIndex = 1
        )
        assertEquals(0, prevIdx)

        val wrapPrevIdx = playlistManager.getPreviousIndex(
            loopMode = LoopMode.Sequential,
            playlistSize = 3,
            currentIndex = 0
        )
        assertEquals(2, wrapPrevIdx)
    }

    @Test
    fun `reset clears shuffle queue and pointer`() {
        playlistManager.updateShuffleQueueIfNeeded(playlistSize = 3, currentIndex = 0)
        playlistManager.reset()

        assertTrue(playlistManager.shuffledIndices.isEmpty())
        assertEquals(-1, playlistManager.shufflePointer)
    }
}
