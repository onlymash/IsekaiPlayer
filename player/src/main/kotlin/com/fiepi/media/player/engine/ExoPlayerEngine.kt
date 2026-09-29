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

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.ktor.KtorDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.preferences.VideoScaleMode
import com.fiepi.media.domain.player.PlayerEngine
import com.fiepi.media.domain.player.model.EngineCapabilities
import com.fiepi.media.domain.player.model.EngineEvent
import com.fiepi.media.domain.player.model.ExternalSubtitle
import com.fiepi.media.domain.player.model.MediaTrack
import com.fiepi.media.domain.player.model.TrackType
import com.fiepi.media.ffmpeg.decoder.FFmpegAudioRenderer
import com.fiepi.media.player.model.ExoPlayerConfig
import com.fiepi.media.player.network.UrlUserInfoAuthProvider
import com.fiepi.media.player.usecase.GetExoPlayerConfigUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * ExoPlayerEngine implements the PlayerEngine interface using AndroidX Media3 ExoPlayer.
 * Features Ktor + OkHttp DataSource and SimpleCache via Koin injection.
 */
@OptIn(UnstableApi::class)
class ExoPlayerEngine(
    context: Context,
    cache: Cache,
    private val getExoPlayerConfig: GetExoPlayerConfigUseCase
) : PlayerEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isReleased = AtomicBoolean(false)

    private var currentConfig = ExoPlayerConfig()
    private var customUserAgent: String? = null

    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioRenderers(
            context: Context,
            extensionRendererMode: Int,
            mediaCodecSelector: MediaCodecSelector,
            enableDecoderFallback: Boolean,
            audioSink: AudioSink,
            eventHandler: Handler,
            eventListener: AudioRendererEventListener,
            out: ArrayList<Renderer>
        ) {
            super.buildAudioRenderers(
                context,
                extensionRendererMode,
                mediaCodecSelector,
                enableDecoderFallback,
                audioSink,
                eventHandler,
                eventListener,
                out
            )
            if (extensionRendererMode != EXTENSION_RENDERER_MODE_OFF) {
                var extensionIndex = out.size
                if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
                    extensionIndex = 0
                }
                out.add(extensionIndex, FFmpegAudioRenderer(eventHandler, eventListener, audioSink))
            }
        }
    }.apply {
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(createMediaSourceFactory(context, cache))
        .build()

    // ExoPlayer-specific capabilities
    override val capabilities: EngineCapabilities = EngineCapabilities(
        supportsFrameStep = false,
        supportsGainVolume = true,
        maxGainVolume = 100,
        supportsOsdStats = false,
        supportsCustomShaders = false,
        supportsAudioDelay = false,
        supportsSubtitleDelay = false,
    )

    private val _isReady = MutableStateFlow(true)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 64)
    override val events: Flow<EngineEvent> = _events.asSharedFlow()

    private var activeSurfaceView: SurfaceView? = null
    private var activeTextureView: TextureView? = null
    private var currentMediaUrl: String = ""
    private val externalSubtitles = mutableListOf<MediaItem.SubtitleConfiguration>()

    private var lastAudioTracks: List<MediaTrack.Audio> = emptyList()
    private var lastSubtitleTracks: List<MediaTrack.Subtitle> = emptyList()
    private var lastVideoTracks: List<MediaTrack.Video> = emptyList()
    private var isPitchCorrectionEnabled = true

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post {
                block()
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onCues(cueGroup: CueGroup) {
            val text = cueGroup.cues
                .mapNotNull { it.text?.toString() }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .ifBlank { null }

            _events.tryEmit(EngineEvent.SubtitleTextUpdated(text))
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            fetchTrackList()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val duration = exoPlayer.duration
            if (duration != C.TIME_UNSET && duration > 0L) {
                _events.tryEmit(EngineEvent.DurationChanged(duration))
            }
        }

        override fun onRenderedFirstFrame() {
            val duration = exoPlayer.duration
            if (duration != C.TIME_UNSET && duration > 0L) {
                _events.tryEmit(EngineEvent.DurationChanged(duration))
            }
            fetchTrackList()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _events.tryEmit(EngineEvent.BufferingStateChanged(true))
                }

                Player.STATE_READY -> {
                    _events.tryEmit(EngineEvent.BufferingStateChanged(false))

                    // Emit FileLoaded FIRST so MediaPlayerImpl sets isLoaded = true before receiving duration/metadata/tracks
                    _events.tryEmit(EngineEvent.FileLoaded)

                    val duration = exoPlayer.duration
                    if (duration != C.TIME_UNSET && duration > 0L) {
                        _events.tryEmit(EngineEvent.DurationChanged(duration))
                    }
                    fetchTrackList()
                }

                Player.STATE_ENDED -> {
                    _events.tryEmit(EngineEvent.BufferingStateChanged(false))
                    _events.tryEmit(EngineEvent.PlaybackEnded)
                }

                Player.STATE_IDLE -> {
                    _events.tryEmit(EngineEvent.BufferingStateChanged(false))
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _events.tryEmit(EngineEvent.PlayPauseStateChanged(playWhenReady))
        }

        override fun onTracksChanged(tracks: Tracks) {
            fetchTrackList()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _events.tryEmit(EngineEvent.PositionChanged(newPosition.positionMs))
            val bufferedPos = exoPlayer.bufferedPosition.coerceAtLeast(0L)
            _events.tryEmit(EngineEvent.BufferedPositionChanged(bufferedPos))
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _events.tryEmit(EngineEvent.PlaybackSpeedChanged(playbackParameters.speed))
        }

        override fun onPlayerError(error: PlaybackException) {
            _events.tryEmit(EngineEvent.BufferingStateChanged(false))
            _events.tryEmit(EngineEvent.FileUnloaded)
        }
    }

    init {
        runOnMain {
            exoPlayer.addListener(playerListener)
        }

        // Observe ExoPlayerConfig and automatically apply settings when changed
        engineScope.launch {
            getExoPlayerConfig().collect { config ->
                currentConfig = config
                if (!isReleased.get()) {
                    setPreferredAudioLanguages(config.preferredAudioLanguages)
                    setPreferredSubtitleLanguages(config.preferredSubtitleLanguages)
                    setVideoScaleMode(config.videoScaleMode)
                    setSpeed(config.playbackSpeed.toDouble())
                    setAudioPitchCorrection(config.audioPitchCorrection)
                    setVolume(config.volume)
                }
            }
        }

        // Optimized position update loop
        engineScope.launch {
            while (isActive) {
                if (!isReleased.get() && (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING)) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val bufferedPos = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                    _events.tryEmit(EngineEvent.PositionChanged(pos))
                    _events.tryEmit(EngineEvent.BufferedPositionChanged(bufferedPos))
                    if (exoPlayer.isPlaying) {
                        delay(200.milliseconds)
                    } else {
                        delay(500.milliseconds)
                    }
                } else {
                    delay(500.milliseconds)
                }
            }
        }
    }

    override fun setSurface(surface: Any?) {
        if (isReleased.get()) return
        runOnMain {
            activeSurfaceView = null
            activeTextureView = null

            when (surface) {
                is SurfaceView -> {
                    activeSurfaceView = surface
                    exoPlayer.setVideoSurfaceView(surface)
                }

                is TextureView -> {
                    activeTextureView = surface
                    exoPlayer.setVideoTextureView(surface)
                }

                is SurfaceHolder -> exoPlayer.setVideoSurfaceHolder(surface)
                is Surface -> exoPlayer.setVideoSurface(surface)
                null -> exoPlayer.clearVideoSurface()
            }
        }
    }

    override fun setSurfaceSize(width: Int, height: Int) {
        // Handled automatically by ExoPlayer's underlying Surface
    }

    private fun setVideoScaleMode(mode: VideoScaleMode) {
        if (isReleased.get()) return
        runOnMain {
            exoPlayer.videoScalingMode = when (mode) {
                VideoScaleMode.Fit, VideoScaleMode.Original -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                VideoScaleMode.Fill -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
        }
    }

    override fun load(url: String, startPositionMs: Long) {
        if (isReleased.get()) return
        runOnMain {
            currentMediaUrl = url
            externalSubtitles.clear()
            _events.tryEmit(EngineEvent.SubtitleTextUpdated(null))
            lastAudioTracks = emptyList()
            lastSubtitleTracks = emptyList()
            lastVideoTracks = emptyList()

            // Reset track selection parameters
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .clearOverrides()
                .build()

            val uri = if (url.startsWith("/")) {
                Uri.fromFile(File(url))
            } else {
                url.toUri()
            }

            val mimeType = when {
                url.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                url.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
                url.contains(".ism", ignoreCase = true) -> MimeTypes.APPLICATION_SS
                else -> null
            }

            val mediaItem = if (mimeType != null) {
                MediaItem.Builder()
                    .setUri(uri)
                    .setMimeType(mimeType)
                    .build()
            } else {
                MediaItem.fromUri(uri)
            }

            if (startPositionMs > 0) {
                exoPlayer.setMediaItem(mediaItem, startPositionMs)
            } else {
                exoPlayer.setMediaItem(mediaItem)
            }
            exoPlayer.playWhenReady = true
            exoPlayer.prepare()
        }
    }

    override fun play() {
        if (isReleased.get()) return
        runOnMain {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
    }

    override fun pause() {
        if (isReleased.get()) return
        runOnMain {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    override fun seekTo(positionMs: Long) {
        if (isReleased.get()) return
        runOnMain {
            exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        }
    }

    override fun setVolume(volume: Double) {
        if (isReleased.get()) return
        runOnMain {
            val normalized = (volume / 100.0).toFloat().coerceIn(0f, 1f)
            exoPlayer.volume = normalized
            _events.tryEmit(EngineEvent.VolumeChanged(volume.toInt()))
        }
    }

    override fun setMute(muted: Boolean) {
        if (isReleased.get()) return
        runOnMain {
            exoPlayer.volume = if (muted) 0f else 1f
        }
    }

    private fun setAudioPitchCorrection(enabled: Boolean) {
        isPitchCorrectionEnabled = enabled
        if (isReleased.get()) return
        runOnMain {
            val speed = exoPlayer.playbackParameters.speed
            val pitch = if (enabled) 1.0f else speed
            exoPlayer.playbackParameters = PlaybackParameters(speed, pitch)
        }
    }

    override fun setUserAgent(userAgent: String?) {
        customUserAgent = userAgent
    }

    override fun setSpeed(speed: Double) {
        if (isReleased.get()) return
        runOnMain {
            val pitch = if (isPitchCorrectionEnabled) 1.0f else speed.toFloat()
            exoPlayer.playbackParameters = PlaybackParameters(speed.toFloat(), pitch)
        }
    }

    override fun setAudioDelay(delayMs: Long) {
        _events.tryEmit(EngineEvent.AudioDelayChanged(delayMs))
    }

    override fun setSubtitleDelay(delayMs: Long) {
        _events.tryEmit(EngineEvent.SubtitleDelayChanged(delayMs))
    }

    override fun setPreferredAudioLanguages(languages: List<String>) {
        if (isReleased.get()) return
        runOnMain {
            val validLanguages = languages.filter { it != "none" }
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setPreferredAudioLanguages(*validLanguages.toTypedArray())
                .build()
        }
    }

    override fun setPreferredSubtitleLanguages(languages: List<String>) {
        if (isReleased.get()) return
        runOnMain {
            val validLanguages = languages.filter { it != "none" && it.isNotBlank() }
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setPreferredTextLanguages(*validLanguages.toTypedArray())
                .build()
        }
    }

    override fun setSubtitleFallback(enabled: Boolean) {
        // Handled via trackSelectionParameters
    }

    override fun selectTrack(type: TrackType, id: Int) {
        if (isReleased.get()) return
        runOnMain {
            val cType = when (type) {
                TrackType.Audio -> C.TRACK_TYPE_AUDIO
                TrackType.Subtitle -> C.TRACK_TYPE_TEXT
                TrackType.Video -> C.TRACK_TYPE_VIDEO
            }

            val currentTracks = exoPlayer.currentTracks
            val trackGroups = currentTracks.groups.filter { it.type == cType }

            val builder = exoPlayer.trackSelectionParameters.buildUpon()

            if (id == -1) {
                builder.setTrackTypeDisabled(cType, true)
            } else if (id == -2) {
                builder.setTrackTypeDisabled(cType, false)
                builder.clearOverridesOfType(cType)
            } else {
                builder.setTrackTypeDisabled(cType, false)
                var count = 0
                outerLoop@ for (group in trackGroups) {
                    val mediaTrackGroup = group.mediaTrackGroup
                    for (i in 0 until mediaTrackGroup.length) {
                        val trackId = count + 1
                        if (trackId == id) {
                            builder.setOverrideForType(
                                TrackSelectionOverride(mediaTrackGroup, i)
                            )
                            break@outerLoop
                        }
                        count++
                    }
                }
            }

            exoPlayer.trackSelectionParameters = builder.build()
            fetchTrackList()
        }
    }

    override fun addSubtitle(subtitle: ExternalSubtitle) {
        addSubtitles(listOf(subtitle))
    }

    override fun addSubtitle(url: String, lang: String?) {
        addSubtitle(ExternalSubtitle(url = url, language = lang))
    }

    override fun addSubtitles(subtitles: List<ExternalSubtitle>) {
        if (isReleased.get() || subtitles.isEmpty()) return
        runOnMain {
            val validConfigs = subtitles.mapNotNull { sub ->
                val ext = sub.extension?.lowercase()
                    ?: ExternalSubtitle.extractExtension(sub.url)
                val mimeType = when {
                    ext == "srt" || sub.mimeType?.contains(
                        "subrip",
                        ignoreCase = true
                    ) == true -> MimeTypes.APPLICATION_SUBRIP

                    ext == "vtt" || ext == "webvtt" || sub.mimeType?.contains(
                        "vtt",
                        ignoreCase = true
                    ) == true -> MimeTypes.TEXT_VTT

                    ext == "ttml" || ext == "dfxp" || ext == "xml" || sub.mimeType?.contains(
                        "ttml",
                        ignoreCase = true
                    ) == true -> MimeTypes.APPLICATION_TTML

                    ext == "ass" || ext == "ssa" || sub.mimeType?.contains(
                        "ssa",
                        ignoreCase = true
                    ) == true -> MimeTypes.TEXT_SSA

                    ext == "smi" || ext == "sami" || sub.mimeType?.contains(
                        "sami",
                        ignoreCase = true
                    ) == true -> "application/x-sami"

                    else -> return@mapNotNull null
                }

                val subUri =
                    if (sub.url.startsWith("/")) Uri.fromFile(File(sub.url)) else sub.url.toUri()
                MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(mimeType)
                    .setLanguage(sub.language)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            }.filter { config ->
                externalSubtitles.none { existing -> existing.uri == config.uri }
            }

            if (validConfigs.isEmpty()) return@runOnMain

            externalSubtitles.addAll(validConfigs)

            val currentPosition = exoPlayer.currentPosition
            val isPlaying = exoPlayer.isPlaying

            val mainUri =
                if (currentMediaUrl.startsWith("/")) Uri.fromFile(File(currentMediaUrl)) else currentMediaUrl.toUri()
            val mediaItem = MediaItem.Builder()
                .setUri(mainUri)
                .setSubtitleConfigurations(externalSubtitles)
                .build()

            exoPlayer.setMediaItem(mediaItem, currentPosition)
            exoPlayer.prepare()
            if (isPlaying) exoPlayer.play()
        }
    }

    override fun sendKey(action: String, key: String) {
        // Key bindings are unavailable in ExoPlayerEngine
    }

    override fun command(vararg args: String) {
        // MPV commands are unavailable in ExoPlayerEngine
    }

    override fun stepFrame(forward: Boolean) {
        if (isReleased.get()) return
        runOnMain {
            val current = exoPlayer.currentPosition
            val offset = if (forward) 33L else -33L
            val position = (current + offset).coerceAtLeast(0L)
            exoPlayer.seekTo(position)
        }
    }

    override suspend fun takeScreenshot(path: String) {
        val bitmap = captureFrameBitmap() ?: return
        val compressFormat = when {
            path.endsWith(".png", ignoreCase = true) -> Bitmap.CompressFormat.PNG
            path.endsWith(".webp", ignoreCase = true) -> Bitmap.CompressFormat.WEBP_LOSSY
            else -> Bitmap.CompressFormat.JPEG
        }
        withContext(Dispatchers.IO) {
            try {
                FileOutputStream(File(path)).use { out ->
                    bitmap.compress(compressFormat, 90, out)
                }
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun grabThumbnail(dimension: Int): Any? {
        val bitmap = captureFrameBitmap() ?: return null
        return bitmap.scale(dimension, dimension, true)
    }

    private suspend fun captureFrameBitmap(): Bitmap? = withContext(Dispatchers.Main) {
        val surfaceView = activeSurfaceView
        val textureView = activeTextureView

        if (textureView != null) {
            runCatching {
                if (textureView.isAvailable) {
                    textureView.bitmap?.let { return@withContext it }
                }
            }
        }

        if (surfaceView != null) {
            val surface = surfaceView.holder.surface
            val videoSize = exoPlayer.videoSize
            val width =
                if (surfaceView.width > 0) surfaceView.width else (videoSize.width.takeIf { it > 0 }
                    ?: 1920)
            val height =
                if (surfaceView.height > 0) surfaceView.height else (videoSize.height.takeIf { it > 0 }
                    ?: 1080)

            return@withContext suspendCancellableCoroutine { continuation ->
                try {
                    val bitmap = createBitmap(width, height)
                    PixelCopy.request(
                        surfaceView,
                        bitmap,
                        { result ->
                            if (result == PixelCopy.SUCCESS) {
                                continuation.resume(bitmap)
                            } else if (surface != null && surface.isValid) {
                                PixelCopy.request(
                                    surface,
                                    bitmap,
                                    { surfaceResult ->
                                        if (surfaceResult == PixelCopy.SUCCESS) {
                                            continuation.resume(bitmap)
                                        } else {
                                            continuation.resume(null)
                                        }
                                    },
                                    mainHandler
                                )
                            } else {
                                continuation.resume(null)
                            }
                        },
                        mainHandler
                    )
                } catch (_: Exception) {
                    continuation.resume(null)
                }
            }
        }
        return@withContext null
    }

    private fun extractFpsFromExoPlayer(): Double {
        val formatFps = exoPlayer.videoFormat?.frameRate?.toDouble()
        if (formatFps != null && formatFps > 0.0) return formatFps

        val currentTracks = exoPlayer.currentTracks
        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val trackFormat = group.getTrackFormat(i)
                    if (trackFormat.frameRate > 0.0f) {
                        return trackFormat.frameRate.toDouble()
                    }
                }
            }
        }
        return 0.0
    }

    private fun extractCodecFromExoPlayer(): String? {
        val vf = exoPlayer.videoFormat
        if (!vf?.sampleMimeType.isNullOrBlank()) {
            return vf.sampleMimeType
        }
        val currentTracks = exoPlayer.currentTracks
        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val trackFormat = group.getTrackFormat(i)
                    if (!trackFormat.sampleMimeType.isNullOrBlank()) {
                        return trackFormat.sampleMimeType
                    }
                }
            }
        }
        return null
    }

    private fun extractBitrateFromExoPlayer(url: String?, durationMs: Long): Long {
        val vf = exoPlayer.videoFormat
        val formatBitrate = (vf?.bitrate?.takeIf { it > 0 }
            ?: vf?.peakBitrate?.takeIf { it > 0 }
            ?: vf?.averageBitrate?.takeIf { it > 0 })?.toLong()
        if (formatBitrate != null && formatBitrate > 0L) {
            return formatBitrate
        }

        val currentTracks = exoPlayer.currentTracks
        for (group in currentTracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val trackFormat = group.getTrackFormat(i)
                    val trackBitrate = (trackFormat.bitrate.takeIf { it > 0 }
                        ?: trackFormat.peakBitrate.takeIf { it > 0 }
                        ?: trackFormat.averageBitrate.takeIf { it > 0 })?.toLong()
                    if (trackBitrate != null && trackBitrate > 0L) {
                        return trackBitrate
                    }
                }
            }
        }

        if (durationMs > 0L && !url.isNullOrBlank()) {
            val path = when {
                url.startsWith("file://") -> url.toUri().path
                url.startsWith("/") -> url
                else -> null
            }
            if (path != null) {
                try {
                    val file = File(path)
                    if (file.exists() && file.length() > 0L) {
                        val fileSizeBits = file.length() * 8L
                        val durationSec = durationMs / 1000.0
                        if (durationSec > 0.0) {
                            return (fileSizeBits / durationSec).toLong()
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        return 0L
    }

    private fun fetchTrackList() {
        val audioTracks = mutableListOf<MediaTrack.Audio>()
        val subtitleTracks = mutableListOf<MediaTrack.Subtitle>()
        val videoTracks = mutableListOf<MediaTrack.Video>()

        val currentTracks = exoPlayer.currentTracks
        val durationMs = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L

        var audioId = 1
        var subId = 1
        var videoId = 1

        for (group in currentTracks.groups) {
            val type = when (group.type) {
                C.TRACK_TYPE_AUDIO -> TrackType.Audio
                C.TRACK_TYPE_TEXT -> TrackType.Subtitle
                C.TRACK_TYPE_VIDEO -> TrackType.Video
                else -> continue
            }

            val mediaTrackGroup = group.mediaTrackGroup
            for (i in 0 until mediaTrackGroup.length) {
                val format = mediaTrackGroup.getFormat(i)
                val isSelected = group.isTrackSelected(i)

                when (type) {
                    TrackType.Audio -> {
                        val id = audioId++
                        audioTracks.add(
                            MediaTrack.Audio(
                                id = id,
                                title = format.label ?: format.id ?: "Audio $id",
                                language = format.language,
                                isSelected = isSelected,
                                codec = format.codecs ?: format.sampleMimeType,
                                bitrate = format.bitrate.toLong().takeIf { it > 0 } ?: 0L,
                                channels = format.channelCount.takeIf { it > 0 } ?: 0,
                                sampleRate = format.sampleRate.takeIf { it > 0 } ?: 0
                            )
                        )
                    }

                    TrackType.Subtitle -> {
                        val id = subId++
                        val isExternal = externalSubtitles.isNotEmpty() && (
                            id <= externalSubtitles.size ||
                                externalSubtitles.any { subConfig ->
                                    subConfig.uri.toString() == format.id ||
                                        subConfig.uri.toString() == format.label ||
                                        subConfig.language == format.language
                                }
                            )
                        subtitleTracks.add(
                            MediaTrack.Subtitle(
                                id = id,
                                title = format.label ?: format.id ?: "Subtitle $id",
                                language = format.language,
                                isSelected = isSelected,
                                isExternal = isExternal,
                                codec = format.codecs ?: format.sampleMimeType
                            )
                        )
                    }

                    TrackType.Video -> {
                        val id = videoId++
                        val width = format.width.takeIf { it > 0 } ?: exoPlayer.videoSize.width
                        val height = format.height.takeIf { it > 0 } ?: exoPlayer.videoSize.height
                        val fps = format.frameRate.toDouble().takeIf { it > 0.0 } ?: extractFpsFromExoPlayer()
                        val codec = format.sampleMimeType ?: format.codecs ?: extractCodecFromExoPlayer()
                        val bitrate = format.bitrate.toLong().takeIf { it > 0 }
                            ?: format.peakBitrate.toLong().takeIf { it > 0 }
                            ?: extractBitrateFromExoPlayer(currentMediaUrl, durationMs)

                        videoTracks.add(
                            MediaTrack.Video(
                                id = id,
                                title = format.label ?: format.id ?: "Video $id",
                                language = format.language,
                                isSelected = isSelected,
                                codec = codec,
                                width = width,
                                height = height,
                                fps = fps,
                                bitrate = bitrate
                            )
                        )
                    }
                }
            }
        }

        if (audioTracks != lastAudioTracks) {
            lastAudioTracks = audioTracks
            _events.tryEmit(EngineEvent.AudioTracksChanged(audioTracks))
        }

        if (subtitleTracks != lastSubtitleTracks) {
            lastSubtitleTracks = subtitleTracks
            _events.tryEmit(EngineEvent.SubtitleTracksChanged(subtitleTracks))
        }

        if (videoTracks != lastVideoTracks) {
            lastVideoTracks = videoTracks
            _events.tryEmit(EngineEvent.VideoTracksChanged(videoTracks))
        }
    }

    override fun stop() {
        if (isReleased.get()) return
        runOnMain {
            exoPlayer.stop()
        }
    }

    override fun release() {
        if (isReleased.getAndSet(true)) return
        runOnMain {
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
        }
    }

    private fun createMediaSourceFactory(
        context: Context,
        cache: Cache
    ): DefaultMediaSourceFactory {
        val ktorClient = HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = currentConfig.networkTimeout
                connectTimeoutMillis = currentConfig.networkTimeout
                socketTimeoutMillis = currentConfig.networkTimeout
            }
            followRedirects = true
            install(Auth) {
                providers.add(UrlUserInfoAuthProvider())
            }
            defaultRequest {
                val defaultUa = currentConfig.userAgent.ifBlank { AppConstants.DEFAULT_USER_AGENT }
                val targetUa = customUserAgent?.takeIf { it.isNotBlank() } ?: defaultUa
                headers[HttpHeaders.UserAgent] = targetUa
            }
        }

        val factory = KtorDataSource.Factory(ktorClient)
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, factory)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return DefaultMediaSourceFactory(cacheDataSourceFactory)
    }
}
