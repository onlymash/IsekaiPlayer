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

package com.fiepi.mpv.controller

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.Surface
import com.fiepi.mpv.MpvLib
import com.fiepi.mpv.model.MpvConfig
import com.fiepi.mpv.model.MpvEventData
import com.fiepi.mpv.model.MpvRuntimeConfig
import com.fiepi.mpv.model.MpvStaticConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong

/**
 * MpvController is the core wrapper around native libmpv, responsible for managing engine lifecycle, property operations, and event dispatching.
 *
 * [Native Multi-Instance Support]
 * 1. Instance Isolation: Each MpvController instance holds an independent [MpvLib] (native mpv_handle),
 *    completely resolving resource contention and race conditions during rapid Android Activity transitions.
 * 2. Thread Safety: All methods interacting with [MpvLib] are protected by [lifecycleLock], ensuring native handles remain valid and operations do not conflict.
 * 3. Lifecycle Management: Responsible for native layer initialization, property observation setup, and resource cleanup on destruction.
 */
@Suppress("SameParameterValue")
class MpvController(
    private val context: Context,
    private val configProvider: MpvConfigProvider
) : MpvLib.EventObserver {

    private val isDebug by lazy { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }

    private val mpvLib = MpvLib()

    private val _events = MutableSharedFlow<MpvEventData>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /**
     * Gets the event flow.
     */
    fun getEvents(): Flow<MpvEventData> = _events.asSharedFlow()

    // Indicates whether the engine is fully ready to receive loadfile commands
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val isInitialized = AtomicBoolean(false)
    private val isDestroyed = AtomicBoolean(false)

    // Caches Surface received from the UI layer
    private var pendingSurface: Surface? = null

    /**
     * Lifecycle lock protecting critical operations like initialize and destroy.
     */
    private val lifecycleLock = Mutex()

    /**
     * Runtime options cache used for diffing updates.
     */
    private val appliedRuntimeOptions = mutableMapOf<String, String>()

    private var initializedStaticConfig: MpvStaticConfig? = null

    private fun observeProperties() {
        configProvider.getObservedProperties().forEach { (property, format) ->
            mpvLib.observeProperty(property, format)
        }
    }

    /**
     * Initializes the player engine.
     * @param config Player configuration (including static and runtime settings)
     */
    suspend fun initialize(config: MpvConfig = MpvConfig()) =
        withContext(Dispatchers.IO) {
            logD("initialize() called, waiting for lock")
            lifecycleLock.withLock {
                if (isDestroyed.get()) {
                    logW("initialize ignored: already destroyed")
                    return@withLock
                }
                logD("initialize() acquired lock")
                if (isInitialized.get()) {
                    logD("Already initialized, skipping")
                    return@withLock
                }

                _isReady.value = false

                logD("Creating MpvLib instance")
                mpvLib.create(context.applicationContext)
                mpvLib.addObserver(this@MpvController)

                // Apply pre-initialization options (e.g., vo, gpu-context)
                val preInitOptions = configProvider.getPreInitOptions(config)
                preInitOptions.forEach { (key, value) ->
                    mpvLib.setOptionString(key, value)
                }

                // Sync runtime options at initialization into cache to avoid full trigger on first updateRuntimeConfig
                appliedRuntimeOptions.clear()
                appliedRuntimeOptions.putAll(
                    configProvider.getRuntimeOptions(
                        config.runtime,
                        config.static.overrideProfile
                    )
                )

                mpvLib.init()
                observeProperties()

                // Apply post-initialization options (e.g., keep-open, idle)
                configProvider.getPostInitOptions().forEach { (key, value) ->
                    mpvLib.setOptionString(key, value)
                }

                initializedStaticConfig = config.static
                isInitialized.set(true)
                logD("MpvLib instance initialized successfully")

                // [Self-Healing Logic]
                // If UI already provided a Surface during initialization (common during cold start), perform native attachment immediately.
                pendingSurface?.let {
                    logI("Auto-attaching pending surface after init: $it")
                    internalAttachSurface(it)
                }

                // Mark as ready, at which point the upper layer can safely send loadfile commands
                _isReady.value = true
                logD("isReady set to true")
            }
        }

    /**
     * Completely restarts native handle.
     * Must be called when static configuration (e.g., vo, gpu-api) changes.
     */
    suspend fun restart(config: MpvConfig) = withContext(Dispatchers.IO) {
        logI("restart() requested, restarting mpv handle...")
        lifecycleLock.withLock {
            if (isDestroyed.get()) return@withLock

            // Clean up old handle
            if (isInitialized.get()) {
                logD("restart: destroying old handle")
                try {
                    mpvLib.setPropertyString("vid", "no")
                    mpvLib.setPropertyString("vo", "null")
                    mpvLib.detachSurface()
                } catch (e: Exception) {
                    logW("Error during restart cleanup: ${e.message}")
                }
                mpvLib.removeObserver(this@MpvController)
                mpvLib.destroy()
                isInitialized.set(false)
                _isReady.value = false
            }

            // Re-create and initialize
            logD("restart: creating new handle")
            mpvLib.create(context.applicationContext)
            mpvLib.addObserver(this@MpvController)

            val preInitOptions = configProvider.getPreInitOptions(config)
            preInitOptions.forEach { (key, value) ->
                mpvLib.setOptionString(key, value)
            }

            appliedRuntimeOptions.clear()
            appliedRuntimeOptions.putAll(
                configProvider.getRuntimeOptions(
                    config.runtime,
                    config.static.overrideProfile
                )
            )

            mpvLib.init()
            observeProperties()

            configProvider.getPostInitOptions().forEach { (key, value) ->
                mpvLib.setOptionString(key, value)
            }

            initializedStaticConfig = config.static
            isInitialized.set(true)

            // Restore Surface
            pendingSurface?.let {
                logI("restart: re-attaching surface")
                internalAttachSurface(it)
            }

            _isReady.value = true
            logI("restart: completed, new handle is ready")
        }
    }

    /**
     * Updates runtime configuration, applying only changed properties (Diffing).
     */
    suspend fun updateRuntimeConfig(config: MpvRuntimeConfig) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                val newOptions = configProvider.getRuntimeOptions(
                    config,
                    initializedStaticConfig?.overrideProfile ?: false
                )

                newOptions.forEach { (key, value) ->
                    // Call native layer only when value changes
                    if (appliedRuntimeOptions[key] != value) {
                        logD("Property diff detected: $key -> $value")
                        mpvLib.setPropertyString(key, value)
                        appliedRuntimeOptions[key] = value
                    }
                }
            }
        }
    }

    suspend fun seekTo(positionMs: Long) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                val targetSec = positionMs / 1000.0
                mpvLib.setPropertyDouble("time-pos", targetSec)
                // Use absolute+keyframes mode for instant seeking
                //mpvLib.command("seek", targetSec.toString(), "absolute+keyframes")
            } else {
                logW("seekTo ignored: not initialized")
            }
        }
    }

    suspend fun playFile(path: String, startPositionMs: Long = 0L) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                if (startPositionMs > 0) {
                    val startSec = startPositionMs / 1000.0
                    mpvLib.command("loadfile", path, "replace", "0", "start=$startSec")
                } else {
                    mpvLib.command("loadfile", path)
                }
            } else {
                logW("playFile ignored: not initialized")
            }
        }
    }

    suspend fun pause(paused: Boolean) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                mpvLib.setPropertyBoolean("pause", paused)
            } else {
                logW("pause ignored: not initialized")
            }
        }
    }

    suspend fun setMute(muted: Boolean) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                mpvLib.setPropertyBoolean("mute", muted)
            }
        }
    }

    suspend fun setSpeed(speed: Double) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                mpvLib.setPropertyDouble("speed", speed)
            }
        }
    }

    suspend fun setVolume(volume: Double) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                mpvLib.setPropertyDouble("volume", volume)
            }
        }
    }

    suspend fun selectTrack(type: String, id: String) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                mpvLib.setPropertyString(type, id)
            }
        }
    }

    suspend fun getPropertyInt(property: String): Int? = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                mpvLib.getPropertyInt(property)
            } else {
                null
            }
        }
    }

    suspend fun getPropertyDouble(property: String): Double? = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                mpvLib.getPropertyDouble(property)
            } else {
                null
            }
        }
    }

    suspend fun getPropertyString(property: String): String? = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                mpvLib.getPropertyString(property)
            } else {
                null
            }
        }
    }

    suspend fun setPropertyString(name: String, value: String) =
        withContext(Dispatchers.Default) {
            lifecycleLock.withLock {
                if (isInitialized.get()) {
                    mpvLib.setPropertyString(name, value)
                }
            }
        }

    suspend fun getTrackList(): List<Map<String, Any>> = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (!isInitialized.get()) return@withContext emptyList()

            val count = mpvLib.getPropertyInt("track-list/count") ?: 0
            val tracks = mutableListOf<Map<String, Any>>()
            for (i in 0 until count) {
                val track = mutableMapOf<String, Any>()
                track["id"] = mpvLib.getPropertyInt("track-list/$i/id") ?: -1
                track["type"] = mpvLib.getPropertyString("track-list/$i/type") ?: ""
                track["title"] = mpvLib.getPropertyString("track-list/$i/title") ?: ""
                track["lang"] = mpvLib.getPropertyString("track-list/$i/lang") ?: ""
                track["selected"] = mpvLib.getPropertyBoolean("track-list/$i/selected") ?: false
                track["external"] = mpvLib.getPropertyBoolean("track-list/$i/external") ?: false
                track["albumart"] = mpvLib.getPropertyBoolean("track-list/$i/albumart") ?: false
                track["codec"] = mpvLib.getPropertyString("track-list/$i/codec") ?: ""
                tracks.add(track)
            }
            tracks
        }
    }

    suspend fun addSubtitle(url: String, lang: String? = null) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isInitialized.get()) {
                if (lang != null) {
                    mpvLib.command("sub-add", url, "auto", "", lang)
                } else {
                    mpvLib.command("sub-add", url, "auto")
                }
            }
        }
    }

    suspend fun setPreferredAudioLanguage(language: String) =
        withContext(Dispatchers.Default) {
            lifecycleLock.withLock {
                if (isInitialized.get()) {
                    mpvLib.setPropertyString("alang", language)
                }
            }
        }

    suspend fun setPreferredSubtitleLanguage(language: String) =
        withContext(Dispatchers.Default) {
            lifecycleLock.withLock {
                if (isInitialized.get()) {
                    mpvLib.setPropertyString("slang", language)
                }
            }
        }

    suspend fun setOption(name: String, value: String) =
        withContext(Dispatchers.Default) {
            lifecycleLock.withLock {
                if (isInitialized.get()) {
                    mpvLib.setOptionString(name, value)
                }
            }
        }

    suspend fun sendKey(action: String, key: String) =
        withContext(Dispatchers.Default) {
            lifecycleLock.withLock {
                if (isInitialized.get()) {
                    mpvLib.command(action, key)
                }
            }
        }

    suspend fun command(vararg args: String) =
        withContext(Dispatchers.Default) {
            lifecycleLock.withLock {
                if (isInitialized.get()) {
                    mpvLib.command(*args)
                }
            }
        }

    suspend fun takeScreenshot(path: String) =
        withContext(Dispatchers.Default) {
            lifecycleLock.withLock {
                if (isInitialized.get()) {
                    mpvLib.command("screenshot-to-file", path)
                }
            }
        }

    /**
     * Captures current frame.
     * @param dimension Target bounding dimension (maximum)
     */
    suspend fun grabThumbnail(dimension: Int): android.graphics.Bitmap? =
        withContext(Dispatchers.Default) {
            lifecycleLock.withLock {
                if (isInitialized.get()) {
                    mpvLib.grabThumbnail(dimension)
                } else {
                    null
                }
            }
        }

    suspend fun attachSurface(surface: Surface) = withContext(Dispatchers.IO) {
        logD("attachSurface($surface) requested")
        lifecycleLock.withLock {
            if (isDestroyed.get()) {
                logW("attachSurface ignored: already destroyed")
                return@withContext
            }
            logD("attachSurface($surface) lock acquired")
            pendingSurface = surface
            if (isInitialized.get()) {
                internalAttachSurface(surface)
            } else {
                logI("attachSurface cached: not initialized yet")
            }
        }
    }

    suspend fun setSurfaceSize(width: Int, height: Int) = withContext(Dispatchers.Default) {
        lifecycleLock.withLock {
            if (isDestroyed.get()) return@withContext
            if (isInitialized.get()) {
                mpvLib.setPropertyString("android-surface-size", "${width}x$height")
            }
        }
    }

    /**
     * Internal attachment implementation.
     * Uses vid switching strategy: re-enable video track after attaching new Surface, which is the lightest method for AV resync.
     */
    private fun internalAttachSurface(surface: Surface) {
        logD("internalAttachSurface: starting resync")

        // Attach new Android Surface
        mpvLib.attachSurface(surface)

        // Self-healing: re-enable video track.
        // Set vid=auto to force decoder to connect to renderer.
        logD("internalAttachSurface: enabling vid")
        mpvLib.setPropertyString("vid", "auto")

        // Perform a nudge seek to ensure frame refreshes immediately
        mpvLib.command("seek", "0", "relative")
    }

    suspend fun detachSurface() = withContext(Dispatchers.IO) {
        logD("detachSurface() requested")
        lifecycleLock.withLock {
            if (isDestroyed.get()) {
                logD("detachSurface ignored: already destroyed")
                return@withContext
            }
            logD("detachSurface() lock acquired")
            pendingSurface = null
            if (isInitialized.get()) {
                // Disable video track before detaching Surface
                // Stops rendering output while keeping audio pipeline intact, preventing stutter caused by vo=null
                logD("detachSurface: disabling vid")
                mpvLib.setPropertyString("vid", "no")

                // Then safely detach window handle reference
                mpvLib.detachSurface()
                logD("Surface detached (vid=no)")
            } else {
                logD("detachSurface: skipping, not initialized")
            }
        }
    }

    suspend fun destroy() = withContext(Dispatchers.IO) {
        logD("destroy() requested")
        // [CRITICAL FIX] Mark destroyed state immediately without waiting for lock.
        // Allows methods waiting on the lock (like attachSurface) to immediately identify "destroyed" and exit.
        if (isDestroyed.getAndSet(true)) {
            logD("destroy: already marked as destroyed, waiting for existing destroy to complete")
        }
        lifecycleLock.withLock {
            logD("destroy() lock acquired")
            if (isInitialized.get()) {
                // Stop VO rendering immediately
                // Critical step: setting vo to null stops native render thread reference to Surface
                // thereby preventing fdsan crashes.
                try {
                    logD("destroy: setting vo to null")
                    // Must disable vid and vo to completely disconnect from Surface
                    mpvLib.setPropertyString("vid", "no")
                    mpvLib.setPropertyString("vo", "null")
                    mpvLib.detachSurface()
                } catch (e: Exception) {
                    logW("Error stopping VO during destroy: ${e.message}")
                }

                isInitialized.set(false)
                mpvLib.removeObserver(this@MpvController)

                // Perform final cleanup
                logD("destroy: calling mpvLib.destroy()")
                mpvLib.destroy()
                logD("MpvLib.destroy() completed")

                _isReady.value = false
                pendingSurface = null
            } else {
                logD("destroy: not initialized, but marked as destroyed")
                _isReady.value = false
                pendingSurface = null
            }
        }
    }

    // --- Native Callback Implementation ---

    override fun eventProperty(property: String, value: Long) {
        when (property) {
            "pause", "paused-for-cache", "eof-reached" -> {
                _events.tryEmit(MpvEventData.PropertyBoolean(property, value != 0L))
            }

            else -> {
                _events.tryEmit(MpvEventData.PropertyLong(property, value))
            }
        }
    }

    override fun eventProperty(property: String, value: Double) {
        when (property) {
            "time-pos", "duration" -> {
                val ms = (value * 1000.0).roundToLong()
                _events.tryEmit(MpvEventData.PropertyLong(property, ms))
            }

            "volume" -> {
                _events.tryEmit(MpvEventData.PropertyDouble(property, value))
            }

            else -> _events.tryEmit(MpvEventData.PropertyDouble(property, value))
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        _events.tryEmit(MpvEventData.PropertyBoolean(property, value))
    }

    override fun eventProperty(property: String, value: String) {
        _events.tryEmit(MpvEventData.PropertyString(property, value))
    }

    override fun eventProperty(property: String) {
        _events.tryEmit(MpvEventData.PropertyNone(property))
    }

    override fun event(eventId: Int) {
        _events.tryEmit(MpvEventData.Event(eventId))
    }

    private fun logD(msg: String) {
        if (isDebug) {
            Log.d(TAG, msg)
        }
    }

    private fun logI(msg: String) {
        if (isDebug) {
            Log.i(TAG, msg)
        }
    }

    private fun logW(msg: String) {
        if (isDebug) {
            Log.w(TAG, msg)
        }
    }

    companion object {
        private const val TAG = "MpvController"
    }
}
