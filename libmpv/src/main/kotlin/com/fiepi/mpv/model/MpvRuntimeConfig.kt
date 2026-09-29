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
 * Hardware decoding, video scaling and HDR tone mapping configuration for mpv
 */
data class MpvDecoderConfig(
    // Hardware acceleration method: mediacodec, mediacodec-copy, no (pure software decoding)
    val hwdec: String = "mediacodec,mediacodec-copy,no",
    // Hardware decoding format whitelist: all,h264,hevc,vp9,av1
    val hwdecCodecs: String = "all",
    // Frame drop strategy: vo, decoder, no
    val framedrop: String = "vo",
    // Video scaling algorithm: bilinear, spline36, ewa_lanczos
    val scale: String = "bilinear",
    // Video downscaling algorithm: mitchell, hermite, bicubic
    val dscale: String = "mitchell",
    // Chroma scaling algorithm: bilinear, mitchell, bicubic
    val cscale: String = "bilinear",
    // Anti-ringing strength for scaling (0.0..1.0)
    val scaleAntiring: Float = 0.0f,
    // Deband filter toggle (removes color banding)
    val deband: Boolean = true,
    // Deband iterations (1..16, default 1)
    val debandIterations: Int = 1,
    // Deband threshold (0..4096, default 48)
    val debandThreshold: Int = 48,
    // Deband range (1..64, default 16)
    val debandRange: Int = 16,
    // Deband grain (0..4096, default 32)
    val debandGrain: Int = 32,
    // HDR tone mapping mode: auto, bt.2446a, spline, reinhard
    val toneMapping: String = "auto",
    // HDR tone mapping max boost (1.0..3.0, default 1.5)
    val toneMappingMaxBoost: Float = 1.5f,
    // HDR gamut mapping mode: perceptual, clip, relative, saturation, desaturate
    val gamutMappingMode: String = "perceptual",
    // HDR hardware passthrough to display Surface (target-colorspace-hint)
    val hdrPassthrough: Boolean = true,
)

/**
 * Audio channels, volume, normalization and delay configuration for mpv
 */
data class MpvAudioConfig(
    // Audio channels / downmix strategy: auto-safe, stereo, 5.1
    val audioChannels: String = "auto-safe",
    // Audio passthrough (HDMI/SPDIF): "" (disabled), "ac3,eac3,dts-hd"
    val audioSpdif: String = "",
    // Pitch correction during speed change
    val audioPitchCorrection: Boolean = true,
    // Player audio volume (e.g. 100.0 for 100% normal volume)
    val volume: Double = 100.0,
    // Maximum volume limit (e.g. 200.0 allows software amplification up to 200%)
    val volumeMax: Double = 200.0,
    // Dynamic range compression / vocal normalization (dynaudnorm filter)
    val dynamicAudioNormalize: Boolean = false,
    // Audio delay (seconds)
    val audioDelay: Double = 0.0,
    // Preferred audio language priority (comma-separated)
    val preferredAudioLang: String = "auto",
)

/**
 * Subtitle language, font, colors, outline, ASS override and fallback configuration for mpv
 */
data class MpvSubtitleConfig(
    // Preferred subtitle language priority (comma-separated)
    val preferredSubtitleLang: String = "auto",
    // External subtitle auto-load strategy: fuzzy, all, exact
    val subAuto: String = "fuzzy",
    // External subtitle codepage: auto, utf-8, gbk, big5
    val subCodepage: String = "auto",
    // Subtitle font size
    val subFontSize: Int = 50,
    // Subtitle font text color (Hex string e.g. "#FFFFFF")
    val fontColorHex: String = "#FFFFFF",
    // Subtitle outline color (Hex string e.g. "#000000")
    val outlineColorHex: String = "#000000",
    // Subtitle outline width
    val outlineWidth: Int = 2,
    // Subtitle shadow color (sub-shadow-color)
    val shadowColorHex: String = "#000000",
    // Subtitle shadow offset (sub-shadow-offset)
    val shadowOffset: Int = 0,
    // Subtitle bold text toggle (sub-bold)
    val subBold: Boolean = false,
    // Subtitle italic text toggle (sub-italic)
    val subItalic: Boolean = false,
    // Subtitle font family name (sub-font)
    val subFont: String = "sans-serif",
    // Custom subtitle fonts directory (sub-fonts-dir)
    val subFontsDir: String = "",
    // Subtitle bottom margin (sub-margin-y)
    val bottomMargin: Int = 12,
    // ASS subtitle style override mode: scale, no, force, strip
    val subAssOverride: String = "scale",
    // Blend subtitles mode: yes, video, no
    val blendSubtitles: String = "yes",
    // Fallback to first available subtitle when no matching language is found
    val subsFallback: Boolean = true,
)

/**
 * Demuxer, cache and network configuration for mpv
 */
data class MpvCacheConfig(
    // Network and demuxer buffer size
    val demuxerMaxBytes: Long = 32L * 1024 * 1024,
    val demuxerMaxBackBytes: Long = 8L * 1024 * 1024,
    // Maximum pre-cache seconds
    val cacheSecs: Int = 30,
    // Back-cache seconds
    val cacheBackSecs: Int = 10,
    // Network connection timeout (seconds)
    val networkTimeout: Int = 20,
    // Network request User-Agent
    val userAgent: String = "libmpv",
)

/**
 * Display scale mode, playback speed and OSD overlay configuration for mpv
 */
data class MpvDisplayConfig(
    // Video scale mode: fit, fill, original
    val videoScaleMode: String = "fit",
    // Playback speed
    val playbackSpeed: Float = 1.0f,
    // OSD overlay stats
    val isOsdVisible: Boolean = false,
    val osdPage: Int = 1,
)

/**
 * Dynamic runtime settings (can be hot-modified during playback, taking effect immediately without interrupting playback)
 */
data class MpvRuntimeConfig(
    val decoder: MpvDecoderConfig = MpvDecoderConfig(),
    val audio: MpvAudioConfig = MpvAudioConfig(),
    val subtitle: MpvSubtitleConfig = MpvSubtitleConfig(),
    val cache: MpvCacheConfig = MpvCacheConfig(),
    val display: MpvDisplayConfig = MpvDisplayConfig(),
)
