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

import android.graphics.Typeface
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BorderColor
import androidx.compose.material.icons.twotone.FontDownload
import androidx.compose.material.icons.twotone.FormatBold
import androidx.compose.material.icons.twotone.FormatColorFill
import androidx.compose.material.icons.twotone.FormatColorText
import androidx.compose.material.icons.twotone.FormatItalic
import androidx.compose.material.icons.twotone.FormatSize
import androidx.compose.material.icons.twotone.Layers
import androidx.compose.material.icons.twotone.LineWeight
import androidx.compose.material.icons.twotone.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.ColorPickerPreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SliderPreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePrefsIntent
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.MpvSubtitleOptions
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import com.fiepi.media.domain.utils.FontScanner
import com.fiepi.media.player.ui.OutlineText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun MpvSubtitleStylePage(
    options: SubtitleOptions,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit = {},
    onIntent: (SubtitlePrefsIntent) -> Unit,
    scrollState: LazyListState = rememberLazyListState(),
) {
    val mpv = options.mpvSubtitle

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            MpvSubtitlePreviewCard(options = mpv)
        }

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_subtitle_header_mpv),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_font),
                            summary = if (mpv.subFont == "sans-serif") stringResource(R.string.settings_subtitle_font_default) else mpv.subFont,
                            onClick = {
                                onNavigateToSubPage(SettingsSubPage.Subtitle.MPV_SUBTITLE_FONT)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FontDownload,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_font_size),
                            summary = "${mpv.subFontSize}",
                            value = mpv.subFontSize.toFloat(),
                            showTickMarks = false,
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(subFontSize = it.roundToInt())
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(subFontSize = 50)
                                    )
                                )
                            },
                            range = 20f..100f,
                            steps = 80,
                            valueFormat = "%.0f",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FormatSize,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_bold),
                            summary = stringResource(R.string.settings_subtitle_bold_summary),
                            checked = mpv.subBold,
                            onCheckedChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(subBold = it)
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FormatBold,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_italic),
                            summary = stringResource(R.string.settings_subtitle_italic_summary),
                            checked = mpv.subItalic,
                            onCheckedChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(subItalic = it)
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FormatItalic,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        ColorPickerPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_text_color),
                            colors = listOf(
                                "#FFFFFF",
                                "#E0E0E0",
                                "#FFFF00",
                                "#00FFFF",
                                "#00FF00",
                                "#B9F6CA",
                                "#80D8FF",
                                "#FF5252",
                                "#FF9800",
                                "#FF4081"
                            ),
                            currentColor = mpv.fontColorHex,
                            onColorChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(fontColorHex = it)
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FormatColorText,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        ColorPickerPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_outline_color),
                            colors = listOf(
                                "#212121",
                                "#FFFFFF",
                                "#D50000",
                                "#880E4F",
                                "#00E676",
                                "#2979FF",
                                "#1A237E",
                                "#FFEA00",
                                "#AA00FF"
                            ),
                            currentColor = mpv.outlineColorHex,
                            onColorChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(outlineColorHex = it)
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.BorderColor,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_outline_width),
                            summary = "${mpv.outlineWidth}",
                            value = mpv.outlineWidth.toFloat(),
                            showTickMarks = true,
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(outlineWidth = it.roundToInt())
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(outlineWidth = 2)
                                    )
                                )
                            },
                            range = 0f..8f,
                            steps = 7,
                            valueFormat = "%.0f",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.LineWeight,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        ColorPickerPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_shadow_color),
                            colors = listOf(
                                "#000000",
                                "#424242",
                                "#FFFFFF",
                                "#D50000",
                                "#00E676",
                                "#2979FF",
                                "#FFEA00"
                            ),
                            currentColor = mpv.shadowColorHex,
                            onColorChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(shadowColorHex = it)
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FormatColorFill,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_shadow_offset),
                            summary = "${mpv.shadowOffset}",
                            value = mpv.shadowOffset.toFloat(),
                            showTickMarks = true,
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(shadowOffset = it.roundToInt())
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(shadowOffset = 0)
                                    )
                                )
                            },
                            range = 0f..10f,
                            steps = 9,
                            valueFormat = "%.0f",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Layers,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_bottom_margin),
                            summary = "${mpv.bottomMargin}",
                            value = mpv.bottomMargin.toFloat(),
                            showTickMarks = false,
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(bottomMargin = it.roundToInt())
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                        mpv.copy(bottomMargin = 12)
                                    )
                                )
                            },
                            range = 0f..120f,
                            steps = 119,
                            valueFormat = "%.0f",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.VerticalAlignBottom,
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

@Composable
private fun MpvSubtitlePreviewCard(options: MpvSubtitleOptions) {
    val parsedTextColor = try {
        Color(options.fontColorHex.toColorInt())
    } catch (_: Exception) {
        Color.White
    }

    val parsedOutlineColor = try {
        Color(options.outlineColorHex.toColorInt())
    } catch (_: Exception) {
        Color.Black
    }

    val parsedShadowColor = try {
        Color(options.shadowColorHex.toColorInt())
    } catch (_: Exception) {
        Color.Black
    }

    val previewFontFamily by produceState<FontFamily?>(
        initialValue = null,
        options.subFont,
        options.subFontsDir
    ) {
        value = if (options.subFont.isBlank() || options.subFont == "sans-serif") {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val targetFontsDir = options.subFontsDir.ifBlank {
                        File(Environment.getExternalStorageDirectory(), "Fonts").absolutePath
                    }
                    val matched = FontScanner.scanFontFamilies(targetFontsDir)
                        .firstOrNull { it.familyName == options.subFont }
                    if (matched != null) {
                        val file = File(matched.filePath)
                        if (file.exists() && file.canRead()) {
                            FontFamily(Typeface.createFromFile(file))
                        } else null
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = (options.bottomMargin * 0.5f).dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (options.shadowOffset > 0) {
                OutlineText(
                    text = stringResource(R.string.settings_subtitle_mpv_preview_sample),
                    style = TextStyle(
                        color = parsedShadowColor,
                        fontSize = (options.subFontSize * 0.35f).sp,
                        fontWeight = if (options.subBold) FontWeight.W900 else FontWeight.W500,
                        fontStyle = if (options.subItalic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = previewFontFamily,
                        textAlign = TextAlign.Center
                    ),
                    outlineColor = parsedShadowColor,
                    outlineWidth = options.outlineWidth.dp,
                    modifier = Modifier.offset(
                        x = (options.shadowOffset * 0.3f).dp,
                        y = (options.shadowOffset * 0.4f).dp
                    )
                )
            }
            OutlineText(
                text = stringResource(R.string.settings_subtitle_mpv_preview_sample),
                style = TextStyle(
                    color = parsedTextColor,
                    fontSize = (options.subFontSize * 0.35f).sp,
                    fontWeight = if (options.subBold) FontWeight.W900 else FontWeight.W500,
                    fontStyle = if (options.subItalic) FontStyle.Italic else FontStyle.Normal,
                    fontFamily = previewFontFamily,
                    textAlign = TextAlign.Center
                ),
                outlineColor = parsedOutlineColor,
                outlineWidth = options.outlineWidth.dp
            )
        }

        Text(
            text = stringResource(R.string.common_preview),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MpvSubtitleStylePagePreview() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            MpvSubtitleStylePage(
                options = SubtitleOptions(
                    mpvSubtitle = MpvSubtitleOptions()
                ),
                contentPadding = PaddingValues(16.dp),
                onIntent = {}
            )
        }
    }
}
