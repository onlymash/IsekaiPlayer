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

package com.fiepi.media.app.ui.screen.settings.pages.gestures

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.VolumeUp
import androidx.compose.material.icons.twotone.Brightness6
import androidx.compose.material.icons.twotone.DoubleArrow
import androidx.compose.material.icons.twotone.FastForward
import androidx.compose.material.icons.twotone.PlayCircle
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material.icons.twotone.SwapHoriz
import androidx.compose.material.icons.twotone.Swipe
import androidx.compose.material.icons.twotone.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SliderPreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun GesturesPage(
    contentPadding: PaddingValues,
    viewModel: GesturesPrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val uiState by viewModel.uiState.collectAsState()

    GesturesPageContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        scrollState = scrollState
    )
}

@Composable
fun GesturesPageContent(
    uiState: GestureUiState,
    onIntent: (GesturesPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    val options = uiState.gestureOptions

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        state = scrollState
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_gestures_header_drag),
                { shapes ->
                    SwitchPreferencesItem(
                        title = stringResource(R.string.settings_gestures_horizontal_drag),
                        summary = stringResource(R.string.settings_gestures_horizontal_drag_summary),
                        checked = options.horizontalDragEnabled,
                        onCheckedChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        horizontalDragEnabled = it
                                    )
                                )
                            )
                        },
                        shapes = shapes,
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Swipe,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SwitchPreferencesItem(
                        title = stringResource(R.string.settings_gestures_vertical_drag_volume),
                        summary = stringResource(R.string.settings_gestures_vertical_drag_volume_summary),
                        checked = options.verticalDragVolumeEnabled,
                        onCheckedChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        verticalDragVolumeEnabled = it
                                    )
                                )
                            )
                        },
                        shapes = shapes,
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.TwoTone.VolumeUp,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SwitchPreferencesItem(
                        title = stringResource(R.string.settings_gestures_vertical_drag_brightness),
                        summary = stringResource(R.string.settings_gestures_vertical_drag_brightness_summary),
                        checked = options.verticalDragBrightnessEnabled,
                        onCheckedChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        verticalDragBrightnessEnabled = it
                                    )
                                )
                            )
                        },
                        shapes = shapes,
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Brightness6,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SwitchPreferencesItem(
                        title = stringResource(R.string.settings_gestures_swap_volume_brightness),
                        summary = stringResource(R.string.settings_gestures_swap_volume_brightness_summary),
                        checked = options.swapVolumeBrightness,
                        onCheckedChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        swapVolumeBrightness = it
                                    )
                                )
                            )
                        },
                        shapes = shapes,
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.SwapHoriz,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        }
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_gestures_header_tap),
                { shapes ->
                    SwitchPreferencesItem(
                        title = stringResource(R.string.settings_gestures_double_tap_seek),
                        summary = stringResource(R.string.settings_gestures_double_tap_seek_summary),
                        checked = options.doubleTapSeekEnabled,
                        onCheckedChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        doubleTapSeekEnabled = it
                                    )
                                )
                            )
                        },
                        shapes = shapes,
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.DoubleArrow,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SliderPreferencesItem(
                        title = stringResource(R.string.settings_gestures_double_tap_seek_duration),
                        summary = stringResource(
                            R.string.settings_gestures_double_tap_seek_duration_summary,
                            options.doubleTapSeekDurationSeconds
                        ),
                        value = options.doubleTapSeekDurationSeconds.toFloat(),
                        onValueChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        doubleTapSeekDurationSeconds = it.roundToInt()
                                    )
                                )
                            )
                        },
                        range = 1f..60f,
                        steps = 58,
                        unit = "s",
                        enabled = options.doubleTapSeekEnabled,
                        shapes = shapes,
                        onReset = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        doubleTapSeekDurationSeconds = 10
                                    )
                                )
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.FastForward,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SwitchPreferencesItem(
                        title = stringResource(R.string.settings_gestures_double_tap_play_pause),
                        summary = stringResource(R.string.settings_gestures_double_tap_play_pause_summary),
                        checked = options.doubleTapPlayPauseEnabled,
                        onCheckedChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        doubleTapPlayPauseEnabled = it
                                    )
                                )
                            )
                        },
                        shapes = shapes,
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.PlayCircle,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SwitchPreferencesItem(
                        title = stringResource(R.string.settings_gestures_long_press),
                        summary = stringResource(R.string.settings_gestures_long_press_summary),
                        checked = options.longPressEnabled,
                        onCheckedChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        longPressEnabled = it
                                    )
                                )
                            )
                        },
                        shapes = shapes,
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.TouchApp,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SliderPreferencesItem(
                        title = stringResource(R.string.settings_gestures_long_press_speed),
                        summary = stringResource(R.string.settings_gestures_long_press_speed_summary),
                        value = options.longPressSpeed,
                        showTickMarks = true,
                        onValueChange = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        longPressSpeed = it
                                    )
                                )
                            )
                        },
                        range = 0.5f..5.0f,
                        steps = 8,
                        unit = "X",
                        valueFormat = "%.1f",
                        enabled = options.longPressEnabled,
                        shapes = shapes,
                        onReset = {
                            onIntent(
                                GesturesPrefsIntent.UpdateGestureOptions(
                                    options.copy(
                                        longPressSpeed = 3.0f
                                    )
                                )
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Speed,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        }
    }
}
