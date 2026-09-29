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

package com.fiepi.media.app.ui.screen.player.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.paging.compose.LazyPagingItems
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.app.ui.navigation.PlayerDrawerNavKey
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.ui.saver.playerDrawerNavKeyListSaver
import com.fiepi.media.app.ui.screen.player.components.page.AudioDrawerPage
import com.fiepi.media.app.ui.screen.player.components.page.DecoderDrawerPage
import com.fiepi.media.app.ui.screen.player.components.page.GesturesDrawerPage
import com.fiepi.media.app.ui.screen.player.components.page.OsdDrawerPage
import com.fiepi.media.app.ui.screen.player.components.page.PlayerDrawerPage
import com.fiepi.media.app.ui.screen.player.components.page.PlaylistDrawerPage
import com.fiepi.media.app.ui.screen.player.components.page.SubtitleDrawerPage
import com.fiepi.media.app.ui.screen.player.components.page.TracksDrawerPage
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerDrawerType
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.screen.settings.model.SettingsCategory
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.transition.AppTransition
import com.fiepi.media.app.ui.transition.AppTransitionType
import com.fiepi.media.domain.model.playlist.PlaylistItem

@Composable
fun PlayerDrawer(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    playlistItems: LazyPagingItems<PlaylistItem>? = null
) {
    val layoutDirection = LocalLayoutDirection.current
    val containerWidth = LocalWindowInfo.current.containerDpSize.width

    val insets =
        WindowInsets.displayCutout.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
    val drawerInsets = insets.only(sides = WindowInsetsSides.Start)

    val startPaddingSize = drawerInsets.asPaddingValues().calculateStartPadding(layoutDirection)
    val drawerWidth = min(containerWidth * 0.9f, 420.dp) + startPaddingSize

    val backStack = rememberSaveable(saver = playerDrawerNavKeyListSaver) {
        mutableStateListOf(state.ui.activeDrawer.toNavKey())
    }

    // Sync activeDrawer from ViewModel to the drawer navigation stack root node
    LaunchedEffect(state.ui.activeDrawer) {
        val targetRoot = state.ui.activeDrawer.toNavKey()
        if (backStack.isEmpty() || backStack.first() != targetRoot) {
            backStack.clear()
            backStack.add(targetRoot)
        }
    }

    val isAtRoot by remember {
        derivedStateOf { backStack.size <= 1 }
    }

    // Use remember to ensure callback logic always reads the latest backStack state
    val currentOnClose by rememberUpdatedState(onClose)
    val onBack = remember {
        {
            if (backStack.size <= 1) {
                currentOnClose()
            } else {
                backStack.removeAt(backStack.size - 1)
            }
            Unit
        }
    }

    ModalDrawerSheet(
        modifier = modifier.requiredWidth(drawerWidth),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = drawerInsets
    ) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            onBack = onBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<PlayerDrawerNavKey.Tracks> {
                    PlayerDrawerScaffold(
                        title = stringResource(R.string.player_drawer_tracks_title),
                        isAtRoot = isAtRoot,
                        onBack = onBack
                    ) { contentPadding, scrollState ->
                        TracksDrawerPage(
                            state = state,
                            onIntent = onIntent,
                            onClose = onClose,
                            contentPadding = contentPadding,
                            scrollState = scrollState
                        )
                    }
                }

                entry<PlayerDrawerNavKey.Playlist> {
                    PlayerDrawerScaffold(
                        title = stringResource(R.string.player_drawer_playlist_title),
                        isAtRoot = isAtRoot,
                        onBack = onBack
                    ) { contentPadding, scrollState ->
                        PlaylistDrawerPage(
                            state = state,
                            playlistItems = playlistItems,
                            onIntent = onIntent,
                            contentPadding = contentPadding,
                            scrollState = scrollState
                        )
                    }
                }

                entry<PlayerDrawerNavKey.Settings> { settingsKey ->
                    val key = settingsKey.key
                    PlayerDrawerScaffold(
                        title = key.getTitle(),
                        isAtRoot = isAtRoot,
                        onBack = onBack
                    ) { contentPadding, scrollState ->
                        when {
                            (key is SettingsNavKey.Category && key.category == SettingsCategory.Player.ordinal) || key is SettingsNavKey.PlayerSubPage -> {
                                PlayerDrawerPage(
                                    settingsKey = key,
                                    contentPadding = contentPadding,
                                    onNavigateToSubPage = { subPageType ->
                                        backStack.add(
                                            PlayerDrawerNavKey.Settings(
                                                SettingsNavKey.PlayerSubPage(
                                                    subPageType
                                                )
                                            )
                                        )
                                    },
                                    scrollState = scrollState
                                )
                            }

                            (key is SettingsNavKey.Category && key.category == SettingsCategory.Decoder.ordinal) || key is SettingsNavKey.DecoderSubPage -> {
                                DecoderDrawerPage(
                                    settingsKey = key,
                                    contentPadding = contentPadding,
                                    onNavigateToSubPage = { subPageType ->
                                        backStack.add(
                                            PlayerDrawerNavKey.Settings(
                                                SettingsNavKey.DecoderSubPage(
                                                    subPageType
                                                )
                                            )
                                        )
                                    },
                                    scrollState = scrollState
                                )
                            }

                            (key is SettingsNavKey.Category && key.category == SettingsCategory.Audio.ordinal) || key is SettingsNavKey.AudioSubPage -> {
                                AudioDrawerPage(
                                    settingsKey = key,
                                    contentPadding = contentPadding,
                                    onNavigateToSubPage = { subPageType ->
                                        backStack.add(
                                            PlayerDrawerNavKey.Settings(
                                                SettingsNavKey.AudioSubPage(
                                                    subPageType
                                                )
                                            )
                                        )
                                    },
                                    scrollState = scrollState
                                )
                            }

                            (key is SettingsNavKey.Category && key.category == SettingsCategory.Subtitle.ordinal) || key is SettingsNavKey.SubtitleSubPage -> {
                                SubtitleDrawerPage(
                                    settingsKey = key,
                                    contentPadding = contentPadding,
                                    onNavigateToSubPage = { subPageType ->
                                        backStack.add(
                                            PlayerDrawerNavKey.Settings(
                                                SettingsNavKey.SubtitleSubPage(
                                                    subPageType
                                                )
                                            )
                                        )
                                    },
                                    scrollState = scrollState
                                )
                            }

                            key is SettingsNavKey.Category && key.category == SettingsCategory.Gestures.ordinal -> {
                                GesturesDrawerPage(
                                    settingsKey = key,
                                    contentPadding = contentPadding,
                                    scrollState = scrollState
                                )
                            }
                        }
                    }
                }

                entry<PlayerDrawerNavKey.Osd> {
                    PlayerDrawerScaffold(
                        title = stringResource(R.string.player_control_osd),
                        isAtRoot = isAtRoot,
                        onBack = onBack
                    ) { contentPadding, scrollState ->
                        OsdDrawerPage(
                            state = state,
                            onIntent = onIntent,
                            contentPadding = contentPadding,
                            scrollState = scrollState
                        )
                    }
                }
            },
            transitionSpec = AppTransition.globalEnter(AppTransitionType.Fade()),
            popTransitionSpec = AppTransition.globalPop(AppTransitionType.Fade()),
            predictivePopTransitionSpec = AppTransition.globalPredictive(AppTransitionType.Fade())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDrawerScaffold(
    title: String,
    isAtRoot: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.displayCutout.only(WindowInsetsSides.Vertical),
    content: @Composable (contentPadding: PaddingValues, scrollState: LazyListState) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scrollState = rememberLazyListState()
    val isFabVisible by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction < 0.5f }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(),
                contentPadding = contentWindowInsets.only(WindowInsetsSides.Top).asPaddingValues()
            )
        },
        contentWindowInsets = contentWindowInsets,
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier
                    .padding(16.dp)
                    .animateFloatingActionButton(
                        visible = isFabVisible,
                        alignment = Alignment.BottomCenter
                    ),
                onClick = rememberHapticClickHandler(onClick = onBack),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (!isAtRoot) Icons.AutoMirrored.TwoTone.ArrowBack else Icons.TwoTone.Close,
                    contentDescription = stringResource(if (!isAtRoot) R.string.common_back else R.string.common_close)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { scaffoldPadding ->
        content(scaffoldPadding, scrollState)
    }
}

