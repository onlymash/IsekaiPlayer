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

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material.icons.twotone.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.playlist.Playlist
import com.fiepi.media.domain.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.IOException

enum class PlaylistCreateType {
    BLANK,
    M3U_LINK,
    M3U_FILE
}

private enum class PlaylistCreateStep {
    SELECT_TYPE,
    INPUT_BLANK,
    INPUT_M3U_LINK,
    INPUT_M3U_FILE
}

@Composable
fun CreatePlaylistDialog(
    onCreateBlank: (title: String) -> Unit,
    onImportM3uUrl: suspend (title: String, url: String) -> Unit,
    onImportM3uUri: suspend (title: String, uri: Uri) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(PlaylistCreateStep.SELECT_TYPE) }
    var selectedType by remember { mutableStateOf(PlaylistCreateType.BLANK) }

    var playlistTitle by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = FileUtils.extractFileName(uri.lastPathSegment)
        }
    }

    when (step) {
        PlaylistCreateStep.SELECT_TYPE -> {
            PlaylistTypeSelectDialog(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                onNext = {
                    step = when (selectedType) {
                        PlaylistCreateType.BLANK -> PlaylistCreateStep.INPUT_BLANK
                        PlaylistCreateType.M3U_LINK -> PlaylistCreateStep.INPUT_M3U_LINK
                        PlaylistCreateType.M3U_FILE -> PlaylistCreateStep.INPUT_M3U_FILE
                    }
                },
                onDismiss = onDismiss
            )
        }

        PlaylistCreateStep.INPUT_BLANK -> {
            BlankPlaylistCreateDialog(
                title = playlistTitle,
                onTitleChange = { playlistTitle = it },
                onCreate = {
                    onCreateBlank(playlistTitle)
                    onDismiss()
                },
                onPrevious = { step = PlaylistCreateStep.SELECT_TYPE },
                onDismiss = onDismiss
            )
        }

        PlaylistCreateStep.INPUT_M3U_LINK -> {
            M3uLinkImportDialog(
                title = playlistTitle,
                url = m3uUrl,
                onTitleChange = { playlistTitle = it },
                onUrlChange = { m3uUrl = it },
                onImportM3uUrl = onImportM3uUrl,
                onPrevious = { step = PlaylistCreateStep.SELECT_TYPE },
                onDismiss = onDismiss
            )
        }

        PlaylistCreateStep.INPUT_M3U_FILE -> {
            M3uFileImportDialog(
                title = playlistTitle,
                selectedFileName = selectedFileName,
                selectedFileUri = selectedFileUri,
                onTitleChange = { playlistTitle = it },
                onSelectFileClick = { filePickerLauncher.launch("*/*") },
                onImportM3uUri = onImportM3uUri,
                onPrevious = { step = PlaylistCreateStep.SELECT_TYPE },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun PlaylistTypeSelectDialog(
    selectedType: PlaylistCreateType,
    onTypeSelected: (PlaylistCreateType) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        PlaylistCreateType.BLANK to Pair(
            stringResource(R.string.playlist_create_option_blank),
            Icons.TwoTone.VideoLibrary
        ),
        PlaylistCreateType.M3U_LINK to Pair(
            stringResource(R.string.playlist_create_option_m3u_link),
            Icons.TwoTone.Link
        ),
        PlaylistCreateType.M3U_FILE to Pair(
            stringResource(R.string.playlist_create_option_m3u_file),
            Icons.TwoTone.Folder
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_create_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                options.forEachIndexed { index, (type, info) ->
                    val (label, icon) = info
                    val shapes = ListItemDefaults.segmentedShapes(
                        index = index,
                        count = options.size
                    )
                    SegmentedListItem(
                        selected = selectedType == type,
                        onClick = rememberHapticClickHandler { onTypeSelected(type) },
                        shapes = shapes,
                        leadingContent = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null
                            )
                        },
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = rememberHapticClickHandler(onNext)
            ) {
                Text(stringResource(R.string.playlist_create_next_button))
            }
        },
        dismissButton = {
            TextButton(onClick = rememberHapticClickHandler(onDismiss)) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun BlankPlaylistCreateDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onCreate: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_create_title_blank)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.playlist_create_dialog_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = rememberHapticClickHandler(onCreate),
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(R.string.source_editor_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = rememberHapticClickHandler(onPrevious)
            ) {
                Text(stringResource(R.string.playlist_create_previous_button))
            }
        }
    )
}

