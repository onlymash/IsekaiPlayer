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

package com.fiepi.media.player.engine

import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.player.PlayerEngine
import com.fiepi.media.domain.player.model.EngineCapabilities
import com.fiepi.media.domain.player.model.EngineEvent
import com.fiepi.media.domain.player.model.ExternalSubtitle
import com.fiepi.media.domain.player.model.MediaTrack
import com.fiepi.media.domain.player.model.TrackType
import com.fiepi.media.player.usecase.GetMpvConfigUseCase
import com.fiepi.mpv.constants.MpvEvent
import com.fiepi.mpv.controller.MpvController
import com.fiepi.mpv.model.MpvConfig
import com.fiepi.mpv.model.MpvEventData
import com.fiepi.mpv.model.MpvRuntimeConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MpvPlayerEngine implements the PlayerEngine interface, bridging the business layer and MpvController.
 * Each MpvPlayerEngine corresponds to an independent native playback instance.
 */
class MpvPlayerEngine(
    private val mpvController: MpvController,
    private val getMpvConfig: GetMpvConfigUseCase
) : PlayerEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isReleased = AtomicBoolean(false)

    // Caches previous track lists
    private var lastAudioTracks: List<MediaTrack.Audio> = emptyList()
    private var lastSubtitleTracks: List<MediaTrack.Subtitle> = emptyList()
    private var lastVideoTracks: List<MediaTrack.Video> = emptyList()

    private var currentFrame: Long = 0
    private var totalFrames: Long = 0
    private var lastTimePosMs: Long = 0L
    private var lastDemuxerCacheSec: Double = 0.0

    override val isReady: StateFlow<Boolean> = mpvController.isReady

    override val capabilities: EngineCapabilities = EngineCapabilities()

    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 64)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val events: Flow<EngineEvent> = merge(
        mpvController.getEvents().mapNotNull { event -> mapMpvEvent(event) },
        _events
    )

    private fun updateBufferedPosition() {
        val bufferedMs = (lastTimePosMs + (lastDemuxerCacheSec * 1000.0).toLong()).coerceAtLeast(lastTimePosMs)
        _events.tryEmit(EngineEvent.BufferedPositionChanged(bufferedMs))
    }

    private fun mapMpvEvent(event: MpvEventData): EngineEvent? {
        return when (event) {
            is MpvEventData.PropertyLong -> {
                when (event.name) {
                    "time-pos" -> {
                        lastTimePosMs = event.value
                        updateBufferedPosition()
                        EngineEvent.PositionChanged(event.value)
                    }
                    "duration" -> {
                        fetchTrackList()
                        EngineEvent.DurationChanged(event.value)
                    }
                    "estimated-frame-count" -> {
                        totalFrames = event.value
                        _events.tryEmit(EngineEvent.FrameInfoChanged(currentFrame, totalFrames))
                        null
                    }
                    else -> null
                }
            }

            is MpvEventData.PropertyDouble -> {
                when (event.name) {
                    "demuxer-cache-duration" -> {
                        lastDemuxerCacheSec = event.value
                        updateBufferedPosition()
                        null
                    }
                    "speed" -> EngineEvent.PlaybackSpeedChanged(event.value.toFloat())
                    "audio-delay" -> EngineEvent.AudioDelayChanged((event.value * 1000).toLong())
                    "sub-delay" -> EngineEvent.SubtitleDelayChanged((event.value * 1000).toLong())
                    "volume" -> EngineEvent.VolumeChanged(event.value.toInt())
                    else -> null
                }
            }

            is MpvEventData.PropertyBoolean -> {
                when (event.name) {
                    "pause" -> EngineEvent.PlayPauseStateChanged(!event.value)
                    "paused-for-cache" -> EngineEvent.BufferingStateChanged(event.value)
                    "eof-reached" -> if (event.value) EngineEvent.PlaybackEnded else null
                    else -> null
                }
            }

            is MpvEventData.PropertyString -> {
                when (event.name) {
                    "hwdec-current" -> EngineEvent.HwdecChanged(event.value)
                    else -> null
                }
            }

            is MpvEventData.PropertyNone -> {
                if (event.name == "track-list") {
                    fetchTrackList()
                }
                null
            }

            is MpvEventData.Event -> {
                when (event.eventId) {
                    MpvEvent.MPV_EVENT_FILE_LOADED -> {
                        fetchTrackList()
                        EngineEvent.FileLoaded
                    }
                    MpvEvent.MPV_EVENT_END_FILE -> EngineEvent.FileUnloaded
                    else -> null
                }
            }
        }
    }

    private val mpvConfig = MutableStateFlow<MpvConfig?>(null)
    private var customUserAgent: String? = null
    private var lastSurface: Any? = null
    private var lastAppliedOsdVisible: Boolean? = null
    private var lastAppliedOsdPage: Int? = null

    init {
        engineScope.launch {
            getMpvConfig().collect { newConfig ->
                val currentConfig = mpvConfig.value
                mpvConfig.value = newConfig

                if (currentConfig != null && currentConfig != newConfig) {
                    if (currentConfig.static != newConfig.static) {
                        // Static config changed, engine must restart
                        mpvController.restart(newConfig)
                        lastAppliedOsdVisible = null
                        lastAppliedOsdPage = null
                        _events.emit(EngineEvent.EngineRestarted)
                        syncOsdState(newConfig.runtime, force = true)
                    } else if (currentConfig.runtime != newConfig.runtime) {
                        // Only runtime config changed, hot update suffices
                        mpvController.updateRuntimeConfig(newConfig.runtime)
                        syncOsdState(newConfig.runtime)
                    }
                }
                applyUserAgent()
            }
        }
    }

    private suspend fun syncOsdState(runtimeConfig: MpvRuntimeConfig, force: Boolean = false) {
        if (!mpvController.isReady.value) return

        val targetVisible = runtimeConfig.display.isOsdVisible
        val targetPage = runtimeConfig.display.osdPage

        if (force || lastAppliedOsdVisible != targetVisible) {
            if (targetVisible) {
                mpvController.command("script-binding", "stats/display-stats-toggle")
                mpvController.command("script-binding", "stats/display-page-$targetPage")
                lastAppliedOsdPage = targetPage
            } else if (lastAppliedOsdVisible == true) {
                mpvController.command("script-binding", "stats/display-stats-toggle")
            }
            lastAppliedOsdVisible = targetVisible
        } else if (targetVisible && lastAppliedOsdPage != targetPage) {
            mpvController.command("script-binding", "stats/display-page-$targetPage")
            lastAppliedOsdPage = targetPage
        }
    }

    private val commandMutex = Mutex()

    /**
     * Helper function: ensures initialization and executes commands sequentially
     */
    private fun withEngine(block: suspend () -> Unit) {
        if (isReleased.get()) {
            return
        }
        engineScope.launch {
            commandMutex.withLock {
                if (isReleased.get()) return@withLock
                if (!mpvController.isReady.value) {
                    val config = mpvConfig.filterNotNull().first()
                    mpvController.initialize(config)
                    syncOsdState(config.runtime, force = true)
                }
                block()
            }
        }
    }

    override fun setSurface(surface: Any?) {
        lastSurface = surface
        withEngine {
            val surfaceObject = when (surface) {
                is SurfaceView -> surface.holder.surface
                is TextureView -> surface.surfaceTexture?.let { Surface(it) }
                is Surface -> surface
                else -> null
            }
            if (surfaceObject != null && surfaceObject.isValid) {
                mpvController.attachSurface(surfaceObject)
            } else {
                mpvController.detachSurface()
            }
        }
    }

    override fun setSurfaceSize(width: Int, height: Int) {
        withEngine {
            mpvController.setSurfaceSize(width, height)
        }
    }

    override fun load(url: String, startPositionMs: Long) = withEngine {
        // Reset track states
        mpvController.selectTrack("aid", "auto")
        mpvController.selectTrack("vid", "auto")
        mpvController.selectTrack("sid", "auto")

        lastAudioTracks = emptyList()
        lastVideoTracks = emptyList()
        lastSubtitleTracks = emptyList()

        _events.tryEmit(EngineEvent.SubtitleTextUpdated(null))

        // Dispatch load command
        mpvController.playFile(url, startPositionMs)
    }

    override fun play() = withEngine {
        mpvController.pause(false)
    }

    override fun pause() = withEngine {
        mpvController.pause(true)
    }

    override fun seekTo(positionMs: Long) = withEngine {
        mpvController.seekTo(positionMs)
    }

    override fun setVolume(volume: Double) = withEngine {
        mpvController.setVolume(volume)
    }

    override fun setMute(muted: Boolean) = withEngine {
        mpvController.setMute(muted)
    }

    override fun setSpeed(speed: Double) = withEngine {
        mpvController.setSpeed(speed)
    }

    override fun setUserAgent(userAgent: String?) = withEngine {
        customUserAgent = userAgent
        applyUserAgent()
    }

    private suspend fun applyUserAgent() {
        val defaultUa = mpvConfig.value?.runtime?.cache?.userAgent?.ifBlank { AppConstants.DEFAULT_USER_AGENT } ?: AppConstants.DEFAULT_USER_AGENT
        val targetUa = customUserAgent?.takeIf { it.isNotBlank() } ?: defaultUa
        mpvController.setPropertyString("user-agent", targetUa)
    }

    override fun setAudioDelay(delayMs: Long) = withEngine {
        mpvController.setPropertyString("audio-delay", (delayMs / 1000.0).toString())
    }

    override fun setSubtitleDelay(delayMs: Long) = withEngine {
        mpvController.setPropertyString("sub-delay", (delayMs / 1000.0).toString())
    }

    override fun setPreferredAudioLanguages(languages: List<String>) = withEngine {
        val validLanguages = languages.filter { it != "none" }
        val mpvLanguage = validLanguages.joinToString(",")
        mpvController.setPreferredAudioLanguage(mpvLanguage)
    }

    override fun setPreferredSubtitleLanguages(languages: List<String>) = withEngine {
        val validLanguages = languages.filter { it != "none" && it.isNotBlank() }
        val mpvLanguage = validLanguages.joinToString(",")
        mpvController.setPreferredSubtitleLanguage(mpvLanguage)
    }

    override fun setSubtitleFallback(enabled: Boolean) = withEngine {
        mpvController.setOption("subs-fallback", if (enabled) "yes" else "no")
    }

    override fun selectTrack(type: TrackType, id: Int) = withEngine {
        val mpvType = when (type) {
            TrackType.Audio -> "aid"
            TrackType.Subtitle -> "sid"
            TrackType.Video -> "vid"
        }
        val mpvValue = when (id) {
            -1 -> "no"
            -2 -> "auto"
            else -> id.toString()
        }
        mpvController.selectTrack(mpvType, mpvValue)
    }

    override fun addSubtitle(url: String, lang: String?) = withEngine {
        mpvController.addSubtitle(url, lang)
    }

    override fun addSubtitle(subtitle: ExternalSubtitle) = withEngine {
        mpvController.addSubtitle(subtitle.url, subtitle.language)
    }

    override fun addSubtitles(subtitles: List<ExternalSubtitle>) = withEngine {
        subtitles.forEach { sub ->
            mpvController.addSubtitle(sub.url, sub.language)
        }
    }

    override suspend fun grabThumbnail(dimension: Int): Any? {
        return mpvController.grabThumbnail(dimension)
    }

    private fun fetchTrackList() = withEngine {
        val audio = mutableListOf<MediaTrack.Audio>()
        val sub = mutableListOf<MediaTrack.Subtitle>()
        val video = mutableListOf<MediaTrack.Video>()

        val rawTracks = mpvController.getTrackList()
        for (map in rawTracks) {
            val type = map["type"] as? String ?: continue
            val id = map["id"] as? Int ?: continue
            val title = map["title"] as? String ?: ""
            val lang = map["lang"] as? String
            val isSelected = map["selected"] as? Boolean ?: false
            val isExternal = map["external"] as? Boolean ?: false
            val codec = map["codec"] as? String

            when (type) {
                "audio" -> {
                    val bitrate = mpvController.getPropertyDouble("audio-bitrate")?.toLong()?.takeIf { it > 0L } ?: 0L
                    val channels = mpvController.getPropertyInt("audio-params/channels") ?: 0
                    val sampleRate = mpvController.getPropertyInt("audio-params/samplerate") ?: 0
                    audio.add(
                        MediaTrack.Audio(
                            id = id,
                            title = title,
                            language = lang,
                            isSelected = isSelected,
                            isExternal = isExternal,
                            codec = codec,
                            bitrate = bitrate,
                            channels = channels,
                            sampleRate = sampleRate
                        )
                    )
                }

                "sub" -> {
                    sub.add(
                        MediaTrack.Subtitle(
                            id = id,
                            title = title,
                            language = lang,
                            isSelected = isSelected,
                            isExternal = isExternal,
                            codec = codec
                        )
                    )
                }

                "video" -> {
                    val width = mpvController.getPropertyInt("width")
                        ?: mpvController.getPropertyDouble("width")?.toInt()
                        ?: mpvController.getPropertyDouble("video-params/w")?.toInt()
                        ?: 0
                    val height = mpvController.getPropertyInt("height")
                        ?: mpvController.getPropertyDouble("height")?.toInt()
                        ?: mpvController.getPropertyDouble("video-params/h")?.toInt()
                        ?: 0
                    val fps = mpvController.getPropertyDouble("container-fps")
                        ?: mpvController.getPropertyDouble("estimated-vf-fps")
                        ?: mpvController.getPropertyDouble("fps")
                        ?: mpvController.getPropertyDouble("video-params/fps")
                        ?: mpvController.getPropertyDouble("video-dec-params/fps")
                        ?: 0.0
                    val videoCodec = codec ?: mpvController.getPropertyString("video-codec")
                        ?: mpvController.getPropertyString("video-format")
                        ?: mpvController.getPropertyString("video-params/codec")

                    var bitrate = (mpvController.getPropertyDouble("video-bitrate")
                        ?: mpvController.getPropertyDouble("bitrate")
                        ?: mpvController.getPropertyDouble("packet-video-bitrate"))?.toLong()?.takeIf { it > 0L } ?: 0L

                    if (bitrate <= 0L) {
                        val fileSize = mpvController.getPropertyDouble("file-size") ?: 0.0
                        val durationSec = mpvController.getPropertyDouble("duration") ?: 0.0
                        if (fileSize > 0.0 && durationSec > 0.0) {
                            bitrate = (fileSize * 8.0 / durationSec).toLong()
                        }
                    }

                    video.add(
                        MediaTrack.Video(
                            id = id,
                            title = title,
                            language = lang,
                            isSelected = isSelected,
                            isExternal = isExternal,
                            codec = videoCodec,
                            width = width,
                            height = height,
                            fps = fps,
                            bitrate = bitrate
                        )
                    )
                }
            }
        }

        // Update state and send event only when data actually changes to prevent invalid UI refreshes
        if (audio != lastAudioTracks) {
            lastAudioTracks = audio
            _events.tryEmit(EngineEvent.AudioTracksChanged(audio))
        }

        if (sub != lastSubtitleTracks) {
            lastSubtitleTracks = sub
            _events.tryEmit(EngineEvent.SubtitleTracksChanged(sub))
        }

        if (video != lastVideoTracks) {
            lastVideoTracks = video
            _events.tryEmit(EngineEvent.VideoTracksChanged(video))
        }
    }

    override fun sendKey(action: String, key: String) = withEngine {
        mpvController.sendKey(action, key)
    }

    override fun command(vararg args: String) = withEngine {
        mpvController.command(*args)
    }

    override fun stepFrame(forward: Boolean) = withEngine {
        val cmd = if (forward) "frame-step" else "frame-back-step"
        mpvController.command(cmd)
        // Refresh frame info manually right after command dispatch
        updateFrameInfo()
    }

    private suspend fun updateFrameInfo() {
        val current = mpvController.getPropertyInt("estimated-frame-number")?.toLong() ?: 0L
        val total = mpvController.getPropertyInt("estimated-frame-count")?.toLong() ?: 0L
        
        currentFrame = current
        totalFrames = total
        _events.emit(EngineEvent.FrameInfoChanged(currentFrame, totalFrames))
    }

    override suspend fun takeScreenshot(path: String) {
        // Use withContext to synchronously wait for mpv command in coroutine
        mpvController.takeScreenshot(path)
    }

    override fun stop() = withEngine {
        mpvController.sendKey("keydown", "STOP")
    }

    override fun release() {
        if (isReleased.getAndSet(true)) return
        engineScope.launch(Dispatchers.IO) {
            mpvController.destroy()
        }
        lastAudioTracks = emptyList()
        lastVideoTracks = emptyList()
        lastSubtitleTracks = emptyList()
    }
}
