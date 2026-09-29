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
data class PlayerLayoutOptions(
    @SerialName("top_left_actions")
    val topLeftActions: List<PlayerAction> = listOf(
        PlayerAction.Screenshot,
        PlayerAction.PiP,
    ),
    @SerialName("top_right_actions")
    val topRightActions: List<PlayerAction> = listOf(
        PlayerAction.PlayerSettings,
        PlayerAction.Osd
    ),
    @SerialName("bottom_left_actions")
    val bottomLeftActions: List<PlayerAction> = listOf(
        PlayerAction.Playlist,
        PlayerAction.Tracks
    ),
    @SerialName("bottom_right_actions")
    val bottomRightActions: List<PlayerAction> = listOf(
        PlayerAction.ScaleModeToggle,
        PlayerAction.LoopModeToggle
    ),
    @SerialName("show_title_bar")
    val showTitleBar: Boolean = true
)

@Serializable
enum class ButtonBorderStyle(val value: String) {
    @SerialName("directional")
    Directional("directional"),

    @SerialName("ambient")
    Ambient("ambient"),

    @SerialName("plain")
    Plain("plain");

    companion object {
        fun fromValue(value: String): ButtonBorderStyle =
            entries.firstOrNull { it.value == value } ?: Plain
    }
}

@Serializable
data class ButtonEffectOptions(
    @SerialName("blur")
    val blur: Boolean = false,
    @SerialName("lens")
    val lens: Boolean = false,
    @SerialName("alpha")
    val alpha: Float = 0.2f,
    @SerialName("use_texture_view")
    val useTextureView: Boolean = false,
    @SerialName("show_border")
    val showBorder: Boolean = true,
    @SerialName("border_style")
    val borderStyle: ButtonBorderStyle = ButtonBorderStyle.Ambient
)

@Serializable
data class PlayerOptions(
    @SerialName("auto_hide_duration_seconds")
    val autoHideDurationSeconds: Int = 5,
    @SerialName("seek_duration_seconds")
    val seekDurationSeconds: Int = 10,
    @SerialName("seek_bar_style")
    val seekBarStyle: SeekBarStyle = SeekBarStyle.Wavy,
    @SerialName("resume_playback")
    val resumePlayback: Boolean = true,
    @SerialName("loop_mode")
    val loopMode: LoopMode = LoopMode.None,
    @SerialName("enable_pip")
    val enablePiP: Boolean = true,
    @SerialName("remember_brightness")
    val rememberBrightness: Boolean = true,
    @SerialName("orientation")
    val orientation: PlayerOrientation = PlayerOrientation.Sensor,
    @SerialName("brightness")
    val brightness: Float = -1f,
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
    @SerialName("screenshot_format")
    val screenshotFormat: ScreenshotFormat = ScreenshotFormat.JPG,
    @SerialName("layout_options")
    val layoutOptions: PlayerLayoutOptions = PlayerLayoutOptions()
)
