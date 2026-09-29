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

package com.fiepi.media.app.ui.screen.settings

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import com.fiepi.media.app.ui.screen.settings.layout.CompactSettingsLayout
import com.fiepi.media.app.ui.screen.settings.layout.ExpandedSettingsLayout


@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit
) {
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val sizeClass = adaptiveInfo.windowSizeClass
    val isWidthAtLeastExpanded =
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    if (isWidthAtLeastExpanded) {
        ExpandedSettingsLayout(
            onNavigateUp = onNavigateUp
        )
    } else {
        CompactSettingsLayout(
            onNavigateUp = onNavigateUp
        )
    }
}
