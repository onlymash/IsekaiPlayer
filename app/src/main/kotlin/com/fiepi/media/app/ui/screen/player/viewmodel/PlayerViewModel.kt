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

package com.fiepi.media.app.ui.screen.player.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fiepi.media.app.R
import com.fiepi.media.app.service.NotificationController
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.playlist.PlaylistItem
import com.fiepi.media.domain.model.preferences.ExoSubtitleOptions
import com.fiepi.media.domain.model.preferences.LoopMode
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.preferences.PlaybackOptions
import com.fiepi.media.domain.model.preferences.PlayerUiOptions
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.player.MediaPlayer
import com.fiepi.media.domain.player.model.PlayerClientType
import com.fiepi.media.domain.usecases.PlayerUseCases
import com.fiepi.media.domain.utils.FileUtils
import com.fiepi.media.player.model.PlayerSurfaceEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * PlayerViewModel coordinates state flow between MediaPlayer and Compose UI.
 * It bridges business logic and UI layer; all UI actions are sent via Intent, state observed via StateFlow.
 *
 * Design Principles:
 * 1. Single Source of Truth: Playback state (progress, duration, tracks) is always aggregated from player.state.
 * 2. MVI Pattern: Drives all state changes via sendIntent.
 * 3. Cross-module Decoupling: UI layer does not directly depend on lower-level MPV types; uses intermediate objects like PlayerSurfaceEvent.
 */
