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

package com.fiepi.media.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R

/**
 * A back button [IconButton] that defaults to [FilledTonalIconButton]
 */
@Composable
fun BackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    enableTonal: Boolean = true
) {
    val backIcon = @Composable {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.common_back)
        )
    }

    if (enableTonal) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = backIcon
        )
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            content = backIcon
        )
    }
}

@Composable
fun BackIconButtonBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    enableTonal: Boolean = true
) {
    Box(modifier = Modifier.padding(start = 16.dp)) {
        BackIconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            enableTonal = enableTonal
        )
    }
}