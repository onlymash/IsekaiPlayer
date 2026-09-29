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

/**
 * Handles playlist index navigation and shuffle queue state management.
 */
internal class PlayerPlaylistManager {

    var shuffledIndices: List<Int> = emptyList()
        private set

    var shufflePointer: Int = -1
        private set

    fun updateShuffleQueueIfNeeded(playlistSize: Int, currentIndex: Int, forceReshuffle: Boolean = false) {
        if (playlistSize == 0) {
            shuffledIndices = emptyList()
            shufflePointer = -1
            return
        }

        val currentIdx = currentIndex.coerceIn(0, playlistSize - 1)
        if (forceReshuffle || shuffledIndices.size != playlistSize || !shuffledIndices.contains(currentIdx)) {
            val remaining = (0 until playlistSize).filter { it != currentIdx }.shuffled()
            shuffledIndices = listOf(currentIdx) + remaining
            shufflePointer = 0
        } else {
            shufflePointer = shuffledIndices.indexOf(currentIdx).coerceAtLeast(0)
        }
    }

    fun getNextIndex(loopMode: LoopMode, playlistSize: Int, currentIndex: Int): Int? {
        if (playlistSize == 0) return null

        return when (loopMode) {
            LoopMode.Shuffle, LoopMode.ShuffleRepeat -> {
                updateShuffleQueueIfNeeded(playlistSize, currentIndex)
                var nextPointer = shufflePointer + 1
                if (nextPointer >= shuffledIndices.size) {
                    if (loopMode == LoopMode.ShuffleRepeat) {
                        updateShuffleQueueIfNeeded(playlistSize, currentIndex, forceReshuffle = true)
                    }
                    nextPointer = 0
                }
                shufflePointer = nextPointer
                shuffledIndices.getOrNull(shufflePointer)
            }

            else -> {
                (currentIndex + 1) % playlistSize
            }
        }
    }

    fun getPreviousIndex(loopMode: LoopMode, playlistSize: Int, currentIndex: Int): Int? {
        if (playlistSize == 0) return null

        return when (loopMode) {
            LoopMode.Shuffle, LoopMode.ShuffleRepeat -> {
                updateShuffleQueueIfNeeded(playlistSize, currentIndex)
                var prevPointer = shufflePointer - 1
                if (prevPointer < 0) {
                    prevPointer = (shuffledIndices.size - 1).coerceAtLeast(0)
                }
                shufflePointer = prevPointer
                shuffledIndices.getOrNull(shufflePointer)
            }

            else -> {
                if (currentIndex - 1 >= 0) {
                    currentIndex - 1
                } else {
                    playlistSize - 1
                }
            }
        }
    }

    fun reset() {
        shuffledIndices = emptyList()
        shufflePointer = -1
    }
}
