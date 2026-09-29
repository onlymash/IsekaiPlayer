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

package com.fiepi.media.data.repository.assets

import android.content.Context
import com.fiepi.media.domain.model.assets.Language
import com.fiepi.media.domain.model.assets.LanguageType
import com.fiepi.media.domain.repository.assets.LanguageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class LanguageRepositoryImpl(
    private val context: Context,
    private val json: Json
) : LanguageRepository {

    private val cache = mutableMapOf<LanguageType, List<Language>>()

    override suspend fun getLanguages(type: LanguageType): List<Language> =
        withContext(Dispatchers.IO) {
            cache[type] ?: run {
                val assetName = when (type) {
                    LanguageType.Audio -> "audio_languages.json"
                    LanguageType.Subtitle -> "subtitles_languages.json"
                }
                val jsonString =
                    context.assets.open(assetName).bufferedReader().use { it.readText() }
                val languages = json.decodeFromString<List<Language>>(jsonString)
                cache[type] = languages
                languages
            }
        }

    override suspend fun resolveCodes(type: LanguageType, ids: List<String>): List<String> {
        if (ids.isEmpty()) return emptyList()
        val all = getLanguages(type)
        return ids.flatMap { id ->
            all.find { it.id == id }?.codes ?: listOf(id)
        }
    }
}