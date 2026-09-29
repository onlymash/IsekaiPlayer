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

package com.fiepi.media.app.ui.screen.player.components.page

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.ui.screen.settings.pages.gestures.GestureUiState
import com.fiepi.media.app.ui.screen.settings.pages.gestures.GesturesPageContent
import com.fiepi.media.app.ui.screen.settings.pages.gestures.GesturesPrefsIntent
import com.fiepi.media.app.ui.screen.settings.pages.gestures.GesturesPrefsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GesturesDrawerPage(
    settingsKey: SettingsNavKey,
    contentPadding: PaddingValues,
    viewModel: GesturesPrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val uiState by viewModel.uiState.collectAsState()

    GesturesDrawerPageContent(
        settingsKey = settingsKey,
        uiState = uiState,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        scrollState = scrollState
    )
}

@Composable
fun GesturesDrawerPageContent(
    settingsKey: SettingsNavKey,
    uiState: GestureUiState,
    onIntent: (GesturesPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    when (settingsKey) {
        is SettingsNavKey.Category -> {
            GesturesPageContent(
                uiState = uiState,
                onIntent = onIntent,
                contentPadding = contentPadding,
                scrollState = scrollState
            )
        }

        else -> {}
    }
}
