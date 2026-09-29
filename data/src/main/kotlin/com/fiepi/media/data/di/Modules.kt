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

package com.fiepi.media.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.room3.Room
import com.fiepi.media.data.database.AppDatabase
import com.fiepi.media.data.repository.assets.LanguageRepositoryImpl
import com.fiepi.media.data.repository.backup.BackupRepositoryImpl
import com.fiepi.media.data.repository.cache.CacheRepositoryImpl
import com.fiepi.media.data.repository.decoder.CodecRepositoryImpl
import com.fiepi.media.data.repository.history.HistoryRepositoryImpl
import com.fiepi.media.data.repository.media.MediaRepositoryImpl
import com.fiepi.media.data.repository.media.local.LocalMediaRepositoryImpl
import com.fiepi.media.data.repository.media.proxy.MediaStreamServerImpl
import com.fiepi.media.data.repository.playlist.PlaylistRepositoryImpl
import com.fiepi.media.data.repository.preferences.AdvancedPrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.AppearancePrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.AudioPrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.DecoderPrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.GeneralPrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.GesturePrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.MediaPrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.PlayerPrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.SubtitlePrefsRepositoryImpl
import com.fiepi.media.data.repository.preferences.serializer.SettingSerializers
import com.fiepi.media.data.repository.source.SourceRepositoryImpl
import com.fiepi.media.data.repository.source.local.LocalSourceRepositoryImpl
import com.fiepi.media.data.repository.source.remote.RemoteSourceRepositoryImpl
import com.fiepi.media.data.repository.stream.NetworkStreamHistoryRepositoryImpl
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.preferences.AdvancedOptions
import com.fiepi.media.domain.model.preferences.AudioOptions
import com.fiepi.media.domain.model.preferences.DecoderOptions
import com.fiepi.media.domain.model.preferences.GeneralOptions
import com.fiepi.media.domain.model.preferences.GestureOptions
import com.fiepi.media.domain.model.preferences.MediaPreferences
import com.fiepi.media.domain.model.preferences.PlayerOptions
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import com.fiepi.media.domain.model.preferences.ThemeOptions
import com.fiepi.media.domain.player.MediaStreamServer
import com.fiepi.media.domain.repository.assets.LanguageRepository
import com.fiepi.media.domain.repository.backup.BackupRepository
import com.fiepi.media.domain.repository.cache.CacheRepository
import com.fiepi.media.domain.repository.decoder.CodecRepository
import com.fiepi.media.domain.repository.history.HistoryRepository
import com.fiepi.media.domain.repository.media.LocalMediaRepository
import com.fiepi.media.domain.repository.media.MediaRepository
import com.fiepi.media.domain.repository.playlist.PlaylistRepository
import com.fiepi.media.domain.repository.preferences.AdvancedPrefsRepository
import com.fiepi.media.domain.repository.preferences.AppearancePrefsRepository
import com.fiepi.media.domain.repository.preferences.AudioPrefsRepository
import com.fiepi.media.domain.repository.preferences.DecoderPrefsRepository
import com.fiepi.media.domain.repository.preferences.GeneralPrefsRepository
import com.fiepi.media.domain.repository.preferences.GesturePrefsRepository
import com.fiepi.media.domain.repository.preferences.MediaPrefsRepository
import com.fiepi.media.domain.repository.preferences.PlayerPrefsRepository
import com.fiepi.media.domain.repository.preferences.SubtitlePrefsRepository
import com.fiepi.media.domain.repository.source.LocalSourceRepository
import com.fiepi.media.domain.repository.source.RemoteSourceRepository
import com.fiepi.media.domain.repository.source.SourceRepository
import com.fiepi.media.domain.repository.stream.NetworkStreamHistoryRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Module for Room database and DAOs.
 */
private val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppConstants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }
    single { get<AppDatabase>().playbackHistoryDao() }
    single { get<AppDatabase>().remoteSourceDao() }
    single { get<AppDatabase>().networkStreamHistoryDao() }
    single { get<AppDatabase>().playlistDao() }
}

/**
 * Module for all type-safe DataStore instances and JSON configuration.
 */
