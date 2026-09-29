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

package com.fiepi.media.ffmpeg.decoder

import android.content.Context
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.TraceUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.decoder.CryptoConfig
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.fiepi.media.ffmpeg.FFmpegLibrary

/** Decodes and renders audio using FFmpeg. */
@UnstableApi
class FFmpegAudioRenderer(
    eventHandler: Handler? = null,
    eventListener: AudioRendererEventListener? = null,
    audioSink: AudioSink
) : androidx.media3.exoplayer.audio.DecoderAudioRenderer<FFmpegAudioDecoder>(eventHandler, eventListener, audioSink) {

    constructor(context: Context) : this(
        context = context,
        eventHandler = null,
        eventListener = null
    )

    constructor(
        context: Context,
        eventHandler: Handler?,
        eventListener: AudioRendererEventListener?,
        vararg audioProcessors: AudioProcessor
    ) : this(
        eventHandler = eventHandler,
        eventListener = eventListener,
        audioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(audioProcessors)
            .build()
    )

    override fun getName(): String = TAG

    override fun supportsFormatInternal(format: Format): Int {
        val mimeType = checkNotNull(format.sampleMimeType)
        return if (!FFmpegLibrary.isAvailable || !MimeTypes.isAudio(mimeType)) {
            C.FORMAT_UNSUPPORTED_TYPE
        } else if (!FFmpegLibrary.supportsFormat(mimeType) ||
            (!sinkSupportsFormat(format, C.ENCODING_PCM_16BIT) &&
                    !sinkSupportsFormat(format, C.ENCODING_PCM_FLOAT))
        ) {
            C.FORMAT_UNSUPPORTED_SUBTYPE
        } else if (format.cryptoType != C.CRYPTO_TYPE_NONE) {
            C.FORMAT_UNSUPPORTED_DRM
        } else {
            C.FORMAT_HANDLED
        }
    }

    override fun supportsMixedMimeTypeAdaptation(): Int {
        return ADAPTIVE_NOT_SEAMLESS
    }

    override fun createDecoder(
        format: Format,
        cryptoConfig: CryptoConfig?
    ): FFmpegAudioDecoder {
        TraceUtil.beginSection("createFFmpegAudioDecoder")
        val initialInputBufferSize =
            if (format.maxInputSize != Format.NO_VALUE) format.maxInputSize else DEFAULT_INPUT_BUFFER_SIZE
        val decoder = FFmpegAudioDecoder(
            format,
            NUM_BUFFERS,
            NUM_BUFFERS,
            initialInputBufferSize,
            shouldOutputFloat(format)
        )
        TraceUtil.endSection()
        return decoder
    }

    override fun getOutputFormat(decoder: FFmpegAudioDecoder): Format {
        return Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_RAW)
            .setChannelCount(decoder.channelCount)
            .setSampleRate(decoder.sampleRate)
            .setPcmEncoding(decoder.encoding)
            .build()
    }

    private fun sinkSupportsFormat(inputFormat: Format, pcmEncoding: @C.PcmEncoding Int): Boolean {
        return sinkSupportsFormat(
            Util.getPcmFormat(pcmEncoding, inputFormat.channelCount, inputFormat.sampleRate)
        )
    }

    private fun shouldOutputFloat(inputFormat: Format): Boolean {
        if (!sinkSupportsFormat(inputFormat, C.ENCODING_PCM_16BIT)) {
            return true
        }
        val formatSupport = getSinkFormatSupport(
            Util.getPcmFormat(C.ENCODING_PCM_FLOAT, inputFormat.channelCount, inputFormat.sampleRate)
        )
        return when (formatSupport) {
            AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY -> MimeTypes.AUDIO_AC3 != inputFormat.sampleMimeType
            AudioSink.SINK_FORMAT_UNSUPPORTED,
            AudioSink.SINK_FORMAT_SUPPORTED_WITH_TRANSCODING -> false
            else -> false
        }
    }

    companion object {
        private const val TAG = "FFmpegAudioRenderer"
        private const val NUM_BUFFERS = 16
        private const val DEFAULT_INPUT_BUFFER_SIZE = 960 * 6
    }
}
