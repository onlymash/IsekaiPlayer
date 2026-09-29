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

package com.fiepi.media.app.ui.screen.settings.pages.decoder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderCodecInfoPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderCscalePage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderDebandPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderDscalePage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderEngineTypePage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderFramedropPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderGpuApiPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderHDRPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderHwdecPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderProfilePage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderScalePage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderVideoSyncPage
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderVoPage
import com.fiepi.media.domain.model.preferences.DecoderOptions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DecoderSubPage(
    contentPadding: PaddingValues,
    type: String,
    viewModel: DecoderPrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.decoderOptions.collectAsState()

    DecoderSubPageContent(
        options = options,
        type = type,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        scrollState = scrollState
    )
}

@Composable
fun DecoderSubPageContent(
    options: DecoderOptions,
    type: String,
    onIntent: (DecoderPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    when (type) {
        SettingsSubPage.Decoder.ENGINE_TYPE -> DecoderEngineTypePage(
            currentEngineType = options.engineType,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onEngineTypeSelected = { onIntent(DecoderPrefsIntent.UpdateEngineType(it)) }
        )

        SettingsSubPage.Decoder.PROFILE -> DecoderProfilePage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.VO -> DecoderVoPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.GPU_API -> DecoderGpuApiPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.HWDEC -> DecoderHwdecPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.SCALE -> DecoderScalePage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.DSCALE -> DecoderDscalePage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.CSCALE -> DecoderCscalePage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.HDR -> DecoderHDRPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.FRAMEDROP -> DecoderFramedropPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.VIDEO_SYNC -> DecoderVideoSyncPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )

        SettingsSubPage.Decoder.CODEC_INFO -> DecoderCodecInfoPage(
            contentPadding = contentPadding,
            scrollState = scrollState
        )

        SettingsSubPage.Decoder.DEBAND -> DecoderDebandPage(
            options = options,
            contentPadding = contentPadding,
            scrollState = scrollState,
            onIntent = onIntent
        )
    }
}
