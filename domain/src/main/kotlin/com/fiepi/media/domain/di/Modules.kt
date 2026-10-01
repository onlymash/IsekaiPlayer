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

package com.fiepi.media.domain.di

import com.fiepi.media.domain.usecase.assets.GetLanguageCodesUseCase
import com.fiepi.media.domain.usecase.assets.GetLanguagesUseCase
import com.fiepi.media.domain.usecase.backup.ExportSettingsUseCase
import com.fiepi.media.domain.usecase.backup.ImportSettingsUseCase
import com.fiepi.media.domain.usecase.cache.ClearThumbnailCacheUseCase
import com.fiepi.media.domain.usecase.cache.ClearVideoCacheUseCase
import com.fiepi.media.domain.usecase.cache.GetCacheInfoUseCase
import com.fiepi.media.domain.usecase.decoder.GetSystemCodecsUseCase
import com.fiepi.media.domain.usecase.history.ClearAllHistoryUseCase
import com.fiepi.media.domain.usecase.history.DeleteHistoryUseCase
import com.fiepi.media.domain.usecase.history.GetHistoryCountUseCase
import com.fiepi.media.domain.usecase.history.GetHistoryPagingUseCase
import com.fiepi.media.domain.usecase.history.GetHistoryUseCase
import com.fiepi.media.domain.usecase.history.GetLastPlayedInDirectoryUseCase
import com.fiepi.media.domain.usecase.history.SaveHistoryUseCase
import com.fiepi.media.domain.usecase.media.CopyFileUseCase
import com.fiepi.media.domain.usecase.media.CreateDirectoryUseCase
import com.fiepi.media.domain.usecase.media.DeleteFilesUseCase
import com.fiepi.media.domain.usecase.media.FileManagementUseCases
import com.fiepi.media.domain.usecase.media.GetMediaFilesUseCase
import com.fiepi.media.domain.usecase.media.HasCachedMediaUseCase
import com.fiepi.media.domain.usecase.media.MoveFileUseCase
import com.fiepi.media.domain.usecase.media.RenameFileUseCase
import com.fiepi.media.domain.usecase.media.SearchMediaFilesUseCase
import com.fiepi.media.domain.usecase.playlist.AddMediaToPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.CreatePlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.DeletePlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistItemByIndexUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistItemCountUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistItemIndexByMediaIdUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistItemPagingUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistsUseCase
import com.fiepi.media.domain.usecase.playlist.ImportM3uPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.RefreshPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.RemoveMediaFromPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.ReorderPlaylistItemsUseCase
import com.fiepi.media.domain.usecase.playlist.ReorderPlaylistsUseCase
import com.fiepi.media.domain.usecase.playlist.UpdatePlaylistTitleUseCase
import com.fiepi.media.domain.usecase.preferences.GetAdvancedOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetAudioOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetBrightnessUseCase
import com.fiepi.media.domain.usecase.preferences.GetDecoderOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetDisplayFieldsUseCase
import com.fiepi.media.domain.usecase.preferences.GetGestureOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetHapticsEnabledUseCase
import com.fiepi.media.domain.usecase.preferences.GetInterceptBackNavigationUseCase
import com.fiepi.media.domain.usecase.preferences.GetPlaybackOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetPlayerOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetPlayerUiOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetSelectedSourceIdUseCase
import com.fiepi.media.domain.usecase.preferences.GetSortOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetSubtitleOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.GetThemeOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.SaveBrightnessUseCase
import com.fiepi.media.domain.usecase.preferences.SaveSelectedSourceIdUseCase
import com.fiepi.media.domain.usecase.preferences.SetHapticsEnabledUseCase
import com.fiepi.media.domain.usecase.preferences.SetInterceptBackNavigationUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateAdvancedOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateAudioOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateDecoderOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateDisplayFieldsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateGestureOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdatePlayerOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateSortOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateSubtitleOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateThemeOptionsUseCase
import com.fiepi.media.domain.usecase.source.ConnectRemoteSourceUseCase
import com.fiepi.media.domain.usecase.source.DeleteRemoteSourceUseCase
import com.fiepi.media.domain.usecase.source.DisconnectRemoteSourceUseCase
import com.fiepi.media.domain.usecase.source.GetMaxSortOrderUseCase
import com.fiepi.media.domain.usecase.source.GetRemoteSourceUseCase
import com.fiepi.media.domain.usecase.source.GetRemoteSourcesUseCase
import com.fiepi.media.domain.usecase.source.GetSourceUseCase
import com.fiepi.media.domain.usecase.source.GetSourcesUseCase
import com.fiepi.media.domain.usecase.source.SaveRemoteSourceUseCase
import com.fiepi.media.domain.usecase.source.UpdateRemoteSourcesUseCase
import com.fiepi.media.domain.usecase.source.ValidateRemoteSourceUseCase
import com.fiepi.media.domain.usecase.stream.ClearStreamHistoryUseCase
import com.fiepi.media.domain.usecase.stream.DeleteStreamUrlUseCase
import com.fiepi.media.domain.usecase.stream.GetStreamHistoryUseCase
import com.fiepi.media.domain.usecase.stream.SaveStreamUrlUseCase
import com.fiepi.media.domain.usecases.AppUseCases
import com.fiepi.media.domain.usecases.BrowserUseCases
import com.fiepi.media.domain.usecases.MediaPlayerUseCases
import com.fiepi.media.domain.usecases.NetworkStreamUseCases
import com.fiepi.media.domain.usecases.PlayerUseCases
import com.fiepi.media.domain.usecases.SourceEditorUseCases
import com.fiepi.media.domain.usecases.SourceManagerUseCases
import com.fiepi.media.domain.usecases.backup.BackupUseCases
import com.fiepi.media.domain.usecases.cache.CacheUseCases
import com.fiepi.media.domain.usecases.playlist.PlaylistUseCases
import com.fiepi.media.domain.usecases.preferences.AdvancedPrefsUseCases
import com.fiepi.media.domain.usecases.preferences.AppearancePrefsUseCases
import com.fiepi.media.domain.usecases.preferences.AudioPrefsUseCases
import com.fiepi.media.domain.usecases.preferences.DecoderPrefsUseCases
import com.fiepi.media.domain.usecases.preferences.GeneralPrefsUseCases
import com.fiepi.media.domain.usecases.preferences.GesturePrefsUseCases
import com.fiepi.media.domain.usecases.preferences.PlayerPrefsUseCases
import com.fiepi.media.domain.usecases.preferences.SubtitlePrefsUseCases
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Preferences related use cases.
 */
