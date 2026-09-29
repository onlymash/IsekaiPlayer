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

package com.fiepi.media.domain.player

import com.fiepi.media.domain.player.model.EngineCapabilities
import com.fiepi.media.domain.player.model.EngineEvent
import com.fiepi.media.domain.player.model.ExternalSubtitle
import com.fiepi.media.domain.player.model.TrackType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Player engine abstract interface
 */
interface PlayerEngine {
    val capabilities: EngineCapabilities
    val isReady: StateFlow<Boolean>
    val events: Flow<EngineEvent> // Exposes raw event stream for high-precision synchronization
    fun setSurface(surface: Any?)
    fun setSurfaceSize(width: Int, height: Int)
    fun load(url: String, startPositionMs: Long = 0L)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Double)
    fun setMute(muted: Boolean)
    fun setSpeed(speed: Double)
    fun setUserAgent(userAgent: String?)
    fun setAudioDelay(delayMs: Long)
    fun setSubtitleDelay(delayMs: Long)
    fun setPreferredAudioLanguages(languages: List<String>)
    fun setPreferredSubtitleLanguages(languages: List<String>)
    fun setSubtitleFallback(enabled: Boolean)
    fun selectTrack(type: TrackType, id: Int)
    fun addSubtitle(subtitle: ExternalSubtitle)
    fun addSubtitle(url: String, lang: String? = null)
    fun addSubtitles(subtitles: List<ExternalSubtitle>)
    fun sendKey(action: String, key: String)
    fun command(vararg args: String)
    fun stepFrame(forward: Boolean)

    suspend fun takeScreenshot(path: String)

    /**
     * Captures a thumbnail
     * @param dimension Thumbnail dimension
     * @return Platform-dependent image object (Bitmap on Android)
     */
    suspend fun grabThumbnail(dimension: Int): Any?

    fun stop()
    fun release()
}
