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

package com.fiepi.media.app.ui.screen.settings.pages.audio

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.LanguageSelectionListPage
import com.fiepi.media.app.ui.screen.settings.components.PreferredLanguageListPage
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.domain.model.assets.Language
import com.fiepi.media.domain.model.preferences.AudioOptions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AudioSubPage(
    contentPadding: PaddingValues,
    type: String,
    onNavigateToSubPage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AudioPrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.audioOptions.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    AudioSubPageContent(
        options = options,
        languages = languages,
        searchQuery = searchQuery,
        type = type,
        onIntent = viewModel::onIntent,
        onNavigateToSubPage = onNavigateToSubPage,
        onBack = onBack,
        contentPadding = contentPadding,
        scrollState = scrollState
    )
}

@Composable
fun AudioSubPageContent(
    options: AudioOptions,
    languages: List<Language>,
    searchQuery: String,
    type: String,
    onIntent: (AudioPrefsIntent) -> Unit,
    onNavigateToSubPage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    when (type) {
        SettingsSubPage.Audio.PREFERRED_LANGUAGE -> PreferredLanguageListPage(
            contentPadding = contentPadding,
            currentLanguageIds = options.preferredAudioLanguages,
            allLanguages = languages,
            onLanguagesChanged = { onIntent(AudioPrefsIntent.UpdatePreferredAudioLanguages(it)) },
            onNavigateToSelect = { onNavigateToSubPage(SettingsSubPage.Audio.SELECT_LANGUAGE) },
            titleRes = R.string.settings_audio_preferred_language,
            scrollState = scrollState
        )

        SettingsSubPage.Audio.SELECT_LANGUAGE -> LanguageSelectionListPage(
            contentPadding = contentPadding,
            currentLanguageIds = options.preferredAudioLanguages,
            allLanguages = languages,
            searchQuery = searchQuery,
            onLanguageSelected = { id ->
                onIntent(AudioPrefsIntent.UpdatePreferredAudioLanguages(options.preferredAudioLanguages + id))
                onIntent(AudioPrefsIntent.SetSearchQuery(""))
                onBack()
            },
            scrollState = scrollState
        )

        SettingsSubPage.Audio.AUDIO_OUTPUT_ORDER -> AudioOutputOrderPage(
            options = options,
            onIntent = onIntent,
            contentPadding = contentPadding,
            scrollState = scrollState
        )
    }
}