private val dataStoreModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    single<DataStore<AudioOptions>>(named("audio_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.audio(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.AUDIO_SETTINGS_NAME) }
        )
    }
    single<DataStore<DecoderOptions>>(named("decoder_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.decoder(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.DECODER_SETTINGS_NAME) }
        )
    }
    single<DataStore<ThemeOptions>>(named("theme_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.theme(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.APPEARANCE_SETTINGS_NAME) }
        )
    }
    single<DataStore<SubtitleOptions>>(named("subtitle_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.subtitle(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.SUBTITLE_SETTINGS_NAME) }
        )
    }
    single<DataStore<PlayerOptions>>(named("player_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.player(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.PLAYER_SETTINGS_NAME) }
        )
    }
    single<DataStore<MediaPreferences>>(named("media_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.media(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.MEDIA_SETTINGS_NAME) }
        )
    }
    single<DataStore<GestureOptions>>(named("gesture_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.gesture(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.GESTURE_SETTINGS_NAME) }
        )
    }
    single<DataStore<AdvancedOptions>>(named("advanced_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.advanced(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.ADVANCED_SETTINGS_NAME) }
        )
    }
    single<DataStore<GeneralOptions>>(named("general_ds")) {
        DataStoreFactory.create(
            serializer = SettingSerializers.general(get()),
            produceFile = { androidContext().filesDir.resolve(AppConstants.GENERAL_SETTINGS_NAME) }
        )
    }
}

/**
 * Module for all Repository implementations.
 */
private val repositoryModule = module {
    // Preference Repositories with specific DataStore injections
    single<MediaPrefsRepository> { MediaPrefsRepositoryImpl(get(named("media_ds"))) }
    single<AppearancePrefsRepository> { AppearancePrefsRepositoryImpl(get(named("theme_ds"))) }
    single<GeneralPrefsRepository> { GeneralPrefsRepositoryImpl(get(named("general_ds"))) }
    single<GesturePrefsRepository> { GesturePrefsRepositoryImpl(get(named("gesture_ds"))) }
    single<PlayerPrefsRepository> { PlayerPrefsRepositoryImpl(get(named("player_ds"))) }
    single<DecoderPrefsRepository> { DecoderPrefsRepositoryImpl(get(named("decoder_ds"))) }
    single<AudioPrefsRepository> { AudioPrefsRepositoryImpl(get(named("audio_ds"))) }
    single<SubtitlePrefsRepository> { SubtitlePrefsRepositoryImpl(get(named("subtitle_ds"))) }
    single<AdvancedPrefsRepository> { AdvancedPrefsRepositoryImpl(get(named("advanced_ds"))) }

    // Other Repositories
    singleOf(::CodecRepositoryImpl) { bind<CodecRepository>() }
    singleOf(::LanguageRepositoryImpl) { bind<LanguageRepository>() }
    singleOf(::LocalMediaRepositoryImpl) { bind<LocalMediaRepository>() }
    singleOf(::LocalSourceRepositoryImpl) { bind<LocalSourceRepository>() }
    singleOf(::RemoteSourceRepositoryImpl) { bind<RemoteSourceRepository>() }
    singleOf(::SourceRepositoryImpl) { bind<SourceRepository>() }
    singleOf(::MediaRepositoryImpl) { bind<MediaRepository>() }
    singleOf(::HistoryRepositoryImpl) { bind<HistoryRepository>() }
    singleOf(::NetworkStreamHistoryRepositoryImpl) { bind<NetworkStreamHistoryRepository>() }
    singleOf(::PlaylistRepositoryImpl) { bind<PlaylistRepository>() }
    singleOf(::CacheRepositoryImpl) { bind<CacheRepository>() }
    single<BackupRepository> {
        BackupRepositoryImpl(
            context = get(),
            remoteSourceRepository = get(),
            audioDataStore = get(named("audio_ds")),
            decoderDataStore = get(named("decoder_ds")),
            themeDataStore = get(named("theme_ds")),
            subtitleDataStore = get(named("subtitle_ds")),
            playerDataStore = get(named("player_ds")),
            mediaDataStore = get(named("media_ds")),
            gestureDataStore = get(named("gesture_ds")),
            advancedDataStore = get(named("advanced_ds")),
            generalDataStore = get(named("general_ds"))
        )
    }
}

/**
 * Module for network related services and servers.
 */
private val networkModule = module {
    single {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = AppConstants.REMOTE_TIMEOUT_MS
                connectTimeoutMillis = AppConstants.REMOTE_TIMEOUT_MS
                socketTimeoutMillis = AppConstants.REMOTE_TIMEOUT_MS
            }
            followRedirects = true
        }
    }
    singleOf(::MediaStreamServerImpl) { bind<MediaStreamServer>() }
}

/**
 * Aggregated data module.
 */
val dataModule: Module = module {
    includes(databaseModule, dataStoreModule, repositoryModule, networkModule)
}
