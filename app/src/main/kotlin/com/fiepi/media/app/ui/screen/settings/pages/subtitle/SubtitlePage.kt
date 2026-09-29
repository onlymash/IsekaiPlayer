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

package com.fiepi.media.app.ui.screen.settings.pages.subtitle

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.AutoAwesome
import androidx.compose.material.icons.twotone.FindReplace
import androidx.compose.material.icons.twotone.Language
import androidx.compose.material.icons.twotone.Subtitles
import androidx.compose.material.icons.twotone.Translate
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
import com.fiepi.media.domain.model.assets.Language
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubtitlePage(
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    viewModel: SubtitlePrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.subtitleOptions.collectAsState()
    val languages by viewModel.languages.collectAsState()

    SubtitlePageContent(
        options = options,
        languages = languages,
        onIntent = viewModel::onIntent,
        contentPadding = contentPadding,
        onNavigateToSubPage = onNavigateToSubPage,
        scrollState = scrollState
    )
}

@Composable
fun SubtitlePageContent(
    options: SubtitleOptions,
    languages: List<Language>,
    onIntent: (SubtitlePrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit,
    scrollState: LazyListState = rememberLazyListState()
) {
    val currentLanguageSummary = if (options.preferredSubtitleLanguages.isEmpty()) {
        stringResource(R.string.settings_lang_preferred_language_none)
    } else {
        options.preferredSubtitleLanguages.joinToString(", ") { id ->
            languages.find { it.id == id }?.native ?: id
        }
    }

    val currentSubCodepageSummary = when (options.subCodepage) {
        "auto" -> stringResource(R.string.settings_subtitle_codepage_auto)
        "utf-8" -> stringResource(R.string.settings_subtitle_codepage_utf8)
        "gbk" -> stringResource(R.string.settings_subtitle_codepage_gbk)
        "big5" -> stringResource(R.string.settings_subtitle_codepage_big5)
        else -> options.subCodepage.uppercase()
    }

    val currentSubAssOverrideSummary = when (options.mpvSubtitle.subAssOverride) {
        "scale" -> stringResource(R.string.settings_subtitle_mpv_ass_override_scale)
        "yes" -> stringResource(R.string.settings_subtitle_mpv_ass_override_yes)
        "no" -> stringResource(R.string.settings_subtitle_mpv_ass_override_no)
        "force" -> stringResource(R.string.settings_subtitle_mpv_ass_override_force)
        "strip" -> stringResource(R.string.settings_subtitle_mpv_ass_override_strip)
        else -> options.mpvSubtitle.subAssOverride
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        state = scrollState
    ) {
        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_subtitle_header_selection),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_preferred_language),
                            summary = currentLanguageSummary,
                            onClick = { onNavigateToSubPage(SettingsSubPage.Subtitle.PREFERRED_LANGUAGE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Language,
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
                title = stringResource(R.string.settings_subtitle_header_exo),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_exo_style),
                            summary = stringResource(R.string.settings_subtitle_exo_style_summary),
                            onClick = { onNavigateToSubPage(SettingsSubPage.Subtitle.EXO_SUBTITLE_STYLE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Subtitles,
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
                title = stringResource(R.string.settings_subtitle_header_mpv),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_mpv_style),
                            summary = stringResource(R.string.settings_subtitle_mpv_style_summary),
                            onClick = { onNavigateToSubPage(SettingsSubPage.Subtitle.MPV_SUBTITLE_STYLE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Subtitles,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_mpv_ass_override),
                            summary = currentSubAssOverrideSummary,
                            onClick = { onNavigateToSubPage(SettingsSubPage.Subtitle.SUB_ASS_OVERRIDE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.AutoAwesome,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_codepage),
                            summary = currentSubCodepageSummary,
                            onClick = { onNavigateToSubPage(SettingsSubPage.Subtitle.SUB_CODEPAGE) },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Translate,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_subtitle_fallback),
                            summary = stringResource(R.string.settings_subtitle_fallback_summary),
                            checked = options.subtitleFallback,
                            onCheckedChange = {
                                onIntent(
                                    SubtitlePrefsIntent.UpdateSubtitleFallback(
                                        it
                                    )
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.FindReplace,
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
