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

package com.fiepi.media.player.component

import com.fiepi.media.domain.player.PlayerEngine
import com.fiepi.media.domain.player.model.EngineCapabilities
import com.fiepi.media.domain.player.model.EngineEvent
import com.fiepi.media.domain.player.model.ExternalSubtitle
import com.fiepi.media.domain.player.model.PlayerEngineType
import com.fiepi.media.domain.player.model.TrackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manages player engine creation, hot-switching, low-level event collection, and engine lifecycle release.
 *
 * CRITICAL ARCHITECTURE RULES:
 * 1. Require explicit [PlayerEngineType] parameter in [getOrInitEngine]. Do NOT default fallback to MPV or ExoPlayer.
 * 2. Synchronize engine [EngineCapabilities] through [onCapabilitiesChanged] upon every [setupEngine] call.
 */
internal class PlayerEngineManager(
    private val playerScope: CoroutineScope,
    private val engineFactory: (PlayerEngineType) -> PlayerEngine,
    private val onEngineEvent: (EngineEvent) -> Unit,
    private val onCapabilitiesChanged: (EngineCapabilities) -> Unit,
    private val bindSurfaceToNewEngine: (PlayerEngine) -> Unit,
    private val unbindSurfaceFromOldEngine: suspend (PlayerEngine) -> Unit
) {

    // Currently active engine and type
    var activeEngine: PlayerEngine? = null
        private set

    var currentEngineType: PlayerEngineType? = null
        private set

    // Engine event subscription job
    private var engineEventsJob: Job? = null

    // Internal engine state flow used to dynamically toggle isReady
    private val _engine = MutableStateFlow<PlayerEngine?>(null)

    // Whether player engine is ready (native instance initialization complete)
    @OptIn(ExperimentalCoroutinesApi::class)
    val isReady: StateFlow<Boolean> = _engine.flatMapLatest {
        it?.isReady ?: flowOf(false)
    }.stateIn(playerScope, SharingStarted.Eagerly, false)

    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 64)
    val events: Flow<EngineEvent> = _events.asSharedFlow()

    fun getOrInitEngine(type: PlayerEngineType): PlayerEngine {
        activeEngine?.let {
            return it
        }
        currentEngineType = type
        val newEngine = engineFactory(type)
        setupEngine(newEngine)
        return newEngine
    }

    fun setupEngine(newEngine: PlayerEngine) {
        engineEventsJob?.cancel()
        activeEngine = newEngine
        _engine.value = newEngine

        // Notify capabilities change on new engine setup
        onCapabilitiesChanged(newEngine.capabilities)

        bindSurfaceToNewEngine(newEngine)

        engineEventsJob = playerScope.launch {
            newEngine.events.collect { event ->
                _events.emit(event)
                onEngineEvent(event)
            }
        }
    }

    suspend fun switchEngineIfNeeded(
        targetType: PlayerEngineType,
        hasCurrentVideo: Boolean,
        onPrepareSwitchState: () -> Unit,
        onLoadNewEngineVideo: suspend (PlayerEngine) -> Unit
    ) {
        if (currentEngineType == targetType && activeEngine != null) return

        if (hasCurrentVideo) {
            onPrepareSwitchState()
        }

        val oldEngine = activeEngine
        activeEngine = null

        if (oldEngine != null) {
            unbindSurfaceFromOldEngine(oldEngine)
            oldEngine.release()
        }

        currentEngineType = targetType
        val newEngine = engineFactory(targetType)
        setupEngine(newEngine)

        if (hasCurrentVideo) {
            onLoadNewEngineVideo(newEngine)
        }
    }

    fun releaseEngine(unbindSurfaceAndRelease: (PlayerEngine?) -> Unit) {
        val engineToRelease = activeEngine
        activeEngine = null
        _engine.value = null
        engineEventsJob?.cancel()

        unbindSurfaceAndRelease(engineToRelease)
    }

    // --- Engine Operation Delegates ---

    fun load(url: String, startPositionMs: Long = 0L) {
        activeEngine?.load(url, startPositionMs)
    }

    fun play() {
        activeEngine?.play()
    }

    fun pause() {
        activeEngine?.pause()
    }

    fun seekTo(positionMs: Long) {
        activeEngine?.seekTo(positionMs)
    }

    fun setSpeed(speed: Double) {
        activeEngine?.setSpeed(speed)
    }

    fun setUserAgent(userAgent: String?) {
        activeEngine?.setUserAgent(userAgent)
    }

    fun setMute(muted: Boolean) {
        activeEngine?.setMute(muted)
    }

    fun setVolume(volume: Double) {
        activeEngine?.setVolume(volume)
    }

    fun setAudioDelay(delayMs: Long) {
        activeEngine?.setAudioDelay(delayMs)
    }

    fun setSubtitleDelay(delayMs: Long) {
        activeEngine?.setSubtitleDelay(delayMs)
    }

    fun selectTrack(type: TrackType, id: Int) {
        activeEngine?.selectTrack(type, id)
    }

    fun addSubtitle(subtitle: ExternalSubtitle) {
        activeEngine?.addSubtitle(subtitle)
    }

    fun addSubtitles(subtitles: List<ExternalSubtitle>) {
        activeEngine?.addSubtitles(subtitles)
    }

    fun sendKey(action: String, key: String) {
        activeEngine?.sendKey(action, key)
    }

    fun stepFrame(forward: Boolean) {
        activeEngine?.stepFrame(forward)
    }

    fun command(vararg args: String) {
        activeEngine?.command(*args)
    }

    suspend fun takeScreenshot(filePath: String) {
        activeEngine?.takeScreenshot(filePath)
    }

    fun setSurface(surface: Any?) {
        activeEngine?.setSurface(surface)
    }

    fun setSurfaceSize(width: Int, height: Int) {
        activeEngine?.setSurfaceSize(width, height)
    }

    val capabilities: EngineCapabilities
        get() = activeEngine?.capabilities ?: EngineCapabilities()
}
