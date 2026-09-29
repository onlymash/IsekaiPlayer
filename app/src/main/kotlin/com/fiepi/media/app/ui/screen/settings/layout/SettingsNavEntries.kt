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

package com.fiepi.media.app.ui.screen.settings.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.ui.screen.settings.components.SettingsScaffold
import com.fiepi.media.app.ui.screen.settings.model.SettingsCategory
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.screen.settings.pages.about.AboutSubPage
import com.fiepi.media.app.ui.screen.settings.pages.advanced.AdvancedSubPage
import com.fiepi.media.app.ui.screen.settings.pages.audio.AudioPrefsIntent
import com.fiepi.media.app.ui.screen.settings.pages.audio.AudioPrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.audio.AudioSubPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.DecoderSubPage
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerSubPage
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePrefsIntent
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitleSubPage
import org.koin.compose.viewmodel.koinViewModel

/**
 * Provides common navigation entry definitions for settings screens.
 */
fun EntryProviderScope<SettingsNavKey>.registerCommonSettingsEntries(
    onBack: () -> Unit,
    onNavigateToSubPage: (SettingsCategory, String) -> Unit,
    onNavigateToKey: (SettingsNavKey) -> Unit,
    showCategoryBackIcon: Boolean = true,
    onCategoryBack: () -> Unit = onBack,
    contentWindowInsets: WindowInsets? = null,
    topBarWindowInsets: WindowInsets? = null
) {
    entry<SettingsNavKey.Category> { key ->
        val category = SettingsCategory.entries[key.category]
        SettingsScaffold(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(category.titleResId),
            onBackClick = onCategoryBack,
            showBackIcon = showCategoryBackIcon,
            onNavigateToKey = onNavigateToKey,
            topBarWindowInsets = topBarWindowInsets,
            contentWindowInsets = contentWindowInsets
        ) { contentPadding ->
            PreferencesPageContent(
                contentPadding = contentPadding,
                category = category,
                onNavigateToSubPage = { subPageType ->
                    onNavigateToSubPage(category, subPageType)
                }
            )
        }
    }

    entry<SettingsNavKey.PlayerSubPage> { key ->
        val titleRes = SettingsSubPage.Player.getTitleResId(key.subPageType)
        SettingsScaffold(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(titleRes),
            onBackClick = onBack,
            onNavigateToKey = onNavigateToKey,
            topBarWindowInsets = topBarWindowInsets,
            contentWindowInsets = contentWindowInsets
        ) { contentPadding ->
            PlayerSubPage(
                contentPadding = contentPadding,
                type = key.subPageType
            )
        }
    }

    entry<SettingsNavKey.AudioSubPage> { key ->
        val audioViewModel: AudioPrefsViewModel = koinViewModel()
        val searchQuery by audioViewModel.searchQuery.collectAsState()

        val isSearch = key.subPageType == SettingsSubPage.Audio.SELECT_LANGUAGE
        val titleRes = SettingsSubPage.Audio.getTitleResId(key.subPageType)

        SettingsScaffold(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(titleRes),
            onBackClick = onBack,
            showSearch = key.subPageType == SettingsSubPage.Audio.SELECT_LANGUAGE,
            searchQuery = searchQuery,
            onSearchQueryChange = { audioViewModel.onIntent(AudioPrefsIntent.SetSearchQuery(it)) },
            onNavigateToKey = if (isSearch) null else onNavigateToKey,
            topBarWindowInsets = topBarWindowInsets,
            contentWindowInsets = contentWindowInsets
        ) { contentPadding ->
            AudioSubPage(
                contentPadding = contentPadding,
                type = key.subPageType,
                onNavigateToSubPage = { subPageType ->
                    onNavigateToSubPage(SettingsCategory.Audio, subPageType)
                },
                onBack = onBack,
                viewModel = audioViewModel
            )
        }
    }

    entry<SettingsNavKey.SubtitleSubPage> { key ->
        val subtitleViewModel: SubtitlePrefsViewModel = koinViewModel()
        val searchQuery by subtitleViewModel.searchQuery.collectAsState()

        val isSearch = key.subPageType == SettingsSubPage.Subtitle.SELECT_LANGUAGE
        val titleRes = SettingsSubPage.Subtitle.getTitleResId(key.subPageType)

        SettingsScaffold(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(titleRes),
            onBackClick = onBack,
            showSearch = key.subPageType == SettingsSubPage.Subtitle.SELECT_LANGUAGE,
            searchQuery = searchQuery,
            onSearchQueryChange = { subtitleViewModel.onIntent(SubtitlePrefsIntent.SetSearchQuery(it)) },
            onNavigateToKey = if (isSearch) null else onNavigateToKey,
            topBarWindowInsets = topBarWindowInsets,
            contentWindowInsets = contentWindowInsets
        ) { contentPadding ->
            SubtitleSubPage(
                contentPadding = contentPadding,
                type = key.subPageType,
                onNavigateToSubPage = { subPageType ->
                    onNavigateToSubPage(SettingsCategory.Subtitle, subPageType)
                },
                onBack = onBack,
                viewModel = subtitleViewModel
            )
        }
    }

    entry<SettingsNavKey.DecoderSubPage> { key ->
        val titleRes = SettingsSubPage.Decoder.getTitleResId(key.subPageType)
        SettingsScaffold(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(titleRes),
            onBackClick = onBack,
            onNavigateToKey = onNavigateToKey,
            topBarWindowInsets = topBarWindowInsets,
            contentWindowInsets = contentWindowInsets
        ) { contentPadding ->
            DecoderSubPage(
                contentPadding = contentPadding,
                type = key.subPageType
            )
        }
    }

    entry<SettingsNavKey.AdvancedSubPage> { key ->
        val titleRes = SettingsSubPage.Advanced.getTitleResId(key.subPageType)
        SettingsScaffold(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(titleRes),
            onBackClick = onBack,
            onNavigateToKey = onNavigateToKey,
            topBarWindowInsets = topBarWindowInsets,
            contentWindowInsets = contentWindowInsets
        ) { contentPadding ->
            AdvancedSubPage(
                contentPadding = contentPadding,
                type = key.subPageType,
                onBack = onBack
            )
        }
    }

    entry<SettingsNavKey.AboutSubPage> { key ->
        val titleRes = SettingsSubPage.About.getTitleResId(key.subPageType)
        SettingsScaffold(
            modifier = Modifier.fillMaxSize(),
            title = stringResource(titleRes),
            onBackClick = onBack,
            onNavigateToKey = onNavigateToKey,
            topBarWindowInsets = topBarWindowInsets,
            contentWindowInsets = contentWindowInsets
        ) { contentPadding ->
            AboutSubPage(
                contentPadding = contentPadding,
                type = key.subPageType
            )
        }
    }
}
