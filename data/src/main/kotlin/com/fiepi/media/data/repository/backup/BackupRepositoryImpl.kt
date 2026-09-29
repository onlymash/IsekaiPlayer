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

package com.fiepi.media.data.repository.backup

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import com.fiepi.media.domain.model.backup.SettingsBackup
import com.fiepi.media.domain.model.preferences.AdvancedOptions
import com.fiepi.media.domain.model.preferences.AudioOptions
import com.fiepi.media.domain.model.preferences.DecoderOptions
import com.fiepi.media.domain.model.preferences.GeneralOptions
import com.fiepi.media.domain.model.preferences.GestureOptions
import com.fiepi.media.domain.model.preferences.MediaPreferences
import com.fiepi.media.domain.model.preferences.PlayerOptions
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import com.fiepi.media.domain.model.preferences.ThemeOptions
import com.fiepi.media.domain.repository.backup.BackupRepository
import com.fiepi.media.domain.repository.source.RemoteSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BackupRepositoryImpl(
    private val context: Context,
    private val remoteSourceRepository: RemoteSourceRepository,
    private val audioDataStore: DataStore<AudioOptions>,
    private val decoderDataStore: DataStore<DecoderOptions>,
    private val themeDataStore: DataStore<ThemeOptions>,
    private val subtitleDataStore: DataStore<SubtitleOptions>,
    private val playerDataStore: DataStore<PlayerOptions>,
    private val mediaDataStore: DataStore<MediaPreferences>,
    private val gestureDataStore: DataStore<GestureOptions>,
    private val advancedDataStore: DataStore<AdvancedOptions>,
    private val generalDataStore: DataStore<GeneralOptions>
) : BackupRepository {

    private val backupJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    override suspend fun exportBackup(uriString: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sources = remoteSourceRepository.getSources().first()
                val backup = SettingsBackup(
                    version = 1,
                    audioOptions = audioDataStore.data.first(),
                    decoderOptions = decoderDataStore.data.first(),
                    themeOptions = themeDataStore.data.first(),
                    subtitleOptions = subtitleDataStore.data.first(),
                    playerOptions = playerDataStore.data.first(),
                    mediaPreferences = mediaDataStore.data.first(),
                    gestureOptions = gestureDataStore.data.first(),
                    advancedOptions = advancedDataStore.data.first(),
                    generalOptions = generalDataStore.data.first(),
                    remoteSources = sources
                )

                val jsonText = backupJson.encodeToString(SettingsBackup.serializer(), backup)
                val uri = uriString.toUri()

                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.write(jsonText.toByteArray(Charsets.UTF_8))
                } ?: error("Failed to open output stream for export")
            }
        }

    override suspend fun importBackup(uriString: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = uriString.toUri()
                val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: error("Failed to open input stream for import")

                val backup = backupJson.decodeFromString(SettingsBackup.serializer(), jsonText)

                audioDataStore.updateData { backup.audioOptions }
                decoderDataStore.updateData { backup.decoderOptions }
                themeDataStore.updateData { backup.themeOptions }
                subtitleDataStore.updateData { backup.subtitleOptions }
                playerDataStore.updateData { backup.playerOptions }
                mediaDataStore.updateData { backup.mediaPreferences }
                gestureDataStore.updateData { backup.gestureOptions }
                advancedDataStore.updateData { backup.advancedOptions }
                generalDataStore.updateData { backup.generalOptions }

                if (backup.remoteSources.isNotEmpty()) {
                    remoteSourceRepository.updateSources(backup.remoteSources)
                }
                Unit
            }
        }
}
