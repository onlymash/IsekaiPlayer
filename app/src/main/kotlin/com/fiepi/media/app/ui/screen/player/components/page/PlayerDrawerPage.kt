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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.ui.screen.settings.model.SettingsCategory
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerPageContent
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerPrefsIntent
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerPrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerSubPageContent
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.PlayerOptions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayerDrawerPage(
    settingsKey: SettingsNavKey,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    viewModel: PlayerPrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.options.collectAsState()

    PlayerDrawerPageContent(
        settingsKey = settingsKey,
        options = options,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        onNavigateToSubPage = onNavigateToSubPage,
        scrollState = scrollState,
    )
}

@Composable
fun PlayerDrawerPageContent(
    settingsKey: SettingsNavKey,
    options: PlayerOptions,
    onIntent: (PlayerPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    scrollState: LazyListState = rememberLazyListState(),
) {

    when (settingsKey) {
        is SettingsNavKey.Category -> {
            PlayerPageContent(
                options = options,
                onIntent = onIntent,
                contentPadding = contentPadding,
                onNavigateToSubPage = onNavigateToSubPage,
                scrollState = scrollState
            )
        }

        is SettingsNavKey.PlayerSubPage -> {
            PlayerSubPageContent(
                options = options,
                type = settingsKey.subPageType,
                onIntent = onIntent,
                contentPadding = contentPadding,
                scrollState = scrollState
            )
        }

        else -> {}
    }
}

@Preview(showBackground = true, name = "Player Page")
@Composable
fun PlayerDrawerPagePreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f))
                .width(400.dp)
                .fillMaxHeight()
        ) {
            PlayerDrawerPageContent(
                settingsKey = SettingsNavKey.Category(SettingsCategory.Player.ordinal),
                options = PlayerOptions(),
                onIntent = {},
                contentPadding = PaddingValues(16.dp),
                onNavigateToSubPage = {}
            )
        }
    }
}
