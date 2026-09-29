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
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePageContent
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePrefsIntent
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitleSubPageContent
import com.fiepi.media.domain.model.assets.Language
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubtitleDrawerPage(
    settingsKey: SettingsNavKey,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    viewModel: SubtitlePrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.subtitleOptions.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    SubtitleDrawerPageContent(
        settingsKey = settingsKey,
        options = options,
        languages = languages,
        searchQuery = searchQuery,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        onNavigateToSubPage = onNavigateToSubPage,
        scrollState = scrollState,
        onBack = { /* Handled by drawer backstack */ }
    )
}

@Composable
fun SubtitleDrawerPageContent(
    settingsKey: SettingsNavKey,
    options: SubtitleOptions,
    languages: List<Language>,
    searchQuery: String,
    onIntent: (SubtitlePrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    onBack: () -> Unit,
    scrollState: LazyListState = rememberLazyListState()
) {
    when (settingsKey) {
        is SettingsNavKey.Category -> {
            SubtitlePageContent(
                options = options,
                languages = languages,
                onIntent = onIntent,
                contentPadding = contentPadding,
                onNavigateToSubPage = onNavigateToSubPage,
                scrollState = scrollState
            )
        }

        is SettingsNavKey.SubtitleSubPage -> {
            SubtitleSubPageContent(
                options = options,
                languages = languages,
                searchQuery = searchQuery,
                type = settingsKey.subPageType,
                onIntent = onIntent,
                contentPadding = contentPadding,
                onNavigateToSubPage = onNavigateToSubPage,
                scrollState = scrollState,
                onBack = onBack
            )
        }

        else -> {}
    }
}
