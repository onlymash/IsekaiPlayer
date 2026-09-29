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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.StatusView
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.ui.screen.settings.components.PreferencesItem
import com.fiepi.media.app.ui.screen.settings.model.SearchablePreference

@Composable
fun SettingsSearchResults(
    contentPadding: PaddingValues,
    results: List<SearchablePreference>,
    onResultClick: (SettingsNavKey) -> Unit
) {
    if (results.isEmpty()) {
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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(results) { index, item ->
            val shapes = ListItemDefaults.segmentedShapes(index = index, count = results.size)
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PreferencesItem(
                    shapes = shapes,
                    title = stringResource(item.titleRes),
                    summary = item.summaryRes?.let { stringResource(it) }
                        ?: stringResource(item.category.titleResId),
                    icon = {
                        Icon(
                            painter = rememberVectorPainter(item.category.icon),
                            contentDescription = null
                        )
                    },
                    onClick = { onResultClick(item.navKey) }
                )
            }
        }
    }
}
