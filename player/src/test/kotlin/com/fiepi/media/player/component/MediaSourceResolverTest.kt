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
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.MediaStreamServer
import com.fiepi.media.domain.player.model.PlayerEngineType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MediaSourceResolverTest {

    private val mockContext = mockk<Context>()
    private val mockStreamServer = mockk<MediaStreamServer>(relaxed = true)

    private lateinit var resolver: MediaSourceResolver

    @Before
    fun setup() {
        resolver = MediaSourceResolver(mockContext, mockStreamServer, getEngineType = { PlayerEngineType.MPV })
    }

    @Test
    fun `resolveMediaUrl for local source returns original path`() {
        val localSource = MediaSource.Local("local_1", "Internal Storage", "/sdcard")
        val file = MediaFile.Video(
            path = "/sdcard/Download/movie.mp4",
            name = "movie",
            sourceType = SourceType.Local,
            id = "1",
            extension = "mp4"
        )

        val result = resolver.resolveMediaUrl(localSource, file)

        assertEquals("/sdcard/Download/movie.mp4", result)
    }

    @Test
    fun `resolveMediaUrl for remote SMB source registers session with streamServer`() {
        val remoteSource = RemoteSource(
            id = "smb_server_1",
            name = "Home SMB",
            type = SourceType.Smb,
            host = "192.168.1.100"
        )
        val source = MediaSource.Remote(remoteSource)
        val file = MediaFile.Video(
            path = "Share/Videos/test.mkv",
            name = "test",
            sourceType = SourceType.Smb,
            id = "2",
            size = 1024L * 1024L,
            extension = "mkv"
        )

        every { mockStreamServer.isRunning() } returns false
        every { mockStreamServer.registerMediaSession(any(), any(), any(), any()) } returns "http://127.0.0.1:8080/stream/session_1"

        val result = resolver.resolveMediaUrl(source, file)

        verify { mockStreamServer.start() }
        verify {
            mockStreamServer.registerMediaSession(
                sessionId = "smb_server_1_${"Share/Videos/test.mkv".hashCode()}",
                source = remoteSource,
                filePath = "Share/Videos/test.mkv",
                knownFileSize = 1024L * 1024L
            )
        }
        assertEquals("http://127.0.0.1:8080/stream/session_1", result)
    }

    @Test
    fun `resolveMediaSource for stream sourceType returns stream MediaSource`() = runTest {
        val file = MediaFile.Video(
            path = "https://example.com/live.m3u8",
            name = "live",
            sourceType = SourceType.Stream,
            id = "3"
        )

        val result = resolver.resolveMediaSource(file) { _, _ -> null }

        assertEquals(MediaSource.NETWORK_STREAM_ID, result?.id)
        assertEquals(SourceType.External, result?.type)
    }
}
