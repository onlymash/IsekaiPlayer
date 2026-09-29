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

package com.fiepi.media.player.usecase

import android.content.Context
import com.fiepi.media.domain.model.assets.Library
import com.fiepi.media.domain.usecase.player.GetNativeLibrariesUseCase
import com.fiepi.mpv.controller.MpvConfigProvider
import com.fiepi.mpv.controller.MpvController
import com.fiepi.mpv.model.MpvConfig
import com.fiepi.mpv.model.MpvStaticConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class GetNativeLibrariesUseCaseImpl(
    private val context: Context,
    private val configProvider: MpvConfigProvider
) : GetNativeLibrariesUseCase {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun invoke(): List<Library> = withContext(Dispatchers.IO) {
        val libraryList = mutableListOf<Library>()

        // Read libraries.json from Assets using kotlinx.serialization
        try {
            context.assets.open("libraries.json").bufferedReader().use { reader ->
                val parsedLibraries = json.decodeFromString<List<Library>>(reader.readText())
                libraryList.addAll(parsedLibraries)
            }
        } catch (_: Exception) {
            // ignore (file might not exist yet if script wasn't run)
        }

        // Stores library name -> new version string to be dynamically updated by MpvController
        val dynamicVersions = mutableMapOf<String, String>()

        // Get real runtime versions exposed via mpv properties (mpv, FFmpeg, libass, libplacebo)
        val controller = MpvController(context, configProvider)
        try {
            val minimalConfig = MpvConfig(
                static = MpvStaticConfig(
                    vo = "null",
                    audioOutput = "null",
                    profile = "sw-fast",
                    msgLevel = "all=no"
                )
            )
            controller.initialize(minimalConfig)
            controller.isReady.first { it }

            controller.getPropertyString("mpv-version")?.let {
                dynamicVersions["mpv"] = it.removePrefix("mpv ")
            }

            controller.getPropertyString("ffmpeg-version")?.let {
                dynamicVersions["FFmpeg"] = it
            }

            controller.getPropertyInt("libass-version")?.let { version ->
                val major = (version shr 24) and 0xFF
                val minor = (version shr 16) and 0xFF
                val micro = (version shr 8) and 0xFF
                dynamicVersions["libass"] = "$major.$minor.$micro"
            }

            controller.getPropertyString("libplacebo-version")?.let {
                dynamicVersions["libplacebo"] = it
            }

        } catch (_: Exception) {
            // ignore
        } finally {
            controller.destroy()
        }

        // Update parsed Library objects with runtime versions obtained from MpvController
        libraryList.map { lib ->
            val updatedVersion = dynamicVersions[lib.name]
            if (updatedVersion != null) {
                lib.copy(version = updatedVersion)
            } else {
                lib
            }
        }
    }
}
