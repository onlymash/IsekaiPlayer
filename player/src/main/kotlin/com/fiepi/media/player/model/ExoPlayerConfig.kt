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

package com.fiepi.media.player.model

import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.preferences.VideoScaleMode

/**
 * Configuration options supported natively and consumed by ExoPlayerEngine.
 */
data class ExoPlayerConfig(
    val preferredAudioLanguages: List<String> = emptyList(),
    val preferredSubtitleLanguages: List<String> = emptyList(),
    val audioPitchCorrection: Boolean = true,
    val volume: Double = 100.0,
    val videoScaleMode: VideoScaleMode = VideoScaleMode.Fit,
    val playbackSpeed: Float = 1.0f,
    val networkTimeout: Long = AppConstants.REMOTE_TIMEOUT_MS,
    val userAgent: String = AppConstants.DEFAULT_USER_AGENT
)
