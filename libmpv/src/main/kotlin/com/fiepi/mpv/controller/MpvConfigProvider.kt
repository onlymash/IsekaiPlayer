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
import android.os.Environment
import androidx.core.content.ContextCompat
import com.fiepi.mpv.constants.MpvFormat
import com.fiepi.mpv.model.MpvConfig
import com.fiepi.mpv.model.MpvRuntimeConfig
import java.io.File

/**
 * MPV player engine option configuration generator
 */
class MpvConfigProvider(private val context: Context) {

    private val configDir: String by lazy {
        File(context.filesDir, "mpv_config").apply { mkdirs() }.path
    }

    private val cacheDir: String by lazy {
        File(context.cacheDir, "mpv_shader_cache").apply { mkdirs() }.path
    }

    private val caCertPath: String by lazy {
        File(context.filesDir, "cacert.pem").path
    }

    /**
     * Reads screen refresh rate so native rendering matches Android device frame rate
     */
    private val displayRefreshRate: Int by lazy {
        val display = ContextCompat.getDisplayOrDefault(context)
        display.mode?.refreshRate?.toInt() ?: 60
    }

    /**
     * Phase 1: Options that must be configured prior to invoking MPVLib.init()
     * Includes paths, platform hardware interfaces, and performance strategies
     */
    fun getPreInitOptions(
        config: MpvConfig = MpvConfig()
    ): Map<String, String> {
        val staticConfig = config.static
        val runtimeConfig = config.runtime
        val options = mutableMapOf<String, String>()

        // System and cache directory settings
        options["config"] = "yes"
        options["config-dir"] = configDir
        options["gpu-shader-cache-dir"] = cacheDir
        options["icc-cache-dir"] = cacheDir

        // Disable ytdl hook script to prevent subprocess execution on Android, which causes fdsan crashes on network 404 / load errors
        options["ytdl"] = "no"

        // Android platform and GPU rendering base options
        options["profile"] = staticConfig.profile
        options["gpu-context"] = if (staticConfig.gpuApi == "vulkan") "androidvk" else "android"
        options["gpu-api"] = staticConfig.gpuApi
        if (staticConfig.gpuApi == "opengl") {
            options["opengl-es"] = "yes"
        }
        options["vo"] = staticConfig.vo

        // Screen refresh rate adaptation and clock synchronization
        options["display-fps-override"] = displayRefreshRate.toString()
        options["video-sync"] = staticConfig.videoSync
        options["interpolation"] = "yes"
        options["tscale"] = "oversample"

        // 开启动态 HDR 峰值检测，计算逐帧亮度直方图，防止暗部被过度压缩
        options["hdr-compute-peak"] = "auto"

        // Threads, hardware decoding base bits, and audio role settings
        options["vd-lavc-dr"] = "yes"
        options["vd-lavc-threads"] = staticConfig.vdLavcThreads.toString()
        options["mediacodec-video-crop"] = "yes"
        options["ao"] = staticConfig.audioOutput
        options["audio-set-media-role"] = "yes"

        // Security, network, and key binding handling
        options["tls-verify"] = if (staticConfig.tlsVerify) "yes" else "no"
        options["tls-ca-file"] = caCertPath
        options["input-default-bindings"] = "yes"

        // Log level
        options["msg-level"] = staticConfig.msgLevel

        // Merge runtime options (setting certain runtime options during initialization avoids reinitializing decoders later)
        options.putAll(getRuntimeOptions(runtimeConfig, staticConfig.overrideProfile))

        return options
    }

    /**
     * Phase 2: Options that must be configured after invoking MPVLib.init()
     */
    fun getPostInitOptions(): Map<String, String> {
        return mapOf(
            // Playback progress is managed by our own Room database; prohibit mpv from auto-saving position on quit
            "save-position-on-quit" to "no",
            // Keep file open after playback ends, remaining on the last frame so users can seek back or replay
            "keep-open" to "always",
            // Do not force window activation before Surface creation is complete
            "force-window" to "no",
            // Maintain an idle state to support subsequent asynchronous loadfile operations
            "idle" to "once"
        )
    }

