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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.MediaField
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.preferences.SortOrder
import com.fiepi.media.domain.model.preferences.SortType

/**
 * Maps [MediaField] entries to their corresponding string resource IDs.
 */
private val MediaField.labelResId: Int
    get() = when (this) {
        MediaField.Thumbnail -> R.string.browser_view_options_field_thumbnail
        MediaField.ItemCount -> R.string.browser_view_options_field_item_count
        MediaField.Size -> R.string.browser_view_options_field_size
        MediaField.Duration -> R.string.browser_view_options_field_duration
        MediaField.Resolution -> R.string.browser_view_options_field_resolution
        MediaField.Date -> R.string.browser_view_options_field_date
        MediaField.Extension -> R.string.browser_view_options_field_extension
    }

/**
 * Stateful wrapper for the Media View Settings Dialog.
 */
@Composable
fun MediaViewSettingsDialog(
    options: MediaOptions,
    displayFields: List<MediaField>,
    isRemote: Boolean,
    onDismiss: () -> Unit,
    onUpdateSortOptions: (MediaOptions) -> Unit,
    onUpdateDisplayFields: (List<MediaField>) -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(0.85f)
    ) {
        MediaViewSettingsDialogContent(
            options = options,
            displayFields = displayFields,
            isRemote = isRemote,
            onDismiss = onDismiss,
            onUpdateSortOptions = onUpdateSortOptions,
            onUpdateDisplayFields = onUpdateDisplayFields
        )
    }
}

@Composable
private fun MediaViewSettingsDialogContent(
    options: MediaOptions,
    displayFields: List<MediaField>,
    isRemote: Boolean,
    onDismiss: () -> Unit,
    onUpdateSortOptions: (MediaOptions) -> Unit = {},
    onUpdateDisplayFields: (List<MediaField>) -> Unit = {}
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(bottom = 24.dp, start = 24.dp, end = 24.dp, top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.browser_view_options_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            MediaViewSettingsOptions(
                options = options,
                displayFields = displayFields,
                isRemote = isRemote,
                onUpdateSortOptions = onUpdateSortOptions,
                onUpdateDisplayFields = onUpdateDisplayFields
            )
        }
    }
}

