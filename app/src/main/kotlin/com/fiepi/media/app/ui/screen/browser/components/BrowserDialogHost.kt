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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserIntent
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserState
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserViewModel
import com.fiepi.media.domain.model.source.SourceType

/**
 * Helper state class for managing Activity launchers in the Browser Screen dialog host.
 */
@Stable
class BrowserDialogHostState(
    val context: Context,
    val onIntent: (BrowserIntent) -> Unit
) {
    internal var filePickerLauncher: ManagedActivityResultLauncher<String, Uri?>? = null
    internal var permissionLauncher: ManagedActivityResultLauncher<Intent, *>? = null

    fun launchM3uFilePicker(playlistId: String) {
        onIntent(BrowserIntent.Dialog.LaunchM3uFilePicker(playlistId))
        filePickerLauncher?.launch("*/*")
    }

    fun handleFilePickerResult(uri: Uri?) {
        onIntent(BrowserIntent.Dialog.HandleM3uFileResult(uri))
    }

    fun requestAllFilesPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = "package:${context.packageName}".toUri()
        }
        permissionLauncher?.launch(intent)
    }
}

/**
 * Remembers and creates a [BrowserDialogHostState] instance.
 */
@Composable
fun rememberBrowserDialogHostState(
    onIntent: (BrowserIntent) -> Unit,
    context: Context = LocalContext.current
): BrowserDialogHostState {
    val dialogHostState = remember(context, onIntent) {
        BrowserDialogHostState(context, onIntent)
    }

    dialogHostState.filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        dialogHostState.handleFilePickerResult(uri)
    }

    dialogHostState.permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        onIntent(BrowserIntent.Storage.OnPermissionResult(Environment.isExternalStorageManager()))
    }

    return dialogHostState
}

/**
 * Hosts all dialogs associated with the Browser Screen.
 */
