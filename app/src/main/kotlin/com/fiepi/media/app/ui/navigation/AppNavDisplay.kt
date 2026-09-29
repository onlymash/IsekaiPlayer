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

package com.fiepi.media.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.fiepi.media.app.activity.PlayerActivity
import com.fiepi.media.app.ui.saver.appNavKeyListSaver
import com.fiepi.media.app.ui.screen.browser.BrowserScreen
import com.fiepi.media.app.ui.screen.settings.SettingsScreen
import com.fiepi.media.app.ui.screen.source.SourceEditorScreen
import com.fiepi.media.app.ui.screen.source.SourceManagerScreen
import com.fiepi.media.app.ui.screen.stream.NetworkStreamScreen
import com.fiepi.media.app.ui.transition.AppTransition
import com.fiepi.media.domain.usecases.AppUseCases
import org.koin.compose.koinInject


@Composable
fun AppNavDisplay(appUseCases: AppUseCases = koinInject()) {
    val context = LocalContext.current

    LifecycleStartEffect(Unit) {
        appUseCases.connectRemoteSourceUseCase()
        onStopOrDispose {
            appUseCases.disconnectRemoteSourceUseCase()
        }
    }

    val backStack = rememberSaveable(saver = appNavKeyListSaver) {
        mutableStateListOf(AppNavKey.Browser)
    }

    fun onNavigateUp() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    fun onNavigate(key: AppNavKey) {
        if (key is AppNavKey.Player) {
            context.startActivity(PlayerActivity.newIntent(context, key.sourceId, key.videoPath))
        } else if (key is AppNavKey.PlaylistPlayer) {
            context.startActivity(
                PlayerActivity.newPlaylistIntent(
                    context,
                    key.playlistId,
                    key.initialMediaId
                )
            )
        } else {
            backStack.add(key)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = ::onNavigateUp,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<AppNavKey.Browser> {
                BrowserScreen(
                    onNavigate = ::onNavigate,
                )
            }
            entry<AppNavKey.Settings> {
                SettingsScreen(
                    onNavigateUp = ::onNavigateUp
                )
            }
            entry<AppNavKey.SourceEditor> { key ->
                SourceEditorScreen(
                    sourceId = key.id,
                    onNavigateUp = ::onNavigateUp
                )
            }
            entry<AppNavKey.SourceManager> {
                SourceManagerScreen(
                    onNavigateToEditor = { id -> onNavigate(AppNavKey.SourceEditor(id)) },
                    onNavigateUp = ::onNavigateUp
                )
            }
            entry<AppNavKey.NetworkStream> {
                NetworkStreamScreen(
                    onNavigateUp = ::onNavigateUp
                )
            }
        },
        transitionSpec = AppTransition.globalEnter(),
        popTransitionSpec = AppTransition.globalPop(),
        predictivePopTransitionSpec = AppTransition.globalPredictive()
    )
}