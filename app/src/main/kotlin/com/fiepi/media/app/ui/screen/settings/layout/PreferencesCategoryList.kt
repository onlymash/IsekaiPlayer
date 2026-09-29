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

package com.fiepi.media.app.ui.screen.settings.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fiepi.media.app.ui.screen.settings.components.PreferencesCategoryItem
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.model.SettingsCategory
import com.fiepi.media.app.ui.theme.AppTheme


@Composable
fun PreferencesCategoryList(
    contentPadding: PaddingValues,
    onCategoryClick: (SettingsCategory) -> Unit,
    selectedCategory: SettingsCategory? = null
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PreferencesGroup(
                items = arrayOf(
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.Appearance,
                            isSelected = SettingsCategory.Appearance == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.Appearance) }
                        )
                    },
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.General,
                            isSelected = SettingsCategory.General == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.General) }
                        )
                    }
                )
            )
        }
        item {
            PreferencesGroup(
                items = arrayOf(
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.Player,
                            isSelected = SettingsCategory.Player == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.Player) }
                        )
                    },
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.Gestures,
                            isSelected = SettingsCategory.Gestures == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.Gestures) }
                        )
                    }
                )
            )
        }
        item {
            PreferencesGroup(
                items = arrayOf(
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.Decoder,
                            isSelected = SettingsCategory.Decoder == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.Decoder) }
                        )
                    },
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.Audio,
                            isSelected = SettingsCategory.Audio == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.Audio) }
                        )
                    },
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.Subtitle,
                            isSelected = SettingsCategory.Subtitle == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.Subtitle) }
                        )
                    },
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.Advanced,
                            isSelected = SettingsCategory.Advanced == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.Advanced) }
                        )
                    },
                )
            )
        }
        item {
            PreferencesGroup(
                items = arrayOf(
                    { shapes ->
                        PreferencesCategoryItem(
                            shapes = shapes,
                            category = SettingsCategory.About,
                            isSelected = SettingsCategory.About == selectedCategory,
                            onClick = { onCategoryClick(SettingsCategory.About) }
                        )
                    }
                )
            )
        }
    }
}

@Preview
@Composable
fun PreferencesCategoryListPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            PreferencesCategoryList(
                contentPadding = PaddingValues(),
                onCategoryClick = {},
                selectedCategory = SettingsCategory.Appearance
            )
        }
    }
}