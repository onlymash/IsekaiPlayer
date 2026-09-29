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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BrowserItem
import com.fiepi.media.app.ui.components.MediaFileThumbnail
import com.fiepi.media.app.ui.components.MetadataTag
import com.fiepi.media.app.ui.components.ProgressMetadataTag
import com.fiepi.media.app.ui.components.StyledDropdownMenu
import com.fiepi.media.app.ui.components.StyledDropdownMenuItem
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.app.ui.utils.formatDate
import com.fiepi.media.app.ui.utils.formatDuration
import com.fiepi.media.domain.model.history.PlaybackHistory

/**
 * A reusable custom item component for displaying playback history records.
 * Uses [com.fiepi.media.app.ui.components.BrowserItem] for consistent browser list layout.
 */
@Composable
fun PlaybackHistoryItem(
    modifier: Modifier = Modifier,
    history: PlaybackHistory,
    title: Any? = null,
    subtitle: Any? = null,
    showThumbnail: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReplayClick: () -> Unit = onClick
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val rawTitle = remember(history) {
        history.title.ifBlank { history.path.substringAfterLast('/') }
    }

    val rawSubtitle = remember(history) {
        val parentPath = history.path.substringBeforeLast('/')
        parentPath.ifEmpty { history.path }
    }

    val progress = remember(history) {
        if (history.durationMs > 0L) {
            (history.positionMs.toFloat() / history.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    val progressText = remember(history) {
        if (history.positionMs >= 0L && history.durationMs > 0L) {
            "${formatDuration(history.positionMs)} / ${formatDuration(history.durationMs)}"
        } else if (history.durationMs > 0L) {
            formatDuration(history.durationMs)
        } else ""
    }

    val resolvedTitle = title ?: rawTitle
    val resolvedSubtitle = subtitle ?: rawSubtitle

    BrowserItem(
        modifier = modifier,
        title = resolvedTitle,
        onClick = rememberHapticClickHandler(onClick),
        leadingContent = {
            MediaFileThumbnail(
                thumbnailUri = history.thumbnailUrl?.toUri(),
                enableThumbnail = showThumbnail,
                icon = Icons.Default.Movie,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        supportingContent = {
            when (resolvedSubtitle) {
                is AnnotatedString -> Text(
                    text = resolvedSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                else -> Text(
                    text = resolvedSubtitle.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        tailingContent = {
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.common_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StyledDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    StyledDropdownMenuItem(
                        text = stringResource(R.string.common_replay),
                        onClick = {
                            isMenuExpanded = false
                            onReplayClick()
                        },
                        leadingIcon = Icons.Outlined.Replay
                    )
                    StyledDropdownMenuItem(
                        text = stringResource(R.string.common_delete),
                        onClick = {
                            isMenuExpanded = false
                            onDeleteClick()
                        },
                        leadingIcon = Icons.Outlined.Delete,
                        isDanger = true
                    )
                }
            }
        },
        tags = {
            if (progressText.isNotEmpty()) {
                ProgressMetadataTag(
                    text = progressText,
                    progress = progress
                )
            }
            MetadataTag(formatDate(history.lastPlayedAt))
        }
    )
}
