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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material.icons.twotone.SdStorage
import androidx.compose.material.icons.twotone.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType

@Composable
fun SourceIcon(
    modifier: Modifier = Modifier,
    source: MediaSource,
    progress: Float = 0f, //0f ~ 1f
    selected: Boolean = false,
) {
    val iconColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    val usedColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(MaterialTheme.shapes.medium)
            .drawBehind {
                // Draw base background (free capacity)
                drawRect(color = bgColor)

                // Draw used capacity from bottom up according to progress
                val usedHeight = size.height * progress
                drawRect(
                    color = usedColor,
                    topLeft = Offset(x = 0f, y = size.height - usedHeight),
                    size = Size(width = size.width, height = usedHeight)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (source) {
                is MediaSource.Local -> if (source.id == MediaSource.INTERNAL_STORAGE_ID) Icons.TwoTone.Storage else Icons.TwoTone.SdStorage
                is MediaSource.External -> if (source.id == MediaSource.NETWORK_STREAM_ID || source.type == SourceType.Stream) Icons.TwoTone.Link else Icons.TwoTone.Storage
                is MediaSource.Remote -> Icons.Filled.Cloud
            },
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = iconColor
        )

        if (source is MediaSource.Remote) {
            BasicText(
                modifier = Modifier
                    .height(10.dp)
                    .offset(y = 2.dp),
                text = when (source.type) {
                    SourceType.Smb -> "smb"
                    SourceType.Ftp -> "ftp"
                    SourceType.WebDav -> "dav"
                    else -> ""
                },
                style = TextStyle(
                    color = textColor,
                    fontWeight = FontWeight.Black
                ),
                autoSize = TextAutoSize.StepBased(4.sp, 20.sp)
            )
        }
    }
}