@Composable
fun MediaViewSettingsOptions(
    options: MediaOptions,
    displayFields: List<MediaField>,
    isRemote: Boolean,
    onUpdateSortOptions: (MediaOptions) -> Unit,
    onUpdateDisplayFields: (List<MediaField>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Card 1: Sort & Order ---
        SettingsCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.browser_view_options_sort_by),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )

                    // Order Toggle Button
                    val orderText = when (options.sortType) {
                        SortType.Name -> if (options.sortOrder == SortOrder.Ascending) R.string.browser_view_options_order_name_asc else R.string.browser_view_options_order_name_desc
                        SortType.Date -> if (options.sortOrder == SortOrder.Ascending) R.string.browser_view_options_order_date_asc else R.string.browser_view_options_order_date_desc
                        SortType.Size -> if (options.sortOrder == SortOrder.Ascending) R.string.browser_view_options_order_size_asc else R.string.browser_view_options_order_size_desc
                        SortType.Duration -> if (options.sortOrder == SortOrder.Ascending) R.string.browser_view_options_order_duration_asc else R.string.browser_view_options_order_duration_desc
                    }

                    CompactSurface(
                        onClick = {
                            val newOrder =
                                if (options.sortOrder == SortOrder.Ascending) SortOrder.Descending else SortOrder.Ascending

                            onUpdateSortOptions(options.copy(sortOrder = newOrder))
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(orderText),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Icon(
                                imageVector = if (options.sortOrder == SortOrder.Ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SortTypeItem(
                        label = stringResource(R.string.browser_view_options_sort_type_name),
                        selected = options.sortType == SortType.Name,
                        icon = Icons.Default.SortByAlpha,
                        onClick = { onUpdateSortOptions(options.copy(sortType = SortType.Name)) }
                    )
                    SortTypeItem(
                        label = stringResource(R.string.browser_view_options_sort_type_date),
                        selected = options.sortType == SortType.Date,
                        icon = Icons.Default.CalendarToday,
                        onClick = { onUpdateSortOptions(options.copy(sortType = SortType.Date)) }
                    )
                    SortTypeItem(
                        label = stringResource(R.string.browser_view_options_sort_type_size),
                        selected = options.sortType == SortType.Size,
                        icon = Icons.Default.SdStorage,
                        onClick = { onUpdateSortOptions(options.copy(sortType = SortType.Size)) }
                    )
                    if (!isRemote) {
                        SortTypeItem(
                            label = stringResource(R.string.browser_view_options_sort_type_duration),
                            selected = options.sortType == SortType.Duration,
                            icon = Icons.Default.Schedule,
                            onClick = { onUpdateSortOptions(options.copy(sortType = SortType.Duration)) }
                        )
                    }
                }
            }
        }

        // --- Card 2: Filters (Expandable) ---
        var filtersExpanded by remember { mutableStateOf(false) }
        ExpandableSettingsCard(
            title = stringResource(R.string.browser_view_options_filters),
            icon = Icons.Default.FilterList,
            expanded = filtersExpanded,
            onExpandChange = { filtersExpanded = it }
        ) {
            Column {
                FilterToggleItem(
                    label = stringResource(R.string.browser_view_options_folders_first),
                    checked = options.foldersFirst,
                    onCheckedChange = { onUpdateSortOptions(options.copy(foldersFirst = it)) },
                    icon = Icons.Default.DriveFolderUpload
                )
                FilterToggleItem(
                    label = stringResource(R.string.browser_view_options_show_subtitles),
                    checked = options.showSubtitles,
                    onCheckedChange = { onUpdateSortOptions(options.copy(showSubtitles = it)) },
                    icon = Icons.Default.Subtitles
                )
                if (isRemote) {
                    FilterToggleItem(
                        label = stringResource(R.string.browser_view_options_show_hidden),
                        checked = options.showHidden,
                        onCheckedChange = { onUpdateSortOptions(options.copy(showHidden = it)) },
                        icon = Icons.Default.Visibility
                    )
                }
            }
        }

        // --- Card 3: Fields (Expandable) ---
        var fieldsExpanded by remember { mutableStateOf(false) }
        ExpandableSettingsCard(
            title = stringResource(R.string.browser_view_options_fields),
            icon = Icons.Default.ViewModule,
            expanded = fieldsExpanded,
            onExpandChange = { fieldsExpanded = it }
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaField.entries.forEach { field ->
                    // Only show field if it's supported by the current source (local vs remote)
                    if (!isRemote || field.supportsRemote) {
                        val isSelected = displayFields.contains(field)
                        FieldChip(
                            label = stringResource(field.labelResId),
                            selected = isSelected,
                            onClick = {
                                val newFields = if (isSelected) {
                                    displayFields - field
                                } else {
                                    displayFields + field
                                }
                                onUpdateDisplayFields(newFields)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceBright,
        tonalElevation = 1.dp
    ) {
        content()
    }
}

@Composable
private fun ExpandableSettingsCard(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")

    SettingsCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun FilterToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.graphicsLayer {
                scaleX = 0.8f
                scaleY = 0.8f
            }
        )
    }
}

@Composable
private fun FieldChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            leadingIcon = {
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(2.dp)
                                    .fillMaxSize(),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
                borderWidth = 1.dp
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SortTypeItem(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * A standard Surface that disables the minimum interactive component size (48dp) constraint.
 * Useful for creating compact clickable elements without layout-breaking padding.
 */
@Composable
private fun CompactSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            content = content
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MediaViewSettingsDialogContentPreview() {
    AppTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MediaViewSettingsDialogContent(
                options = MediaOptions(
                    sortType = SortType.Date,
                    sortOrder = SortOrder.Descending
                ),
                displayFields = MediaField.entries,
                isRemote = true,
                onDismiss = {},
            )
        }
    }
}
