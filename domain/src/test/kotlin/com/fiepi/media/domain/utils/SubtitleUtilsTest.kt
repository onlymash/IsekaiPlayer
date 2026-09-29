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

class SubtitleUtilsTest {

    @Test
    fun testExtractLanguageCode_validLanguageCodes() {
        assertEquals("zh", SubtitleUtils.extractLanguageCode("Movie.zh.srt"))
        assertEquals("zh-Hans", SubtitleUtils.extractLanguageCode("Movie.zh-Hans.ass"))
        assertEquals("zh_CN", SubtitleUtils.extractLanguageCode("Movie.zh_CN.srt"))
        assertEquals("eng", SubtitleUtils.extractLanguageCode("Movie.eng.vtt"))
        assertEquals("chs", SubtitleUtils.extractLanguageCode("/path/to/Movie.chs.srt"))
        assertEquals(
            "cht",
            SubtitleUtils.extractLanguageCode("http://example.com/Movie.cht.ass?token=123#frag")
        )
    }

    @Test
    fun testExtractLanguageCode_invalidOrMissingLanguageCodes() {
        assertNull(SubtitleUtils.extractLanguageCode("Movie.srt"))
        assertNull(SubtitleUtils.extractLanguageCode("a.srt"))
        assertNull(SubtitleUtils.extractLanguageCode("Movie.aVeryLongLanguageCodeHeader.srt"))
        assertNull(SubtitleUtils.extractLanguageCode(""))
        assertNull(SubtitleUtils.extractLanguageCode(null))
    }

    @Test
    fun testExtensionFunction() {
        assertEquals("eng", "http://server/subs/movie.eng.srt?auth=token".extractSubtitleLanguage())
        assertNull("movie.srt".extractSubtitleLanguage())
        assertEquals("movie", "http://server/subs/movie.eng.srt?auth=token".extractSubtitleName())
    }

    @Test
    fun testParse_singlePass() {
        val info1 = SubtitleUtils.parse("http://example.com/Movie.zh-Hans.ass?token=123#frag")
        assertEquals("Movie", info1.baseName)
        assertEquals("zh-Hans", info1.language)
        assertEquals("ass", info1.extension)

        val info2 = SubtitleUtils.parse("Movie.srt")
        assertEquals("Movie", info2.baseName)
        assertNull(info2.language)
        assertEquals("srt", info2.extension)

        val info3 = "movie.eng.vtt".parseSubtitleInfo()
        assertEquals("movie", info3.baseName)
        assertEquals("eng", info3.language)
        assertEquals("vtt", info3.extension)
    }

    @Test
    fun testIsAssociatedWithVideo() {
        assert(SubtitleUtils.isAssociatedWithVideo("Movie", "Movie"))
        assert(SubtitleUtils.isAssociatedWithVideo("Movie.zh", "Movie"))
        assert(SubtitleUtils.isAssociatedWithVideo("Movie_eng", "Movie"))
        assert(!SubtitleUtils.isAssociatedWithVideo("Movie2", "Movie"))
        assert(!SubtitleUtils.isAssociatedWithVideo("Unrelated", "Movie"))
    }

    @Test
    fun testExtractSubtitleName_removesLanguageAndExtension() {
        assertEquals("Movie", SubtitleUtils.extractSubtitleName("Movie.zh.srt"))
        assertEquals("Movie", SubtitleUtils.extractSubtitleName("Movie.zh-Hans.ass"))
        assertEquals("Movie", SubtitleUtils.extractSubtitleName("Movie.zh_CN.srt"))
        assertEquals("Movie", SubtitleUtils.extractSubtitleName("Movie.eng.vtt"))
        assertEquals("Movie", SubtitleUtils.extractSubtitleName("/path/to/Movie.chs.srt"))
        assertEquals(
            "Movie",
            SubtitleUtils.extractSubtitleName("http://example.com/Movie.cht.ass?token=123#frag")
        )
        assertEquals("Movie", SubtitleUtils.extractSubtitleName("Movie.srt"))
        assertEquals("sub", SubtitleUtils.extractSubtitleName("sub.srt"))
        assertEquals("", SubtitleUtils.extractSubtitleName(""))
        assertEquals("", SubtitleUtils.extractSubtitleName(null))
    }
}
