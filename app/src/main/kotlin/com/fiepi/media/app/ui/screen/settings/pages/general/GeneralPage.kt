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

package com.fiepi.media.app.ui.screen.settings.pages.general

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.FileDownload
import androidx.compose.material.icons.twotone.FileUpload
import androidx.compose.material.icons.twotone.Navigation
import androidx.compose.material.icons.twotone.Translate
import androidx.compose.material.icons.twotone.Vibration
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
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.domain.config.AppConstants
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun GeneralPage(
    contentPadding: PaddingValues,
    viewModel: GeneralPrefsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is GeneralEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    GeneralPageContent(
        state = state,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding
    )
}

@Composable
fun GeneralPageContent(
    state: GeneralUiState,
    onIntent: (GeneralPrefsIntent) -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onIntent(GeneralPrefsIntent.ExportBackup(it.toString())) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onIntent(GeneralPrefsIntent.ImportBackup(it.toString())) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_general_header_general),
                items = arrayOf(
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_general_haptics),
                            summary = stringResource(R.string.settings_general_haptics_summary),
                            checked = state.isHapticsEnabled,
                            onCheckedChange = { onIntent(GeneralPrefsIntent.SetHapticsEnabled(it)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Vibration,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_general_app_language),
                            summary = stringResource(R.string.settings_general_app_language_summary),
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Translate,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                )
            )
        }
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_general_header_navigation),
                items = arrayOf(
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_general_intercept_back),
                            summary = stringResource(R.string.settings_general_intercept_back_summary),
                            checked = state.interceptBackNavigation,
                            onCheckedChange = {
                                onIntent(
                                    GeneralPrefsIntent.SetInterceptBackNavigation(
                                        it
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Navigation,
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
                title = stringResource(R.string.settings_general_header_backup),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_general_export_settings),
                            summary = stringResource(R.string.settings_general_export_settings_summary),
                            onClick = {
                                val dateTimeStr = LocalDateTime.now()
                                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                                exportLauncher.launch("${AppConstants.APP_NAME}_backup_$dateTimeStr.json")
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FileUpload,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_general_import_settings),
                            summary = stringResource(R.string.settings_general_import_settings_summary),
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FileDownload,
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
