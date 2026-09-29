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

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.utils.formatDate
import com.fiepi.media.app.ui.utils.getResolutionLabel
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.MediaField

@Composable
fun VideoItem(
    item: MediaFile.Video,
    displayFields: List<MediaField>,
    onClick: (MediaFile.Video) -> Unit,
    modifier: Modifier = Modifier,
    title: Any = item.name,
    isLastPlayed: Boolean = false,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    isSelected: Boolean = false,
    onLongClick: ((MediaFile.Video) -> Unit)? = null
) {
    val context = LocalContext.current
    BrowserItem(
        title = title,
        onClick = { onClick(item) },
        onLongClick = onLongClick?.let { { it(item) } },
        enabled = enabled,
        isSelected = isSelected,
        modifier = modifier,
        isHighlight = isLastPlayed,
        leadingContent = {
            MediaFileThumbnail(
                thumbnailUri = item.thumbnailUrl?.toUri(),
                enableThumbnail = displayFields.contains(MediaField.Thumbnail),
                icon = Icons.Default.Movie,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                overlay = {
                    item.duration?.let { duration ->
                        if (duration > 0 && displayFields.contains(MediaField.Duration)) {
                            DurationTag(
                                duration = duration,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }
                    }
                }
            )
        },
        supportingContent = supportingContent,
        tags = {
            item.extension?.let { extension ->
                if (displayFields.contains(MediaField.Extension)) {
                    MetadataTag(text = extension.uppercase())
                }
            }
            item.size?.let { size ->
                if (displayFields.contains(MediaField.Size)) {
                    MetadataTag(text = Formatter.formatFileSize(context, size))
                }
            }
            if (displayFields.contains(MediaField.Resolution)) {
                val w = item.width
                val h = item.height
                if (w != null && h != null && w > 0 && h > 0) {
                    MetadataTag(text = getResolutionLabel(w, h))
                }
            }
            item.lastModified?.let { lastModified ->
                if (displayFields.contains(MediaField.Date)) {
                    MetadataTag(text = formatDate(lastModified))
                }
            }
        }
    )
}

@Composable
fun FolderItem(
    item: MediaFile.Folder,
    displayFields: List<MediaField>,
    onClick: (MediaFile.Folder) -> Unit,
    modifier: Modifier = Modifier,
    title: Any = item.name,
    isLastPlayed: Boolean = false,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    onLongClick: ((MediaFile.Folder) -> Unit)? = null
) {
    val context = LocalContext.current
    BrowserItem(
        title = title,
        onClick = { onClick(item) },
        onLongClick = onLongClick?.let { { it(item) } },
        enabled = enabled,
        isSelected = isSelected,
        modifier = modifier,
        isHighlight = isLastPlayed,
        leadingContent = {
            MediaFileThumbnail(
                icon = Icons.Default.Folder,
                iconTint = MaterialTheme.colorScheme.primary
            )
        },
        tags = {
            item.itemCount?.let { count ->
                if (displayFields.contains(MediaField.ItemCount) && count > 0) {
                    MetadataTag(
                        text = stringResource(
                            R.string.browser_media_items_count,
                            count
                        )
                    )
                }
            }
            item.size?.let { size ->
                if (displayFields.contains(MediaField.Size) && size > 0) {
                    MetadataTag(text = Formatter.formatFileSize(context, size))
                }
            }
            item.lastModified?.let { lastModified ->
                if (displayFields.contains(MediaField.Date)) {
                    MetadataTag(text = formatDate(lastModified))
                }
            }
        }
    )
}

@Composable
fun SubtitleItem(
    item: MediaFile.Subtitle,
    displayFields: List<MediaField>,
    onClick: (MediaFile.Subtitle) -> Unit,
    modifier: Modifier = Modifier,
    title: Any = item.name,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    onLongClick: ((MediaFile.Subtitle) -> Unit)? = null
) {
    val context = LocalContext.current
    BrowserItem(
        title = title,
        onClick = { onClick(item) },
        onLongClick = onLongClick?.let { { it(item) } },
        enabled = enabled,
        isSelected = isSelected,
        modifier = modifier,
        leadingContent = {
            MediaFileThumbnail(
                icon = Icons.Default.Subtitles,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        tags = {
            item.language?.let { language ->
                MetadataTag(text = language.uppercase())
            }
            item.extension?.let { extension ->
                if (displayFields.contains(MediaField.Extension)) {
                    MetadataTag(text = extension.uppercase())
                }
            }
            item.size?.let { size ->
                if (displayFields.contains(MediaField.Size)) {
                    MetadataTag(text = Formatter.formatFileSize(context, size))
                }
            }
            item.lastModified?.let { lastModified ->
                if (displayFields.contains(MediaField.Date)) {
                    MetadataTag(text = formatDate(lastModified))
                }
            }
        }
    )
}

@Composable
fun MediaFileThumbnail(
    icon: ImageVector,
    iconTint: Color,
    contentScale: ContentScale = ContentScale.Crop,
    thumbnailUri: Uri? = null,
    enableThumbnail: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    var isImageLoaded by remember(thumbnailUri) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 60.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailUri != null) {
            AsyncImage(
                model = if (thumbnailUri.scheme != AppConstants.MEDIA_FRAME_SCHEME || enableThumbnail) thumbnailUri else null,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onSuccess = { isImageLoaded = true },
                onError = { isImageLoaded = false }
            )
        }

        if (!isImageLoaded) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = iconTint
            )
        }
        overlay()
    }
}