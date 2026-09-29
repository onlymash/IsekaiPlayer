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

package com.fiepi.media.app.ui.saver

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import com.fiepi.media.app.ui.navigation.AppNavKey
import com.fiepi.media.app.ui.navigation.PlayerDrawerNavKey
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.utils.JsonUtils

val appNavKeyListSaver = Saver<MutableList<AppNavKey>, List<String>>(
    save = { navKeyList ->
        navKeyList.map { navKey -> JsonUtils.encodeToString(navKey) }
    },
    restore = { stringList ->
        val restoredList = stringList.map { jsonString ->
            JsonUtils.decodeFromString<AppNavKey>(jsonString)
        }
        mutableStateListOf(*restoredList.toTypedArray())
    }
)

val playerDrawerNavKeyListSaver = Saver<MutableList<PlayerDrawerNavKey>, List<String>>(
    save = { navKeyList ->
        navKeyList.map { navKey -> JsonUtils.encodeToString(navKey) }
    },
    restore = { stringList ->
        val restoredList = stringList.map { jsonString ->
            JsonUtils.decodeFromString<PlayerDrawerNavKey>(jsonString)
        }
        mutableStateListOf(*restoredList.toTypedArray())
    }
)

val settingsNavKeyListSaver = Saver<MutableList<SettingsNavKey>, List<String>>(
    save = { navKeyList ->
        navKeyList.map { navKey -> JsonUtils.encodeToString(navKey) }
    },
    restore = { stringList ->
        val restoredList = stringList.map { jsonString ->
            JsonUtils.decodeFromString<SettingsNavKey>(jsonString)
        }
        mutableStateListOf(*restoredList.toTypedArray())
    }
)