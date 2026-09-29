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

import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages Surface binding lifecycle, thread serialization (surfaceMutex), Surface identity verification,
 * and dynamic Surface refresh rate optimizations.
 *
 * Uses callback [onSetEngineSurface] to adjust engine surface without directly referencing engine instances.
 */
internal class PlayerSurfaceManager(
    private val onSetEngineSurface: (surface: Any?) -> Unit
) {

    // Caches current valid Surface object for self-healing during engine switch/creation
    var lastSurfaceObject: Any? = null
        private set

    // Surface operation mutex, ensuring strict serialization of Surface operations in multi-instance environments
    val surfaceMutex = Mutex()

    /**
     * Binds new Surface and applies video frame rate settings.
     */
    fun setSurface(
        surface: Any,
        getCurrentFps: () -> Double?,
        playerScope: CoroutineScope
    ) {
        playerScope.launch {
            surfaceMutex.withLock {
                lastSurfaceObject = surface
                onSetEngineSurface(surface)

                getCurrentFps()?.let { fps ->
                    if (fps > 0.0) applySurfaceFrameRate(fps)
                }
            }
        }
    }

    /**
     * Detaches Surface safely with identity verification.
     */
    fun detachSurface(
        surface: Any,
        playerScope: CoroutineScope
    ) {
        playerScope.launch {
            surfaceMutex.withLock {
                val matches = lastSurfaceObject === surface
                if (matches) {
                    clearSurfaceFrameRate()
                    lastSurfaceObject = null
                    onSetEngineSurface(null)
                }
            }
        }
    }

    /**
     * Binds cached Surface to the active engine.
     */
    fun bindCachedSurfaceToEngine(playerScope: CoroutineScope) {
        playerScope.launch {
            surfaceMutex.withLock {
                lastSurfaceObject?.let {
                    onSetEngineSurface(it)
                }
            }
        }
    }

    /**
     * Unbinds the surface from the old engine during engine switch.
     * PRESERVES [lastSurfaceObject] reference so the incoming new engine inherits the active UI surface!
     */
    suspend fun detachOldEngineSurface(unbindOldEngine: suspend () -> Unit) {
        surfaceMutex.withLock {
            unbindOldEngine()
        }
    }

    /**
     * Fully clears surface reference and detaches from engine (used when all UI clients leave or on release).
     */
    suspend fun clearSurfaceAndDetach() {
        surfaceMutex.withLock {
            clearSurfaceFrameRate()
            lastSurfaceObject = null
            onSetEngineSurface(null)
        }
    }

    /**
     * Applies Surface-level refresh rate optimization.
     */
    fun applySurfaceFrameRate(fps: Double) {
        if (fps <= 0.0) return
        val surface = lastSurfaceObject as? Surface ?: return
        try {
            surface.setFrameRate(
                fps.toFloat(),
                Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE,
                Surface.CHANGE_FRAME_RATE_ALWAYS
            )
        } catch (_: Exception) {
        }
    }

    /**
     * Clears Surface refresh rate settings.
     */
    fun clearSurfaceFrameRate() {
        val surface = lastSurfaceObject as? Surface ?: return
        try {
            surface.setFrameRate(0f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
        } catch (_: Exception) {
            // ignore
        }
    }
}
