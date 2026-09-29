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

package com.fiepi.media.app.ui.screen.stream

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.twotone.DeleteSweep
import androidx.compose.material.icons.twotone.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiepi.media.app.R
import com.fiepi.media.app.activity.PlayerActivity
import com.fiepi.media.app.ui.components.BackIconButtonBox
import com.fiepi.media.app.ui.components.MetadataTag
import com.fiepi.media.app.ui.components.StyledDropdownMenu
import com.fiepi.media.app.ui.components.StyledDropdownMenuItem
import com.fiepi.media.app.ui.screen.stream.viewmodel.NetworkStreamEffect
import com.fiepi.media.app.ui.screen.stream.viewmodel.NetworkStreamIntent
import com.fiepi.media.app.ui.screen.stream.viewmodel.NetworkStreamState
import com.fiepi.media.app.ui.screen.stream.viewmodel.NetworkStreamViewModel
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.app.ui.utils.formatDate
import com.fiepi.media.domain.model.stream.NetworkStreamHistory
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NetworkStreamScreen(
    onNavigateUp: () -> Unit,
    viewModel: NetworkStreamViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NetworkStreamEffect.LaunchPlayer -> {
                    context.startActivity(PlayerActivity.newIntent(context, effect.url))
                }
            }
        }
    }

    NetworkStreamContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp
    )
}

@Composable
fun NetworkStreamContent(
    state: NetworkStreamState,
    onIntent: (NetworkStreamIntent) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val linkCopiedMessage = stringResource(R.string.network_stream_link_copied)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var activeMenuUrl by remember { mutableStateOf<String?>(null) }
    var pendingDeleteUrl by remember { mutableStateOf<String?>(null) }
    var isClearAllDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.network_stream_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    BackIconButtonBox(onClick = onNavigateUp)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Section 1: URL Input Box
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.inputUrl,
                            onValueChange = { onIntent(NetworkStreamIntent.UpdateInputUrl(it)) },
                            label = { Text(stringResource(R.string.network_stream_input_label)) },
                            placeholder = { Text(stringResource(R.string.network_stream_input_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 4,
                            isError = state.errorMessageResId != null,
                            trailingIcon = {
                                if (state.inputUrl.isNotEmpty()) {
                                    IconButton(onClick = {
                                        onIntent(
                                            NetworkStreamIntent.UpdateInputUrl("")
                                        )
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = stringResource(R.string.common_reset)
                                        )
                                    }
                                }
                            }
                        )

                        if (state.errorMessageResId != null) {
                            Text(
                                text = stringResource(state.errorMessageResId),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        Button(
                            onClick = { onIntent(NetworkStreamIntent.PlayStream(state.inputUrl)) },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.network_stream_play_button))
                        }
                    }
                }

                // Section 2: History Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.network_stream_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.historyList.isNotEmpty()) {
                            TextButton(onClick = { isClearAllDialogOpen = true }) {
                                Icon(
                                    imageVector = Icons.TwoTone.DeleteSweep,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.network_stream_clear_history))
                            }
                        }
                    }
                }

                // Section 3: History List Items
                if (state.historyList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.network_stream_history_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = state.historyList,
                        key = { _, item -> item.url }
                    ) { index, item ->
                        SegmentedListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            onClick = {
                                onIntent(NetworkStreamIntent.PlayStream(item.url))
                            },
                            shapes = ListItemDefaults.segmentedShapes(
                                index,
                                state.historyList.size
                            ),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 8.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            ),
                            content = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.TwoTone.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        if (item.title.isNotBlank()) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = item.url,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        MetadataTag(formatDate(item.lastPlayedAt))
                                    }

                                    Box {
                                        IconButton(onClick = { activeMenuUrl = item.url }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = stringResource(R.string.common_more),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        StyledDropdownMenu(
                                            expanded = activeMenuUrl == item.url,
                                            onDismissRequest = { activeMenuUrl = null }
                                        ) {
                                            StyledDropdownMenuItem(
                                                text = stringResource(R.string.network_stream_copy_link),
                                                onClick = {
                                                    activeMenuUrl = null
                                                    coroutineScope.launch {
                                                        clipboard.setClipEntry(
                                                            ClipEntry(
                                                                ClipData.newPlainText(
                                                                    "URL",
                                                                    item.url
                                                                )
                                                            )
                                                        )
                                                    }
                                                    Toast.makeText(
                                                        context,
                                                        linkCopiedMessage,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                leadingIcon = Icons.Outlined.ContentCopy
                                            )
                                            StyledDropdownMenuItem(
                                                text = stringResource(R.string.common_delete),
                                                onClick = {
                                                    activeMenuUrl = null
                                                    pendingDeleteUrl = item.url
                                                },
                                                leadingIcon = Icons.Outlined.Delete,
                                                isDanger = true
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (pendingDeleteUrl != null) {
        val urlToDelete = pendingDeleteUrl!!
        AlertDialog(
            onDismissRequest = { pendingDeleteUrl = null },
            title = { Text(stringResource(R.string.network_stream_delete_single_dialog_title)) },
            text = { Text(stringResource(R.string.network_stream_delete_single_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(NetworkStreamIntent.DeleteHistory(urlToDelete))
                        pendingDeleteUrl = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteUrl = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (isClearAllDialogOpen) {
        AlertDialog(
            onDismissRequest = { isClearAllDialogOpen = false },
            title = { Text(stringResource(R.string.network_stream_clear_all_dialog_title)) },
            text = { Text(stringResource(R.string.network_stream_clear_all_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(NetworkStreamIntent.ClearHistory)
                        isClearAllDialogOpen = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.network_stream_clear_history),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { isClearAllDialogOpen = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Preview(device = Devices.TABLET)
@Composable
fun NetworkStreamScreenPreview() {
    val mockState = NetworkStreamState(
        inputUrl = "https://example.com/live/stream.m3u8",
        historyList = listOf(
            NetworkStreamHistory(
                title = "stream.m3u8",
                url = "https://example.com/live/stream.m3u8",
                lastPlayedAt = System.currentTimeMillis()
            ),
            NetworkStreamHistory(
                url = "http://192.168.1.100:8080/video.mp4",
                lastPlayedAt = System.currentTimeMillis() - 86400000L
            )
        )
    )

    AppTheme {
        NetworkStreamContent(
            state = mockState,
            onIntent = {},
            onNavigateUp = {}
        )
    }
}
