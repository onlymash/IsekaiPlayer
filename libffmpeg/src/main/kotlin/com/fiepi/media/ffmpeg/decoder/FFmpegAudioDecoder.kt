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

@file:Suppress("SameParameterValue")

package com.fiepi.media.ffmpeg.decoder

import androidx.annotation.Keep
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.decoder.SimpleDecoderOutputBuffer
import com.fiepi.media.ffmpeg.FFmpegLibrary
import java.nio.ByteBuffer

/** FFmpeg audio decoder. */
@Keep
@Suppress("UNCHECKED_CAST")
@UnstableApi
class FFmpegAudioDecoder(
    format: Format,
    numInputBuffers: Int,
    numOutputBuffers: Int,
    initialInputBufferSize: Int,
    outputFloat: Boolean
) : androidx.media3.decoder.SimpleDecoder<DecoderInputBuffer, SimpleDecoderOutputBuffer, FFmpegDecoderException>(
    arrayOfNulls<DecoderInputBuffer>(numInputBuffers) as Array<DecoderInputBuffer>,
    arrayOfNulls<SimpleDecoderOutputBuffer>(numOutputBuffers) as Array<SimpleDecoderOutputBuffer>
) {

    private val codecName: String
    private val extraData: ByteArray?

    val encoding: @C.PcmEncoding Int
    private var outputBufferSize: Int

    private var nativeContext: Long = 0
    private var hasOutputFormat = false

    @Volatile
    var channelCount = 0
        private set

    @Volatile
    var sampleRate = 0
        private set

    init {
        if (!FFmpegLibrary.isAvailable) {
            throw FFmpegDecoderException("Failed to load decoder native libraries.")
        }
        val mimeType = checkNotNull(format.sampleMimeType)
        codecName = checkNotNull(FFmpegLibrary.getCodecName(mimeType))
        extraData = getExtraData(mimeType, format.initializationData)
        encoding = if (outputFloat) C.ENCODING_PCM_FLOAT else C.ENCODING_PCM_16BIT
        outputBufferSize = if (outputFloat) INITIAL_OUTPUT_BUFFER_SIZE_32BIT else INITIAL_OUTPUT_BUFFER_SIZE_16BIT
        nativeContext = ffmpegInitialize(
            codecName,
            extraData,
            outputFloat,
            format.sampleRate,
            format.channelCount
        )
        if (nativeContext == 0L) {
            throw FFmpegDecoderException("Initialization failed.")
        }
        setInitialInputBufferSize(initialInputBufferSize)
    }

    override fun getName(): String {
        return "ffmpeg" + FFmpegLibrary.version + "-" + codecName
    }

    override fun createInputBuffer(): DecoderInputBuffer {
        return DecoderInputBuffer(
            DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DIRECT,
            FFmpegLibrary.inputBufferPaddingSize
        )
    }

    override fun createOutputBuffer(): SimpleDecoderOutputBuffer {
        return SimpleDecoderOutputBuffer { outputBuffer -> releaseOutputBuffer(outputBuffer) }
    }

    override fun createUnexpectedDecodeException(error: Throwable): FFmpegDecoderException {
        return FFmpegDecoderException("Unexpected decode error", error)
    }

    override fun decode(
        inputBuffer: DecoderInputBuffer,
        outputBuffer: SimpleDecoderOutputBuffer,
        reset: Boolean
    ): FFmpegDecoderException? {
        if (reset) {
            nativeContext = ffmpegReset(nativeContext, extraData)
            if (nativeContext == 0L) {
                return FFmpegDecoderException("Error resetting (see logcat).")
            }
        }
        val inputData = Util.castNonNull(inputBuffer.data)
        val inputSize = inputData.limit()
        var outputData = outputBuffer.init(inputBuffer.timeUs, outputBufferSize)
        val result = ffmpegDecode(
            nativeContext, inputData, inputSize, outputBuffer, outputData, outputBufferSize
        )
        if (result == AUDIO_DECODER_ERROR_OTHER) {
            return FFmpegDecoderException("Error decoding (see logcat).")
        } else if (result == AUDIO_DECODER_ERROR_INVALID_DATA) {
            outputBuffer.shouldBeSkipped = true
            return null
        } else if (result == 0) {
            outputBuffer.shouldBeSkipped = true
            return null
        }
        if (!hasOutputFormat) {
            channelCount = ffmpegGetChannelCount(nativeContext)
            sampleRate = ffmpegGetSampleRate(nativeContext)
            if (sampleRate == 0 && "alac" == codecName) {
                checkNotNull(extraData)
                val parsableExtraData = ParsableByteArray(extraData)
                parsableExtraData.position = extraData.size - 4
                sampleRate = parsableExtraData.readUnsignedIntToInt()
            }
            hasOutputFormat = true
        }
        outputData = checkNotNull(outputBuffer.data)
        outputData.position(0)
        outputData.limit(result)
        return null
    }

    @Keep
    @Suppress("unused")
    private fun growOutputBuffer(outputBuffer: SimpleDecoderOutputBuffer, requiredSize: Int): ByteBuffer {
        outputBufferSize = requiredSize
        return outputBuffer.grow(requiredSize)
    }

    override fun release() {
        super.release()
        ffmpegRelease(nativeContext)
        nativeContext = 0L
    }

    private external fun ffmpegInitialize(
        codecName: String,
        extraData: ByteArray?,
        outputFloat: Boolean,
        rawSampleRate: Int,
        rawChannelCount: Int
    ): Long

    private external fun ffmpegDecode(
        context: Long,
        inputData: ByteBuffer,
        inputSize: Int,
        decoderOutputBuffer: SimpleDecoderOutputBuffer,
        outputData: ByteBuffer,
        outputSize: Int
    ): Int

    private external fun ffmpegGetChannelCount(context: Long): Int

    private external fun ffmpegGetSampleRate(context: Long): Int

    private external fun ffmpegReset(context: Long, extraData: ByteArray?): Long

    private external fun ffmpegRelease(context: Long)

    companion object {
        private const val INITIAL_OUTPUT_BUFFER_SIZE_16BIT = 65535
        private const val INITIAL_OUTPUT_BUFFER_SIZE_32BIT = INITIAL_OUTPUT_BUFFER_SIZE_16BIT * 2

        private const val AUDIO_DECODER_ERROR_INVALID_DATA = -1
        private const val AUDIO_DECODER_ERROR_OTHER = -2

        private val flacStreamMarker = byteArrayOf(
            'f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte()
        )
        private const val FLAC_METADATA_TYPE_STREAM_INFO = 0
        private const val FLAC_METADATA_BLOCK_HEADER_SIZE = 4
        private const val FLAC_STREAM_INFO_DATA_SIZE = 34

        private fun getExtraData(mimeType: String, initializationData: List<ByteArray>): ByteArray? {
            return when (mimeType) {
                MimeTypes.AUDIO_AAC, MimeTypes.AUDIO_OPUS -> initializationData.getOrNull(0)
                MimeTypes.AUDIO_ALAC -> getAlacExtraData(initializationData)
                MimeTypes.AUDIO_VORBIS -> getVorbisExtraData(initializationData)
                MimeTypes.AUDIO_FLAC -> getFlacExtraData(initializationData)
                else -> null
            }
        }

        private fun getAlacExtraData(initializationData: List<ByteArray>): ByteArray {
            val magicCookie = initializationData[0]
            val alacAtomLength = 12 + magicCookie.size
            val alacAtom = ByteBuffer.allocate(alacAtomLength)
            alacAtom.putInt(alacAtomLength)
            alacAtom.putInt(0x616c6163)
            alacAtom.putInt(0)
            alacAtom.put(magicCookie, 0, magicCookie.size)
            return alacAtom.array()
        }

        private fun getVorbisExtraData(initializationData: List<ByteArray>): ByteArray {
            val header0 = initializationData[0]
            val header1 = initializationData[1]
            val extraData = ByteArray(header0.size + header1.size + 6)
            extraData[0] = (header0.size shr 8).toByte()
            extraData[1] = (header0.size and 0xFF).toByte()
            System.arraycopy(header0, 0, extraData, 2, header0.size)
            extraData[header0.size + 2] = 0
            extraData[header0.size + 3] = 0
            extraData[header0.size + 4] = (header1.size shr 8).toByte()
            extraData[header0.size + 5] = (header1.size and 0xFF).toByte()
            System.arraycopy(header1, 0, extraData, header0.size + 6, header1.size)
            return extraData
        }

        private fun getFlacExtraData(initializationData: List<ByteArray>): ByteArray? {
            for (i in initializationData.indices) {
                val out = extractFlacStreamInfo(initializationData[i])
                if (out != null) {
                    return out
                }
            }
            return null
        }

        private fun extractFlacStreamInfo(data: ByteArray): ByteArray? {
            var offset = 0
            if (arrayStartsWith(data, flacStreamMarker)) {
                offset = flacStreamMarker.size
            }

            if (data.size - offset == FLAC_STREAM_INFO_DATA_SIZE) {
                val streamInfo = ByteArray(FLAC_STREAM_INFO_DATA_SIZE)
                System.arraycopy(data, offset, streamInfo, 0, FLAC_STREAM_INFO_DATA_SIZE)
                return streamInfo
            }

            if (data.size >= offset + FLAC_METADATA_BLOCK_HEADER_SIZE) {
                val type = data[offset].toInt() and 0x7F
                val length = ((data[offset + 1].toInt() and 0xFF) shl 16) or
                        ((data[offset + 2].toInt() and 0xFF) shl 8) or
                        (data[offset + 3].toInt() and 0xFF)

                if (type == FLAC_METADATA_TYPE_STREAM_INFO &&
                    length == FLAC_STREAM_INFO_DATA_SIZE &&
                    data.size >= offset + FLAC_METADATA_BLOCK_HEADER_SIZE + FLAC_STREAM_INFO_DATA_SIZE
                ) {
                    val streamInfo = ByteArray(FLAC_STREAM_INFO_DATA_SIZE)
                    System.arraycopy(
                        data,
                        offset + FLAC_METADATA_BLOCK_HEADER_SIZE,
                        streamInfo,
                        0,
                        FLAC_STREAM_INFO_DATA_SIZE
                    )
                    return streamInfo
                }
            }

            return null
        }

        private fun arrayStartsWith(data: ByteArray, prefix: ByteArray): Boolean {
            if (data.size < prefix.size) {
                return false
            }
            for (i in prefix.indices) {
                if (data[i] != prefix[i]) {
                    return false
                }
            }
            return true
        }
    }
}
