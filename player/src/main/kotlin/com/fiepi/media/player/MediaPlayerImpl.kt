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

package com.fiepi.media.player

import android.content.Context
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.LoopMode
import com.fiepi.media.domain.model.preferences.PlaybackOptions
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.MediaPlayer
import com.fiepi.media.domain.player.MediaStreamServer
import com.fiepi.media.domain.player.PlayerEngine
import com.fiepi.media.domain.player.model.EngineEvent
import com.fiepi.media.domain.player.model.ExternalSubtitle
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.player.model.MediaMetadata
import com.fiepi.media.domain.player.model.PlayerClientType
import com.fiepi.media.domain.player.model.PlayerEngineType
import com.fiepi.media.domain.player.model.TrackType
import com.fiepi.media.domain.usecases.MediaPlayerUseCases
import com.fiepi.media.domain.utils.FileUtils
import com.fiepi.media.player.component.MediaSourceResolver
import com.fiepi.media.player.component.PlayerAudioManager
import com.fiepi.media.player.component.PlayerEngineManager
import com.fiepi.media.player.component.PlayerHistoryTracker
import com.fiepi.media.player.component.PlayerPlaylistManager
import com.fiepi.media.player.component.PlayerScreenshotHelper
import com.fiepi.media.player.component.PlayerSurfaceManager
import com.fiepi.media.player.component.PlayerTrackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

/**
 * MediaPlayerImpl is the core implementation class for player business logic using Facade + Component Composition.
 * MediaPlayerImpl acts as a central hub/mediator, orchestrating specialized component managers via callbacks and delegates.
 *
 * CRITICAL ARCHITECTURE RULES FOR AGENTS:
 * 1. LAZY ENGINE INITIALIZATION: Do NOT assign a default fallback engine type (such as MPV) to `currentEngineType`
 *    or `engineManager` before playback preferences are emitted by DataStore. `currentEngineType` MUST remain null initially.
 *    On cold start, engine creation must wait for `playbackOptionsFlow.first()` to create the exact target engine directly,
 *    preventing wasteful creation/destruction of MPV and eliminating engine switching black-screen glitches.
 * 2. ENGINE SWITCHING CONDITION: Hot-switching via `switchEngineIfNeeded` MUST only be triggered if an engine instance
 *    is ALREADY active (`previousEngineType` != null). Initial engine creation during cold start must never trigger hot-switching.
 * 3. ACTIVE SURFACE INHERITANCE: During engine hot-switching, `surfaceManager.detachOldEngineSurface` unbinds the old engine
 *    surface while strictly PRESERVING `surfaceManager.lastSurfaceObject`. The newly created engine automatically inherits
 *    and binds to the active surface. Never clear `lastSurfaceObject` during engine switch.
 * 4. CALLBACK DECOUPLING: Sub-components (e.g. [PlayerTrackManager], [PlayerSurfaceManager], [PlayerAudioManager]) must NEVER
 *    directly hold or accept [PlayerEngine] parameters. All engine operations must be routed via callbacks to [PlayerEngineManager].
 */
