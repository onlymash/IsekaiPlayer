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

@file:OptIn(UnstableApi::class)

package com.fiepi.media.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.fiepi.media.domain.player.MediaPlayer
import com.fiepi.media.domain.player.PlayerEngine
import com.fiepi.media.domain.player.model.PlayerEngineType
import com.fiepi.media.domain.usecase.player.GetNativeLibrariesUseCase
import com.fiepi.media.player.MediaPlayerImpl
import com.fiepi.media.player.engine.ExoPlayerEngine
import com.fiepi.media.player.engine.MpvPlayerEngine
import com.fiepi.media.player.usecase.GetExoPlayerConfigUseCase
import com.fiepi.media.player.usecase.GetMpvConfigUseCase
import com.fiepi.media.player.usecase.GetMpvRuntimeConfigUseCase
import com.fiepi.media.player.usecase.GetMpvStaticConfigUseCase
import com.fiepi.media.player.usecase.GetNativeLibrariesUseCaseImpl
import com.fiepi.mpv.controller.MpvConfigProvider
import com.fiepi.mpv.controller.MpvController
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.io.File
import java.util.concurrent.TimeUnit

val playerModule: Module = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    single<Cache> {
        val context: Context = get()
        val cacheDir = File(context.cacheDir, "exoplayer_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(512 * 1024 * 1024L) // 512MB
        val databaseProvider = StandaloneDatabaseProvider(context)
        SimpleCache(cacheDir, evictor, databaseProvider)
    }

    factoryOf(::GetMpvStaticConfigUseCase)
    factoryOf(::GetMpvRuntimeConfigUseCase)
    factoryOf(::GetMpvConfigUseCase)
    factoryOf(::GetExoPlayerConfigUseCase)
    factoryOf(::GetNativeLibrariesUseCaseImpl) { bind<GetNativeLibrariesUseCase>() }

    singleOf(::MpvConfigProvider)
    factoryOf(::MpvController)
    factoryOf(::MpvPlayerEngine)
    factoryOf(::ExoPlayerEngine)

    single<(PlayerEngineType) -> PlayerEngine> {
        { type ->
            when (type) {
                PlayerEngineType.MPV -> get<MpvPlayerEngine>()
                PlayerEngineType.EXO_PLAYER -> get<ExoPlayerEngine>()
            }
        }
    }

    singleOf(::MediaPlayerImpl) { bind<MediaPlayer>() }
}
