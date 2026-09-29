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

package com.fiepi.media.app.ui.screen.settings.pages.subtitle.subpage

import android.content.Intent
import android.graphics.Typeface
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.FolderSpecial
import androidx.compose.material.icons.twotone.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SelectablePreferencesItem
import com.fiepi.media.domain.utils.FontScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SubtitleFontSelectionPage(
    selectedFont: String,
    customFontsDir: String,
    onFontSelected: (String) -> Unit,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
) {
    val context = LocalContext.current

    var isPermissionGranted by remember {
        mutableStateOf(Environment.isExternalStorageManager())
    }

    LifecycleResumeEffect(Unit) {
        isPermissionGranted = Environment.isExternalStorageManager()
        onPauseOrDispose { }
    }

    val defaultFontsDir = remember {
        File(Environment.getExternalStorageDirectory(), "Fonts").absolutePath
    }

    val currentFontsDir = customFontsDir.ifBlank { defaultFontsDir }

    val scannedFonts by produceState(
        initialValue = emptyList(),
        isPermissionGranted,
        currentFontsDir
    ) {
        value = if (isPermissionGranted) {
            withContext(Dispatchers.IO) {
                FontScanner.scanFontFamilies(currentFontsDir)
            }
        } else {
            emptyList()
        }
    }

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        if (!isPermissionGranted) {
            item {
                PreferencesGroup(
                    items = listOf { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_font_permission_required),
                            summary = stringResource(R.string.settings_subtitle_font_permission_summary),
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Lock,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        "package:${context.packageName}".toUri()
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent =
                                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                )
            }
        }

        item {
            val groupItems = mutableListOf<@Composable (shapes: ListItemShapes) -> Unit>()

            // Default system font option
            groupItems.add { shapes ->
                SelectablePreferencesItem(
                    shapes = shapes,
                    title = stringResource(R.string.settings_subtitle_font_default),
                    summary = "sans-serif",
                    selected = selectedFont == "sans-serif",
                    onClick = { onFontSelected("sans-serif") }
                )
            }

            // Scanned custom fonts
            for (font in scannedFonts) {
                val customFontFamily = remember(font.filePath) {
                    try {
                        val file = File(font.filePath)
                        if (file.exists() && file.canRead()) {
                            FontFamily(Typeface.createFromFile(file))
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }

                groupItems.add { shapes ->
                    SelectablePreferencesItem(
                        shapes = shapes,
                        title = font.familyName,
                        titleFontFamily = customFontFamily,
                        summary = font.fileName,
                        selected = selectedFont == font.familyName,
                        onClick = { onFontSelected(font.familyName) }
                    )
                }
            }

            // Empty state if permission granted but no fonts found
            if (isPermissionGranted && scannedFonts.isEmpty()) {
                groupItems.add { shapes ->
                    PreferencesItem(
                        shapes = shapes,
                        title = stringResource(R.string.settings_subtitle_font_no_fonts_found),
                        summary = stringResource(
                            R.string.settings_subtitle_font_no_fonts_summary,
                            currentFontsDir
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.TwoTone.FolderSpecial,
                                contentDescription = null
                            )
                        },
                        onClick = {}
                    )
                }
            }

            PreferencesGroup(
                title = stringResource(R.string.settings_subtitle_font),
                items = groupItems
            )
        }

        item {
            Text(
                text = stringResource(
                    R.string.settings_subtitle_font_directory_hint,
                    currentFontsDir
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    }
}
