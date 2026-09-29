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

package com.fiepi.media.app.ui.screen.settings.pages.advanced

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.screen.settings.pages.advanced.subpage.UserAgentEditPage
import com.fiepi.media.domain.model.preferences.AdvancedOptions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdvancedSubPage(
    contentPadding: PaddingValues,
    type: String,
    onBack: () -> Unit = {},
    viewModel: AdvancedPrefsViewModel = koinViewModel()
) {
    val options by viewModel.advancedOptions.collectAsState()

    AdvancedSubPageContent(
        options = options,
        type = type,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        contentPadding = contentPadding
    )
}

@Composable
fun AdvancedSubPageContent(
    options: AdvancedOptions,
    type: String,
    onIntent: (AdvancedPrefsIntent) -> Unit,
    onBack: () -> Unit = {},
    contentPadding: PaddingValues
) {
    when (type) {
        SettingsSubPage.Advanced.USER_AGENT -> UserAgentEditPage(
            options = options,
            onIntent = onIntent,
            contentPadding = contentPadding,
            onBack = onBack
        )
    }
}
