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

import com.fiepi.media.domain.player.model.PlayerEngineType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DecoderOptions(
    @SerialName("engine_type")
    val engineType: PlayerEngineType = PlayerEngineType.MPV,

    @SerialName("profile")
    val profile: String = "fast",
    @SerialName("vo")
    val vo: String = "gpu",
    @SerialName("gpu_api")
    val gpuApi: String = "opengl",
    @SerialName("hwdec")
    val hwdec: List<String> = listOf("mediacodec", "mediacodec-copy", "no"),
    @SerialName("framedrop")
    val framedrop: String = "vo",
    @SerialName("video_sync")
    val videoSync: String = "audio",
    @SerialName("tone_mapping")
    val toneMapping: String = "auto",
    @SerialName("tone_mapping_max_boost")
    val toneMappingMaxBoost: Float = 1.5f,
    @SerialName("gamut_mapping_mode")
    val gamutMappingMode: String = "perceptual",
    @SerialName("hdr_passthrough")
    val hdrPassthrough: Boolean = true,

    @SerialName("override_profile")
    val overrideProfile: Boolean = false,
    @SerialName("scale")
    val scale: String = "bilinear",
    @SerialName("dscale")
    val dscale: String = "mitchell",
    @SerialName("cscale")
    val cscale: String = "bilinear",
    @SerialName("scale_antiring")
    val scaleAntiring: Float = 0.0f,
    @SerialName("deband")
    val deband: Boolean = true,
    @SerialName("deband_iterations")
    val debandIterations: Int = 1,
    @SerialName("deband_threshold")
    val debandThreshold: Int = 48,
    @SerialName("deband_range")
    val debandRange: Int = 16,
    @SerialName("deband_grain")
    val debandGrain: Int = 32
)
