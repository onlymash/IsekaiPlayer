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

package com.fiepi.mpv.model


/**
 * Static initialization parameters (must be configured before MPVLib.init())
 * Modifying these parameters requires re-creating/initializing the MpvController instance
 */
data class MpvStaticConfig(
    // Video output mode: gpu
    val vo: String = "gpu",
    // GPU backend graphics API: opengl, vulkan
    val gpuApi: String = "opengl",
    // Base rendering quality profile: fast, default, high-quality, gpu-hq, low-latency, sw-fast
    val profile: String = "fast",
    // AV sync strategy: audio, display-resample
    val videoSync: String = "audio",
    // Software decoding CPU threads: 0 (auto), 1, 2, 4
    val vdLavcThreads: Int = 0,
    // Audio output backend: aaudio,audiotrack,opensles
    val audioOutput: String = "aaudio,audiotrack,opensles",
    // Log output level: all=warn, all=debug, all=info
    val msgLevel: String = "all=warn",
    // Whether to verify TLS certificates
    val tlsVerify: Boolean = true,
    // Whether to allow manually overriding video processing options in profile presets
    val overrideProfile: Boolean = false
)
