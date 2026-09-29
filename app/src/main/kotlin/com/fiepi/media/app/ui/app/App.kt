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

package com.fiepi.media.app.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fiepi.media.app.ui.components.AppHapticsProvider
import com.fiepi.media.app.ui.navigation.AppNavDisplay
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.ThemeOptions
import com.fiepi.media.domain.usecase.preferences.GetThemeOptionsUseCase
import org.koin.compose.koinInject


@Composable
fun App(
    getThemeOptions: GetThemeOptionsUseCase = koinInject()
) {
    val themeOptions by getThemeOptions().collectAsState(initial = ThemeOptions())

    AppHapticsProvider {
        AppTheme(themeOptions = themeOptions) {
            AppNavDisplay()
        }
    }
}