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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.fiepi.media.app.ui.components.AppBackHandler
import com.fiepi.media.app.ui.navigation.SettingsNavKey
import com.fiepi.media.app.ui.screen.settings.layout.SettingsSearchResults
import com.fiepi.media.app.ui.screen.settings.model.SettingsSearchViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScaffold(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBackIcon: Boolean = true,
    showSearch: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    topBarWindowInsets: WindowInsets? = null,
    contentWindowInsets: WindowInsets? = null,
    scrollBehavior: TopAppBarScrollBehavior? = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
    onNavigateToKey: ((SettingsNavKey) -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    var isSearching by remember { mutableStateOf(false) }

    val searchViewModel: SettingsSearchViewModel = koinViewModel()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val globalSearchQuery by searchViewModel.searchQuery.collectAsState()

    // Exit search automatically when showSearch is false
    LaunchedEffect(showSearch) {
        if (!showSearch) {
            isSearching = false
            searchViewModel.setSearchQuery("")
        }
    }

    AppBackHandler(enabled = isSearching) {
        isSearching = false
    }

    Scaffold(
        modifier = modifier.then(
            if (scrollBehavior != null && !isSearching) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            else Modifier
        ),
        topBar = {
            SettingsTopAppBar(
                title = title,
                onBackClick = onBackClick,
                showBackIcon = showBackIcon,
                showSearch = showSearch,
                isSearching = isSearching,
                onSearchingChange = { isSearching = it },
                searchQuery = if (onNavigateToKey != null) globalSearchQuery else searchQuery,
                onSearchQueryChange = {
                    if (onNavigateToKey != null) {
                        searchViewModel.setSearchQuery(it)
                    } else {
                        onSearchQueryChange(it)
                    }
                },
                windowInsets = topBarWindowInsets ?: TopAppBarDefaults.windowInsets,
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = contentWindowInsets ?: ScaffoldDefaults.contentWindowInsets,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { contentPadding ->
        if (onNavigateToKey != null && isSearching && globalSearchQuery.isNotEmpty()) {
            SettingsSearchResults(
                contentPadding = contentPadding,
                results = searchResults,
                onResultClick = { key ->
                    isSearching = false
                    searchViewModel.setSearchQuery("")
                    onNavigateToKey(key)
                }
            )
        } else {
            content(contentPadding)
        }
    }
}
