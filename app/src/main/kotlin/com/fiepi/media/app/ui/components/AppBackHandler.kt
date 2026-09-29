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

package com.fiepi.media.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * A universal back handler for the app.
 *
 * It uses [NavigationBackHandler] internally to provide support for predictive back gestures
 * and better integration with the navigation logic.
 *
 * Importantly, it automatically disables itself during Android Studio Previews
 * (using [LocalInspectionMode]) to prevent rendering errors caused by missing
 * system back dispatchers.
 *
 * @param enabled Whether the back handler is currently active.
 * @param onBack The callback to be invoked when the back gesture or button is performed.
 */
@Composable
fun AppBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    // Skip registration in Preview environment to avoid rendering crashes.
    if (LocalInspectionMode.current) return

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = enabled,
        onBackCompleted = onBack
    )
}
