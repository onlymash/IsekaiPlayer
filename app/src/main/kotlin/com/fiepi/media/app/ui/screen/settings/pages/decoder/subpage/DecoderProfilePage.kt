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
import com.fiepi.media.app.ui.screen.settings.pages.decoder.DecoderPrefsIntent
import com.fiepi.media.domain.model.preferences.DecoderOptions


@Composable
fun DecoderProfilePage(
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
                title = stringResource(R.string.settings_decoder_profile),
                options = listOf(
                    "fast",
                    "default",
                    "high-quality",
                    "gpu-hq",
                    "low-latency",
                    "sw-fast"
                ),
                currentValue = options.profile,
                onValueChange = { onIntent(DecoderPrefsIntent.UpdateProfile(it)) },
                titleProvider = {
                    when (it) {
                        "fast" -> stringResource(R.string.settings_decoder_profile_fast)
                        "default" -> stringResource(R.string.settings_decoder_profile_default)
                        "high-quality" -> stringResource(R.string.settings_decoder_profile_high_quality)
                        "gpu-hq" -> stringResource(R.string.settings_decoder_profile_gpu_hq)
                        "low-latency" -> stringResource(R.string.settings_decoder_profile_low_latency)
                        "sw-fast" -> stringResource(R.string.settings_decoder_profile_sw_fast)
                        else -> it
                    }
                },
                summaryProvider = {
                    when (it) {
                        "fast" -> stringResource(R.string.settings_decoder_profile_fast_summary)
                        "default" -> stringResource(R.string.settings_decoder_profile_default_summary)
                        "high-quality" -> stringResource(R.string.settings_decoder_profile_high_quality_summary)
                        "gpu-hq" -> stringResource(R.string.settings_decoder_profile_gpu_hq_summary)
                        "low-latency" -> stringResource(R.string.settings_decoder_profile_low_latency_summary)
                        "sw-fast" -> stringResource(R.string.settings_decoder_profile_sw_fast_summary)
                        else -> null
                    }
                }
            )
        }
    }

}