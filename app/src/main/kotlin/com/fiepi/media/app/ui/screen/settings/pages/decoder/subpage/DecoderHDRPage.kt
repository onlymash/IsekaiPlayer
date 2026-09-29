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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.HdrOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SliderPreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.app.ui.screen.settings.pages.decoder.DecoderPrefsIntent
import com.fiepi.media.domain.model.preferences.DecoderOptions


@Composable
fun DecoderHDRPage(
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
                title = stringResource(R.string.settings_decoder_hdr_passthrough),
                items = listOf(
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_decoder_hdr_passthrough),
                            summary = stringResource(R.string.settings_decoder_hdr_passthrough_summary),
                            checked = options.hdrPassthrough,
                            onCheckedChange = { onIntent(DecoderPrefsIntent.UpdateHdrPassthrough(it)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.HdrOn,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            )
        }

        item {
            DecoderOptionList(
                title = stringResource(R.string.settings_decoder_tone_mapping),
                options = listOf("auto", "bt.2446a", "spline", "reinhard"),
                currentValue = options.toneMapping,
                enabled = !options.hdrPassthrough,
                onValueChange = { onIntent(DecoderPrefsIntent.UpdateToneMapping(it)) },
                titleProvider = {
                    when (it) {
                        "auto" -> stringResource(R.string.settings_decoder_tone_mapping_auto)
                        "bt.2446a" -> stringResource(R.string.settings_decoder_tone_mapping_bt2446a)
                        "spline" -> stringResource(R.string.settings_decoder_tone_mapping_spline)
                        "reinhard" -> stringResource(R.string.settings_decoder_tone_mapping_reinhard)
                        else -> it.replaceFirstChar { char -> char.uppercase() }
                    }
                },
                summaryProvider = {
                    when (it) {
                        "auto" -> stringResource(R.string.settings_decoder_tone_mapping_auto_summary)
                        "bt.2446a" -> stringResource(R.string.settings_decoder_tone_mapping_bt2446a_summary)
                        "spline" -> stringResource(R.string.settings_decoder_tone_mapping_spline_summary)
                        "reinhard" -> stringResource(R.string.settings_decoder_tone_mapping_reinhard_summary)
                        else -> null
                    }
                }
            )
        }

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_decoder_tone_mapping_max_boost),
                items = listOf(
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            enabled = !options.hdrPassthrough,
                            title = stringResource(R.string.settings_decoder_tone_mapping_max_boost),
                            summary = stringResource(R.string.settings_decoder_tone_mapping_max_boost_summary),
                            value = options.toneMappingMaxBoost,
                            onValueChange = {
                                onIntent(
                                    DecoderPrefsIntent.UpdateToneMappingMaxBoost(
                                        it
                                    )
                                )
                            },
                            range = 1.0f..3.0f,
                            steps = 19,
                            showTickMarks = false,
                            valueFormat = "%.2f",
                            onReset = { onIntent(DecoderPrefsIntent.UpdateToneMappingMaxBoost(1.5f)) }
                        )
                    }
                )
            )
        }

        item {
            DecoderOptionList(
                title = stringResource(R.string.settings_decoder_gamut_mapping_mode),
                options = listOf("perceptual", "clip", "relative", "saturation", "desaturate"),
                currentValue = options.gamutMappingMode,
                enabled = !options.hdrPassthrough,
                onValueChange = { onIntent(DecoderPrefsIntent.UpdateGamutMappingMode(it)) },
                titleProvider = {
                    when (it) {
                        "perceptual" -> stringResource(R.string.settings_decoder_gamut_mapping_perceptual)
                        "clip" -> stringResource(R.string.settings_decoder_gamut_mapping_clip)
                        "relative" -> stringResource(R.string.settings_decoder_gamut_mapping_relative)
                        "saturation" -> stringResource(R.string.settings_decoder_gamut_mapping_saturation)
                        "desaturate" -> stringResource(R.string.settings_decoder_gamut_mapping_desaturate)
                        else -> it.replaceFirstChar { char -> char.uppercase() }
                    }
                },
                summaryProvider = {
                    when (it) {
                        "perceptual" -> stringResource(R.string.settings_decoder_gamut_mapping_perceptual_summary)
                        "clip" -> stringResource(R.string.settings_decoder_gamut_mapping_clip_summary)
                        "relative" -> stringResource(R.string.settings_decoder_gamut_mapping_relative_summary)
                        "saturation" -> stringResource(R.string.settings_decoder_gamut_mapping_saturation_summary)
                        "desaturate" -> stringResource(R.string.settings_decoder_gamut_mapping_desaturate_summary)
                        else -> null
                    }
                }
            )
        }
    }
}