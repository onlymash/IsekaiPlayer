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

class FileUtilsTest {

    @Test
    fun testExtractFileName_withPathsAndUrls() {
        assertEquals(
            "movie.mp4",
            FileUtils.extractFileName("http://example.com/videos/movie.mp4?token=123#play")
        )
        assertEquals(
            "movie.mp4",
            FileUtils.extractFileName("/storage/emulated/0/Download/movie.mp4")
        )
        assertEquals("movie.mp4", FileUtils.extractFileName("C:\\Users\\Media\\movie.mp4"))
        assertEquals("movie.mp4", FileUtils.extractFileName("movie.mp4"))
        assertEquals("", FileUtils.extractFileName(""))
        assertEquals("", FileUtils.extractFileName(null))
    }

    @Test
    fun testExtractExtension_withVariousFormats() {
        assertEquals("mp4", FileUtils.extractExtension("movie.mp4"))
        assertEquals("mkv", FileUtils.extractExtension("http://example.com/video.MKV?token=123"))
        assertEquals("gz", FileUtils.extractExtension("archive.tar.gz"))
        assertEquals("srt", FileUtils.extractExtension("/path/to/subtitle.zh.SRT"))
        assertNull(FileUtils.extractExtension("filename_no_extension"))
        assertNull(FileUtils.extractExtension(".gitignore"))
        assertNull(FileUtils.extractExtension(""))
        assertNull(FileUtils.extractExtension(null))
    }

    @Test
    fun testExtensionFunctions() {
        assertEquals("video.mkv", "http://server/video.mkv?auth=1".extractFileName())
        assertEquals("mkv", "http://server/video.mkv?auth=1".extractExtension())
        assertEquals("video", "http://server/video.mkv?auth=1".extractNameWithoutExtension())
    }

    @Test
    fun testExtractNameWithoutExtension() {
        assertEquals(
            "movie",
            FileUtils.extractNameWithoutExtension("/storage/emulated/0/Download/movie.mp4")
        )
        assertEquals(
            "movie.zh",
            FileUtils.extractNameWithoutExtension("http://example.com/videos/movie.zh.srt?token=123#play")
        )
        assertEquals(
            "filename_no_extension",
            FileUtils.extractNameWithoutExtension("filename_no_extension")
        )
        assertEquals("", FileUtils.extractNameWithoutExtension(""))
        assertEquals("", FileUtils.extractNameWithoutExtension(null))
    }

    @Test
    fun testDecodeUrl() {
        assertEquals("My Video (2026)", FileUtils.decodeUrl("My%20Video%20%282026%29"))
        assertEquals("测试视频", FileUtils.decodeUrl("%E6%B5%8B%E8%AF%95%E8%A7%86%E9%A2%91"))
        assertEquals("simple_video", FileUtils.decodeUrl("simple_video"))
        assertEquals("", FileUtils.decodeUrl(""))
        assertEquals("", FileUtils.decodeUrl(null))
        assertEquals("My Video (2026)", "My%20Video%20%282026%29".decodeUrl())
    }
}
