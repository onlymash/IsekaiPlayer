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

package com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SliderPreferencesItem
import com.fiepi.media.app.ui.screen.settings.pages.decoder.DecoderPrefsIntent
import com.fiepi.media.domain.model.preferences.DecoderOptions
import kotlin.math.roundToInt


@Composable
fun DecoderDebandPage(
    options: DecoderOptions,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    onIntent: (DecoderPrefsIntent) -> Unit
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_decoder_deband),
                items = listOf(
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_decoder_deband_iterations),
                            summary = stringResource(R.string.settings_decoder_deband_iterations_summary),
                            value = options.debandIterations.toFloat(),
                            showTickMarks = true,
                            onValueChange = {
                                onIntent(
                                    DecoderPrefsIntent.UpdateDebandIterations(
                                        it.roundToInt()
                                    )
                                )
                            },
                            range = 0f..16f,
                            steps = 15,
                            onReset = { onIntent(DecoderPrefsIntent.UpdateDebandIterations(1)) }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_decoder_deband_threshold),
                            summary = stringResource(R.string.settings_decoder_deband_threshold_summary),
                            value = options.debandThreshold.toFloat(),
                            onValueChange = {
                                onIntent(
                                    DecoderPrefsIntent.UpdateDebandThreshold(
                                        it.roundToInt()
                                    )
                                )
                            },
                            range = 0f..4096f,
                            steps = 4095,
                            showTickMarks = false,
                            onReset = { onIntent(DecoderPrefsIntent.UpdateDebandThreshold(48)) }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_decoder_deband_range),
                            summary = stringResource(R.string.settings_decoder_deband_range_summary),
                            value = options.debandRange.toFloat(),
                            onValueChange = {
                                onIntent(
                                    DecoderPrefsIntent.UpdateDebandRange(
                                        it.roundToInt()
                                    )
                                )
                            },
                            range = 1f..64f,
                            steps = 62,
                            onReset = { onIntent(DecoderPrefsIntent.UpdateDebandRange(16)) }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_decoder_deband_grain),
                            summary = stringResource(R.string.settings_decoder_deband_grain_summary),
                            value = options.debandGrain.toFloat(),
                            onValueChange = {
                                onIntent(
                                    DecoderPrefsIntent.UpdateDebandGrain(
                                        it.roundToInt()
                                    )
                                )
                            },
                            range = 0f..4096f,
                            steps = 4095,
                            showTickMarks = false,
                            onReset = { onIntent(DecoderPrefsIntent.UpdateDebandGrain(32)) }
                        )
                    }
                )
            )
        }
    }

}