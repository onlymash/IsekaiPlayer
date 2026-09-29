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

package com.fiepi.media.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MimeTypeUtilsTest {

    @Test
    fun testGetMimeType_forVideoFormats() {
        assertEquals("video/mp4", MimeTypeUtils.getMimeType("mp4"))
        assertEquals("video/mp4", MimeTypeUtils.getMimeType("movie.mp4"))
        assertEquals(
            "video/x-matroska",
            MimeTypeUtils.getMimeType("http://server/video.mkv?token=123")
        )
        assertEquals("video/webm", MimeTypeUtils.getMimeType("webm"))
        assertEquals("video/x-msvideo", MimeTypeUtils.getMimeType("avi"))
        assertEquals("video/quicktime", MimeTypeUtils.getMimeType("mov"))
    }

    @Test
    fun testGetMimeType_forAudioFormats() {
        assertEquals("audio/mpeg", MimeTypeUtils.getMimeType("mp3"))
        assertEquals("audio/flac", MimeTypeUtils.getMimeType("audio.flac"))
        assertEquals("audio/aac", MimeTypeUtils.getMimeType("aac"))
        assertEquals("audio/ogg", MimeTypeUtils.getMimeType("ogg"))
    }

    @Test
    fun testGetMimeType_forSubtitleFormats() {
        assertEquals("application/x-subrip", MimeTypeUtils.getMimeType("movie.zh.srt"))
        assertEquals("text/vtt", MimeTypeUtils.getMimeType("vtt"))
        assertEquals("text/x-ssa", MimeTypeUtils.getMimeType("ass"))
        assertEquals("text/x-ssa", MimeTypeUtils.getMimeType("ssa"))
        assertEquals("text/x-lrc", MimeTypeUtils.getMimeType("lrc"))
    }

    @Test
    fun testGetMimeType_forInvalidOrUnknownExtensions() {
        assertNull(MimeTypeUtils.getMimeType("unknown_extension_xyz_123"))
        assertNull(MimeTypeUtils.getMimeType(""))
        assertNull(MimeTypeUtils.getMimeType(null))
    }

    @Test
    fun testExtensionFunction() {
        assertEquals("video/mp4", "movie.mp4".getMimeType())
        assertEquals("application/x-subrip", "sub.srt".getMimeType())
    }
}
