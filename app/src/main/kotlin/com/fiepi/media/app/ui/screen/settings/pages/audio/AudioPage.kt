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

package com.fiepi.media.app.ui.screen.settings.pages.audio

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.QueueMusic
import androidx.compose.material.icons.twotone.AvTimer
import androidx.compose.material.icons.twotone.Compress
import androidx.compose.material.icons.twotone.GraphicEq
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material.icons.twotone.MusicNote
import androidx.compose.material.icons.twotone.Output
import androidx.compose.material.icons.twotone.Tune
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
import com.fiepi.media.domain.model.assets.Language
import com.fiepi.media.domain.model.preferences.AudioOptions
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun AudioPage(
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    viewModel: AudioPrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.audioOptions.collectAsState()
    val languages by viewModel.languages.collectAsState()

    AudioPageContent(
        options = options,
        languages = languages,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        onNavigateToSubPage = onNavigateToSubPage,
        scrollState = scrollState
    )
}

@Composable
fun AudioPageContent(
    options: AudioOptions,
    languages: List<Language>,
    onIntent: (AudioPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    scrollState: LazyListState = rememberLazyListState()
) {
    val currentLanguageSummary = if (options.preferredAudioLanguages.isEmpty()) {
        stringResource(R.string.settings_lang_preferred_language_none)
    } else {
        options.preferredAudioLanguages.joinToString(", ") { id ->
            languages.find { it.id == id }?.native ?: id
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        state = scrollState
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_audio_header_playback),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_preferred_language),
                            summary = currentLanguageSummary,
                            onClick = { onNavigateToSubPage(SettingsSubPage.Audio.PREFERRED_LANGUAGE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Language,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_request_focus),
                            summary = stringResource(R.string.settings_audio_request_focus_summary),
                            checked = options.requestAudioFocus,
                            onCheckedChange = { onIntent(AudioPrefsIntent.ToggleRequestAudioFocus(it)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.MusicNote,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_enable_background_audio),
                            summary = stringResource(R.string.settings_audio_enable_background_audio_summary),
                            checked = options.enableBackgroundAudio,
                            onCheckedChange = { onIntent(AudioPrefsIntent.ToggleBackgroundAudio(it)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.TwoTone.QueueMusic,
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
                title = stringResource(R.string.settings_audio_header_exoplayer),
                items = arrayOf(
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_exo_volume),
                            summary = stringResource(R.string.settings_audio_exo_volume_summary),
                            value = options.exoVolume.toFloat(),
                            onValueChange = {
                                onIntent(
                                    AudioPrefsIntent.UpdateExoVolume(
                                        it.roundToInt().coerceIn(0, 100)
                                    )
                                )
                            },
                            onReset = { onIntent(AudioPrefsIntent.UpdateExoVolume(100)) },
                            valueFormat = "%.0f%%",
                            range = 0f..100f,
                            steps = 99,
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.GraphicEq,
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
                title = stringResource(R.string.settings_audio_header_mpv),
                items = arrayOf(
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_mpv_volume),
                            summary = stringResource(R.string.settings_audio_mpv_volume_summary),
                            value = options.mpvVolume.toFloat(),
                            onValueChange = {
                                onIntent(
                                    AudioPrefsIntent.UpdateMpvVolume(
                                        it.roundToInt().coerceIn(0, 200)
                                    )
                                )
                            },
                            onReset = { onIntent(AudioPrefsIntent.UpdateMpvVolume(100)) },
                            valueFormat = "%.0f%%",
                            range = 0f..200f,
                            steps = 199,
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.GraphicEq,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_output_order),
                            summary = options.audioOutputBackends.joinToString(", "),
                            onClick = { onNavigateToSubPage(SettingsSubPage.Audio.AUDIO_OUTPUT_ORDER) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Output,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_dynamic_normalize),
                            summary = stringResource(R.string.settings_audio_dynamic_normalize_summary),
                            checked = options.dynamicAudioNormalize,
                            onCheckedChange = {
                                onIntent(
                                    AudioPrefsIntent.UpdateDynamicAudioNormalize(
                                        it
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Compress,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_pitch_correction),
                            summary = stringResource(R.string.settings_audio_pitch_correction_summary),
                            checked = options.audioPitchCorrection,
                            onCheckedChange = {
                                onIntent(
                                    AudioPrefsIntent.UpdateAudioPitchCorrection(
                                        it
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Tune,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_audio_delay),
                            summary = stringResource(R.string.settings_audio_delay_summary),
                            value = options.audioDelay / 1000f,
                            onValueChange = { onIntent(AudioPrefsIntent.UpdateAudioDelay((it * 1000).toLong())) },
                            onReset = { onIntent(AudioPrefsIntent.UpdateAudioDelay(0L)) },
                            valueFormat = "%.2fs",
                            range = -5f..5f,
                            steps = 999, // 10ms per step
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.AvTimer,
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
