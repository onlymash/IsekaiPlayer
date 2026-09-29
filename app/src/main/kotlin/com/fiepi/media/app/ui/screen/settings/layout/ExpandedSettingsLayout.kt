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

import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.SplitPaneLayout
import com.fiepi.media.app.ui.components.rememberSplitPaneState
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.ui.saver.settingsNavKeyListSaver
import com.fiepi.media.app.ui.screen.settings.components.SettingsScaffold
import com.fiepi.media.app.ui.screen.settings.model.SettingsCategory
import com.fiepi.media.app.ui.transition.AppTransition
import com.fiepi.media.app.ui.transition.AppTransitionType


@Composable
fun ExpandedSettingsLayout(
    onNavigateUp: () -> Unit
) {
    val backStack = rememberSaveable(saver = settingsNavKeyListSaver) {
        mutableStateListOf(SettingsNavKey.Category(SettingsCategory.Appearance.ordinal))
    }

    val selectedCategory = (backStack.firstOrNull() as? SettingsNavKey.Category)?.let {
        SettingsCategory.entries[it.category]
    } ?: SettingsCategory.Appearance

    fun onBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    val splitPaneState = rememberSplitPaneState(
        initialLeftWidth = 400.dp,
        minLeftWidth = 320.dp,
        minRightWidth = 400.dp
    )

    SplitPaneLayout(
        modifier = Modifier.fillMaxSize(),
        state = splitPaneState,
        leftContent = {
            SettingsScaffold(
                modifier = Modifier.fillMaxSize(),
                title = stringResource(R.string.common_settings),
                onBackClick = onNavigateUp,
                showSearch = true,
                onNavigateToKey = { key ->
                    if (key is SettingsNavKey.Category) {
                        backStack.clear()
                        backStack.add(key)
                    } else {
                        // If it's a sub-page, switch to corresponding category first then open sub-page
                        val category = when (key) {
                            is SettingsNavKey.PlayerSubPage -> SettingsCategory.Player
                            is SettingsNavKey.AudioSubPage -> SettingsCategory.Audio
                            is SettingsNavKey.SubtitleSubPage -> SettingsCategory.Subtitle
                            is SettingsNavKey.DecoderSubPage -> SettingsCategory.Decoder
                            is SettingsNavKey.AdvancedSubPage -> SettingsCategory.Advanced
                            is SettingsNavKey.AboutSubPage -> SettingsCategory.About
                            else -> null
                        }
                        if (category != null) {
                            backStack.clear()
                            backStack.add(SettingsNavKey.Category(category.ordinal))
                            backStack.add(key)
                        }
                    }
                },
                topBarWindowInsets = TopAppBarDefaults.windowInsets.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Left
                ),
                contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
                    WindowInsetsSides.Left + WindowInsetsSides.Vertical
                )
            ) { contentPadding ->
                PreferencesCategoryList(
                    contentPadding = contentPadding,
                    onCategoryClick = { category ->
                        backStack.clear()
                        backStack.add(SettingsNavKey.Category(category.ordinal))
                    },
                    selectedCategory = selectedCategory
                )
            }
        },
        rightContent = {
            NavDisplay(
                backStack = backStack,
                onBack = ::onBack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    registerCommonSettingsEntries(
                        onBack = ::onBack,
                        onNavigateToKey = { key ->
                            backStack.add(key)
                        },
                        onNavigateToSubPage = { category, subPageType ->
                            when (category) {
                                SettingsCategory.Player -> backStack.add(
                                    SettingsNavKey.PlayerSubPage(
                                        subPageType
                                    )
                                )

                                SettingsCategory.Audio -> backStack.add(
                                    SettingsNavKey.AudioSubPage(
                                        subPageType
                                    )
                                )

                                SettingsCategory.Subtitle -> backStack.add(
                                    SettingsNavKey.SubtitleSubPage(
                                        subPageType
                                    )
                                )

                                SettingsCategory.Decoder -> backStack.add(
                                    SettingsNavKey.DecoderSubPage(
                                        subPageType
                                    )
                                )

                                SettingsCategory.Advanced -> backStack.add(
                                    SettingsNavKey.AdvancedSubPage(
                                        subPageType
                                    )
                                )

                                SettingsCategory.About -> backStack.add(
                                    SettingsNavKey.AboutSubPage(
                                        subPageType
                                    )
                                )

                                else -> {}
                            }
                        },
                        showCategoryBackIcon = false,
                        onCategoryBack = {}, // Root category in tablet mode does not need back logic
                        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(
                            WindowInsetsSides.Right + WindowInsetsSides.Vertical
                        ),
                        topBarWindowInsets = TopAppBarDefaults.windowInsets.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Right
                        )
                    )
                },
                transitionSpec = AppTransition.globalEnter(AppTransitionType.Fade()),
                popTransitionSpec = AppTransition.globalPop(AppTransitionType.Fade()),
                predictivePopTransitionSpec = AppTransition.globalPredictive(AppTransitionType.Fade())
            )
        }
    )
}
