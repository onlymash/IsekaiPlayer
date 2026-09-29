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
import androidx.compose.material.icons.automirrored.twotone.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiepi.media.app.R

/**
 * Floating action button displayed when item selection mode is active.
 * Triggers adding selected video files to a playlist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionFloatingActionButton(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isAddToPlaylistEnabled: Boolean,
    onAddToPlaylistClick: () -> Unit
) {
    FloatingActionButton(
        onClick = {
            if (isAddToPlaylistEnabled) {
                onAddToPlaylistClick()
            }
        },
        modifier = modifier.animateFloatingActionButton(
            visible = isVisible,
            alignment = Alignment.BottomEnd
        )
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.TwoTone.PlaylistAdd,
            contentDescription = stringResource(R.string.playlist_add_to)
        )
    }
}