private val useCasePrefsModule = module {
    factoryOf(::GetHapticsEnabledUseCase)
    factoryOf(::SetHapticsEnabledUseCase)
    factoryOf(::GetThemeOptionsUseCase)
    factoryOf(::UpdateThemeOptionsUseCase)
    factoryOf(::GetInterceptBackNavigationUseCase)
    factoryOf(::SetInterceptBackNavigationUseCase)
    factoryOf(::GetSortOptionsUseCase)
    factoryOf(::UpdateSortOptionsUseCase)
    factoryOf(::GetDisplayFieldsUseCase)
    factoryOf(::UpdateDisplayFieldsUseCase)
    factoryOf(::GetBrightnessUseCase)
    factoryOf(::SaveBrightnessUseCase)
    factoryOf(::GetGestureOptionsUseCase)
    factoryOf(::UpdateGestureOptionsUseCase)
    factoryOf(::GetPlayerOptionsUseCase)
    factoryOf(::UpdatePlayerOptionsUseCase)
    factoryOf(::GetAudioOptionsUseCase)
    factoryOf(::UpdateAudioOptionsUseCase)
    factoryOf(::GetSubtitleOptionsUseCase)
    factoryOf(::UpdateSubtitleOptionsUseCase)
    factoryOf(::GetDecoderOptionsUseCase)
    factoryOf(::UpdateDecoderOptionsUseCase)
    factoryOf(::GetSystemCodecsUseCase)
    factoryOf(::GetAdvancedOptionsUseCase)
    factoryOf(::UpdateAdvancedOptionsUseCase)
    factoryOf(::GetPlaybackOptionsUseCase)
    factoryOf(::GetPlayerUiOptionsUseCase)
    factoryOf(::ExportSettingsUseCase)
    factoryOf(::ImportSettingsUseCase)
}

/**
 * Source and Media related use cases.
 */
