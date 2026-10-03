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

package com.fiepi.media.domain.model.preferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GestureOptions(
    @SerialName("horizontal_drag_enabled")
    val horizontalDragEnabled: Boolean = true,
    @SerialName("vertical_drag_volume_enabled")
    val verticalDragVolumeEnabled: Boolean = true,
    @SerialName("vertical_drag_brightness_enabled")
    val verticalDragBrightnessEnabled: Boolean = true,
    @SerialName("swap_volume_brightness")
    val swapVolumeBrightness: Boolean = false,
    @SerialName("double_tap_seek_enabled")
    val doubleTapSeekEnabled: Boolean = true,
    @SerialName("double_tap_seek_duration_seconds")
    val doubleTapSeekDurationSeconds: Int = 10,
    @SerialName("double_tap_play_pause_enabled")
    val doubleTapPlayPauseEnabled: Boolean = true,
    @SerialName("long_press_enabled")
    val longPressEnabled: Boolean = true,
    @SerialName("long_press_speed")
    val longPressSpeed: Float = 3.0f,
)