@Composable
fun BrowserDialogHost(
    state: BrowserState,
    viewModel: BrowserViewModel
) {
    val context = LocalContext.current

    if (state.dialogs.isCreatePlaylistVisible) {
        CreatePlaylistDialog(
            isCreating = state.dialogs.isCreatingPlaylist,
            errorMessageRes = state.dialogs.createPlaylistErrorRes,
            onCreateBlank = { title ->
                viewModel.onIntent(BrowserIntent.Playlist.Create(title))
            },
            onImportM3uUrl = { title, url ->
                viewModel.onIntent(BrowserIntent.Playlist.ImportFromUrl(title, url))
            },
            onImportM3uUri = { title, uri ->
                viewModel.onIntent(BrowserIntent.Playlist.ImportFromUri(title, uri))
            },
            onDismiss = { viewModel.onIntent(BrowserIntent.Dialog.DismissCreatePlaylist) }
        )
    }

    if (state.dialogs.isCreateBlankPlaylistVisible) {
        var playlistTitle by remember { mutableStateOf("") }
        BlankPlaylistCreateDialog(
            title = playlistTitle,
            onTitleChange = { playlistTitle = it },
            onCreate = {
                val titleToCreate = playlistTitle.trim()
                if (titleToCreate.isNotBlank()) {
                    viewModel.onIntent(BrowserIntent.Playlist.Create(titleToCreate))
                }
                viewModel.onIntent(BrowserIntent.Dialog.DismissCreateBlankPlaylist)
            },
            onPrevious = { viewModel.onIntent(BrowserIntent.Dialog.DismissCreateBlankPlaylist) },
            onDismiss = { viewModel.onIntent(BrowserIntent.Dialog.DismissCreateBlankPlaylist) }
        )
    }

    state.dialogs.playlistToRename?.let { playlist ->
        PlaylistInputDialog(
            title = stringResource(R.string.playlist_rename_dialog_title),
            initialText = playlist.title,
            onConfirm = { newName ->
                if (newName.isNotBlank()) {
                    viewModel.onIntent(BrowserIntent.Playlist.Rename(playlist.id, newName))
                }
                viewModel.onIntent(BrowserIntent.Dialog.DismissRenamePlaylist)
            },
            onDismiss = { viewModel.onIntent(BrowserIntent.Dialog.DismissRenamePlaylist) }
        )
    }

    state.dialogs.playlistToDelete?.let { playlist ->
        PlaylistDeleteDialog(
            playlist = playlist,
            onConfirm = {
                viewModel.onIntent(BrowserIntent.Playlist.Delete(playlist.id))
                viewModel.onIntent(BrowserIntent.Dialog.DismissDeletePlaylist)
            },
            onDismiss = { viewModel.onIntent(BrowserIntent.Dialog.DismissDeletePlaylist) }
        )
    }

    if (state.dialogs.isSortVisible) {
        MediaViewSettingsDialog(
            options = state.options,
            displayFields = state.displayFields,
            isRemote = state.sourceType != SourceType.Local && state.sourceType != SourceType.External,
            onDismiss = { viewModel.onIntent(BrowserIntent.Config.DismissSortDialog) },
            onUpdateSortOptions = { viewModel.onIntent(BrowserIntent.Config.UpdateSortOptions(it)) },
            onUpdateDisplayFields = { viewModel.onIntent(BrowserIntent.Config.UpdateDisplayFields(it)) }
        )
    }

    if (state.dialogs.isAddToPlaylistVisible) {
        val playlists = state.playlists
        AddToPlaylistDialog(
            playlists = playlists,
            onSelectPlaylist = { playlistId ->
                val targetPlaylist = playlists.find { it.id == playlistId }
                val addedCount = state.selectedVideos.size
                viewModel.onIntent(BrowserIntent.Selection.AddToPlaylist(playlistId))
                targetPlaylist?.let { playlist ->
                    Toast.makeText(
                        context,
                        context.applicationContext.getString(
                            R.string.playlist_added_toast,
                            addedCount,
                            playlist.title
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onCreateNewPlaylist = {
                viewModel.onIntent(BrowserIntent.Dialog.ShowCreateBlankPlaylist(fromAddToPlaylist = true))
            },
            onDismiss = {
                viewModel.onIntent(BrowserIntent.Selection.DismissAddToPlaylist)
            }
        )
    }

    state.dialogs.fileToRename?.let { file ->
        RenameFileDialog(
            initialName = file.name,
            onConfirm = { newName ->
                viewModel.onIntent(BrowserIntent.FileAction.ConfirmRename(file, newName))
            },
            onDismiss = {
                viewModel.onIntent(BrowserIntent.FileAction.DismissRename)
            }
        )
    }

    if (state.dialogs.isDeleteConfirmVisible) {
        DeleteConfirmDialog(
            selectedCount = state.selection.selectedPaths.size,
            onConfirm = {
                viewModel.onIntent(BrowserIntent.FileAction.ConfirmDelete)
            },
            onDismiss = {
                viewModel.onIntent(BrowserIntent.FileAction.DismissDelete)
            }
        )
    }

    state.dialogs.folderPickerOperation?.let { operation ->
        val pickerInitialPath = state.dialogs.folderPickerCurrentPath
            ?: state.mediaNavigationState.currentPath
        val sourceDirectoryPath = state.mediaNavigationState.currentPath
        val activeStorageRoot = state.source.current.rootPath
        FolderPickerDialog(
            initialPath = pickerInitialPath,
            sourceDirectoryPath = sourceDirectoryPath,
            storageRootPath = activeStorageRoot,
            operationType = operation,
            onConfirm = { targetPath ->
                viewModel.onIntent(BrowserIntent.FileAction.ConfirmFolderPicker(targetPath))
            },
            onRequestCreateFolder = { parentPath ->
                viewModel.onIntent(
                    BrowserIntent.FileAction.ShowCreateFolder(
                        parentPath = parentPath,
                        currentFolderPickerPath = parentPath
                    )
                )
            },
            onDismiss = {
                viewModel.onIntent(BrowserIntent.FileAction.DismissFolderPicker)
            }
        )
    }

    state.dialogs.createFolderParentPath?.let { parentPath ->
        CreateFolderDialog(
            isCreating = state.dialogs.isCreatingFolder,
            onCreate = { folderName ->
                viewModel.onIntent(BrowserIntent.FileAction.CreateFolder(parentPath, folderName))
            },
            onDismiss = {
                viewModel.onIntent(BrowserIntent.FileAction.DismissCreateFolder)
            }
        )
    }

    state.dialogs.conflictFileName?.let { conflictFileName ->
        FileConflictDialog(
            conflictFileName = conflictFileName,
            onDecision = { decision ->
                viewModel.onIntent(BrowserIntent.FileAction.ResolveConflict(decision))
            },
            onDismiss = {
                viewModel.onIntent(BrowserIntent.FileAction.DismissConflict)
            }
        )
    }
}