private val useCaseMediaModule = module {
    // Assets
    factoryOf(::GetLanguagesUseCase)
    factoryOf(::GetLanguageCodesUseCase)

    // Media
    factoryOf(::GetMediaFilesUseCase)
    factoryOf(::HasCachedMediaUseCase)
    factoryOf(::SearchMediaFilesUseCase)
    factoryOf(::RenameFileUseCase)
    factoryOf(::DeleteFilesUseCase)
    factoryOf(::CopyFileUseCase)
    factoryOf(::MoveFileUseCase)
    factoryOf(::CreateDirectoryUseCase)

    // Cache
    factoryOf(::GetCacheInfoUseCase)
    factoryOf(::ClearThumbnailCacheUseCase)
    factoryOf(::ClearVideoCacheUseCase)

    // History
    factoryOf(::GetHistoryUseCase)
    factoryOf(::GetHistoryPagingUseCase)
    factoryOf(::DeleteHistoryUseCase)
    factoryOf(::GetLastPlayedInDirectoryUseCase)
    factoryOf(::SaveHistoryUseCase)
    factoryOf(::ClearAllHistoryUseCase)
    factoryOf(::GetHistoryCountUseCase)

    // Source
    factoryOf(::GetRemoteSourcesUseCase)
    factoryOf(::GetSourcesUseCase)
    factoryOf(::GetSourceUseCase)
    factoryOf(::GetMaxSortOrderUseCase)
    factoryOf(::GetRemoteSourceUseCase)
    factoryOf(::SaveRemoteSourceUseCase)
    factoryOf(::UpdateRemoteSourcesUseCase)
    factoryOf(::DeleteRemoteSourceUseCase)
    factoryOf(::ConnectRemoteSourceUseCase)
    factoryOf(::DisconnectRemoteSourceUseCase)
    factoryOf(::ValidateRemoteSourceUseCase)
    factoryOf(::GetSelectedSourceIdUseCase)
    factoryOf(::SaveSelectedSourceIdUseCase)

    // Playlist
    factoryOf(::GetPlaylistsUseCase)
    factoryOf(::GetPlaylistUseCase)
    factoryOf(::GetPlaylistItemPagingUseCase)
    factoryOf(::GetPlaylistItemByIndexUseCase)
    factoryOf(::GetPlaylistItemCountUseCase)
    factoryOf(::GetPlaylistItemIndexByMediaIdUseCase)
    factoryOf(::CreatePlaylistUseCase)
    factoryOf(::UpdatePlaylistTitleUseCase)
    factoryOf(::DeletePlaylistUseCase)
    factoryOf(::AddMediaToPlaylistUseCase)
    factoryOf(::RemoveMediaFromPlaylistUseCase)
    factoryOf(::ReorderPlaylistItemsUseCase)
    factoryOf(::ReorderPlaylistsUseCase)
    factoryOf(::ImportM3uPlaylistUseCase)
    factoryOf(::RefreshPlaylistUseCase)

    // Stream
    factoryOf(::GetStreamHistoryUseCase)
    factoryOf(::SaveStreamUrlUseCase)
    factoryOf(::DeleteStreamUrlUseCase)
    factoryOf(::ClearStreamHistoryUseCase)
}

/**
 * Wrapper UseCase classes for UI consumption.
 */
private val useCaseAggregateModule = module {
    factoryOf(::GeneralPrefsUseCases)
    factoryOf(::AppearancePrefsUseCases)
    factoryOf(::AudioPrefsUseCases)
    factoryOf(::SubtitlePrefsUseCases)
    factoryOf(::DecoderPrefsUseCases)
    factoryOf(::GesturePrefsUseCases)
    factoryOf(::PlayerPrefsUseCases)
    factoryOf(::AdvancedPrefsUseCases)
    factoryOf(::CacheUseCases)
    factoryOf(::BackupUseCases)

    factoryOf(::AppUseCases)
    factoryOf(::BrowserUseCases)
    factoryOf(::FileManagementUseCases)
    factoryOf(::NetworkStreamUseCases)
    factoryOf(::SourceEditorUseCases)
    factoryOf(::SourceManagerUseCases)
    factoryOf(::PlayerUseCases)
    factoryOf(::MediaPlayerUseCases)
    factoryOf(::PlaylistUseCases)
}

/**
 * Aggregated domain module.
 */
val domainModule = module {
    includes(
        useCasePrefsModule,
        useCaseMediaModule,
        useCaseAggregateModule
    )
}
