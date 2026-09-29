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

import com.fiepi.media.domain.model.history.PlaybackHistory
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.usecase.history.GetHistoryUseCase
import com.fiepi.media.domain.usecase.history.SaveHistoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PlayerHistoryTrackerTest {

    private val mockGetHistoryUseCase = mockk<GetHistoryUseCase>()
    private val mockSaveHistoryUseCase = mockk<SaveHistoryUseCase>(relaxed = true)
    private lateinit var historyTracker: PlayerHistoryTracker

    @Before
    fun setup() {
        historyTracker = PlayerHistoryTracker(
            getHistory = mockGetHistoryUseCase,
            saveHistory = mockSaveHistoryUseCase,
            getCurrentState = { MediaPlaybackState() },
            getCurrentSource = { null }
        )
    }

    @Test
    fun `getResumePosition returns 0 if saved history position is less than 5000ms`() = runTest {
        coEvery { mockGetHistoryUseCase("source1", "/path/video.mp4") } returns PlaybackHistory(
            sourceId = "source1",
            path = "/path/video.mp4",
            title = "video.mp4",
            positionMs = 3000L,
            durationMs = 120000L,
            lastPlayedAt = 1000L
        )

        val (pos, duration) = historyTracker.getResumePosition(
            "source1",
            "/path/video.mp4",
            120000L
        )

        assertEquals(0L, pos)
        assertEquals(120000L, duration)
    }

    @Test
    fun `getResumePosition returns recorded position if greater than 5000ms`() = runTest {
        coEvery { mockGetHistoryUseCase("source1", "/path/video.mp4") } returns PlaybackHistory(
            sourceId = "source1",
            path = "/path/video.mp4",
            title = "video.mp4",
            positionMs = 45000L,
            durationMs = 120000L,
            lastPlayedAt = 1000L
        )

        val (pos, duration) = historyTracker.getResumePosition(
            "source1",
            "/path/video.mp4",
            120000L
        )

        assertEquals(45000L, pos)
        assertEquals(120000L, duration)
    }

    @Test
    fun `saveHistoryInternal saves history when position is greater than 5000ms`() = runTest {
        historyTracker.saveHistoryInternal(
            sourceId = "source1",
            path = "/path/video.mp4",
            positionMs = 10000L,
            durationMs = 60000L
        )

        coVerify {
            mockSaveHistoryUseCase(
                "source1",
                "/path/video.mp4",
                10000L,
                60000L,
                title = "",
                thumbnailUrl = null,
                sourceType = SourceType.Local,
                isCompleted = null
            )
        }
    }

    @Test
    fun `saveHistoryInternal skips saving when position is between 1 and 5000ms`() = runTest {
        historyTracker.saveHistoryInternal(
            sourceId = "source1",
            path = "/path/video.mp4",
            positionMs = 2000L,
            durationMs = 60000L
        )

        coVerify(exactly = 0) {
            mockSaveHistoryUseCase(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }
}
