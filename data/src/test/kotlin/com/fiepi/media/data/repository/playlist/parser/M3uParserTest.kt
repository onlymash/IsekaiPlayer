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

package com.fiepi.media.data.repository.playlist.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class M3uParserTest {

    @Test
    fun parse_simpleM3u_returnsEntries() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:120,Sample Video 1
            http://example.com/video1.mp4
            #EXTINF:180,Sample Video 2
            http://example.com/video2.mp4
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(2, entries.size)
        assertEquals("Sample Video 1", entries[0].title)
        assertEquals("http://example.com/video1.mp4", entries[0].url)
        assertEquals(120L, entries[0].durationSeconds)

        assertEquals("Sample Video 2", entries[1].title)
        assertEquals("http://example.com/video2.mp4", entries[1].url)
        assertEquals(180L, entries[1].durationSeconds)
    }

    @Test
    fun parse_extendedM3uWithAttributes_extractsMetadata() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cctv1" tvg-logo="https://example.com/logo.png" http-user-agent="Mozilla/5.0 Chrome/149" group-title="News Channels",CCTV-1 HD
            http://example.com/live/cctv1.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("CCTV-1 HD", entry.title)
        assertEquals("http://example.com/live/cctv1.m3u8", entry.url)
        assertEquals(-1L, entry.durationSeconds)
        assertEquals("https://example.com/logo.png", entry.logoUrl)
        assertEquals("News Channels", entry.groupTitle)
        assertEquals("Mozilla/5.0 Chrome/149", entry.headers["User-Agent"])
    }

    @Test
    fun parse_withVlcOptHeaders_extractsHttpHeaders() {
        val m3uContent = """
            #EXTM3U
            #EXTVLCOPT:http-user-agent=CustomUserAgent/1.0
            #EXTVLCOPT:http-referrer=https://example.com/referer
            #EXTINF:0,Stream with Custom Headers
            https://example.com/stream.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("Stream with Custom Headers", entry.title)
        assertEquals("CustomUserAgent/1.0", entry.headers["User-Agent"])
        assertEquals("https://example.com/referer", entry.headers["Referer"])
    }

    @Test
    fun parse_relativePaths_resolvesWithBaseUri() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:300,Local Episode 1
            episodes/ep01.mkv
        """.trimIndent()

        val entries = M3uParser.parse(
            inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)),
            baseUri = "/storage/emulated/0/Movies/playlist.m3u"
        )

        assertEquals(1, entries.size)
        assertEquals("/storage/emulated/0/Movies/episodes/ep01.mkv", entries[0].url)
    }

    @Test
    fun parse_utf8Bom_stripsBomSuccessfully() {
        val bom = "\uFEFF"
        val m3uContent = """
            $bom#EXTM3U
            #EXTINF:60,BOM Test
            http://example.com/bom.mp4
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(1, entries.size)
        assertEquals("BOM Test", entries[0].title)
    }

    @Test
    fun parse_missingExtInfTitle_usesUrlFallbackTitle() {
        val m3uContent = """
            #EXTM3U
            http://example.com/path/to/my_video_file.mkv
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(1, entries.size)
        assertEquals("my_video_file.mkv", entries[0].title)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_jsonContent_throwsException() {
        val jsonContent = """
            {
              "title": "My Playlist",
              "channels": [
                { "name": "Channel 1", "url": "http://example.com/stream1" }
              ]
            }
        """.trimIndent()

        M3uParser.parse(ByteArrayInputStream(jsonContent.toByteArray(Charsets.UTF_8)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_htmlContent_throwsException() {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head><title>404 Not Found</title></head>
            <body><h1>404 Page Not Found</h1></body>
            </html>
        """.trimIndent()

        M3uParser.parse(ByteArrayInputStream(htmlContent.toByteArray(Charsets.UTF_8)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_plsContent_throwsException() {
        val plsContent = """
            [playlist]
            NumberOfEntries=1
            File1=http://example.com/stream
            Title1=Sample Stream
        """.trimIndent()

        M3uParser.parse(ByteArrayInputStream(plsContent.toByteArray(Charsets.UTF_8)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun parse_plainTextNoUrls_throwsException() {
        val textContent = """
            This is just a regular text file.
            It contains no URLs or paths.
            Some more random text lines.
        """.trimIndent()

        M3uParser.parse(ByteArrayInputStream(textContent.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun parse_extGrpTag_extractsGroupTitle() {
        val m3uContent = """
            #EXTM3U
            #EXTGRP:Sports
            #EXTINF:0,ESPN Live
            http://example.com/espn.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(1, entries.size)
        assertEquals("Sports", entries[0].groupTitle)
    }

    @Test
    fun parse_kodiProp_extractsHeaders() {
        val m3uContent = """
            #EXTM3U
            #KODIPROP:inputstream.adaptive.license_type=widevine
            #KODIPROP:http-user-agent=KodiUserAgent/20.0
            #KODIPROP:http-referrer=https://kodi.tv
            #EXTINF:-1,Kodi Stream
            http://example.com/kodi.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(1, entries.size)
        assertEquals("KodiUserAgent/20.0", entries[0].headers["User-Agent"])
        assertEquals("https://kodi.tv", entries[0].headers["Referer"])
    }

    @Test
    fun parse_streamInf_extractsVariantTitle() {
        val m3uContent = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080,CODECS="avc1.64002a,mp4a.40.2"
            http://example.com/hls/1080p.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(1, entries.size)
        assertEquals("Stream (1920x1080 - 5.0 Mbps - avc1.64002a,mp4a.40.2)", entries[0].title)
        assertEquals("http://example.com/hls/1080p.m3u8", entries[0].url)
    }

    @Test
    fun parse_unquotedAndQuotedAttributes_handlesCommasAndQuotes() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id=cctv1 group-title="News, Global" http-user-agent='CustomAgent/1.0',CCTV 1
            http://example.com/cctv1.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)))

        assertEquals(1, entries.size)
        assertEquals("CCTV 1", entries[0].title)
        assertEquals("News, Global", entries[0].groupTitle)
        assertEquals("CustomAgent/1.0", entries[0].headers["User-Agent"])
    }

    @Test
    fun parse_customTagHandler_handlesCustomTags() {
        val m3uContent = """
            #EXTM3U
            #MYCUSTOMTAG:custom_logo=http://example.com/custom.png
            #EXTINF:-1,Custom Stream
            http://example.com/custom.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(
            inputStream = ByteArrayInputStream(m3uContent.toByteArray(Charsets.UTF_8)),
            customTagHandler = { line, state ->
                if (line.startsWith("#MYCUSTOMTAG:")) {
                    state.pendingLogo = line.substringAfter("custom_logo=")
                    true
                } else {
                    false
                }
            }
        )

        assertEquals(1, entries.size)
        assertEquals("http://example.com/custom.png", entries[0].logoUrl)
    }
}
