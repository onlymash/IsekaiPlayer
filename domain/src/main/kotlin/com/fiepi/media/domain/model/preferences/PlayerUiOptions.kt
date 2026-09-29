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
enum class SeekBarStyle(val value: String) {
    @SerialName("wavy")
    Wavy("wavy"),

    @SerialName("straight")
    Straight("straight");

    companion object {
        fun fromValue(value: String): SeekBarStyle =
            entries.firstOrNull { it.value == value } ?: Wavy
    }
}

@Serializable
enum class PlayerOrientation(val value: String) {
    @SerialName("default")
    Default("default"),

    @SerialName("sensor")
    Sensor("sensor"),

    @SerialName("landscape")
    Landscape("landscape"),

    @SerialName("portrait")
    Portrait("portrait"),

    @SerialName("video")
    Video("video");

    companion object {
        fun fromValue(value: String): PlayerOrientation =
            entries.firstOrNull { it.value == value } ?: Default
    }
}

/**
 * UI and interaction related options for the player screen.
 */
@Serializable
data class PlayerUiOptions(
    @SerialName("seek_bar_style")
    val seekBarStyle: SeekBarStyle = SeekBarStyle.Wavy,
    @SerialName("auto_hide_duration_seconds")
    val autoHideDurationSeconds: Int = 4,
    @SerialName("seek_duration_seconds")
    val seekDurationSeconds: Int = 10,
    @SerialName("double_tap_seek_duration_seconds")
    val doubleTapSeekDurationSeconds: Int = 10,
    @SerialName("enable_pip")
    val enablePiP: Boolean = true,
    @SerialName("orientation")
    val orientation: PlayerOrientation = PlayerOrientation.Default,
    @SerialName("remember_brightness")
    val rememberBrightness: Boolean = true,
    @SerialName("enable_background_audio")
    val enableBackgroundAudio: Boolean = true,
    @SerialName("button_effect")
    val buttonEffect: ButtonEffectOptions = ButtonEffectOptions(),
    @SerialName("always_full_screen")
    val alwaysFullScreen: Boolean = false,
    @SerialName("video_scale_mode")
    val videoScaleMode: VideoScaleMode = VideoScaleMode.Fit,
    @SerialName("playback_speed")
    val playbackSpeed: Float = 1.0f,
    @SerialName("is_osd_visible")
    val isOsdVisible: Boolean = false,
    @SerialName("osd_page")
    val osdPage: Int = 1,
    @SerialName("gesture_options")
    val gestureOptions: GestureOptions = GestureOptions(),
    @SerialName("layout_options")
    val layoutOptions: PlayerLayoutOptions = PlayerLayoutOptions()
)
