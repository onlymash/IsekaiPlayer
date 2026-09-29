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

package com.fiepi.media.domain.model.preferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode(val value: String) {
    @SerialName("system")
    System("system"),

    @SerialName("light")
    Light("light"),

    @SerialName("dark")
    Dark("dark");

    companion object Companion {
        fun fromValue(value: String): ThemeMode {
            return entries.firstOrNull { it.value == value } ?: System
        }
    }
}

@Serializable
data class ThemeOptions(
    @SerialName("theme_mode")
    val themeMode: ThemeMode = ThemeMode.System,
    @SerialName("high_contrast_dark_theme_enabled")
    val highContrastDarkThemeEnabled: Boolean = false
)
