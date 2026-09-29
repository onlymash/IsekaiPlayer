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

package com.fiepi.media.player.frame

import android.content.Context
import coil3.ComponentRegistry
import com.fiepi.media.domain.player.MediaStreamServer
import com.fiepi.media.domain.repository.media.MediaRepository
import com.fiepi.media.domain.repository.source.SourceRepository
import com.fiepi.mpv.controller.MpvConfigProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Helper to register player-specific Coil components.
 * This keeps :app from needing a direct dependency on :libmpv.
 */
object PlayerCoilHelper : KoinComponent {
    fun register(registry: ComponentRegistry.Builder, context: Context) {
        val mediaRepository: MediaRepository by inject()
        val sourceRepository: SourceRepository by inject()
        val streamServer: MediaStreamServer by inject()
        val mpvConfigProvider: MpvConfigProvider by inject()

        registry.add(
            MediaFrameFetcher.Factory(
                context,
                mediaRepository,
                sourceRepository,
                streamServer,
                mpvConfigProvider
            )
        )
    }
}
