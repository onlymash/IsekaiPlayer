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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BackIconButtonBox

@Composable
fun SettingsTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBackIcon: Boolean = true,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    showSearch: Boolean = false,
    isSearching: Boolean = false,
    onSearchingChange: (Boolean) -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    val textFieldState = rememberTextFieldState(searchQuery)
    val focusRequester = remember { FocusRequester() }

    // Auto-focus and collapse TopAppBar automatically
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
            // Force collapse LargeTopAppBar if in searching state
            scrollBehavior?.let { it.state.heightOffset = it.state.heightOffsetLimit }
        }
    }

    // Lock collapsed state continuously in search mode to prevent expanding title on pull-down
    if (isSearching) {
        SideEffect {
            scrollBehavior?.let { it.state.heightOffset = it.state.heightOffsetLimit }
        }
    }

    // Sync external parameter to internal state
    LaunchedEffect(searchQuery) {
        if (textFieldState.text.toString() != searchQuery) {
            textFieldState.setTextAndPlaceCursorAtEnd(searchQuery)
        }
    }

    // Sync internal state to external callback
    LaunchedEffect(textFieldState.text) {
        onSearchQueryChange(textFieldState.text.toString())
    }

    LargeTopAppBar(
        modifier = modifier,
        title = {
            AnimatedContent(
                targetState = isSearching,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "TitleTransition"
            ) { searching ->
                if (searching) {
                    BasicSearchInput(
                        state = textFieldState,
                        placeholderText = stringResource(R.string.browser_search_placeholder_collapsed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                } else {
                    Text(title)
                }
            }
        },
        navigationIcon = {
            AnimatedContent(
                targetState = isSearching,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "NavIconTransition"
            ) { searching ->
                if (searching) {
                    BackIconButtonBox(onClick = {
                        onSearchingChange(false)
                        textFieldState.setTextAndPlaceCursorAtEnd("")
                    })
                } else if (showBackIcon) {
                    BackIconButtonBox(onClick = onBackClick)
                }
            }
        },
        actions = {
            AnimatedContent(
                targetState = isSearching,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ActionsTransition"
            ) { searching ->
                if (searching) {
                    if (textFieldState.text.isNotEmpty()) {
                        IconButton(onClick = { textFieldState.setTextAndPlaceCursorAtEnd("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                } else if (showSearch) {
                    IconButton(onClick = { onSearchingChange(true) }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior
    )
}

/**
 * Minimalist search input component without unnecessary backgrounds or outer frames
 */
@Composable
private fun BasicSearchInput(
    state: androidx.compose.foundation.text.input.TextFieldState,
    placeholderText: String,
    modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.titleLarge.copy(
        color = MaterialTheme.colorScheme.onSurface
    )

    BasicTextField(
        state = state,
        modifier = modifier,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorator = { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholderText,
                        style = textStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}