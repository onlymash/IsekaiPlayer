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

package com.fiepi.media.app.ui.screen.settings.pages.decoder

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material.icons.twotone.BlurOn
import androidx.compose.material.icons.twotone.DeveloperBoard
import androidx.compose.material.icons.twotone.HdrOn
import androidx.compose.material.icons.twotone.HighQuality
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.SettingsSuggest
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material.icons.twotone.Sync
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material.icons.twotone.VideoSettings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.domain.model.preferences.DecoderOptions
import com.fiepi.media.domain.player.model.PlayerEngineType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DecoderPage(
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    viewModel: DecoderPrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.decoderOptions.collectAsState()

    DecoderPageContent(
        options = options,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        onNavigateToSubPage = onNavigateToSubPage,
        scrollState = scrollState
    )
}

@Composable
fun DecoderPageContent(
    options: DecoderOptions,
    onIntent: (DecoderPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    scrollState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_decoder_header_engine),
                items = listOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_decoder_player_engine_type),
                            summary = when (options.engineType) {
                                PlayerEngineType.MPV -> stringResource(R.string.settings_decoder_player_engine_mpv)
                                PlayerEngineType.EXO_PLAYER -> stringResource(R.string.settings_decoder_player_engine_exo)
                            },
                            onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.ENGINE_TYPE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Memory,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            )
        }

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_decoder_header_info),
                items = listOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_decoder_codec_info),
                            summary = stringResource(R.string.settings_decoder_codec_info_summary),
                            onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.CODEC_INFO) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.DeveloperBoard,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            )
        }

        if (options.engineType == PlayerEngineType.MPV) {
            mpvEngineItems(
                options = options,
                onIntent = onIntent,
                onNavigateToSubPage = onNavigateToSubPage
            )
        }
    }
}


private fun LazyListScope.mpvEngineItems(
    options: DecoderOptions,
    onIntent: (DecoderPrefsIntent) -> Unit,
    onNavigateToSubPage: (String) -> Unit
) {
    item {
        PreferencesGroup(
            title = stringResource(R.string.settings_decoder_header_rendering),
            items = listOf(
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_profile),
                        summary = when (options.profile) {
                            "fast" -> stringResource(R.string.settings_decoder_profile_fast)
                            "default" -> stringResource(R.string.settings_decoder_profile_default)
                            "high-quality" -> stringResource(R.string.settings_decoder_profile_high_quality)
                            "gpu-hq" -> stringResource(R.string.settings_decoder_profile_gpu_hq)
                            "low-latency" -> stringResource(R.string.settings_decoder_profile_low_latency)
                            "sw-fast" -> stringResource(R.string.settings_decoder_profile_sw_fast)
                            else -> options.profile
                        },
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.PROFILE) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Speed,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_vo),
                        summary = when (options.vo) {
                            "gpu" -> stringResource(R.string.settings_decoder_vo_gpu)
                            "gpu-next" -> stringResource(R.string.settings_decoder_vo_gpu_next)
                            else -> options.vo
                        },
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.VO) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.VideoSettings,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_gpu_api),
                        summary = when (options.gpuApi) {
                            "opengl" -> stringResource(R.string.settings_decoder_gpu_api_opengl)
                            "vulkan" -> stringResource(R.string.settings_decoder_gpu_api_vulkan)
                            else -> options.gpuApi
                        },
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.GPU_API) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.SettingsSuggest,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        )
    }

    item {
        PreferencesGroup(
            title = stringResource(R.string.settings_decoder_header_performance),
            items = listOf(
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_hwdec),
                        summary = options.hwdec.joinToString(", "),
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.HWDEC) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Memory,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_framedrop),
                        summary = when (options.framedrop) {
                            "vo" -> stringResource(R.string.settings_decoder_framedrop_vo)
                            "decoder" -> stringResource(R.string.settings_decoder_framedrop_decoder)
                            "no" -> stringResource(R.string.settings_decoder_framedrop_no)
                            else -> options.framedrop
                        },
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.FRAMEDROP) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.BlurOn,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_video_sync),
                        summary = when (options.videoSync) {
                            "audio" -> stringResource(R.string.settings_decoder_video_sync_audio)
                            "display-resample" -> stringResource(R.string.settings_decoder_video_sync_display_resample)
                            "display-resample-vdrop" -> stringResource(R.string.settings_decoder_video_sync_display_resample_vdrop)
                            "display-resample-desync" -> stringResource(R.string.settings_decoder_video_sync_display_resample_desync)
                            "display-tempo" -> stringResource(R.string.settings_decoder_video_sync_display_tempo)
                            "display-vdrop" -> stringResource(R.string.settings_decoder_video_sync_display_vdrop)
                            "display-adrop" -> stringResource(R.string.settings_decoder_video_sync_display_adrop)
                            "display-desync" -> stringResource(R.string.settings_decoder_video_sync_display_desync)
                            else -> options.videoSync
                        },
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.VIDEO_SYNC) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Sync,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        )
    }

    item {
        PreferencesGroup(
            title = stringResource(R.string.settings_decoder_header_processing),
            items = listOf(
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_hdr_settings),
                        summary = stringResource(R.string.settings_decoder_hdr_settings_summary),
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.HDR) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.HdrOn,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SwitchPreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_override_profile),
                        summary = stringResource(R.string.settings_decoder_override_profile_summary),
                        checked = options.overrideProfile,
                        onCheckedChange = {
                            onIntent(
                                DecoderPrefsIntent.UpdateOverrideProfile(
                                    it
                                )
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.Tune,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_scale),
                        enabled = options.overrideProfile,
                        summary = scaleSummary(options.scale),
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.SCALE) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.HighQuality,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_dscale),
                        enabled = options.overrideProfile,
                        summary = scaleSummary(options.dscale),
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.DSCALE) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.HighQuality,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_cscale),
                        enabled = options.overrideProfile,
                        summary = scaleSummary(options.cscale),
                        onClick = { onNavigateToSubPage(SettingsSubPage.Decoder.CSCALE) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.HighQuality,
                                contentDescription = null
                            )
                        }
                    )
                },
                { shapes ->
                    SwitchPreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_decoder_deband),
                        enabled = options.overrideProfile,
                        summary = stringResource(R.string.settings_decoder_deband_summary),
                        checked = options.deband,
                        onCheckedChange = { onIntent(DecoderPrefsIntent.UpdateDeband(it)) },
                        onSecondaryClick = { onNavigateToSubPage(SettingsSubPage.Decoder.DEBAND) },
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.AutoAwesome,
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        )
    }
}

@Composable
private fun scaleSummary(value: String): String {
    return when (value) {
        "bilinear" -> stringResource(R.string.settings_decoder_scale_bilinear)
        "hermite" -> stringResource(R.string.settings_decoder_scale_hermite)
        "mitchell" -> stringResource(R.string.settings_decoder_scale_mitchell)
        "catmull_rom" -> stringResource(R.string.settings_decoder_scale_catmull_rom)
        "spline36" -> stringResource(R.string.settings_decoder_scale_spline36)
        "lanczos" -> stringResource(R.string.settings_decoder_scale_lanczos)
        "ewa_lanczos" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos)
        "ewa_lanczossharp" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos_sharp)
        "ewa_lanczos4sharpest" -> stringResource(R.string.settings_decoder_scale_ewa_lanczos_4sharpest)
        "oversample" -> stringResource(R.string.settings_decoder_scale_oversample)
        else -> value
    }
}