class PlayerViewModel(
    private val application: Application,
    private val player: MediaPlayer,
    private val useCases: PlayerUseCases,
    private val notificationController: NotificationController
) : ViewModel() {

    // Local state (pure UI state maintained by ViewModel, e.g., panel visibility, brightness, temporary notifications)
    private val _uiState = MutableStateFlow(UIState())
    private val _playbackOptions = MutableStateFlow(PlaybackOptions())
    private val _playerUiOptions = MutableStateFlow(PlayerUiOptions())
    private val _exoSubtitleOptions = MutableStateFlow(ExoSubtitleOptions())

    val uiState get() = _uiState.value
    val playbackOptions get() = _playbackOptions.value
    val playerUiOptions get() = _playerUiOptions.value

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistItemPagingFlow: Flow<PagingData<PlaylistItem>> = player.state
        .map { it.playlistId }
        .distinctUntilChanged()
        .flatMapLatest { playlistId ->
            if (playlistId == null) {
                flowOf(PagingData.empty())
            } else {
                val initialIndex = player.state.value.currentIndex
                useCases.playlist.getPlaylistItemPaging(
                    playlistId = playlistId,
                    initialKey = if (initialIndex >= 0) initialIndex else null
                )
            }
        }
        .cachedIn(viewModelScope)

    // Aggregate business player state and UI options state
    val state: StateFlow<PlayerState> = combine(
        _uiState,
        _playerUiOptions,
        _playbackOptions,
        _exoSubtitleOptions,
        player.state
    ) { ui, uiOpts, playbackOpts, subOpts, playback ->
        PlayerState(
            playback = playback,
            ui = ui,
            uiOptions = uiOpts,
            playbackOptions = playbackOpts,
            exoSubtitleOptions = subOpts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerState())

    private val _effect = Channel<PlayerEffect>(Channel.BUFFERED)
    val effect: Flow<PlayerEffect> = _effect.receiveAsFlow()

    private var autoHideControlsJob: Job? = null
    private var clearStatusJob: Job? = null
    private var hideQuickBarJob: Job? = null
    private var autoExitScreenshotJob: Job? = null

    init {
        // Increment player client count
        player.incrementClientCount(PlayerClientType.UI)

        // Observe preference options
        viewModelScope.launch {
            useCases.getPlaybackOptions().collect { _playbackOptions.value = it }
        }
        viewModelScope.launch {
            useCases.getSubtitleOptions().collect { _exoSubtitleOptions.value = it.exoSubtitle }
        }
        viewModelScope.launch {
            useCases.getPlayerUiOptions().collect { options ->
                _playerUiOptions.value = options
                _uiState.update {
                    it.copy(
                        isOsdVisible = options.isOsdVisible,
                        currentOsdPage = options.osdPage
                    )
                }
            }
        }

        // Load and apply initial brightness
        viewModelScope.launch {
            if (useCases.getPlayerUiOptions().first().rememberBrightness) {
                useCases.getBrightness().first().let { lastBrightness ->
                    if (lastBrightness >= 0) {
                        _uiState.update { it.copy(brightness = lastBrightness) }
                        _effect.send(PlayerEffect.SetWindowBrightness(lastBrightness))
                    }
                }
            }
        }

        // Dynamically manage screen keep-awake: stay awake only during playback
        viewModelScope.launch {
            player.state.map { it.isPlaying }.distinctUntilChanged().collect { isPlaying ->
                _effect.send(PlayerEffect.KeepScreenOn(isPlaying))

                // Auto-exit screenshot mode optimization: added debouncing
                if (isPlaying && _uiState.value.controlMode == PlayerControlMode.Screenshot) {
                    // If video starts playing, launch a short delayed check task
                    // Filters out transient play states caused by frame-stepping
                    autoExitScreenshotJob?.cancel()
                    autoExitScreenshotJob = launch {
                        delay(300.milliseconds)
                        if (player.state.value.isPlaying && _uiState.value.controlMode == PlayerControlMode.Screenshot) {
                            sendIntent(PlayerIntent.SetControlMode(PlayerControlMode.Normal))
                        }
                    }
                } else if (!isPlaying) {
                    // If video is paused, cancel any pending exit task
                    autoExitScreenshotJob?.cancel()
                }
            }
        }
    }

    /**
     * Core MVI method: handles all Intents from UI
     */
    fun sendIntent(intent: PlayerIntent) {
        // Refresh control panel auto-hide schedule for any user interaction except background transition
        if (intent !is PlayerIntent.EnterBackground && intent !is PlayerIntent.HandleSurfaceEvent) {
            scheduleAutoHideControls()
        }

        when (intent) {
            is PlayerIntent.LoadMedia -> loadMedia(intent.sourceId, intent.videoPath)
            is PlayerIntent.LoadPlaylist -> player.playPlaylist(
                intent.playlistId,
                intent.initialMediaId,
                intent.initialIndex
            )

            is PlayerIntent.PlayAtIndex -> player.playAtIndex(intent.index)
            is PlayerIntent.LoadVideo -> player.load(intent.uri.toString())

            is PlayerIntent.HandleSurfaceEvent -> {
                when (val event = intent.event) {
                    is PlayerSurfaceEvent.Created -> {
                        _uiState.update { it.copy(isSurfaceReady = true) }
                        player.setSurface(event.surface)
                        player.setSurfaceSize(event.width, event.height)
                    }

                    is PlayerSurfaceEvent.Changed -> {
                        player.setSurfaceSize(event.width, event.height)
                    }

                    is PlayerSurfaceEvent.Destroyed -> {
                        _uiState.update { it.copy(isSurfaceReady = false) }
                        player.detachSurface(event.surface)
                    }
                }
            }

            PlayerIntent.EnterBackground -> {
                // When onStop triggers (entering background or screen lock):
                // If user manually clicked "Background Audio", allow playback regardless of global setting
                // Otherwise, follow global settings to decide whether to pause
                if (!uiState.isEnteringBackgroundAudioManually && !playerUiOptions.enableBackgroundAudio) {
                    player.pause()
                }
                // Reset flag after executing background entry logic
                _uiState.update { it.copy(isEnteringBackgroundAudioManually = false) }
            }

            PlayerIntent.TogglePlay -> player.togglePlay()
            is PlayerIntent.SetPlay -> if (intent.play) player.play() else player.pause()
            is PlayerIntent.SetPlaybackRate -> {
                _uiState.update {
                    it.copy(
                        isFastForwarding = intent.isTemporary
                    )
                }
                if (intent.persist) {
                    viewModelScope.launch {
                        val current = useCases.playerPrefs.getPlayerOptions().first()
                        useCases.playerPrefs.updatePlayerOptions(current.copy(playbackSpeed = intent.speed))
                    }
                } else {
                    player.setPlaybackRate(intent.speed)
                }
            }

            is PlayerIntent.SetSystemVolume -> {
                showVolumeBar()
                player.setSystemVolume(intent.volume)
            }

            is PlayerIntent.SetGainVolume -> {
                showVolumeBar()
                player.setGainVolume(intent.gain)
            }

            is PlayerIntent.AdjustVolume -> {
                showVolumeBar()
                player.adjustVolume(intent.up)
            }

            is PlayerIntent.SetBrightness -> setBrightness(intent.brightness)

            is PlayerIntent.SetVideoScaleMode -> {
                viewModelScope.launch {
                    val current = useCases.playerPrefs.getPlayerOptions().first()
                    useCases.playerPrefs.updatePlayerOptions(current.copy(videoScaleMode = intent.mode))
                }
            }

            is PlayerIntent.SeekTo -> player.seekTo(intent.positionMs)
            is PlayerIntent.SeekBy -> {
                if (player.seekBy(intent.offsetMs)) {
                    val seconds = (abs(intent.offsetMs) / 1000).toInt()
                    showNotification(
                        if (intent.offsetMs > 0) PlayerNotification.SeekForward(seconds)
                        else PlayerNotification.SeekBackward(seconds)
                    )
                }
            }

            is PlayerIntent.SetAudioDelay -> {
                if (intent.persist) {
                    viewModelScope.launch {
                        val current = useCases.audioPrefs.getAudioOptions().first()
                        useCases.audioPrefs.updateAudioOptions(current.copy(audioDelay = intent.delayMs))
                    }
                } else {
                    player.setAudioDelay(intent.delayMs)
                }
            }

            is PlayerIntent.SetSubtitleDelay -> player.setSubtitleDelay(intent.delayMs)

            is PlayerIntent.SelectTrack -> player.selectTrack(intent.type, intent.id)
            is PlayerIntent.AddSubtitle -> addSubtitle(intent.uri)

            is PlayerIntent.OnScrubbing -> player.onScrubbing(intent.positionMs)
            is PlayerIntent.OnScrubbingFinished -> player.onScrubbingFinished(intent.positionMs)

            PlayerIntent.ToggleControls -> {
                if (uiState.quickBarMode != QuickBarMode.None) {
                    setControlsVisible(true)
                } else {
                    setControlsVisible(!uiState.isControlsVisible)
                }
            }

            is PlayerIntent.SetControlsVisible -> setControlsVisible(intent.visible)

            is PlayerIntent.SetPiPMode -> _uiState.update { it.copy(isPiP = intent.active) }
            is PlayerIntent.SetActiveDrawer -> _uiState.update { it.copy(activeDrawer = intent.type) }
            is PlayerIntent.SetBackgroundAudio -> {
                viewModelScope.launch {
                    val currentAudio = useCases.audioPrefs.getAudioOptions().first()
                    useCases.audioPrefs.updateAudioOptions(currentAudio.copy(enableBackgroundAudio = intent.enabled))
                }
            }

            PlayerIntent.ToggleLock -> toggleLock()
            PlayerIntent.ToggleLoopMode -> {
                viewModelScope.launch {
                    val current = useCases.playerPrefs.getPlayerOptions().first()
                    val modes = LoopMode.entries
                    val nextIndex = (modes.indexOf(current.loopMode) + 1) % modes.size
                    useCases.playerPrefs.updatePlayerOptions(current.copy(loopMode = modes[nextIndex]))
                }
            }

            PlayerIntent.NotifyUserInteraction -> scheduleAutoHideControls()
            PlayerIntent.ExitPlayer -> player.release()

            PlayerIntent.TogglePiP -> viewModelScope.launch { _effect.send(PlayerEffect.TriggerEnterPiP) }
            PlayerIntent.EnterBackgroundAudio -> {
                _uiState.update { it.copy(isEnteringBackgroundAudioManually = true) }
                viewModelScope.launch {
                    _effect.send(PlayerEffect.TriggerEnterBackgroundAudio)
                }
            }

            PlayerIntent.PlayNext -> player.playNext()
            PlayerIntent.PlayPrevious -> player.playPrevious()

            is PlayerIntent.SetControlMode -> {
                val oldMode = _uiState.value.controlMode
                val newMode = intent.mode

                if (oldMode == newMode) return

                // Side effects on exiting old mode
                if (oldMode == PlayerControlMode.Screenshot) {
                    player.setMute(false)
                }

                // Side effects on entering new mode
                when (newMode) {
                    PlayerControlMode.Screenshot -> {
                        player.pause()
                        player.setMute(true)
                        _uiState.update {
                            it.copy(
                                controlMode = newMode,
                                isControlsVisible = false
                            )
                        }
                        // Sync frame info immediately upon entering mode
                        viewModelScope.launch {
                            player.stepFrame(true) // Trigger a slight step to refresh underlying properties
                            player.stepFrame(false)
                        }
                    }

                    PlayerControlMode.PlaybackSpeed -> {
                        _uiState.update {
                            it.copy(
                                controlMode = newMode,
                                isControlsVisible = false
                            )
                        }
                    }

                    PlayerControlMode.AudioDelay -> {
                        _uiState.update {
                            it.copy(
                                controlMode = newMode,
                                isControlsVisible = false
                            )
                        }
                    }

                    PlayerControlMode.SubtitleDelay -> {
                        _uiState.update {
                            it.copy(
                                controlMode = newMode,
                                isControlsVisible = false
                            )
                        }
                    }

                    PlayerControlMode.Normal -> {
                        _uiState.update { it.copy(controlMode = newMode, isControlsVisible = true) }
                    }
                }
            }

            is PlayerIntent.StepFrame -> {
                player.stepFrame(intent.forward)
            }

            is PlayerIntent.SetOsdVisible -> {
                viewModelScope.launch {
                    val current = useCases.playerPrefs.getPlayerOptions().first()
                    useCases.playerPrefs.updatePlayerOptions(current.copy(isOsdVisible = intent.visible))
                }
            }

            is PlayerIntent.SetOsdPage -> {
                viewModelScope.launch {
                    val current = useCases.playerPrefs.getPlayerOptions().first()
                    useCases.playerPrefs.updatePlayerOptions(current.copy(osdPage = intent.page))
                }
            }

            PlayerIntent.TakeScreenshot -> {
                if (Environment.isExternalStorageManager()) {
                    viewModelScope.launch {
                        val file = player.takeScreenshot()
                        if (file != null) {
                            notificationController.showScreenshotNotification(
                                application,
                                file,
                                file.name
                            )
                        }
                    }
                } else {
                    viewModelScope.launch {
                        _effect.send(PlayerEffect.ShowToast(application.getString(R.string.error_permission_denied_storage)))
                    }
                }
            }

            is PlayerIntent.ExecuteCommand -> player.command(*intent.args.split(" ").toTypedArray())

            is PlayerIntent.OnKeyEvent -> player.sendKey(intent.action, intent.mpvKey)
            is PlayerIntent.OnScrollEvent -> {
                intent.commands.forEach { player.sendKey(it.first, it.second) }
            }

            PlayerIntent.ClearStatus -> {
                clearStatusJob?.cancel()
                _uiState.update { it.copy(activeNotification = null) }
            }
        }
    }

    private fun showVolumeBar() {
        autoHideControlsJob?.cancel()
        autoHideControlsJob = null
        _uiState.update {
            it.copy(
                quickBarMode = QuickBarMode.Volume,
                isControlsVisible = false
            )
        }
        hideQuickBarJob?.cancel()
        hideQuickBarJob = viewModelScope.launch {
            delay(2.seconds)
            _uiState.update { it.copy(quickBarMode = QuickBarMode.None) }
        }
    }

    private fun showBrightnessBar() {
        autoHideControlsJob?.cancel()
        autoHideControlsJob = null
        _uiState.update {
            it.copy(
                quickBarMode = QuickBarMode.Brightness,
                isControlsVisible = false
            )
        }
        hideQuickBarJob?.cancel()
        hideQuickBarJob = viewModelScope.launch {
            delay(3.seconds)
            _uiState.update { it.copy(quickBarMode = QuickBarMode.None) }
        }
    }

    private fun setBrightness(value: Float) {
        val capped = value.coerceIn(0f, 1f)
        _uiState.update { it.copy(brightness = capped) }
        showBrightnessBar()
        viewModelScope.launch {
            _effect.send(PlayerEffect.SetWindowBrightness(capped))
            useCases.saveBrightness(capped)
        }
    }

    private fun showNotification(type: PlayerNotification) {
        clearStatusJob?.cancel()
        _uiState.update { it.copy(activeNotification = type) }
        clearStatusJob = viewModelScope.launch {
            delay(1.seconds)
            _uiState.update { it.copy(activeNotification = null) }
        }
    }

    /**
     * Business load logic: resolves full playlist from sourceId and videoPath and submits to player.
     * Safely handles network and DNS failures (such as IPv6-only domains in IPv4 environments).
     */
    private fun loadMedia(sourceId: String, videoPath: String) {
        viewModelScope.launch {
            try {
                val mediaSource = useCases.getMediaSource(sourceId) ?: run {
                    _effect.send(PlayerEffect.FinishPlayer)
                    return@launch
                }
                val parentPath = videoPath.substringBeforeLast('/', "")
                val sortOptions = runCatching {
                    useCases.getSortOptions(mediaSource is MediaSource.Remote).first()
                }.getOrDefault(MediaOptions())

                val allFiles = runCatching {
                    useCases.getMediaFiles(parentPath, mediaSource, sortOptions)
                }.getOrDefault(emptyList())

                val playlist = allFiles.filterIsInstance<MediaFile.Video>()
                val currentIndex = playlist.indexOfFirst { it.path == videoPath }

                if (playlist.isNotEmpty() && currentIndex != -1) {
                    player.setPlaylist(playlist, currentIndex, mediaSource)
                } else {
                    // Fallback to single video play if directory listing fails (e.g. DNS error or offline server)
                    val rawName = FileUtils.extractFileName(videoPath)
                    val decodedName = FileUtils.decodeUrl(rawName).ifBlank { rawName }
                    val fallbackVideo = MediaFile.Video(
                        id = "$sourceId/$videoPath",
                        path = videoPath,
                        name = FileUtils.extractNameWithoutExtension(decodedName),
                        sourceId = sourceId,
                        sourceType = mediaSource.type,
                        extension = FileUtils.extractExtension(videoPath),
                        externalSubtitles = emptyList()
                    )
                    player.setPlaylist(listOf(fallbackVideo), 0, mediaSource)
                }
            } catch (_: Exception) {
                _effect.send(PlayerEffect.ShowToast(application.getString(R.string.player_error_load_failed)))
                _effect.send(PlayerEffect.FinishPlayer)
            }
        }
    }

    private fun setControlsVisible(visible: Boolean) {
        if (uiState.controlMode != PlayerControlMode.Normal && visible) return
        if (visible) {
            hideQuickBarJob?.cancel()
            _uiState.update {
                it.copy(
                    isControlsVisible = true,
                    quickBarMode = QuickBarMode.None
                )
            }
            scheduleAutoHideControls()
        } else {
            autoHideControlsJob?.cancel()
            _uiState.update { it.copy(isControlsVisible = false) }
        }
    }

    private fun toggleLock() {
        val newLock = !uiState.isLocked
        _uiState.update { it.copy(isLocked = newLock, isControlsVisible = !newLock) }
        scheduleAutoHideControls()
    }

    private fun scheduleAutoHideControls() {
        if (!uiState.isControlsVisible && !uiState.isLocked) return
        autoHideControlsJob?.cancel()
        autoHideControlsJob = viewModelScope.launch {
            delay(playerUiOptions.autoHideDurationSeconds.seconds)
            _uiState.update { it.copy(isControlsVisible = false) }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                application.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return uri.lastPathSegment
    }

    private fun addSubtitle(uri: Uri) {
        val fileName = getFileNameFromUri(uri)
        val extension = FileUtils.extractExtension(fileName) ?: ""

        if (extension !in AppConstants.SUBTITLE_EXTENSIONS) {
            viewModelScope.launch {
                _effect.send(PlayerEffect.ShowToast(application.getString(R.string.player_error_unsupported_subtitle)))
            }
            return
        }
        player.addSubtitle(uri.toString())
    }

    override fun onCleared() {
        player.decrementClientCount(PlayerClientType.UI)
        autoHideControlsJob?.cancel()
    }
}
