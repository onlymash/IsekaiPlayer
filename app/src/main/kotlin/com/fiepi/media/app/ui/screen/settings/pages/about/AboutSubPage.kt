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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.OpenInNew
import androidx.compose.material.icons.twotone.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiepi.media.app.ui.screen.settings.model.SettingsSubPage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutSubPage(
    contentPadding: PaddingValues,
    type: String,
    viewModel: AboutViewModel = koinViewModel()
) {
    val uriHandler = LocalUriHandler.current

    when (type) {
        SettingsSubPage.About.LIBRARIES -> {
            val librariesList by viewModel.libraries.collectAsStateWithLifecycle()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(librariesList) { index, lib ->
                    val description = lib.description
                    val licenseName = lib.licenseName
                    val licenseUrl = lib.licenseUrl
                    val projectUrl = lib.projectUrl

                    SegmentedListItem(
                        shapes = ListItemDefaults.segmentedShapes(index, librariesList.size),
                        onClick = {
                            if (projectUrl.isNotBlank()) {
                                uriHandler.openUri(projectUrl)
                            }
                        },
                        content = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = lib.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = lib.version,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        },
                        supportingContent = {
                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!description.isNullOrBlank()) {
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (licenseName.isNotBlank()) {
                                    SuggestionChip(
                                        onClick = {
                                            if (licenseUrl.isNotBlank()) {
                                                uriHandler.openUri(licenseUrl)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = licenseName,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.TwoTone.Description,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        },
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            if (projectUrl.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.TwoTone.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        SettingsSubPage.About.COPYRIGHT -> {
            val copyrightText by viewModel.copyrightText.collectAsStateWithLifecycle()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        SelectionContainer {
                            Text(
                                text = copyrightText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