@Composable
private fun SettingsNavKey.getTitle(): String {
    return when (this) {
        is SettingsNavKey.Category -> {
            when (category) {
                SettingsCategory.Player.ordinal -> stringResource(R.string.player_settings_player)
                SettingsCategory.Decoder.ordinal -> stringResource(R.string.player_settings_decoder)
                SettingsCategory.Audio.ordinal -> stringResource(R.string.player_settings_audio)
                SettingsCategory.Subtitle.ordinal -> stringResource(R.string.player_settings_subtitle)
                SettingsCategory.Gestures.ordinal -> stringResource(R.string.player_settings_gestures)
                else -> ""
            }
        }

        is SettingsNavKey.PlayerSubPage -> stringResource(
            SettingsSubPage.Player.getTitleResId(
                subPageType
            )
        )

        is SettingsNavKey.DecoderSubPage -> stringResource(
            SettingsSubPage.Decoder.getTitleResId(
                subPageType
            )
        )

        is SettingsNavKey.AudioSubPage -> stringResource(
            SettingsSubPage.Audio.getTitleResId(
                subPageType
            )
        )

        is SettingsNavKey.SubtitleSubPage -> stringResource(
            SettingsSubPage.Subtitle.getTitleResId(
                subPageType
            )
        )

        else -> ""
    }
}

private fun PlayerDrawerType.toNavKey(): PlayerDrawerNavKey {
    return when (this) {
        PlayerDrawerType.Tracks -> PlayerDrawerNavKey.Tracks
        PlayerDrawerType.Playlist -> PlayerDrawerNavKey.Playlist
        PlayerDrawerType.Osd -> PlayerDrawerNavKey.Osd
        PlayerDrawerType.Decoder -> PlayerDrawerNavKey.Settings(
            SettingsNavKey.Category(
                SettingsCategory.Decoder.ordinal
            )
        )

        PlayerDrawerType.Player -> PlayerDrawerNavKey.Settings(
            SettingsNavKey.Category(
                SettingsCategory.Player.ordinal
            )
        )

        PlayerDrawerType.Audio -> PlayerDrawerNavKey.Settings(
            SettingsNavKey.Category(
                SettingsCategory.Audio.ordinal
            )
        )

        PlayerDrawerType.Subtitle -> PlayerDrawerNavKey.Settings(
            SettingsNavKey.Category(
                SettingsCategory.Subtitle.ordinal
            )
        )

        PlayerDrawerType.Gestures -> PlayerDrawerNavKey.Settings(
            SettingsNavKey.Category(
                SettingsCategory.Gestures.ordinal
            )
        )
    }
}
