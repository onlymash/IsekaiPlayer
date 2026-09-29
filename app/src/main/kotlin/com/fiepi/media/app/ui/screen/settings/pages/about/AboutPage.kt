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

package com.fiepi.media.app.ui.screen.settings.pages.about

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.LibraryBooks
import androidx.compose.material.icons.automirrored.twotone.Send
import androidx.compose.material.icons.twotone.Code
import androidx.compose.material.icons.twotone.Copyright
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferencesItem
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.config.AppConstants

@Composable
fun AboutPage(
    contentPadding: PaddingValues,
    onNavigateToSubPage: (String) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
    }
    val versionName = packageInfo?.versionName ?: "1.0.0"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(32.dp))
            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${stringResource(id = R.string.settings_about_version)} $versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.settings_about_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            PreferencesGroup(
                title = stringResource(id = R.string.settings_about_header_legal),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(id = R.string.settings_about_copyright),
                            summary = stringResource(id = R.string.settings_about_copyright_summary),
                            onClick = {
                                onNavigateToSubPage(SettingsSubPage.About.COPYRIGHT)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Copyright,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(id = R.string.settings_about_libraries),
                            summary = stringResource(id = R.string.settings_about_libraries_summary),
                            onClick = {
                                onNavigateToSubPage(SettingsSubPage.About.LIBRARIES)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.TwoTone.LibraryBooks,
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
                title = stringResource(id = R.string.settings_about_header_links),
                items = arrayOf(
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(id = R.string.settings_about_developer),
                            summary = stringResource(id = R.string.settings_about_developer_summary),
                            onClick = {
                                uriHandler.openUri(AppConstants.APP_LINK_DEVELOPER)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Person,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(id = R.string.settings_about_github),
                            summary = stringResource(id = R.string.settings_about_github_summary),
                            onClick = {
                                uriHandler.openUri(AppConstants.APP_LINK_REPOSITORY)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.TwoTone.Code,
                                    contentDescription = null
                                )
                            }
                        )
                    },
                    { shapes ->
                        PreferencesItem(
                            shapes = shapes,
                            title = stringResource(id = R.string.settings_about_telegram),
                            summary = stringResource(id = R.string.settings_about_telegram_summary),
                            onClick = {
                                uriHandler.openUri(AppConstants.APP_LINK_TELEGRAM_CHANNEL)
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.TwoTone.Send,
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

@Preview(showBackground = true)
@Composable
private fun AboutPagePreview() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            AboutPage(
                contentPadding = PaddingValues(0.dp),
                onNavigateToSubPage = {}
            )
        }
    }
}

