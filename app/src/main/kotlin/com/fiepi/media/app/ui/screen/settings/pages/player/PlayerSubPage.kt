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

package com.fiepi.media.app.ui.screen.settings.pages.player

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.screen.settings.pages.player.subpage.ButtonEffectsPage
import com.fiepi.media.app.ui.screen.settings.pages.player.subpage.LoopModePage
import com.fiepi.media.app.ui.screen.settings.pages.player.subpage.OrientationPage
import com.fiepi.media.app.ui.screen.settings.pages.player.subpage.PlayerLayoutPage
import com.fiepi.media.app.ui.screen.settings.pages.player.subpage.ScreenshotFormatPage
import com.fiepi.media.app.ui.screen.settings.pages.player.subpage.SeekBarPage
import com.fiepi.media.domain.model.preferences.PlayerOptions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayerSubPage(
    type: String,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    viewModel: PlayerPrefsViewModel = koinViewModel()
) {
    val options by viewModel.options.collectAsState()

    PlayerSubPageContent(
        type = type,
        options = options,
        contentPadding = contentPadding,
        scrollState = scrollState,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun PlayerSubPageContent(
    type: String,
    options: PlayerOptions,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    onIntent: (PlayerPrefsIntent) -> Unit,
) {
    when (type) {
        SettingsSubPage.Player.SEEK_BAR_STYLE -> SeekBarPage(
            currentStyle = options.seekBarStyle,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onStyleSelected = { onIntent(PlayerPrefsIntent.UpdateSeekBarStyle(it)) }
        )

        SettingsSubPage.Player.ORIENTATION -> OrientationPage(
            currentOrientation = options.orientation,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onOrientationSelected = { onIntent(PlayerPrefsIntent.UpdateOrientation(it)) }
        )

        SettingsSubPage.Player.LOOP_MODE -> LoopModePage(
            currentMode = options.loopMode,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onLoopModeSelected = { onIntent(PlayerPrefsIntent.UpdateLoopMode(it)) }
        )

        SettingsSubPage.Player.BUTTON_EFFECTS -> ButtonEffectsPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Player.PLAYER_LAYOUT -> PlayerLayoutPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Player.SCREENSHOT_FORMAT -> ScreenshotFormatPage(
            currentFormat = options.screenshotFormat,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onFormatSelected = { onIntent(PlayerPrefsIntent.UpdateScreenshotFormat(it)) }
        )
    }
}
