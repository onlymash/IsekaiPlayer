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
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.LanguageSelectionListPage
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferredLanguageListPage
import com.fiepi.media.app.ui.screen.settings.components.SelectablePreferencesItem
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.subpage.ExoSubtitleStylePage
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.subpage.MpvSubtitleStylePage
import com.fiepi.media.app.ui.screen.settings.pages.subtitle.subpage.SubtitleFontSelectionPage
import com.fiepi.media.domain.model.assets.Language
import com.fiepi.media.domain.model.preferences.SubtitleOptions
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubtitleSubPage(
    contentPadding: PaddingValues,
    type: String,
    onNavigateToSubPage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: SubtitlePrefsViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val options by viewModel.subtitleOptions.collectAsState()
    val languages by viewModel.languages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    SubtitleSubPageContent(
        options = options,
        languages = languages,
        searchQuery = searchQuery,
        type = type,
        onIntent = viewModel::onIntent,
        onNavigateToSubPage = onNavigateToSubPage,
        onBack = onBack,
        contentPadding = contentPadding,
        scrollState = scrollState
    )
}

@Composable
fun SubtitleSubPageContent(
    options: SubtitleOptions,
    languages: List<Language>,
    searchQuery: String,
    type: String,
    onIntent: (SubtitlePrefsIntent) -> Unit,
    onNavigateToSubPage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    when (type) {
        SettingsSubPage.Subtitle.PREFERRED_LANGUAGE -> PreferredLanguageListPage(
            contentPadding = contentPadding,
            currentLanguageIds = options.preferredSubtitleLanguages,
            allLanguages = languages,
            onLanguagesChanged = { onIntent(SubtitlePrefsIntent.UpdatePreferredSubtitleLanguages(it)) },
            onNavigateToSelect = { onNavigateToSubPage(SettingsSubPage.Subtitle.SELECT_LANGUAGE) },
            titleRes = R.string.settings_subtitle_preferred_language,
            scrollState = scrollState
        )

        SettingsSubPage.Subtitle.SELECT_LANGUAGE -> LanguageSelectionListPage(
            contentPadding = contentPadding,
            currentLanguageIds = options.preferredSubtitleLanguages,
            allLanguages = languages,
            searchQuery = searchQuery,
            onLanguageSelected = { id ->
                onIntent(SubtitlePrefsIntent.UpdatePreferredSubtitleLanguages(options.preferredSubtitleLanguages + id))
                onIntent(SubtitlePrefsIntent.SetSearchQuery(""))
                onBack()
            },
            scrollState = scrollState
        )

        SettingsSubPage.Subtitle.SUB_CODEPAGE -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                state = scrollState
            ) {
                item {
                    PreferencesGroup(
                        title = stringResource(R.string.settings_subtitle_codepage),
                        items = listOf("auto", "utf-8", "gbk", "big5").map { codepage ->
                            val content: @Composable (ListItemShapes) -> Unit = { shapes ->
                                SelectablePreferencesItem(
                                    title = when (codepage) {
                                        "auto" -> stringResource(R.string.settings_subtitle_codepage_auto)
                                        "utf-8" -> stringResource(R.string.settings_subtitle_codepage_utf8)
                                        "gbk" -> stringResource(R.string.settings_subtitle_codepage_gbk)
                                        "big5" -> stringResource(R.string.settings_subtitle_codepage_big5)
                                        else -> codepage.uppercase()
                                    },
                                    summary = when (codepage) {
                                        "auto" -> stringResource(R.string.settings_subtitle_codepage_auto_summary)
                                        "utf-8" -> stringResource(R.string.settings_subtitle_codepage_utf8_summary)
                                        "gbk" -> stringResource(R.string.settings_subtitle_codepage_gbk_summary)
                                        "big5" -> stringResource(R.string.settings_subtitle_codepage_big5_summary)
                                        else -> null
                                    },
                                    selected = options.subCodepage == codepage,
                                    onClick = {
                                        onIntent(
                                            SubtitlePrefsIntent.UpdateSubCodepage(
                                                codepage
                                            )
                                        )
                                    },
                                    shapes = shapes
                                )
                            }
                            content
                        }.toTypedArray()
                    )
                }
            }
        }

        SettingsSubPage.Subtitle.SUB_ASS_OVERRIDE -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                state = scrollState
            ) {
                item {
                    PreferencesGroup(
                        title = stringResource(R.string.settings_subtitle_mpv_ass_override),
                        items = listOf("scale", "yes", "no", "force", "strip").map { overrideMode ->
                            val content: @Composable (ListItemShapes) -> Unit = { shapes ->
                                SelectablePreferencesItem(
                                    title = when (overrideMode) {
                                        "scale" -> stringResource(R.string.settings_subtitle_mpv_ass_override_scale)
                                        "yes" -> stringResource(R.string.settings_subtitle_mpv_ass_override_yes)
                                        "no" -> stringResource(R.string.settings_subtitle_mpv_ass_override_no)
                                        "force" -> stringResource(R.string.settings_subtitle_mpv_ass_override_force)
                                        "strip" -> stringResource(R.string.settings_subtitle_mpv_ass_override_strip)
                                        else -> overrideMode
                                    },
                                    summary = when (overrideMode) {
                                        "scale" -> stringResource(R.string.settings_subtitle_mpv_ass_override_scale_summary)
                                        "yes" -> stringResource(R.string.settings_subtitle_mpv_ass_override_yes_summary)
                                        "no" -> stringResource(R.string.settings_subtitle_mpv_ass_override_no_summary)
                                        "force" -> stringResource(R.string.settings_subtitle_mpv_ass_override_force_summary)
                                        "strip" -> stringResource(R.string.settings_subtitle_mpv_ass_override_strip_summary)
                                        else -> null
                                    },
                                    selected = options.mpvSubtitle.subAssOverride == overrideMode,
                                    onClick = {
                                        onIntent(
                                            SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                                                options.mpvSubtitle.copy(subAssOverride = overrideMode)
                                            )
                                        )
                                    },
                                    shapes = shapes
                                )
                            }
                            content
                        }.toTypedArray()
                    )
                }
            }
        }

        SettingsSubPage.Subtitle.EXO_SUBTITLE_STYLE -> ExoSubtitleStylePage(
            options = options,
            contentPadding = contentPadding,
            onNavigateToSubPage = onNavigateToSubPage,
            onIntent = onIntent,
            scrollState = scrollState
        )

        SettingsSubPage.Subtitle.EXO_SUBTITLE_FONT -> {
            val exo = options.exoSubtitle
            SubtitleFontSelectionPage(
                selectedFont = exo.fontFamily,
                customFontsDir = exo.fontsDir,
                onFontSelected = { font ->
                    onIntent(
                        SubtitlePrefsIntent.UpdateExoSubtitleOptions(
                            exo.copy(fontFamily = font)
                        )
                    )
                },
                contentPadding = contentPadding,
                scrollState = scrollState
            )
        }

        SettingsSubPage.Subtitle.MPV_SUBTITLE_STYLE -> MpvSubtitleStylePage(
            options = options,
            contentPadding = contentPadding,
            onNavigateToSubPage = onNavigateToSubPage,
            onIntent = onIntent,
            scrollState = scrollState
        )

        SettingsSubPage.Subtitle.MPV_SUBTITLE_FONT -> {
            val mpv = options.mpvSubtitle
            SubtitleFontSelectionPage(
                selectedFont = mpv.subFont,
                customFontsDir = mpv.subFontsDir,
                onFontSelected = { font ->
                    onIntent(
                        SubtitlePrefsIntent.UpdateMpvSubtitleOptions(
                            mpv.copy(subFont = font)
                        )
                    )
                },
                contentPadding = contentPadding,
                scrollState = scrollState
            )
        }
    }
}
