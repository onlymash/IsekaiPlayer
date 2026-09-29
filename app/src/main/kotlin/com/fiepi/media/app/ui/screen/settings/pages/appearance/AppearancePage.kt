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

package com.fiepi.media.app.ui.screen.settings.pages.appearance

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Contrast
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SingleChoicePreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.domain.model.preferences.ThemeMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppearancePage(
    contentPadding: PaddingValues,
    viewModel: AppearancePrefsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AppearancePageContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding
    )
}

@Composable
fun AppearancePageContent(
    uiState: AppearanceUiState,
    onIntent: (AppearancePrefsIntent) -> Unit,
    contentPadding: PaddingValues
) {
    val themeOptions = uiState.themeOptions

    val themeModeEntries = listOf(
        R.string.settings_appearance_theme_mode_system,
        R.string.settings_appearance_theme_mode_light,
        R.string.settings_appearance_theme_mode_dark,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_appearance_header_theme),
                items = arrayOf(
                    { shapes ->
                        SingleChoicePreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_appearance_theme_mode),
                            options = ThemeMode.entries,
                            currentValue = themeOptions.themeMode,
                            onValueChange = { onIntent(AppearancePrefsIntent.SetThemeMode(it)) },
                            optionToText = { stringResource(themeModeEntries[it.ordinal]) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Palette,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_appearance_theme_high_contrast),
                            summary = stringResource(R.string.settings_appearance_theme_high_contrast_summary),
                            checked = themeOptions.highContrastDarkThemeEnabled,
                            onCheckedChange = {
                                onIntent(
                                    AppearancePrefsIntent.SetHighContrastEnabled(
                                        it
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Contrast,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                )
            )
        }
    }
}