class MediaPlayerImpl(
    private val context: Context,
    private val streamServer: MediaStreamServer,
    private val engineFactory: (PlayerEngineType) -> PlayerEngine, // Factory function to create new engine instances
    private val useCases: MediaPlayerUseCases,
) : MediaPlayer {

    // Player internal coroutine scope bound to player lifecycle
    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Video loading/switching job; new tasks cancel old ones to prevent race conditions
    private var loadJob: Job? = null
    private var activePlaylistId: String? = null
    private var currentPlayingVideo: MediaFile.Video? = null

    // Internal mutable playback state flow
    private val _state = MutableStateFlow(MediaPlaybackState())

    // Read-only playback state flow exposed to ViewModel
    override val state: StateFlow<MediaPlaybackState> = _state.asStateFlow()

    private val engineManager: PlayerEngineManager = PlayerEngineManager(
        playerScope = playerScope,
        engineFactory = engineFactory,
        onEngineEvent = ::handleEngineEvent,
        onCapabilitiesChanged = { capabilities ->
            _state.update { it.copy(capabilities = capabilities) }
        },
        bindSurfaceToNewEngine = { _ ->
            surfaceManager.bindCachedSurfaceToEngine(playerScope)
        },
        unbindSurfaceFromOldEngine = { oldEngine ->
            surfaceManager.detachOldEngineSurface {
                oldEngine.setSurface(null)
            }
        }
    )

    private var playbackOptions = PlaybackOptions()

    // Current media source context
    private var currentSource: MediaSource? = null

    private var uiClients = 0
    private var bgClients = 0

    private val currentEngineType get() = engineManager.currentEngineType

    // Tracks whether playback was active before scrubbing
    private var wasPlayingBeforeScrubbing = false

    // Timestamp timer restricting preview seek frequency
    private var lastScrubbingSeekTime = 0L

    private val surfaceManager = PlayerSurfaceManager(
        onSetEngineSurface = { surface -> engineManager.setSurface(surface) }
    )
    private val mediaSourceResolver = MediaSourceResolver(
        context = context,
        streamServer = streamServer,
        getEngineType = { currentEngineType ?: playbackOptions.engineType }
    )
    private val historyTracker = PlayerHistoryTracker(
        getHistory = useCases.getHistory,
        saveHistory = useCases.saveHistory,
        getCurrentState = { _state.value },
        getCurrentSource = { currentSource }
    )
    private val playlistManager = PlayerPlaylistManager()
    private val audioManager = PlayerAudioManager(
        context = context,
        onAudioFocusLost = { pause() },
        onSetEngineVolume = { volume -> engineManager.setVolume(volume) }
    )
    private val trackManager = PlayerTrackManager(
        getEngineType = { currentEngineType ?: playbackOptions.engineType },
        onSelectTrack = { type, id -> engineManager.selectTrack(type, id) },
        onAddSubtitleToEngine = { subtitle -> engineManager.addSubtitle(subtitle) },
        onAddSubtitlesToEngine = { subtitles -> engineManager.addSubtitles(subtitles) },
        onAddSubtitleToState = { subtitleFile ->
            _state.update { currentState ->
                val currentPlaylist = currentState.playlist.toMutableList()
                val currentIndex = currentState.currentIndex
                if (currentIndex in currentPlaylist.indices) {
                    val currentVideo = currentPlaylist[currentIndex]
                    if (currentVideo.externalSubtitles.none { it.path == subtitleFile.path }) {
                        val updatedSubtitles = currentVideo.externalSubtitles + subtitleFile
                        currentPlaylist[currentIndex] =
                            currentVideo.copy(externalSubtitles = updatedSubtitles)
                        currentState.copy(playlist = currentPlaylist)
                    } else {
                        currentState
                    }
                } else {
                    currentState
                }
            }
        }
    )
    private val screenshotHelper = PlayerScreenshotHelper(
        context = context,
        takeEngineScreenshot = { filePath -> engineManager.takeScreenshot(filePath) }
    )

    // Whether player engine is ready (native instance initialization complete)
    override val isReady: StateFlow<Boolean> = engineManager.isReady

    override val events: Flow<EngineEvent> = engineManager.events

    // --- Preference Cache ---
    private val playbackOptionsFlow = useCases.getPlaybackOptions().shareIn(
        scope = playerScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )


    private fun updateShuffleQueueIfNeeded(forceReshuffle: Boolean = false) {
        playlistManager.updateShuffleQueueIfNeeded(
            playlistSize = _state.value.playlist.size,
            currentIndex = _state.value.currentIndex,
            forceReshuffle = forceReshuffle
        )
    }

    init {
        // Observe and sync business playback preferences and engine type
        playerScope.launch {
            playbackOptionsFlow.collect { options ->
                val previousLoopMode = playbackOptions.loopMode
                val previousAudioFocus = playbackOptions.requestAudioFocus
                val previousEngineType = currentEngineType
                playbackOptions = options

                if (options.loopMode != previousLoopMode &&
                    (options.loopMode == LoopMode.Shuffle || options.loopMode == LoopMode.ShuffleRepeat)
                ) {
                    updateShuffleQueueIfNeeded(forceReshuffle = true)
                }

                if (options.requestAudioFocus != previousAudioFocus) {
                    audioManager.handleAudioFocusPreferenceChanged(
                        enabled = options.requestAudioFocus,
                        isPlaying = _state.value.isPlaying,
                        onFocusDenied = { pause() }
                    )
                }

                // Hot-switch engine only if an engine is currently active and type was changed
                if (previousEngineType != null && previousEngineType != options.engineType) {
                    switchEngineIfNeeded(options.engineType)
                }
            }
        }

        // Initialize local UI volume state
        _state.update {
            it.copy(volumeState = audioManager.getInitialVolumeState(_state.value.capabilities.maxGainVolume))
        }
    }

    private suspend fun getOrInitEngine(
        type: PlayerEngineType? = currentEngineType
    ): PlayerEngine {
        engineManager.activeEngine?.let {
            return it
        }
        val options = playbackOptionsFlow.first()
        val targetType = type ?: options.engineType
        return engineManager.getOrInitEngine(targetType)
    }

    private suspend fun switchEngineIfNeeded(targetType: PlayerEngineType) {
        val state = _state.value
        val video = state.playlist.getOrNull(state.currentIndex)
        val isPlaying = state.isPlaying
        val switchPos = state.currentPosition

        engineManager.switchEngineIfNeeded(
            targetType = targetType,
            hasCurrentVideo = video != null,
            onPrepareSwitchState = {
                _state.update {
                    it.copy(
                        isLoaded = false,
                        hwdec = null,
                        isPlaying = isPlaying,
                        subtitleText = null,
                        currentPosition = switchPos,
                        targetSeekPosition = if (switchPos > 0) switchPos else null,
                        isSeeking = switchPos > 0,
                        audioTracks = emptyList(),
                        subtitleTracks = emptyList(),
                        videoTracks = emptyList()
                    )
                }
                trackManager.resetForNewVideo(
                    expectedSubtitlesCount = video?.externalSubtitles?.size ?: 0
                )
            },
            onLoadNewEngineVideo = { newEngine ->
                if (video != null) {
                    val playUrl = mediaSourceResolver.resolveMediaUrl(currentSource, video)
                    isReady.first { it }
                    newEngine.load(playUrl, switchPos)
                    if (isPlaying) engineManager.play() else engineManager.pause()
                }
            }
        )
    }

    // --- Event Handling Logic ---

    /**
     * Core handler for low-level events
     */
    private fun handleEngineEvent(event: EngineEvent) {
        when (event) {
            is EngineEvent.PositionChanged -> {
                val current = _state.value
                // [PREVENT 0 OVERWRITE] Update only after loading is complete, preventing instant 0 position on load from overwriting resume position
                if (!current.isLoaded) return

                if (current.isSeeking) {
                    val target = current.targetSeekPosition ?: 0L
                    // Allow 1-second margin of error to check if seek has physically arrived.
                    // Narrowing margin allows more precise capture of resume point.
                    if (abs(event.positionMs - target) < 1000L) {
                        _state.update {
                            it.copy(
                                isSeeking = false,
                                targetSeekPosition = null,
                                scrubbingPosition = null,
                                currentPosition = event.positionMs
                            )
                        }
                    }
                } else if (current.scrubbingPosition == null) {
                    // Regular progress update when not seeking or scrubbing
                    _state.update { it.copy(currentPosition = event.positionMs) }
                }
            }

            is EngineEvent.DurationChanged -> {
                // Duration filtering: prevents duration leftover from previous item from affecting loading state of new item
                if (_state.value.isLoaded) {
                    _state.update { it.copy(duration = event.durationMs) }
                }
            }

            is EngineEvent.FrameInfoChanged -> {
                _state.update {
                    it.copy(
                        currentFrame = event.current,
                        totalFrames = event.total
                    )
                }
            }

            is EngineEvent.PlaybackSpeedChanged -> _state.update { it.copy(playbackSpeed = event.speed) }

            is EngineEvent.AudioDelayChanged -> _state.update { it.copy(audioDelay = event.delayMs) }

            is EngineEvent.SubtitleDelayChanged -> _state.update { it.copy(subtitleDelay = event.delayMs) }

            is EngineEvent.SubtitleTextUpdated -> _state.update { it.copy(subtitleText = event.text) }

            is EngineEvent.HwdecChanged -> _state.update { it.copy(hwdec = event.hwdec) }

            is EngineEvent.VolumeChanged -> {
                // Sync software gain volume (0-50) returned from Native layer
                if (_state.value.gainVolume != event.gain) {
                    _state.update { it.copy(volumeState = it.volumeState.copy(gainVolume = event.gain)) }
                }
            }

            is EngineEvent.PlayPauseStateChanged -> {
                if (_state.value.isLoaded && _state.value.isPlaying != event.isPlaying) {
                    _state.update { it.copy(isPlaying = event.isPlaying) }
                }
            }

            is EngineEvent.BufferingStateChanged -> _state.update { it.copy(isBuffering = event.isBuffering) }

            is EngineEvent.BufferedPositionChanged -> {
                if (_state.value.isLoaded) {
                    _state.update { it.copy(bufferedPosition = event.bufferedPositionMs) }
                }
            }

            is EngineEvent.PlaybackEnded -> {
                if (_state.value.isLoaded) {
                    _state.update { it.copy(isEnded = true, isPlaying = false) }
                    handlePlaybackEnd()
                }
            }

            EngineEvent.FileLoaded -> {
                val wasLoaded = _state.value.isLoaded
                // Video file loaded successfully
                _state.update { it.copy(isLoaded = true, isEnded = false) }

                // Restore playback rate after restart/load
                engineManager.setSpeed(_state.value.playbackSpeed.toDouble())

                // [DELAYED EXTERNAL SUBTITLE LOADING]
                val video = currentPlayingVideo ?: _state.value.playlist.getOrNull(_state.value.currentIndex)
                val source = currentSource

                trackManager.loadExternalSubtitlesForVideo(
                    video = video,
                    source = source,
                    wasLoaded = wasLoaded,
                    mediaSourceResolver = mediaSourceResolver,
                    playerScope = playerScope
                )

                if (_state.value.isPlaying) engineManager.play() else engineManager.pause()
            }

            is EngineEvent.AudioTracksChanged -> {
                // Audio tracks updated, sync interlock state
                if (event.tracks.isNotEmpty() || !_state.value.isLoaded) {
                    _state.update { it.copy(audioTracks = event.tracks) }
                    trackManager.onAudioTracksChanged(event.tracks)
                }
            }

            is EngineEvent.SubtitleTracksChanged -> {
                if (event.tracks.isNotEmpty() || !_state.value.isLoaded) {
                    _state.update { it.copy(subtitleTracks = event.tracks) }
                    trackManager.onSubtitleTracksChanged(
                        tracks = event.tracks,
                        currentSource = currentSource,
                        playerScope = playerScope
                    )
                }
            }

            is EngineEvent.VideoTracksChanged -> {
                if (event.tracks.isNotEmpty() || !_state.value.isLoaded) {
                    _state.update { it.copy(videoTracks = event.tracks) }
                    trackManager.onVideoTracksChanged(event.tracks)
                    val selectedVideo = event.tracks.firstOrNull { it.isSelected } ?: event.tracks.firstOrNull()
                    if (selectedVideo != null) {
                        surfaceManager.applySurfaceFrameRate(selectedVideo.fps)
                    }
                }
            }

            EngineEvent.FileUnloaded -> {
                // When file is unloaded, reset only loading state, preserving track list until new video begins loading
                _state.update {
                    it.copy(
                        isLoaded = false,
                        bufferedPosition = 0L
                    )
                }
                surfaceManager.clearSurfaceFrameRate()
            }

            EngineEvent.EngineRestarted -> {
                val video = currentPlayingVideo ?: _state.value.playlist.getOrNull(_state.value.currentIndex)
                if (video != null) {
                    // If currently playing, automatically resume after engine restart
                    _state.update {
                        it.copy(
                            isLoaded = false,
                            audioTracks = emptyList(),
                            subtitleTracks = emptyList(),
                            videoTracks = emptyList()
                        )
                    }

                    // Reset track tracking state to ensure auto-selection logic is re-triggered after restart
                    trackManager.resetForNewVideo(expectedSubtitlesCount = video.externalSubtitles.size)

                    playerScope.launch {
                        val playUrl = mediaSourceResolver.resolveMediaUrl(currentSource, video)
                        val currentPos = _state.value.currentPosition
                        isReady.first { it }
                        engineManager.load(playUrl, currentPos)
                    }
                }
            }
        }
    }

    // --- Public API Implementations ---

    override fun load(url: String) {
        // Save current video progress
        historyTracker.triggerHistorySave(playerScope)

        val rawName = FileUtils.extractNameWithoutExtension(url)
        val decodedName = FileUtils.decodeUrl(rawName).ifBlank { rawName }

        val video = MediaFile.Video(
            path = url,
            name = decodedName,
            sourceType = SourceType.External,
            id = url,
            extension = FileUtils.extractExtension(url),
            externalSubtitles = emptyList()
        )
        setPlaylist(listOf(video), 0, MediaSource.External())
    }

    override fun load(video: MediaFile.Video, source: MediaSource) {
        setPlaylist(listOf(video), 0, source)
    }

    override fun setPlaylist(
        playlist: List<MediaFile.Video>,
        initialIndex: Int,
        source: MediaSource
    ) {
        // Before changing playlist, save progress of previous video (if present)
        historyTracker.triggerHistorySave(playerScope)

        activePlaylistId = null
        currentSource = source
        _state.update {
            it.copy(
                playlist = playlist,
                playlistId = null,
                totalPlaylistItems = 0,
                currentIndex = initialIndex,
                sourceId = source.id,
                isLoaded = false
            )
        }
        updateShuffleQueueIfNeeded(forceReshuffle = true)
        playAtIndex(initialIndex)
    }

    override fun playPlaylist(
        playlistId: String,
        initialMediaId: String?,
        initialIndex: Int
    ) {
        historyTracker.triggerHistorySave(playerScope)

        playerScope.launch {
            activePlaylistId = playlistId
            val totalCount = useCases.playlist.getPlaylistItemCount(playlistId)
            if (totalCount == 0) return@launch

            val targetIndex = if (!initialMediaId.isNullOrBlank()) {
                val foundIndex = useCases.playlist.getPlaylistItemIndexByMediaId(playlistId, initialMediaId)
                foundIndex ?: initialIndex
            } else {
                initialIndex
            }.coerceIn(0 until totalCount)

            _state.update {
                it.copy(
                    playlist = emptyList(),
                    playlistId = playlistId,
                    totalPlaylistItems = totalCount,
                    currentIndex = targetIndex,
                    isLoaded = false
                )
            }
            updateShuffleQueueIfNeeded(forceReshuffle = true)
            playAtIndex(targetIndex)
        }
    }

    override fun playNext() {
        playerScope.launch {
            val currentPlaylistId = activePlaylistId
            val playlistSize = if (currentPlaylistId != null) {
                useCases.playlist.getPlaylistItemCount(currentPlaylistId)
            } else {
                _state.value.playlist.size
            }
            if (playlistSize == 0) return@launch

            val nextIndex = playlistManager.getNextIndex(
                loopMode = playbackOptions.loopMode,
                playlistSize = playlistSize,
                currentIndex = _state.value.currentIndex
            )
            if (nextIndex != null) {
                historyTracker.triggerHistorySave(playerScope)
                playAtIndex(nextIndex)
            }
        }
    }

    override fun playPrevious() {
        playerScope.launch {
            val currentPlaylistId = activePlaylistId
            val playlistSize = if (currentPlaylistId != null) {
                useCases.playlist.getPlaylistItemCount(currentPlaylistId)
            } else {
                _state.value.playlist.size
            }
            if (playlistSize == 0) return@launch

            val prevIndex = playlistManager.getPreviousIndex(
                loopMode = playbackOptions.loopMode,
                playlistSize = playlistSize,
                currentIndex = _state.value.currentIndex
            )
            if (prevIndex != null) {
                historyTracker.triggerHistorySave(playerScope)
                playAtIndex(prevIndex)
            }
        }
    }

    override fun playAtIndex(index: Int) {
        val currentPlaylistId = activePlaylistId

        loadJob?.cancel()
        loadJob = playerScope.launch {
            val video = if (currentPlaylistId != null) {
                useCases.playlist.getPlaylistItemByIndex(currentPlaylistId, index)?.media
            } else {
                _state.value.playlist.getOrNull(index)
            } ?: return@launch

            currentPlayingVideo = video

            // [ENGINE REUSE LOGIC]
            // If an engine exists, reuse it; otherwise initialize a new engine.
            val engine = getOrInitEngine()

            // Disable current video track before loading new video to assist clearing leftover frames.
            // Pairs with PlayerScreen black mask to provide a smooth transition experience.
            engine.selectTrack(TrackType.Video, -1)

            val resolved = runCatching {
                mediaSourceResolver.resolveSourceAndPlayUrl(
                    video = video,
                    currentSource = currentSource,
                    getMediaSource = { id, path -> useCases.getMediaSource(id, path) }
                )
            }.getOrNull()

            if (resolved == null) {
                // If resolving media source failed (e.g. source removed or offline), auto skip to next item
                playNext()
                return@launch
            }

            val (source, playUrl) = resolved
            currentSource = source

            var initialPos = 0L
            var duration = video.duration ?: 0L

            // Query playback history using the video's specific sourceId and path
            val effectiveSourceId = video.sourceId ?: source.id
            if (source !is MediaSource.External && playbackOptions.resumePlayback) {
                val (resumePos, resolvedDuration) = historyTracker.getResumePosition(
                    effectiveSourceId,
                    video.path,
                    duration
                )
                initialPos = resumePos
                duration = resolvedDuration
            }

            // Update UI state with basic info of new video
            _state.update {
                it.copy(
                    currentIndex = index,
                    sourceId = effectiveSourceId,
                    isLoaded = false,
                    isEnded = false,
                    isPlaying = true,
                    scrubbingPosition = null,
                    subtitleText = null,
                    // If progress resume is needed, pre-enable isSeeking state.
                    // Forces handleEngineEvent to ignore 0 progress reported by mpv during initial load.
                    targetSeekPosition = if (initialPos > 0) initialPos else null,
                    isSeeking = initialPos > 0,
                    mediaMetadata = MediaMetadata(
                        mediaId = video.id,
                        title = video.name,
                        subtitle = video.name, // Temporary subtitle, can be passed by business layer later
                        artworkUri = video.thumbnailUrl
                    ),
                    duration = duration,
                    currentPosition = initialPos,
                    audioTracks = emptyList(),
                    subtitleTracks = emptyList(),
                    videoTracks = emptyList()
                )
            }
            updateShuffleQueueIfNeeded()

            // Initialize track tracking variables for this playback session
            trackManager.resetForNewVideo(expectedSubtitlesCount = video.externalSubtitles.size)

            // Apply custom User-Agent if specified by media file metadata
            engineManager.setUserAgent(video.userAgent)

            // Wait for engine to be ready and load
            isReady.first { it }
            engine.load(playUrl, initialPos)

            play()
        }
    }

    override fun play() {
        if (audioManager.requestAudioFocus(playbackOptions.requestAudioFocus)) {
            _state.update { it.copy(isPlaying = true, isEnded = false) }
            engineManager.play()
        } else {
            _state.update { it.copy(isPlaying = false) }
        }
    }

    override fun pause() {
        _state.update { it.copy(isPlaying = false) }
        historyTracker.triggerHistorySave(playerScope)
        engineManager.pause()
        audioManager.abandonAudioFocus()
    }

    override fun togglePlay() {
        val current = _state.value
        val currentPos = current.scrubbingPosition ?: current.currentPosition
        val shouldRestart =
            (current.isEnded || (current.duration > 0 && (currentPos + 200L) >= current.duration))

        if (shouldRestart) {
            seekTo(0)
            play()
        } else {
            if (current.isPlaying) pause() else play()
        }
    }

    override fun seekTo(positionMs: Long) {
        performInternalSeek(positionMs)
    }

    /**
     * Performs internal seek, including optimistic UI update logic
     */
    private fun performInternalSeek(positionMs: Long) {
        val duration = _state.value.duration
        val safePos =
            if (duration > 0) positionMs.coerceIn(0, duration) else positionMs.coerceAtLeast(0)

        _state.update {
            it.copy(
                isSeeking = true,
                targetSeekPosition = safePos,
                // [OPTIMISTIC UPDATE] Reflect on UI immediately, preventing auto-saves triggered at this moment from saving old progress (or 0)
                currentPosition = safePos,
                isEnded = false
            )
        }
        engineManager.seekTo(safePos)
    }

    override fun seekBy(offsetMs: Long): Boolean {
        val current = _state.value.scrubbingPosition ?: _state.value.currentPosition
        val duration = _state.value.duration
        if (duration <= 0 && offsetMs > 0) return false

        val target = (current + offsetMs).coerceIn(0, duration)
        if (offsetMs > 0 && (target + 200L) >= duration) return false
        if (offsetMs < 0 && current <= target) return false

        seekTo(target)
        return true
    }

    override fun onScrubbing(positionMs: Long) {
        val currentPlayback = _state.value
        if (currentPlayback.scrubbingPosition == null) {
            wasPlayingBeforeScrubbing = currentPlayback.isPlaying
            // Pause when starting scrub to avoid noise
            engineManager.pause()
        }
        _state.update { it.copy(scrubbingPosition = positionMs) }

        // [SCRUB PREVIEW] Throttle seek (~6.6 fps) to ensure smoothness while reducing SMB network/engine stress
        val now = System.currentTimeMillis()
        if (now - lastScrubbingSeekTime >= 150L) {
            lastScrubbingSeekTime = now
            engineManager.seekTo(positionMs)
        }
    }

    override fun onScrubbingFinished(positionMs: Long) {
        _state.update { it.copy(scrubbingPosition = null) }
        seekTo(positionMs)
        if (wasPlayingBeforeScrubbing) play() else pause()
    }

    // --- Volume & Performance ---

    override fun setSystemVolume(volume: Int) {
        audioManager.setSystemVolume(
            systemVol = volume,
            playerScope = playerScope
        ) { systemVol ->
            _state.update {
                it.copy(
                    volumeState = it.volumeState.copy(systemVolume = systemVol)
                )
            }
        }
    }

    override fun setGainVolume(gain: Int) {
        playerScope.launch {
            val currentAudio = useCases.audioPrefs.getAudioOptions().first()
            val engineType = currentEngineType ?: playbackOptions.engineType
            val maxGain = if (engineType == PlayerEngineType.EXO_PLAYER) 100 else 200
            val cappedGain = gain.coerceIn(0, maxGain)
            val updatedAudio = if (engineType == PlayerEngineType.EXO_PLAYER) {
                currentAudio.copy(exoVolume = cappedGain)
            } else {
                currentAudio.copy(mpvVolume = cappedGain)
            }
            useCases.audioPrefs.updateAudioOptions(updatedAudio)
        }
    }

    override fun setMute(muted: Boolean) {
        engineManager.setMute(muted)
    }

    override fun adjustVolume(up: Boolean) {
        val current = _state.value
        audioManager.adjustVolume(
            up = up,
            currentSystemVol = current.systemVolume,
            playerScope = playerScope
        ) { systemVol ->
            _state.update {
                it.copy(
                    volumeState = it.volumeState.copy(systemVolume = systemVol)
                )
            }
        }
    }

    override fun setPlaybackRate(speed: Float) {
        _state.update { it.copy(playbackSpeed = speed) }
        engineManager.setSpeed(speed.toDouble())
    }

    override fun setAudioDelay(delayMs: Long) {
        _state.update { it.copy(audioDelay = delayMs) }
        engineManager.setAudioDelay(delayMs)
    }

    override fun setSubtitleDelay(delayMs: Long) {
        _state.update { it.copy(subtitleDelay = delayMs) }
        engineManager.setSubtitleDelay(delayMs)
    }

    override fun selectTrack(type: TrackType, id: Int) {
        trackManager.selectTrack(type, id)
    }

    override fun addSubtitle(url: String) {
        trackManager.addSubtitle(
            url = url,
            mediaSourceResolver = mediaSourceResolver,
            playerScope = playerScope
        )
    }

    override fun addSubtitles(subtitles: List<ExternalSubtitle>) {
        trackManager.addSubtitles(
            subtitles = subtitles,
            mediaSourceResolver = mediaSourceResolver,
            playerScope = playerScope
        )
    }

    override fun sendKey(action: String, key: String) {
        engineManager.sendKey(action, key)
    }

    override fun stepFrame(forward: Boolean) {
        engineManager.stepFrame(forward)
    }

    override fun command(vararg args: String) {
        engineManager.command(*args)
    }

    override suspend fun takeScreenshot(): File? {
        val playerOptions = useCases.playerPrefs.getPlayerOptions().first()
        return screenshotHelper.takeScreenshot(
            isLoaded = _state.value.isLoaded,
            engineType = currentEngineType ?: playbackOptions.engineType,
            format = playerOptions.screenshotFormat
        )
    }

    override fun setSurface(surface: Any) {
        surfaceManager.setSurface(
            surface = surface,
            getCurrentFps = { _state.value.selectedVideoTrack?.fps },
            playerScope = playerScope
        )
    }

    override fun detachSurface(surface: Any) {
        surfaceManager.detachSurface(
            surface = surface,
            playerScope = playerScope
        )
    }

    override fun setSurfaceSize(width: Int, height: Int) {
        engineManager.setSurfaceSize(width, height)
    }

    // --- Lifecycle & Exit ---

    override fun incrementClientCount(type: PlayerClientType) {
        if (type == PlayerClientType.UI) uiClients++ else bgClients++
    }

    override fun decrementClientCount(type: PlayerClientType) {
        if (type == PlayerClientType.UI) uiClients-- else bgClients--

        // When all UI clients leave, force detach Surface and disable video track.
        // Ensures obsolete physical Surface is released immediately even if Service plays audio in background.
        if (type == PlayerClientType.UI && uiClients <= 0) {
            playerScope.launch {
                surfaceManager.clearSurfaceAndDetach()
                engineManager.selectTrack(TrackType.Video, -1)
            }
        }

        if (uiClients <= 0 && bgClients <= 0) {
            release()
        }
    }

    override fun release() {
        // Save current video progress
        historyTracker.triggerHistorySave(playerScope)

        // Reset business state immediately
        _state.update { it.copy(isLoaded = false, isPlaying = false, isEnded = false) }

        audioManager.abandonAudioFocus()
        playlistManager.reset()
        streamServer.clearAllSessions()
        streamServer.stop()

        currentSource = null
        loadJob?.cancel()

        engineManager.releaseEngine { engineToRelease ->
            val releaseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            releaseScope.launch {
                // [ULTIMATE REINFORCEMENT] Prior to destroying Native instance, unbinding must execute mutably/exclusively.
                // Ensures old instance vo=null command never executes later on the timeline than new instance vo=gpu.
                surfaceManager.clearSurfaceAndDetach()
                engineToRelease?.release()
            }
        }
    }

    private fun handlePlaybackEnd() {
        val state = _state.value
        val video = currentPlayingVideo ?: state.playlist.getOrNull(state.currentIndex)
        val source = currentSource

        playerScope.launch {
            // Playback ended, force reset progress to 0 (mark completed), and set completed flag
            if (video != null && source != null) {
                historyTracker.saveHistoryImmediate(
                    forceReset = true,
                    video = video,
                    targetSource = source,
                    isCompleted = true
                )
            }

            // Handle loop mode
            when (playbackOptions.loopMode) {
                LoopMode.RepeatCurrent -> {
                    performInternalSeek(0)
                    play()
                }

                LoopMode.Sequential -> {
                    val nextIndex = _state.value.currentIndex + 1
                    if (nextIndex < _state.value.playlist.size) {
                        playNext()
                    }
                }

                LoopMode.Shuffle -> {
                    playlistManager.updateShuffleQueueIfNeeded(
                        _state.value.playlist.size,
                        _state.value.currentIndex
                    )
                    if (playlistManager.shufflePointer + 1 < playlistManager.shuffledIndices.size) {
                        playNext()
                    }
                }

                LoopMode.RepeatList,
                LoopMode.ShuffleRepeat -> playNext()

                else -> {}
            }
        }
    }
}
