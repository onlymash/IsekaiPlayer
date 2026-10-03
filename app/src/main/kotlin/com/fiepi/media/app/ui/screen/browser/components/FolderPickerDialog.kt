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

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.window.core.layout.WindowSizeClass
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.media.FileOperationType
import java.io.File

/**
 * Responsive dialog enabling users to browse local subfolders and select a target directory
 * for Copy or Move operations. Adaptive layout is driven by [currentWindowAdaptiveInfoV2]:
 * displays a two-pane layout when window width is medium or wider, and a single-pane
 * column layout when window width is compact. Unified on a single [Dialog] container.
 *
 * @param modifier Modifier to be applied to the dialog container.
 * @param initialPath Starting directory path for navigation.
 * @param storageRootPath Root directory path of the active storage source (e.g., SD card or USB).
 * @param operationType Current operation type (Copy or Move).
 * @param onConfirm Callback with the selected target directory path.
 * @param onRequestCreateFolder Callback to open the "Create New Folder" dialog under a parent path.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@Composable
fun FolderPickerDialog(
    modifier: Modifier = Modifier,
    initialPath: String,
    storageRootPath: String = "",
    operationType: FileOperationType,
    onConfirm: (targetPath: String) -> Unit,
    onRequestCreateFolder: (parentPath: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isPreview = LocalInspectionMode.current
    val effectiveStorageRoot = remember(initialPath, storageRootPath, isPreview) {
        when {
            storageRootPath.isNotEmpty() -> storageRootPath
            isPreview -> "/storage/emulated/0"
            else -> {
                val defaultExternal = try {
                    Environment.getExternalStorageDirectory().absolutePath
                } catch (_: Exception) {
                    "/storage/emulated/0"
                }
                if (initialPath.startsWith(defaultExternal)) {
                    defaultExternal
                } else {
                    initialPath.substringBefore('/', "").ifEmpty { defaultExternal }
                }
            }
        }
    }

    var currentPath by rememberSaveable { mutableStateOf(initialPath) }
    var subFolders by remember(currentPath, isPreview) {
        mutableStateOf(
            if (isPreview) {
                listOf(
                    File("$currentPath/Movies"),
                    File("$currentPath/Music"),
                    File("$currentPath/Pictures"),
                    File("$currentPath/Videos"),
                    File("$currentPath/Documents"),
                    File("$currentPath/Downloads")
                )
            } else {
                emptyList()
            }
        )
    }

    LaunchedEffect(currentPath, isPreview) {
        if (!isPreview) {
            val dir = File(currentPath)
            if (dir.exists() && dir.isDirectory) {
                val list = dir.listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
                subFolders = list?.sortedBy { it.name.lowercase() } ?: emptyList()
            } else {
                subFolders = emptyList()
            }
        }
    }

    val parentFile = File(currentPath).parentFile
    val canGoUp = parentFile != null &&
            parentFile.canRead() &&
            currentPath != "/" &&
            currentPath != effectiveStorageRoot &&
            currentPath.startsWith(effectiveStorageRoot)

    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val sizeClass = adaptiveInfo.windowSizeClass
    val useTwoPaneLayout =
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

    val containerModifier = if (useTwoPaneLayout) {
        modifier
            .fillMaxWidth(0.90f)
            .widthIn(max = 720.dp)
            .fillMaxHeight(0.88f)
            .heightIn(max = 400.dp)
    } else {
        modifier
            .fillMaxWidth(0.88f)
            .widthIn(max = 480.dp)
            .fillMaxHeight(0.75f)
            .heightIn(max = 560.dp)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = containerModifier,
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            if (useTwoPaneLayout) {
                FolderPickerTwoPaneContent(
                    currentPath = currentPath,
                    canGoUp = canGoUp,
                    storageRootPath = effectiveStorageRoot,
                    operationType = operationType,
                    subFolders = subFolders,
                    onGoUp = { if (canGoUp) currentPath = parentFile.absolutePath },
                    onGoHome = { currentPath = effectiveStorageRoot },
                    onRequestCreateFolder = { onRequestCreateFolder(currentPath) },
                    onSelectFolder = { currentPath = it.absolutePath },
                    onConfirm = { onConfirm(currentPath) },
                    onDismiss = onDismiss
                )
            } else {
                FolderPickerSinglePaneContent(
                    currentPath = currentPath,
                    canGoUp = canGoUp,
                    storageRootPath = effectiveStorageRoot,
                    operationType = operationType,
                    subFolders = subFolders,
                    onGoUp = { if (canGoUp) currentPath = parentFile.absolutePath },
                    onGoHome = { currentPath = effectiveStorageRoot },
                    onRequestCreateFolder = { onRequestCreateFolder(currentPath) },
                    onSelectFolder = { currentPath = it.absolutePath },
                    onConfirm = { onConfirm(currentPath) },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun FolderPickerSinglePaneContent(
    currentPath: String,
    canGoUp: Boolean,
    storageRootPath: String,
    operationType: FileOperationType,
    subFolders: List<File>,
    onGoUp: () -> Unit,
    onGoHome: () -> Unit,
    onRequestCreateFolder: () -> Unit,
    onSelectFolder: (File) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.file_dialog_select_target_directory),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        FolderPickerNavigationActions(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            canGoUp = canGoUp,
            isAtStorageRoot = currentPath == storageRootPath,
            onGoUp = onGoUp,
            onGoHome = onGoHome,
            onRequestCreateFolder = onRequestCreateFolder
        )

        HorizontalDivider()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            FolderList(
                subFolders = subFolders,
                onSelectFolder = onSelectFolder
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FolderPickerPathText(
            currentPath = currentPath,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        FolderPickerConfirmActions(
            modifier = Modifier.fillMaxWidth(),
            operationType = operationType,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun FolderPickerTwoPaneContent(
    currentPath: String,
    canGoUp: Boolean,
    storageRootPath: String,
    operationType: FileOperationType,
    subFolders: List<File>,
    onGoUp: () -> Unit,
    onGoHome: () -> Unit,
    onRequestCreateFolder: () -> Unit,
    onSelectFolder: (File) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Left Panel: Title, Navigation Controls, Current Path, and Action Buttons
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .padding(end = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.file_dialog_select_target_directory),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            FolderPickerNavigationActions(
                canGoUp = canGoUp,
                isAtStorageRoot = currentPath == storageRootPath,
                onGoUp = onGoUp,
                onGoHome = onGoHome,
                onRequestCreateFolder = onRequestCreateFolder
            )

            Spacer(modifier = Modifier.height(16.dp))

            FolderPickerPathText(
                currentPath = currentPath,
                maxLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            FolderPickerConfirmActions(
                modifier = Modifier.fillMaxWidth(),
                operationType = operationType,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }

        VerticalDivider(modifier = Modifier.fillMaxHeight())

        // Right Panel: Subfolders List
        Box(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .padding(start = 16.dp)
        ) {
            FolderList(
                subFolders = subFolders,
                onSelectFolder = onSelectFolder
            )
        }
    }
}

@Composable
private fun FolderPickerNavigationActions(
    modifier: Modifier = Modifier,
    canGoUp: Boolean,
    isAtStorageRoot: Boolean,
    onGoUp: () -> Unit,
    onGoHome: () -> Unit,
    onRequestCreateFolder: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = rememberHapticClickHandler { onGoUp() },
            enabled = canGoUp
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.common_back)
            )
        }

        FilledTonalIconButton(
            onClick = rememberHapticClickHandler { onGoHome() },
            enabled = !isAtStorageRoot
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = stringResource(R.string.file_dialog_device_home_path)
            )
        }

        FilledTonalIconButton(
            onClick = rememberHapticClickHandler { onRequestCreateFolder() }
        ) {
            Icon(
                imageVector = Icons.Outlined.CreateNewFolder,
                contentDescription = stringResource(R.string.file_dialog_create_folder_title)
            )
        }
    }
}

@Composable
private fun FolderPickerConfirmActions(
    modifier: Modifier = Modifier,
    operationType: FileOperationType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.common_cancel))
        }

        Spacer(modifier = Modifier.width(8.dp))

        val isCopy = operationType == FileOperationType.Copy
        TextButton(
            onClick = rememberHapticClickHandler { onConfirm() }
        ) {
            Text(
                if (isCopy) stringResource(R.string.file_dialog_copy_here)
                else stringResource(R.string.file_dialog_move_here)
            )
        }
    }
}

@Composable
private fun FolderPickerPathText(
    currentPath: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2
) {
    Text(
        text = currentPath,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
private fun FolderList(
    subFolders: List<File>,
    onSelectFolder: (File) -> Unit
) {
    if (subFolders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.file_dialog_empty_folder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(subFolders, key = { it.absolutePath }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFolder(folder) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Portrait",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=portrait"
)
@Composable
private fun FolderPickerDialogPortraitPreview() {
    AppTheme {
        Box(Modifier.fillMaxSize()) {
            FolderPickerDialog(
                initialPath = "/storage/emulated/0",
                operationType = FileOperationType.Copy,
                onConfirm = {},
                onRequestCreateFolder = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(
    name = "Landscape",
    showBackground = true,
    device = "spec:width=891dp,height=411dp,orientation=landscape"
)
@Composable
private fun FolderPickerDialogLandscapePreview() {
    AppTheme {
        Box(Modifier.fillMaxSize()) {
            FolderPickerDialog(
                initialPath = "/storage/emulated/0",
                operationType = FileOperationType.Move,
                onConfirm = {},
                onRequestCreateFolder = {},
                onDismiss = {}
            )
        }
    }
}
