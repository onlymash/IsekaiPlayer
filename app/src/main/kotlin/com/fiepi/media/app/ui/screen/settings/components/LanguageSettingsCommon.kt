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

package com.fiepi.media.app.ui.screen.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.twotone.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.StatusView
import com.fiepi.media.app.ui.components.StyledDropdownMenu
import com.fiepi.media.app.ui.components.StyledDropdownMenuItem
import com.fiepi.media.domain.model.assets.Language

@Composable
fun PreferredLanguageListPage(
    contentPadding: PaddingValues,
    currentLanguageIds: List<String>,
    allLanguages: List<Language>,
    onLanguagesChanged: (List<String>) -> Unit,
    onNavigateToSelect: () -> Unit,
    titleRes: Int,
    scrollState: LazyListState = rememberLazyListState(),
    header: (LazyListScope.() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        state = scrollState
    ) {
        header?.invoke(this)
        item {
            PreferencesLabel(stringResource(titleRes))
        }

        if (currentLanguageIds.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.settings_lang_preferred_language_none),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            itemsIndexed(currentLanguageIds, key = { _, id -> id }) { index, id ->
                val language = allLanguages.find { it.id == id }
                val name = language?.name ?: id
                val codesStr = language?.codes?.joinToString(", ") ?: ""

                val shapes = ListItemDefaults.segmentedShapes(
                    index = index,
                    count = currentLanguageIds.size
                )
                var menuExpanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SegmentedListItem(
                        selected = false,
                        onClick = { },
                        shapes = shapes,
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = {
                            Text(
                                text = (index + 1).toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        },
                        supportingContent = { Text(codesStr) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                                }
                                StyledDropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    StyledDropdownMenuItem(
                                        text = stringResource(R.string.common_move_up),
                                        leadingIcon = Icons.Outlined.ArrowUpward,
                                        enabled = index > 0,
                                        onClick = {
                                            menuExpanded = false
                                            val newList = currentLanguageIds.toMutableList()
                                            val temp = newList[index]
                                            newList[index] = newList[index - 1]
                                            newList[index - 1] = temp
                                            onLanguagesChanged(newList)
                                        }
                                    )
                                    StyledDropdownMenuItem(
                                        text = stringResource(R.string.common_delete),
                                        leadingIcon = Icons.Outlined.Delete,
                                        isDanger = true,
                                        onClick = {
                                            menuExpanded = false
                                            onLanguagesChanged(currentLanguageIds.filterIndexed { i, _ -> i != index })
                                        }
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                    ) {
                        Text(name)
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onNavigateToSelect
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_lang_add_language))
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionListPage(
    contentPadding: PaddingValues,
    currentLanguageIds: List<String>,
    allLanguages: List<Language>,
    searchQuery: String,
    onLanguageSelected: (String) -> Unit,
    scrollState: LazyListState = rememberLazyListState(),
    header: (LazyListScope.() -> Unit)? = null
) {
    val filteredLanguages = remember(searchQuery, allLanguages) {
        allLanguages
            .filter { lang ->
                lang.name.contains(searchQuery, ignoreCase = true) ||
                        lang.english.contains(searchQuery, ignoreCase = true) ||
                        lang.chinese.contains(searchQuery, ignoreCase = true) ||
                        lang.native.contains(searchQuery, ignoreCase = true) ||
                        lang.codes.any { it.contains(searchQuery, ignoreCase = true) }
            }
    }

    if (filteredLanguages.isEmpty() && searchQuery.isNotEmpty()) {
        StatusView(
            modifier = Modifier.padding(contentPadding),
            icon = Icons.TwoTone.SearchOff,
            message = stringResource(R.string.settings_search_empty),
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        state = scrollState
    ) {
        header?.invoke(this)
        item {
            PreferencesLabel(stringResource(R.string.settings_lang_all_languages))
        }

        itemsIndexed(filteredLanguages, key = { _, lang -> lang.id }) { index, lang ->
            val isAdded = currentLanguageIds.contains(lang.id)
            val shapes = ListItemDefaults.segmentedShapes(
                index = index,
                count = filteredLanguages.size
            )
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SegmentedListItem(
                    selected = false,
                    enabled = !isAdded,
                    onClick = { onLanguageSelected(lang.id) },
                    shapes = shapes,
                    verticalAlignment = Alignment.CenterVertically,
                    supportingContent = { Text(lang.codes.joinToString(", ")) },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    )
                ) {
                    Text(lang.name)
                }
            }
        }
    }
}
