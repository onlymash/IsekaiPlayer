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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.twotone.FormatSize
import androidx.compose.material.icons.twotone.LineWeight
import androidx.compose.material.icons.twotone.Opacity
import androidx.compose.material.icons.twotone.VerticalAlignBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.ColorPickerPreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SliderPreferencesItem
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.SubtitlePrefsIntent
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.ExoSubtitleOptions
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import com.fiepi.media.player.ui.ExoSubtitleOverlay
import kotlin.math.roundToInt

@Composable
fun ExoSubtitleStylePage(
    options: SubtitleOptions,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit = {},
    onIntent: (SubtitlePrefsIntent) -> Unit,
    scrollState: LazyListState = rememberLazyListState(),
) {
    val exo = options.exoSubtitle

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            ExoSubtitlePreviewCard(options = exo)
        }

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_subtitle_header_exo),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_font),
                            summary = if (exo.fontFamily == "sans-serif") stringResource(R.string.settings_subtitle_font_default) else exo.fontFamily,
                            onClick = {
                                onNavigateToSubPage(SettingsSubPage.Subtitle.EXO_SUBTITLE_FONT)
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
                            summary = "${exo.fontSizeSp} sp",
                            value = exo.fontSizeSp.toFloat(),
                            showTickMarks = false,
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(fontSizeSp = it.roundToInt())
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(fontSizeSp = 18)
                                    )
                                )
                            },
                            range = 12f..36f,
                            steps = 23,
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
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_font_weight),
                            summary = "${exo.fontWeight}",
                            value = exo.fontWeight.toFloat(),
                            showTickMarks = true,
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(fontWeight = (it / 100).roundToInt() * 100)
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(fontWeight = 500)
                                    )
                                )
                            },
                            range = 100f..900f,
                            steps = 7,
                            valueFormat = "%.0f",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FormatBold,
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
                            currentColor = exo.textColorHex,
                            onColorChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(textColorHex = it)
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
                            currentColor = exo.outlineColorHex,
                            onColorChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(outlineColorHex = it)
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
                            summary = "${exo.outlineWidthDp} dp",
                            value = exo.outlineWidthDp.toFloat(),
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(outlineWidthDp = it.roundToInt())
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(outlineWidthDp = 2)
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
                            title = stringResource(R.string.settings_subtitle_bg_color),
                            colors = listOf(
                                "#000000",
                                "#37474F",
                                "#1A237E",
                                "#1B5E20",
                                "#4A148C",
                                "#3E2723",
                                "#FFFFFF"
                            ),
                            currentColor = exo.bgColorHex,
                            onColorChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(bgColorHex = it)
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
                            title = stringResource(R.string.settings_subtitle_bg_opacity),
                            summary = "${(exo.bgOpacity * 100).roundToInt()}%",
                            value = exo.bgOpacity,
                            showTickMarks = false,
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(bgOpacity = it)
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(bgOpacity = 0.1f)
                                    )
                                )
                            },
                            range = 0f..1f,
                            steps = 19,
                            valueFormat = "%.2f",
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Opacity,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_bottom_padding),
                            summary = "${exo.bottomPaddingDp} dp",
                            value = exo.bottomPaddingDp.toFloat(),
                            showTickMarks = false,
                            onValueChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(bottomPaddingDp = it.roundToInt())
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                                        exo.copy(bottomPaddingDp = 12)
                                    )
                                )
                            },
                            range = 0f..120f,
                            steps = 119,
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
private fun ExoSubtitlePreviewCard(options: ExoSubtitleOptions) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ExoSubtitleOverlay(
            text = stringResource(R.string.settings_subtitle_exo_preview_sample),
            options = options
        )

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
private fun ExoSubtitleStylePagePreview() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            ExoSubtitleStylePage(
                options = SubtitleOptions(
                    exoSubtitle = ExoSubtitleOptions(
                        fontSizeSp = 18,
                        textColorHex = "#FFFFFF",
                        bgColorHex = "#000000",
                        bgOpacity = 0.1f,
                        bottomPaddingDp = 12,
                        fontWeight = 500
                    )
                ),
                contentPadding = PaddingValues(16.dp),
                onIntent = {}
            )
        }
    }
}
