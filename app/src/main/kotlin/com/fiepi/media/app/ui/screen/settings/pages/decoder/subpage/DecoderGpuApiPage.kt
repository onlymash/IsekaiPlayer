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
fun DecoderGpuApiPage(
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
                title = stringResource(R.string.settings_decoder_gpu_api),
                options = listOf("opengl", "vulkan"),
                currentValue = options.gpuApi,
                onValueChange = { onIntent(DecoderPrefsIntent.UpdateGpuApi(it)) },
                titleProvider = {
                    when (it) {
                        "opengl" -> stringResource(R.string.settings_decoder_gpu_api_opengl)
                        "vulkan" -> stringResource(R.string.settings_decoder_gpu_api_vulkan)
                        else -> it
                    }
                },
                summaryProvider = {
                    when (it) {
                        "opengl" -> stringResource(R.string.settings_decoder_gpu_api_opengl_summary)
                        "vulkan" -> stringResource(R.string.settings_decoder_gpu_api_vulkan_summary)
                        else -> null
                    }
                }
            )
        }
    }

}