@Composable
fun M3uLinkImportDialog(
    title: String,
    url: String,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onImportM3uUrl: suspend (title: String, url: String) -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessageRes by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.playlist_create_title_m3u_link)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        onTitleChange(it)
                        if (errorMessageRes != null) errorMessageRes = null
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    placeholder = { Text(stringResource(R.string.playlist_create_dialog_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        onUrlChange(it)
                        if (errorMessageRes != null) errorMessageRes = null
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = errorMessageRes != null,
                    supportingText = errorMessageRes?.let { resId ->
                        {
                            Text(
                                text = stringResource(resId),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    placeholder = { Text(stringResource(R.string.playlist_create_m3u_url_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = rememberHapticClickHandler {
                    val trimmedTitle = title.trim()
                    val trimmedUrl = url.trim()

                    if (trimmedTitle.isBlank()) {
                        errorMessageRes = R.string.playlist_import_error_title_empty
                        return@rememberHapticClickHandler
                    }

                    if (trimmedUrl.isBlank() || (!trimmedUrl.startsWith(
                            "http://",
                            ignoreCase = true
                        ) &&
                                !trimmedUrl.startsWith("https://", ignoreCase = true))
                    ) {
                        errorMessageRes = R.string.playlist_import_error_invalid_url
                        return@rememberHapticClickHandler
                    }

                    errorMessageRes = null
                    isLoading = true
                    scope.launch {
                        try {
                            onImportM3uUrl(trimmedTitle, trimmedUrl)
                            isLoading = false
                            onDismiss()
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessageRes =
                                if (e.message?.contains("Network") == true || e is IOException) {
                                    R.string.playlist_import_error_network
                                } else {
                                    R.string.playlist_import_error_parse
                                }
                        }
                    }
                },
                enabled = url.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.playlist_create_import_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = rememberHapticClickHandler(onPrevious),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.playlist_create_previous_button))
            }
        }
    )
}

@Composable
fun M3uFileImportDialog(
    title: String,
    selectedFileName: String?,
    selectedFileUri: Uri?,
    onTitleChange: (String) -> Unit,
    onSelectFileClick: () -> Unit,
    onImportM3uUri: suspend (title: String, uri: Uri) -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessageRes by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.playlist_create_title_m3u_file)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        onTitleChange(it)
                        if (errorMessageRes != null) errorMessageRes = null
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    placeholder = { Text(stringResource(R.string.playlist_create_dialog_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = rememberHapticClickHandler(onSelectFileClick),
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileOpen,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedFileName?.let {
                            stringResource(R.string.playlist_create_m3u_file_selected, it)
                        } ?: stringResource(R.string.playlist_create_m3u_file_select),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                errorMessageRes?.let { resId ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(resId),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = rememberHapticClickHandler {
                    val uri = selectedFileUri ?: return@rememberHapticClickHandler
                    val trimmedTitle = title.trim()

                    if (trimmedTitle.isBlank()) {
                        errorMessageRes = R.string.playlist_import_error_title_empty
                        return@rememberHapticClickHandler
                    }

                    errorMessageRes = null
                    isLoading = true
                    scope.launch {
                        try {
                            onImportM3uUri(trimmedTitle, uri)
                            isLoading = false
                            onDismiss()
                        } catch (_: Exception) {
                            isLoading = false
                            errorMessageRes = R.string.playlist_import_error_parse
                        }
                    }
                },
                enabled = selectedFileUri != null && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.playlist_create_import_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = rememberHapticClickHandler(onPrevious),
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.playlist_create_previous_button))
            }
        }
    )
}

@Composable
fun PlaylistInputDialog(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.playlist_create_dialog_placeholder)) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = rememberHapticClickHandler { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.source_editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = rememberHapticClickHandler(onDismiss)) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
fun PlaylistDeleteDialog(
    playlist: Playlist,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_delete_dialog_title)) },
        text = { Text(stringResource(R.string.playlist_delete_dialog_message, playlist.title)) },
        confirmButton = {
            TextButton(
                onClick = rememberHapticClickHandler(onConfirm)
            ) {
                Text(
                    text = stringResource(R.string.common_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = rememberHapticClickHandler(onDismiss)) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/**
 * Dialog enabling the user to choose an existing playlist to add selected items to, or create a new playlist.
 */
@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onSelectPlaylist: (playlistId: String) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_select_target_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Create New Playlist option
                    item {
                        SegmentedListItem(
                            selected = false,
                            onClick = rememberHapticClickHandler {
                                onDismiss()
                                onCreateNewPlaylist()
                            },
                            shapes = ListItemDefaults.segmentedShapes(
                                index = 0,
                                count = playlists.size + 1
                            ),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.playlist_create_dialog_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Existing Playlists
                    itemsIndexed(
                        items = playlists,
                        key = { _, playlist -> playlist.id }
                    ) { index, playlist ->
                        val shapes = ListItemDefaults.segmentedShapes(
                            index = index + 1,
                            count = playlists.size + 1
                        )
                        val isEditable = playlist.isEditable
                        SegmentedListItem(
                            selected = false,
                            enabled = isEditable,
                            onClick = rememberHapticClickHandler {
                                if (isEditable) {
                                    onSelectPlaylist(playlist.id)
                                }
                            },
                            shapes = shapes,
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.TwoTone.PlaylistAdd,
                                    contentDescription = null
                                )
                            },
                            supportingContent = {
                                val supportingText = if (isEditable) {
                                    stringResource(R.string.playlist_item_count, playlist.itemCount)
                                } else {
                                    "${
                                        stringResource(
                                            R.string.playlist_item_count,
                                            playlist.itemCount
                                        )
                                    } · ${stringResource(R.string.playlist_readonly)}"
                                }
                                Text(
                                    text = supportingText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = ListItemDefaults.segmentedColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright
                            )
                        ) {
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = rememberHapticClickHandler(onDismiss)) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}


// --- Previews ---

@Preview(showBackground = true)
@Composable
fun PlaylistTypeSelectDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PlaylistTypeSelectDialog(
                selectedType = PlaylistCreateType.BLANK,
                onTypeSelected = {},
                onNext = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BlankPlaylistCreateDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            BlankPlaylistCreateDialog(
                title = "My Favorites",
                onTitleChange = {},
                onCreate = {},
                onPrevious = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun M3uLinkImportDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            M3uLinkImportDialog(
                title = "IPTV Channels",
                url = "https://example.com/playlist.m3u8",
                onTitleChange = {},
                onUrlChange = {},
                onImportM3uUrl = { _, _ -> },
                onPrevious = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun M3uFileImportDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            M3uFileImportDialog(
                title = "Local M3U Playlist",
                selectedFileName = "my_playlist.m3u",
                selectedFileUri = null,
                onTitleChange = {},
                onSelectFileClick = {},
                onImportM3uUri = { _, _ -> },
                onPrevious = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistInputDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PlaylistInputDialog(
                title = "Rename Playlist",
                initialText = "Watch Later",
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistDeleteDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PlaylistDeleteDialog(
                playlist = Playlist(id = "1", title = "Favorites", itemCount = 5),
                onConfirm = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddToPlaylistDialogPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AddToPlaylistDialog(
                playlists = emptyList(),
                onSelectPlaylist = {},
                onCreateNewPlaylist = {},
                onDismiss = {}
            )
        }
    }
}
