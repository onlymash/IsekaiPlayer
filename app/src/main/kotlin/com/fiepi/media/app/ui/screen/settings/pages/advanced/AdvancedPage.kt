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

package com.fiepi.media.app.ui.screen.settings.pages.advanced

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.DeleteSweep
import androidx.compose.material.icons.twotone.FastForward
import androidx.compose.material.icons.twotone.FastRewind
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.PermIdentity
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Timer
import androidx.compose.material.icons.twotone.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SliderPreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.cache.CacheInfo
import com.fiepi.media.domain.model.preferences.AdvancedOptions
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun AdvancedPage(
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    viewModel: AdvancedPrefsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val options by viewModel.advancedOptions.collectAsState()
    val cacheInfo by viewModel.cacheInfo.collectAsState()
    val historyCount by viewModel.historyCount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AdvancedEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AdvancedPageContent(
        options = options,
        cacheInfo = cacheInfo,
        historyCount = historyCount,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        onNavigateToSubPage = onNavigateToSubPage
    )
}

@Composable
fun AdvancedPageContent(
    options: AdvancedOptions,
    cacheInfo: CacheInfo,
    historyCount: Int,
    onIntent: (AdvancedPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_advanced_header_storage_cache),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_clear_thumbnails),
                            summary = "${stringResource(R.string.settings_advanced_clear_thumbnails_summary)} (${
                                Formatter.formatShortFileSize(
                                    context,
                                    cacheInfo.thumbnailCacheBytes
                                )
                            })",
                            onClick = { onIntent(AdvancedPrefsIntent.ClearThumbnailCache) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.DeleteSweep,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_clear_video_cache),
                            summary = "${stringResource(R.string.settings_advanced_clear_video_cache_summary)} (${
                                Formatter.formatShortFileSize(
                                    context,
                                    cacheInfo.videoCacheBytes
                                )
                            })",
                            onClick = { onIntent(AdvancedPrefsIntent.ClearVideoCache) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.VideoLibrary,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_clear_history),
                            summary = "${stringResource(R.string.settings_advanced_clear_history_summary)} (${
                                stringResource(
                                    R.string.settings_advanced_history_count_format,
                                    historyCount
                                )
                            })",
                            onClick = { onIntent(AdvancedPrefsIntent.ClearPlaybackHistory) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.History,
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
                title = stringResource(R.string.settings_advanced_header_connection),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_user_agent),
                            summary = options.userAgent,
                            onClick = { onNavigateToSubPage(SettingsSubPage.Advanced.USER_AGENT) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.PermIdentity,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_timeout),
                            summary = stringResource(R.string.settings_advanced_timeout_summary),
                            value = options.networkTimeout.toFloat(),
                            onValueChange = { onIntent(AdvancedPrefsIntent.UpdateNetworkTimeout(it.roundToInt())) },
                            onReset = { onIntent(AdvancedPrefsIntent.UpdateNetworkTimeout((AppConstants.REMOTE_TIMEOUT_MS / 1000).toInt())) },
                            range = 5f..60f,
                            steps = 10,
                            unit = "s",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Timer,
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
                title = stringResource(R.string.settings_advanced_header_cache),
                items = arrayOf(
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_cache_secs),
                            summary = stringResource(R.string.settings_advanced_cache_secs_summary),
                            value = options.cacheSecs.toFloat(),
                            onValueChange = { onIntent(AdvancedPrefsIntent.UpdateCacheSecs(it.roundToInt())) },
                            onReset = { onIntent(AdvancedPrefsIntent.UpdateCacheSecs(30)) },
                            range = 0f..300f,
                            steps = 29,
                            unit = "s",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FastForward,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_cache_back_secs),
                            summary = stringResource(R.string.settings_advanced_cache_back_secs_summary),
                            value = options.cacheBackSecs.toFloat(),
                            onValueChange = { onIntent(AdvancedPrefsIntent.UpdateCacheBackSecs(it.roundToInt())) },
                            onReset = { onIntent(AdvancedPrefsIntent.UpdateCacheBackSecs(10)) },
                            range = 0f..120f,
                            steps = 23,
                            unit = "s",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FastRewind,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        val currentMb = (options.demuxerMaxBytes / (1024 * 1024)).toFloat()
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_demuxer_max_bytes),
                            summary = stringResource(R.string.settings_advanced_demuxer_max_bytes_summary),
                            value = currentMb,
                            onValueChange = {
                                onIntent(AdvancedPrefsIntent.UpdateDemuxerMaxBytes((it * 1024 * 1024).roundToLong()))
                            },
                            onReset = {
                                onIntent(AdvancedPrefsIntent.UpdateDemuxerMaxBytes(32 * 1024 * 1024L))
                            },
                            range = 8f..512f,
                            steps = 62,
                            unit = "MB",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Save,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        val currentBackMb = (options.demuxerMaxBackBytes / (1024 * 1024)).toFloat()
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_demuxer_max_back_bytes),
                            summary = stringResource(R.string.settings_advanced_demuxer_max_back_bytes_summary),
                            value = currentBackMb,
                            onValueChange = {
                                onIntent(AdvancedPrefsIntent.UpdateDemuxerMaxBackBytes((it * 1024 * 1024).roundToLong()))
                            },
                            onReset = {
                                onIntent(AdvancedPrefsIntent.UpdateDemuxerMaxBackBytes(8 * 1024 * 1024L))
                            },
                            range = 0f..256f,
                            steps = 31,
                            unit = "MB",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.History,
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
                title = stringResource(R.string.settings_advanced_header_security),
                items = arrayOf(
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_advanced_tls_verify),
                            summary = stringResource(R.string.settings_advanced_tls_verify_summary),
                            checked = options.tlsVerify,
                            onCheckedChange = { onIntent(AdvancedPrefsIntent.UpdateTlsVerify(it)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Security,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            )
        }
    }
}
