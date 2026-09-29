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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BlurOn
import androidx.compose.material.icons.twotone.DashboardCustomize
import androidx.compose.material.icons.twotone.FastForward
import androidx.compose.material.icons.twotone.Fullscreen
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.LinearScale
import androidx.compose.material.icons.twotone.PhotoCamera
import androidx.compose.material.icons.twotone.PictureInPicture
import androidx.compose.material.icons.twotone.Repeat
import androidx.compose.material.icons.twotone.ScreenRotation
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material.icons.twotone.Timer
import androidx.compose.material.icons.twotone.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SliderPreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.domain.model.preferences.LoopMode
import com.fiepi.media.domain.model.preferences.PlayerOptions
import com.fiepi.media.domain.model.preferences.PlayerOrientation
import com.fiepi.media.domain.model.preferences.ScreenshotFormat
import com.fiepi.media.domain.model.preferences.SeekBarStyle
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun PlayerPage(
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    viewModel: PlayerPrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.options.collectAsState()

    PlayerPageContent(
        options = options,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        onNavigateToSubPage = onNavigateToSubPage,
        scrollState = scrollState
    )
}

@Composable
fun PlayerPageContent(
    options: PlayerOptions,
    onIntent: (PlayerPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    scrollState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_player_header_ui),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_seek_bar_style),
                            summary = when (options.seekBarStyle) {
                                SeekBarStyle.Wavy -> stringResource(R.string.settings_player_seek_bar_style_wavy)
                                SeekBarStyle.Straight -> stringResource(R.string.settings_player_seek_bar_style_straight)
                            },
                            onClick = { onNavigateToSubPage(SettingsSubPage.Player.SEEK_BAR_STYLE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.LinearScale,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_button_effects),
                            summary = stringResource(R.string.settings_player_button_effects_summary),
                            onClick = { onNavigateToSubPage(SettingsSubPage.Player.BUTTON_EFFECTS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.BlurOn,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_layout),
                            summary = stringResource(R.string.settings_player_layout_summary),
                            onClick = { onNavigateToSubPage(SettingsSubPage.Player.PLAYER_LAYOUT) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.DashboardCustomize,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_orientation),
                            summary = when (options.orientation) {
                                PlayerOrientation.Default -> stringResource(R.string.settings_player_orientation_default)
                                PlayerOrientation.Sensor -> stringResource(R.string.settings_player_orientation_sensor)
                                PlayerOrientation.Landscape -> stringResource(R.string.settings_player_orientation_landscape)
                                PlayerOrientation.Portrait -> stringResource(R.string.settings_player_orientation_portrait)
                                PlayerOrientation.Video -> stringResource(R.string.settings_player_orientation_video)
                            },
                            onClick = { onNavigateToSubPage(SettingsSubPage.Player.ORIENTATION) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.ScreenRotation,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_always_full_screen),
                            summary = stringResource(R.string.settings_player_always_full_screen_summary),
                            checked = options.alwaysFullScreen,
                            onCheckedChange = { onIntent(PlayerPrefsIntent.UpdateAlwaysFullScreen(it)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Fullscreen,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            )
        }

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_category_player),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_loop_mode),
                            summary = when (options.loopMode) {
                                LoopMode.None -> stringResource(R.string.settings_player_loop_mode_none)
                                LoopMode.RepeatCurrent -> stringResource(R.string.settings_player_loop_mode_current)
                                LoopMode.Sequential -> stringResource(R.string.settings_player_loop_mode_sequential)
                                LoopMode.RepeatList -> stringResource(R.string.settings_player_loop_mode_list)
                                LoopMode.Shuffle -> stringResource(R.string.settings_player_loop_mode_shuffle)
                                LoopMode.ShuffleRepeat -> stringResource(R.string.settings_player_loop_mode_shuffle_repeat)
                            },
                            onClick = { onNavigateToSubPage(SettingsSubPage.Player.LOOP_MODE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Repeat,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_resume_playback),
                            summary = stringResource(R.string.settings_player_resume_playback_summary),
                            checked = options.resumePlayback,
                            onCheckedChange = { onIntent(PlayerPrefsIntent.UpdateResumePlayback(it)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.History,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_remember_brightness),
                            summary = stringResource(R.string.settings_player_remember_brightness_summary),
                            checked = options.rememberBrightness,
                            onCheckedChange = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateRememberBrightness(
                                        it
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.WbSunny,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.player_control_playback_speed),
                            summary = stringResource(R.string.settings_player_playback_speed_summary),
                            value = options.playbackSpeed,
                            onValueChange = { onIntent(PlayerPrefsIntent.UpdatePlaybackSpeed(it)) },
                            onReset = { onIntent(PlayerPrefsIntent.UpdatePlaybackSpeed(1.0f)) },
                            valueFormat = "%.2fX",
                            range = 0.1f..2.0f,
                            steps = 189,
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Speed,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            )
        }

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_player_header_controls),
                items = arrayOf(
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_enable_pip),
                            summary = stringResource(R.string.settings_player_enable_pip_summary),
                            checked = options.enablePiP,
                            onCheckedChange = { onIntent(PlayerPrefsIntent.UpdateEnablePiP(it)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.PictureInPicture,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_auto_hide_duration),
                            summary = stringResource(
                                R.string.settings_player_auto_hide_duration_summary,
                                options.autoHideDurationSeconds
                            ),
                            value = options.autoHideDurationSeconds.toFloat(),
                            onValueChange = { onIntent(PlayerPrefsIntent.UpdateAutoHideDuration(it.roundToInt())) },
                            onReset = { onIntent(PlayerPrefsIntent.UpdateAutoHideDuration(5)) },
                            range = 1f..15f,
                            steps = 13,
                            unit = "s",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Timer,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_seek_duration),
                            summary = stringResource(
                                R.string.settings_player_seek_duration_summary,
                                options.seekDurationSeconds
                            ),
                            value = options.seekDurationSeconds.toFloat(),
                            onValueChange = { onIntent(PlayerPrefsIntent.UpdateSeekDuration(it.roundToInt())) },
                            onReset = { onIntent(PlayerPrefsIntent.UpdateSeekDuration(10)) },
                            range = 1f..60f,
                            steps = 58,
                            unit = "s",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FastForward,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_screenshot_format),
                            summary = when (options.screenshotFormat) {
                                ScreenshotFormat.JPG -> stringResource(R.string.settings_player_screenshot_format_jpg)
                                ScreenshotFormat.PNG -> stringResource(R.string.settings_player_screenshot_format_png)
                                ScreenshotFormat.WEBP -> stringResource(R.string.settings_player_screenshot_format_webp)
                            },
                            onClick = { onNavigateToSubPage(SettingsSubPage.Player.SCREENSHOT_FORMAT) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.PhotoCamera,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            )
        }
    }
}