    /**
     * Converts runtime configuration to mpv options map
     */
    fun getRuntimeOptions(
        config: MpvRuntimeConfig,
        overrideProfile: Boolean = false
    ): Map<String, String> {
        val options = mutableMapOf<String, String>()

        // Decoding and rendering
        options["hwdec"] = config.decoder.hwdec
        options["hwdec-codecs"] = config.decoder.hwdecCodecs
        options["framedrop"] = config.decoder.framedrop

        // Seek and scrubbing performance optimizations
        options["hr-seek"] = "no"
        options["hr-seek-framedrop"] = "yes"

        // Apply specific video processing algorithms only when override is enabled, otherwise dictated by profile preset
        if (overrideProfile) {
            options["scale"] = config.decoder.scale
            options["dscale"] = config.decoder.dscale
            options["cscale"] = config.decoder.cscale
            options["scale-antiring"] = config.decoder.scaleAntiring.toString()
            options["deband"] = if (config.decoder.deband) "yes" else "no"
            options["deband-iterations"] = config.decoder.debandIterations.toString()
            options["deband-threshold"] = config.decoder.debandThreshold.toString()
            options["deband-range"] = config.decoder.debandRange.toString()
            options["deband-grain"] = config.decoder.debandGrain.toString()
        }

        options["tone-mapping"] = config.decoder.toneMapping
        options["tone-mapping-max-boost"] = config.decoder.toneMappingMaxBoost.toString()
        options["gamut-mapping-mode"] = config.decoder.gamutMappingMode
        options["target-colorspace-hint"] = if (config.decoder.hdrPassthrough) "yes" else "no"

        // Audio
        options["audio-channels"] = config.audio.audioChannels
        options["audio-spdif"] = config.audio.audioSpdif
        options["audio-pitch-correction"] = if (config.audio.audioPitchCorrection) "yes" else "no"
        options["volume"] = config.audio.volume.toString()
        options["volume-max"] = config.audio.volumeMax.toString()

        // Dynamic range compression (implemented via af filter)
        if (config.audio.dynamicAudioNormalize) {
            options["af"] = "lavfi=[dynaudnorm]"
        } else {
            options["af"] = ""
        }

        // Language preferences
        options["alang"] = config.audio.preferredAudioLang
        options["slang"] = config.subtitle.preferredSubtitleLang

        // Subtitles
        options["sub-auto"] = config.subtitle.subAuto
        options["sub-codepage"] = config.subtitle.subCodepage
        options["sub-font-size"] = config.subtitle.subFontSize.toString()
        options["sub-color"] = config.subtitle.fontColorHex
        options["sub-border-color"] = config.subtitle.outlineColorHex
        options["sub-border-size"] = config.subtitle.outlineWidth.toString()
        options["sub-shadow-color"] = config.subtitle.shadowColorHex
        options["sub-shadow-offset"] = config.subtitle.shadowOffset.toString()
        options["sub-bold"] = if (config.subtitle.subBold) "yes" else "no"
        options["sub-italic"] = if (config.subtitle.subItalic) "yes" else "no"
        options["sub-font"] = config.subtitle.subFont
        val targetFontsDir = config.subtitle.subFontsDir.ifBlank {
            File(Environment.getExternalStorageDirectory(), "Fonts").absolutePath
        }
        if (File(targetFontsDir).exists()) {
            options["sub-fonts-dir"] = targetFontsDir
        }
        options["sub-margin-y"] = config.subtitle.bottomMargin.toString()
        options["sub-ass-override"] = config.subtitle.subAssOverride
        options["blend-subtitles"] = config.subtitle.blendSubtitles
        options["subs-fallback"] = if (config.subtitle.subsFallback) "yes" else "no"

        // Cache and network
        options["demuxer-max-bytes"] = config.cache.demuxerMaxBytes.toString()
        options["demuxer-max-back-bytes"] = config.cache.demuxerMaxBackBytes.toString()
        options["demuxer-readahead-secs"] = "1"        // Reduce readahead time to 1 second
        options["demuxer-raw-threads"] = "yes"         // Enable dedicated Demuxer thread
        options["cache-secs"] = config.cache.cacheSecs.toString()
        options["cache-back-secs"] = config.cache.cacheBackSecs.toString()
        options["network-timeout"] = config.cache.networkTimeout.toString()
        options["user-agent"] = config.cache.userAgent

        // Video scale
        when (config.display.videoScaleMode) {
            "fit" -> {
                options["video-unscaled"] = "no"
                options["panscan"] = "0.0"
            }
            "fill" -> {
                options["video-unscaled"] = "no"
                options["panscan"] = "1.0"
            }
            "original" -> {
                options["video-unscaled"] = "yes"
            }
        }

        options["speed"] = config.display.playbackSpeed.toString()
        options["audio-delay"] = config.audio.audioDelay.toString()

        return options
    }

    /**
     * Defines the list of properties to observe and their corresponding formats
     */
    fun getObservedProperties(): Map<String, Int> {
        return mapOf(
            "time-pos" to MpvFormat.MPV_FORMAT_DOUBLE,
            "duration" to MpvFormat.MPV_FORMAT_DOUBLE,
            "pause" to MpvFormat.MPV_FORMAT_FLAG,
            "paused-for-cache" to MpvFormat.MPV_FORMAT_FLAG,
            "demuxer-cache-duration" to MpvFormat.MPV_FORMAT_DOUBLE,
            "eof-reached" to MpvFormat.MPV_FORMAT_FLAG,
            "speed" to MpvFormat.MPV_FORMAT_DOUBLE,
            "volume" to MpvFormat.MPV_FORMAT_DOUBLE,
            "width" to MpvFormat.MPV_FORMAT_INT64,
            "height" to MpvFormat.MPV_FORMAT_INT64,
            "fps" to MpvFormat.MPV_FORMAT_DOUBLE,
            "container-fps" to MpvFormat.MPV_FORMAT_DOUBLE,
            "estimated-vf-fps" to MpvFormat.MPV_FORMAT_DOUBLE,
            "track-list" to MpvFormat.MPV_FORMAT_NONE,
            "estimated-frame-count" to MpvFormat.MPV_FORMAT_INT64,
            "hwdec-current" to MpvFormat.MPV_FORMAT_STRING,
            "audio-delay" to MpvFormat.MPV_FORMAT_DOUBLE,
            "sub-delay" to MpvFormat.MPV_FORMAT_DOUBLE,
        )
    }
}
