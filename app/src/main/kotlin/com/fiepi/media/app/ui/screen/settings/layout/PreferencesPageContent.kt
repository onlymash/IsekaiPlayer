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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.fiepi.media.app.ui.screen.settings.model.SettingsCategory
import com.fiepi.media.app.ui.screen.settings.pages.about.AboutPage
import com.fiepi.media.app.ui.screen.settings.pages.advanced.AdvancedPage
import com.fiepi.media.app.ui.screen.settings.pages.appearance.AppearancePage
import com.fiepi.media.app.ui.screen.settings.pages.audio.AudioPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.DecoderPage
import com.fiepi.media.app.ui.screen.settings.pages.general.GeneralPage
import com.fiepi.media.app.ui.screen.settings.pages.gestures.GesturesPage
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerPage
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePage

@Composable
fun PreferencesPageContent(
    contentPadding: PaddingValues,
    category: SettingsCategory,
    onNavigateToSubPage: (String) -> Unit
) {
    when (category) {
        SettingsCategory.Appearance -> AppearancePage(
            contentPadding = contentPadding
        )

        SettingsCategory.General -> GeneralPage(
            contentPadding = contentPadding
        )

        SettingsCategory.Gestures -> GesturesPage(
            contentPadding = contentPadding
        )

        SettingsCategory.Player -> PlayerPage(
            contentPadding = contentPadding,
            onNavigateToSubPage = onNavigateToSubPage
        )

        SettingsCategory.Decoder -> DecoderPage(
            contentPadding = contentPadding,
            onNavigateToSubPage = onNavigateToSubPage
        )

        SettingsCategory.Audio -> AudioPage(
            contentPadding = contentPadding,
            onNavigateToSubPage = onNavigateToSubPage
        )

        SettingsCategory.Subtitle -> SubtitlePage(
            contentPadding = contentPadding,
            onNavigateToSubPage = onNavigateToSubPage
        )

        SettingsCategory.Advanced -> AdvancedPage(
            contentPadding = contentPadding,
            onNavigateToSubPage = onNavigateToSubPage
        )

        SettingsCategory.About -> AboutPage(
            contentPadding = contentPadding,
            onNavigateToSubPage = onNavigateToSubPage
        )
    }
}
