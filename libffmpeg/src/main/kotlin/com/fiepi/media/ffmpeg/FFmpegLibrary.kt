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

package com.fiepi.media.ffmpeg

import androidx.annotation.Keep
import androidx.media3.common.C
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.LibraryLoader
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi

/** Configures and queries the underlying native library. */
@UnstableApi
@Keep
object FFmpegLibrary {

    init {
        MediaLibraryInfo.registerModule("media3.decoder.ffmpeg")
    }

    private const val TAG = "FFmpegLibrary"

    private val LOADER = object : LibraryLoader("media-ffmpeg-jni") {
        override fun loadLibrary(name: String) {
            System.loadLibrary(name)
        }
    }

    private var versionInternal: String? = null
    private var paddingSizeInternal = C.LENGTH_UNSET

    /**
     * Override the names of the FFmpeg native libraries. If an application wishes to call this
     * method, it must do so before calling any other method defined by this class, and before
     * instantiating a [com.fiepi.media.ffmpeg.decoder.FFmpegAudioRenderer]
     * instance.
     *
     * @param libraries The names of the FFmpeg native libraries.
     */
    @JvmStatic
    fun setLibraries(vararg libraries: String) {
        LOADER.setLibraries(*libraries)
    }

    /** Returns whether the underlying library is available, loading it if necessary. */
    @JvmStatic
    val isAvailable: Boolean
        get() = LOADER.isAvailable

    /** Returns the version of the underlying library if available, or null otherwise. */
    @JvmStatic
    val version: String?
        get() {
            if (!isAvailable) {
                return null
            }
            if (versionInternal == null) {
                versionInternal = ffmpegGetVersion()
            }
            return versionInternal
        }

    /**
     * Returns the required amount of padding for input buffers in bytes, or [C.LENGTH_UNSET] if
     * the underlying library is not available.
     */
    @JvmStatic
    val inputBufferPaddingSize: Int
        get() {
            if (!isAvailable) {
                return C.LENGTH_UNSET
            }
            if (paddingSizeInternal == C.LENGTH_UNSET) {
                paddingSizeInternal = ffmpegGetInputBufferPaddingSize()
            }
            return paddingSizeInternal
        }

    /**
     * Returns whether the underlying library supports the specified MIME type.
     *
     * @param mimeType The MIME type to check.
     */
    @JvmStatic
    fun supportsFormat(mimeType: String): Boolean {
        if (!isAvailable) {
            return false
        }
        val codecName = getCodecName(mimeType) ?: return false
        if (!ffmpegHasDecoder(codecName)) {
            Log.w(TAG, "No $codecName decoder available. Check the FFmpeg build configuration.")
            return false
        }
        return true
    }

    /**
     * Returns the name of the FFmpeg decoder that could be used to decode the format, or `null`
     * if it's unsupported.
     */
    @JvmStatic
    internal fun getCodecName(mimeType: String): String? {
        return when (mimeType) {
            MimeTypes.AUDIO_AAC -> "aac"
            MimeTypes.AUDIO_MPEG, MimeTypes.AUDIO_MPEG_L1, MimeTypes.AUDIO_MPEG_L2 -> "mp3"
            MimeTypes.AUDIO_AC3 -> "ac3"
            MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "eac3"
            MimeTypes.AUDIO_TRUEHD -> "truehd"
            MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_EXPRESS, MimeTypes.AUDIO_DTS_HD -> "dca"
            MimeTypes.AUDIO_VORBIS -> "vorbis"
            MimeTypes.AUDIO_OPUS -> "opus"
            MimeTypes.AUDIO_AMR_NB -> "amrnb"
            MimeTypes.AUDIO_AMR_WB -> "amrwb"
            MimeTypes.AUDIO_FLAC -> "flac"
            MimeTypes.AUDIO_ALAC -> "alac"
            MimeTypes.AUDIO_MLAW -> "pcm_mulaw"
            MimeTypes.AUDIO_ALAW -> "pcm_alaw"
            MimeTypes.VIDEO_H264 -> "h264"
            MimeTypes.VIDEO_H265 -> "hevc"
            else -> null
        }
    }

    @Keep
    @JvmStatic
    private external fun ffmpegGetVersion(): String

    @Keep
    @JvmStatic
    private external fun ffmpegGetInputBufferPaddingSize(): Int

    @Keep
    @JvmStatic
    private external fun ffmpegHasDecoder(codecName: String): Boolean
}