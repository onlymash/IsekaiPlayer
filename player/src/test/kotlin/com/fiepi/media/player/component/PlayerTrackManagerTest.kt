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
import com.fiepi.media.domain.player.model.ExternalSubtitle
import com.fiepi.media.domain.player.model.MediaTrack
import com.fiepi.media.domain.player.model.PlayerEngineType
import com.fiepi.media.domain.player.model.TrackType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerTrackManagerTest {

    private val mockEngine = mockk<PlayerEngine>(relaxed = true)
    private val trackManager = PlayerTrackManager(
        getEngineType = { PlayerEngineType.MPV },
        onSelectTrack = { type, id -> mockEngine.selectTrack(type, id) },
        onAddSubtitleToEngine = { subtitle -> mockEngine.addSubtitle(subtitle) },
        onAddSubtitlesToEngine = { subtitles -> mockEngine.addSubtitles(subtitles) }
    )

    @Test
    fun `resetForNewVideo resets all internal track states`() {
        trackManager.selectTrack(TrackType.Audio, 1)
        trackManager.selectTrack(TrackType.Video, 2)

        trackManager.resetForNewVideo(expectedSubtitlesCount = 3)

        assertEquals(null, trackManager.lastSelectedAudioId)
        assertEquals(null, trackManager.lastSelectedVideoId)
        assertEquals(3, trackManager.expectedExternalSubtitlesCount)
        assertFalse(trackManager.autoSubtitleSelectionDone)
    }

    @Test
    fun `selectTrack allows disabling video when audio track is active`() {
        trackManager.selectTrack(TrackType.Audio, 1)

        val success = trackManager.selectTrack(TrackType.Video, -1)

        assertTrue(success)
        verify { mockEngine.selectTrack(TrackType.Video, -1) }
    }

    @Test
    fun `selectTrack blocks disabling video when audio track is also disabled`() {
        // Video is active (1), Audio is disabled (-1)
        trackManager.selectTrack(TrackType.Video, 1)
        trackManager.selectTrack(TrackType.Audio, -1)

        // Attempting to also disable Video
        val success = trackManager.selectTrack(TrackType.Video, -1)

        assertFalse(success)
        verify(exactly = 0) { mockEngine.selectTrack(TrackType.Video, -1) }
    }

    @Test
    fun `onAudioTracksChanged updates lastSelectedAudioId`() {
        val tracks = listOf(
            MediaTrack.Audio(id = 1, title = "A1", language = "eng", isSelected = false),
            MediaTrack.Audio(id = 2, title = "A2", language = "chi", isSelected = true)
        )

        trackManager.onAudioTracksChanged(tracks)

        assertEquals(2, trackManager.lastSelectedAudioId)
    }

    @Test
    fun `onVideoTracksChanged updates lastSelectedVideoId`() {
        val tracks = listOf(
            MediaTrack.Video(id = 10, title = "V1", language = null, isSelected = true)
        )

        trackManager.onVideoTracksChanged(tracks)

        assertEquals(10, trackManager.lastSelectedVideoId)
    }

    @Test
    fun `addSubtitle triggers onAddSubtitleToState callback`() {
        var addedSubtitlePath: String? = null
        val customTrackManager = PlayerTrackManager(
            getEngineType = { PlayerEngineType.MPV },
            onSelectTrack = { type, id -> mockEngine.selectTrack(type, id) },
            onAddSubtitleToEngine = { subtitle -> mockEngine.addSubtitle(subtitle) },
            onAddSubtitlesToEngine = { subtitles -> mockEngine.addSubtitles(subtitles) },
            onAddSubtitleToState = { subtitleFile ->
                addedSubtitlePath = subtitleFile.path
            }
        )

        val mockResolver = mockk<MediaSourceResolver>(relaxed = true)
        every { mockResolver.getEngineType } returns { PlayerEngineType.MPV }
        every { mockResolver.resolveExternalUrl(any(), any()) } returns "http://example.com/test.srt"
        customTrackManager.addSubtitle(
            subtitle = ExternalSubtitle(url = "http://example.com/test.srt"),
            mediaSourceResolver = mockResolver,
            playerScope = CoroutineScope(Dispatchers.Unconfined)
        )

        assertEquals("http://example.com/test.srt", addedSubtitlePath)
    }
}
