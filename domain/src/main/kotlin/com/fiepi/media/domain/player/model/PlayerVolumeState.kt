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

package com.fiepi.media.domain.player.model

/**
 * Detailed volume state for system volume and additional software gain volume
 */
data class PlayerVolumeState(
    val systemVolume: Int = 0,
    val systemMaxVolume: Int = 15,
    val gainVolume: Int = 100, // percentage 0-200% (MPV) or 0-100% (ExoPlayer)
    val maxGainVolume: Int = 200
)
