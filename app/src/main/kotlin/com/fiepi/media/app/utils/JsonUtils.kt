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

package com.fiepi.media.app.utils

import kotlinx.serialization.json.Json

object JsonUtils {

    val defaultJsonParser: Json
        get() = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    val prettyPrintJsonParser: Json
        get() = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

    inline fun <reified T> decodeFromString(jsonString: String): T {
        return defaultJsonParser.decodeFromString(jsonString)
    }

    inline fun <reified T> encodeToString(value: T): String {
        return defaultJsonParser.encodeToString(value)
    }
}