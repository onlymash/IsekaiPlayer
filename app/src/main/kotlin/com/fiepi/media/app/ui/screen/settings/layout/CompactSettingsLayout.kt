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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.ui.saver.settingsNavKeyListSaver
import com.fiepi.media.app.ui.screen.settings.components.SettingsScaffold
import com.fiepi.media.app.ui.screen.settings.model.SettingsCategory
import com.fiepi.media.app.ui.transition.AppTransition
import com.fiepi.media.app.ui.transition.AppTransitionType


@Composable
fun CompactSettingsLayout(
    onNavigateUp: () -> Unit
) {
    val backStack = rememberSaveable(saver = settingsNavKeyListSaver) {
        mutableStateListOf(SettingsNavKey.CategoryList)
    }

    fun onBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = ::onBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<SettingsNavKey.CategoryList>(
                metadata = NavDisplay.transitionSpec {
                    AppTransition.enter(AppTransitionType.Fade(keepBackground = true))
                        .invoke(this)
                } + NavDisplay.popTransitionSpec {
                    AppTransition.pop(AppTransitionType.Fade())
                        .invoke(this)
                } + NavDisplay.predictivePopTransitionSpec { edge ->
                    AppTransition.predictive(AppTransitionType.Fade())
                        .invoke(this, edge)
                }
            ) {
                SettingsScaffold(
                    modifier = Modifier.fillMaxSize(),
                    title = stringResource(R.string.common_settings),
                    onBackClick = onNavigateUp,
                    showSearch = true,
                    onNavigateToKey = { key ->
                        backStack.add(key)
                    }
                ) { contentPadding ->
                    PreferencesCategoryList(
                        contentPadding = contentPadding,
                        onCategoryClick = { category ->
                            backStack.add(SettingsNavKey.Category(category.ordinal))
                        }
                    )
                }
            }

            registerCommonSettingsEntries(
                onBack = ::onBack,
                onNavigateToKey = { key -> backStack.add(key) },
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
                }
            )
        },
        transitionSpec = AppTransition.globalEnter(),
        popTransitionSpec = AppTransition.globalPop(),
        predictivePopTransitionSpec = AppTransition.globalPredictive()
    )
}
