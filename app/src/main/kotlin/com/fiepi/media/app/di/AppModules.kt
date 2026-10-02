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

package com.fiepi.media.app.di

import com.fiepi.media.app.service.FileOperationManager
import com.fiepi.media.app.service.NotificationController
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserViewModel
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerViewModel
import com.fiepi.media.app.ui.screen.settings.model.SettingsSearchViewModel
import com.fiepi.media.app.ui.screen.settings.pages.about.AboutViewModel
import com.fiepi.media.app.ui.screen.settings.pages.advanced.AdvancedPrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.appearance.AppearancePrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.audio.AudioPrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.decoder.DecoderPrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage.DecoderCodecInfoViewModel
import com.fiepi.media.app.ui.screen.settings.pages.general.GeneralPrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.gestures.GesturesPrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerPrefsViewModel
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePrefsViewModel
import com.fiepi.media.app.ui.screen.source.viewmodel.SourceEditorViewModel
import com.fiepi.media.app.ui.screen.source.viewmodel.SourceManagerViewModel
import com.fiepi.media.app.ui.screen.stream.viewmodel.NetworkStreamViewModel
import com.fiepi.media.app.ui.screen.task.viewmodel.FileTaskViewModel
import com.fiepi.media.data.di.dataModule
import com.fiepi.media.domain.di.domainModule
import com.fiepi.media.player.di.playerModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

private val appModule = module {
    singleOf(::NotificationController)
    singleOf(::FileOperationManager)

    viewModelOf(::AppearancePrefsViewModel)
    viewModel { AboutViewModel(androidContext(), get()) }
    viewModelOf(::AudioPrefsViewModel)
    viewModelOf(::SubtitlePrefsViewModel)
    viewModelOf(::DecoderPrefsViewModel)
    viewModelOf(::DecoderCodecInfoViewModel)
    viewModelOf(::GeneralPrefsViewModel)
    viewModelOf(::GesturesPrefsViewModel)
    viewModelOf(::PlayerPrefsViewModel)
    viewModelOf(::AdvancedPrefsViewModel)
    viewModel { SettingsSearchViewModel(androidContext()) }

    viewModelOf(::BrowserViewModel)
    viewModelOf(::FileTaskViewModel)
    viewModelOf(::SourceManagerViewModel)
    viewModelOf(::NetworkStreamViewModel)
    viewModel { params ->
        SourceEditorViewModel(
            sourceId = params.getOrNull(),
            useCases = get(),
        )
    }
    viewModelOf(::PlayerViewModel)
}

val appModules: List<Module>
    get() = listOf(
        domainModule,
        dataModule,
        playerModule,
        appModule,
    )
