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

import android.app.Activity

/**
 * Screen refresh rate adaptation utility
 */
@Suppress("unused")
object DisplayUtils {

    /**
     * Sets high refresh rate mode for Activity (typically the system maximum)
     * Suitable for pages requiring smooth UI interaction like MainActivity
     */
    fun setHighRefreshRate(activity: Activity) {
        val window = activity.window
        val display = activity.display ?: return

        // Query all supported modes and find the one with the highest refresh rate
        val modes = display.supportedModes
        val highestMode = modes.maxByOrNull { it.refreshRate } ?: return

        val params = window.attributes
        if (params.preferredDisplayModeId != highestMode.modeId) {
            params.preferredDisplayModeId = highestMode.modeId
            window.attributes = params
        }
    }
}
