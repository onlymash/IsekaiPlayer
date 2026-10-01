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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.twotone.PlaylistAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionFloatingToolbar(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    isAddToPlaylistEnabled: Boolean,
    onCopyClick: () -> Unit,
    onMoveClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier
    ) {
        // Copy
        ToolbarIconButton(
            tooltipText = stringResource(R.string.file_action_copy),
            onClick = onCopyClick
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.file_action_copy)
            )
        }

        // Move
        ToolbarIconButton(
            tooltipText = stringResource(R.string.file_action_move),
            onClick = onMoveClick
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                contentDescription = stringResource(R.string.file_action_move)
            )
        }

        // Rename (Only when single file selected)
        ToolbarIconButton(
            tooltipText = stringResource(R.string.file_action_rename),
            enabled = selectedCount == 1,
            onClick = onRenameClick
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.file_action_rename)
            )
        }

        // Delete
        ToolbarIconButton(
            tooltipText = stringResource(R.string.file_action_delete),
            onClick = onDeleteClick
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.file_action_delete),
                tint = MaterialTheme.colorScheme.error
            )
        }

        // Add to Playlist (Only when selected items are ALL videos)
        ToolbarIconButton(
            tooltipText = stringResource(R.string.file_action_add_to_playlist),
            enabled = isAddToPlaylistEnabled,
            onClick = onAddToPlaylistClick
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.TwoTone.PlaylistAdd,
                contentDescription = stringResource(R.string.file_action_add_to_playlist)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarIconButton(
    tooltipText: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val tooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(tooltipText) } },
        state = tooltipState
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            content = content
        )
    }
}
