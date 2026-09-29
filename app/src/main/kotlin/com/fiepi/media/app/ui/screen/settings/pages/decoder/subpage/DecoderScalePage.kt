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


@Composable
fun DecoderScalePage(
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
            DecoderOptionList(
                title = stringResource(R.string.settings_decoder_scale),
                options = listOf(
                    "bilinear",
                    "hermite",
                    "mitchell",
                    "catmull_rom",
                    "spline36",
                    "lanczos",
                    "ewa_lanczos",
                    "ewa_lanczossharp",
                    "ewa_lanczos4sharpest",
                    "oversample"
                ),
                currentValue = options.scale,
                onValueChange = { onIntent(DecoderPrefsIntent.UpdateScale(it)) },
                titleProvider = {
                    when (it) {
                        "bilinear" -> stringResource(R.string.settings_decoder_scale_bilinear)
                        "hermite" -> stringResource(R.string.settings_decoder_scale_hermite)
                        "mitchell" -> stringResource(R.string.settings_decoder_scale_mitchell)
                        "catmull_rom" -> stringResource(R.string.settings_decoder_scale_catmull_rom)
                        "spline36" -> stringResource(R.string.settings_decoder_scale_spline36)
                        "lanczos" -> stringResource(R.string.settings_decoder_scale_lanczos)
                        "ewa_lanczos" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos)
                        "ewa_lanczossharp" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos_sharp)
                        "ewa_lanczos4sharpest" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos_4sharpest)
                        "oversample" -> stringResource(R.string.settings_decoder_scale_oversample)
                        else -> it
                    }
                },
                summaryProvider = {
                    when (it) {
                        "bilinear" -> stringResource(R.string.settings_decoder_scale_bilinear_summary)
                        "hermite" -> stringResource(R.string.settings_decoder_scale_hermite_summary)
                        "mitchell" -> stringResource(R.string.settings_decoder_scale_mitchell_summary)
                        "catmull_rom" -> stringResource(R.string.settings_decoder_scale_catmull_rom_summary)
                        "spline36" -> stringResource(R.string.settings_decoder_scale_spline36_summary)
                        "lanczos" -> stringResource(R.string.settings_decoder_scale_lanczos_summary)
                        "ewa_lanczos" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos_summary)
                        "ewa_lanczossharp" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos_sharp_summary)
                        "ewa_lanczos4sharpest" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos_4sharpest_summary)
                        "oversample" -> stringResource(R.string.settings_decoder_scale_oversample_summary)
                        else -> null
                    }
                }
            )
        }

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_decoder_scale_antiring),
                items = listOf(
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_decoder_scale_antiring),
                            summary = stringResource(R.string.settings_decoder_scale_antiring_summary),
                            value = options.scaleAntiring,
                            onValueChange = { onIntent(DecoderPrefsIntent.UpdateScaleAntiring(it)) },
                            range = 0f..1f,
                            steps = 19,
                            showTickMarks = false,
                            valueFormat = "%.2f",
                            onReset = { onIntent(DecoderPrefsIntent.UpdateScaleAntiring(0f)) }
                        )
                    }
                )
            )
        }
    }
}