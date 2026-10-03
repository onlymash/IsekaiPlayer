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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fiepi.media.app.R
import com.fiepi.media.domain.model.media.FileOperationType
import java.io.File

/**
 * Responsive dialog enabling users to browse local subfolders and select a target directory
 * for Copy or Move operations. Displays a fixed action navigation bar, full target path,
 * and a "Create New Folder" action.
 *
 * @param initialPath Starting directory path for navigation.
 * @param operationType Current operation type (Copy or Move).
 * @param onConfirm Callback with the selected target directory path.
 * @param onRequestCreateFolder Callback to open the "Create New Folder" dialog under a parent path.
 * @param onDismiss Callback when the dialog is dismissed.
 */
@Composable
fun FolderPickerDialog(
    initialPath: String,
    operationType: FileOperationType,
    onConfirm: (targetPath: String) -> Unit,
    onRequestCreateFolder: (parentPath: String) -> Unit,
    onDismiss: () -> Unit
) {
    val storageRootPath = remember(initialPath) {
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        if (initialPath.startsWith(externalStorage)) {
            externalStorage
        } else {
            initialPath.substringBefore('/', "").ifEmpty { externalStorage }
        }
    }

    var currentPath by remember { mutableStateOf(initialPath) }
    var subFolders by remember { mutableStateOf<List<File>>(emptyList()) }

    LaunchedEffect(currentPath) {
        val dir = File(currentPath)
        if (dir.exists() && dir.isDirectory) {
            val list = dir.listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
            subFolders = list?.sortedBy { it.name.lowercase() } ?: emptyList()
        } else {
            subFolders = emptyList()
        }
    }

    val parentFile = File(currentPath).parentFile
    val canGoUp = parentFile != null && parentFile.canRead() && currentPath != "/"

    AlertDialog(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 480.dp)
            .fillMaxHeight(0.65f)
            .heightIn(max = 500.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.file_dialog_select_target_directory)) },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Fixed 3-icon Action Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back / Go Up
                    IconButton(
                        onClick = {
                            if (canGoUp) {
                                currentPath = parentFile.absolutePath
                            }
                        },
                        enabled = canGoUp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }

                    // Go to Storage Root
                    IconButton(
                        onClick = { currentPath = storageRootPath },
                        enabled = currentPath != storageRootPath
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = stringResource(R.string.source_selection_devices_list_title)
                        )
                    }

                    // Create New Folder
                    IconButton(
                        onClick = { onRequestCreateFolder(currentPath) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CreateNewFolder,
                            contentDescription = stringResource(R.string.file_dialog_create_folder_title)
                        )
                    }
                }

                // Subfolders List Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (subFolders.isEmpty()) {
                        Text(
                            text = stringResource(R.string.file_dialog_empty_folder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(subFolders, key = { it.absolutePath }) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentPath = folder.absolutePath }
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

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Full Path Display
                Text(
                    text = currentPath,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val isCopy = operationType == FileOperationType.Copy
            TextButton(
                onClick = { onConfirm(currentPath) }
            ) {
                Text(
                    if (isCopy) stringResource(R.string.file_dialog_copy_here)
                    else stringResource(R.string.file_dialog_move_here)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
