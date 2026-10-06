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
object DisplayUtils {

    /**
     * Sets high refresh rate mode for Activity (typically the system maximum)
     * Suitable for pages requiring smooth UI interaction like MainActivity.
     * Safely prefers higher refresh rates without altering physical display resolution.
     */
    fun setHighRefreshRate(activity: Activity) {
        val window = activity.window
        val display = activity.display ?: return

        val currentMode = display.mode ?: return
        val modes = display.supportedModes ?: emptyArray()

        // Query all supported modes and find the highest refresh rate supported by the display
        val maxRefreshRate = modes.maxOfOrNull { it.refreshRate } ?: return

        val params = window.attributes
        var changed = false

        // Set preferred refresh rate without forcing a specific display mode resolution
        if (params.preferredRefreshRate != maxRefreshRate) {
            params.preferredRefreshRate = maxRefreshRate
            changed = true
        }

        // Safely set preferredDisplayModeId ONLY if there is a mode matching the current physical resolution
        // with a strictly higher refresh rate than the active mode, ensuring display resolution is never changed
        val highestModeInCurrentRes = modes
            .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
            .maxByOrNull { it.refreshRate }

        val targetModeId =
            if (highestModeInCurrentRes != null && highestModeInCurrentRes.refreshRate > currentMode.refreshRate) {
                highestModeInCurrentRes.modeId
            } else {
                0 // 0 clears preferred mode override, preserving user and system display resolution
            }

        if (params.preferredDisplayModeId != targetModeId) {
            params.preferredDisplayModeId = targetModeId
            changed = true
        }

        if (changed) {
            window.attributes = params
        }
    }
}
