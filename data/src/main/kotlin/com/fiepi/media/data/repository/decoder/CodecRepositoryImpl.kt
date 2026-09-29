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

package com.fiepi.media.data.repository.decoder

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import com.fiepi.media.domain.model.decoder.CodecInfo
import com.fiepi.media.domain.repository.decoder.CodecRepository

class CodecRepositoryImpl : CodecRepository {

    override fun getSystemCodecs(): List<CodecInfo> {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        return codecList.codecInfos
            .filter { !it.isEncoder }
            .distinctBy { it.name }
            .map { info ->
                val types = info.supportedTypes.toList()
                val isVid = types.any { it.startsWith("video/", ignoreCase = true) }
                val isAud = types.any { it.startsWith("audio/", ignoreCase = true) }
                val shortName = getShortFormatName(types)
                val maxRes = if (isVid) getMaxResolutionForDecoder(info, types) else null
                CodecInfo(
                    name = info.name,
                    canonicalName = info.canonicalName,
                    shortFormatName = shortName,
                    maxResolution = maxRes,
                    isHardware = info.isHardwareAccelerated,
                    isSoftwareOnly = info.isSoftwareOnly,
                    isVendor = info.isVendor,
                    mimeTypes = types,
                    isVideo = isVid,
                    isAudio = isAud
                )
            }
    }

    private fun getShortFormatName(mimeTypes: List<String>): String {
        val names = mimeTypes.map { mime ->
            when (mime.lowercase()) {
                "video/avc" -> "H.264 / AVC"
                "video/hevc" -> "H.265 / HEVC"
                "video/av01" -> "AV1"
                "video/x-vnd.on2.vp9" -> "VP9"
                "video/x-vnd.on2.vp8" -> "VP8"
                "video/mpeg2" -> "MPEG-2"
                "video/mp4v-es" -> "MPEG-4"
                "video/3gpp" -> "H.263"
                "video/dolby-vision" -> "Dolby Vision"
                "video/vc1" -> "VC-1"
                "audio/mp4a-latm" -> "AAC"
                "audio/mpeg" -> "MP3"
                "audio/flac" -> "FLAC"
                "audio/ac3" -> "AC-3"
                "audio/eac3" -> "E-AC-3"
                "audio/opus" -> "Opus"
                "audio/vorbis" -> "Vorbis"
                "audio/raw" -> "PCM"
                "audio/alac" -> "ALAC"
                "audio/g711-alaw" -> "G.711 a-law"
                "audio/g711-mlaw" -> "G.711 mu-law"
                "audio/amr-wb" -> "AMR-WB"
                "audio/3gpp" -> "AMR-NB"
                else -> {
                    val subtype = mime.substringAfter('/')
                    if (subtype.startsWith("x-")) subtype.substring(2)
                        .uppercase() else subtype.uppercase()
                }
            }
        }.distinct()
        return if (names.isNotEmpty()) names.joinToString(" / ") else ""
    }

    private fun getMaxResolutionForDecoder(info: MediaCodecInfo, mimeTypes: List<String>): String? {
        if (!info.isHardwareAccelerated) return null
        var maxW = 0
        var maxH = 0

        for (mime in mimeTypes) {
            if (!mime.startsWith("video/", ignoreCase = true)) continue
            try {
                val caps = info.getCapabilitiesForType(mime)
                val videoCaps = caps?.videoCapabilities ?: continue
                val w = videoCaps.supportedWidths.upper
                val h = videoCaps.supportedHeights.upper
                if (w * h > maxW * maxH) {
                    maxW = w
                    maxH = h
                }
            } catch (_: Exception) {
                // Ignore if capabilities cannot be retrieved for specific mime
            }
        }

        if (maxW == 0 || maxH == 0) return null

        val label = when {
            maxW >= 7680 || maxH >= 4320 -> "8K"
            maxW >= 3840 || maxH >= 2160 -> "4K"
            maxW >= 2560 || maxH >= 1440 -> "2K"
            maxW >= 1920 || maxH >= 1080 -> "1080p"
            maxW >= 1280 || maxH >= 720 -> "720p"
            else -> null
        }

        return if (label != null) {
            "$label (${maxW}x${maxH})"
        } else {
            "${maxW}x${maxH}"
        }
    }
}
