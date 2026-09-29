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

package com.fiepi.media.app.ui.screen.browser.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.twotone.CloudQueue
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.AppBackHandler
import com.fiepi.media.app.ui.hooks.LocalAppHaptics
import com.fiepi.media.app.ui.theme.AppTheme

/**
 * A Floating Action Button with a menu for the Browser Screen.
 * Follows the official Material 3 sample implementation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserFloatingActionButton(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onAddRemoteSource: () -> Unit,
    onPlayNetworkStream: () -> Unit,
    onCreatePlaylist: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val appHaptics = LocalAppHaptics.current
    val tooltipState = rememberTooltipState()

    AppBackHandler(enabled = expanded) { expanded = false }

    LaunchedEffect(tooltipState.isVisible) {
        if (tooltipState.isVisible) {
            appHaptics.performLongClick()
        }
    }

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = expanded,
        horizontalAlignment = Alignment.End,
        button = {
            val toggleContentDescription =
                stringResource(R.string.browser_fab_toggle_menu_content_description)
            val expandedDescription =
                stringResource(R.string.browser_fab_toggle_menu_expanded_description)
            val collapsedDescription =
                stringResource(R.string.browser_fab_toggle_menu_collapsed_description)

            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    if (expanded) TooltipAnchorPosition.Start else TooltipAnchorPosition.Above
                ),
                tooltip = {
                    PlainTooltip(
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Assertive
                            paneTitle = toggleContentDescription
                        }
                    ) {
                        Text(toggleContentDescription)
                    }
                },
                state = tooltipState,
            ) {
                ToggleFloatingActionButton(
                    modifier = Modifier
                        .semantics {
                            traversalIndex = -1f
                            stateDescription =
                                if (expanded) expandedDescription else collapsedDescription
                            contentDescription = toggleContentDescription
                        }
                        .animateFloatingActionButton(
                            visible = isVisible || expanded,
                            alignment = Alignment.BottomEnd,
                        )
                        .focusRequester(focusRequester),
                    checked = expanded,
                    onCheckedChange = { expanded = !expanded },
                    containerColor = ToggleFloatingActionButtonDefaults.containerColor(),
                ) {
                    val imageVector by remember {
                        derivedStateOf {
                            if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add
                        }
                    }
                    Icon(
                        painter = rememberVectorPainter(imageVector),
                        contentDescription = null,
                        modifier = Modifier.animateIcon({ checkedProgress }),
                    )
                }
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onCreatePlaylist()
            },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.TwoTone.PlaylistAdd,
                    contentDescription = null
                )
            },
            text = {
                Text(text = stringResource(R.string.playlist_create_dialog_title))
            }
        )
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onPlayNetworkStream()
            },
            icon = {
                Icon(
                    imageVector = Icons.TwoTone.Link,
                    contentDescription = null
                )
            },
            text = {
                Text(text = stringResource(R.string.browser_fab_menu_play_network_stream))
            }
        )
        val closeMenuLabel = stringResource(R.string.browser_fab_toggle_menu_close_action_label)
        FloatingActionButtonMenuItem(
            modifier = Modifier.semantics {
                isTraversalGroup = true
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = closeMenuLabel,
                        action = {
                            expanded = false
                            true
                        },
                    )
                )
            },
            onClick = {
                expanded = false
                onAddRemoteSource()
            },
            icon = {
                Icon(
                    imageVector = Icons.TwoTone.CloudQueue,
                    contentDescription = null
                )
            },
            text = {
                Text(text = stringResource(R.string.browser_fab_menu_add_source))
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BrowserFloatingActionButtonPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            BrowserFloatingActionButton(
                isVisible = true,
                onAddRemoteSource = {},
                onPlayNetworkStream = {},
                onCreatePlaylist = {}
            )
        }
    }
}
