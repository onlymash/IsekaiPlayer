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

package com.fiepi.media.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.fiepi.media.app.di.appModules
import com.fiepi.media.player.frame.PlayerCoilHelper
import com.fiepi.media.player.utils.AssetUtils
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

class App : Application(), SingletonImageLoader.Factory, KoinComponent {

    private val httpClient: HttpClient by inject()

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val imageDispatcher = Dispatchers.IO.limitedParallelism(4)

        return ImageLoader.Builder(context)
            .interceptorCoroutineContext(imageDispatcher)
            .fetcherCoroutineContext(imageDispatcher)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .components {
                add(KtorNetworkFetcherFactory(httpClient = { httpClient }))
                PlayerCoilHelper.register(this, context)
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            modules(appModules)
        }
        // Explicitly set the singleton image loader factory to ensure it's recognized
        SingletonImageLoader.setSafe(this)
        AssetUtils.copyAssets(this)
    }
}
