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

package com.fiepi.media.app.ui.screen.source

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.twotone.CloudQueue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BackIconButtonBox
import com.fiepi.media.app.ui.components.SourceIcon
import com.fiepi.media.app.ui.components.StatusView
import com.fiepi.media.app.ui.components.StyledDropdownMenu
import com.fiepi.media.app.ui.components.StyledDropdownMenuItem
import com.fiepi.media.app.ui.hooks.LocalAppHaptics
import com.fiepi.media.app.ui.screen.source.viewmodel.SourceManagerIntent
import com.fiepi.media.app.ui.screen.source.viewmodel.SourceManagerState
import com.fiepi.media.app.ui.screen.source.viewmodel.SourceManagerViewModel
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.utils.url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SourceManagerScreen(
    onNavigateToEditor: (String?) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: SourceManagerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    SourceManagerContent(
        state = state,
        onIntent = viewModel::onIntent,
        onEditSource = { onNavigateToEditor(it.id) },
        onAddSource = { onNavigateToEditor(null) },
        onNavigateUp = onNavigateUp
    )
}

@Composable
private fun SourceManagerContent(
    state: SourceManagerState,
    onIntent: (SourceManagerIntent) -> Unit,
    onEditSource: (RemoteSource) -> Unit,
    onAddSource: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val appHaptics = LocalAppHaptics.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val localSources = remember { mutableStateListOf<RemoteSource>() }

    val reorderState = rememberReorderableState(
        onReorderSave = { onIntent(SourceManagerIntent.SaveReorderedSources(it)) },
        onHapticFeedback = { appHaptics.performLongClick() }
    )

    var sourceToDelete by remember { mutableStateOf<RemoteSource?>(null) }

    LaunchedEffect(state.remoteSources) {
        if (!reorderState.isDragging) {
            localSources.clear()
            localSources.addAll(state.remoteSources)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.source_manager_title)) },
                navigationIcon = {
                    BackIconButtonBox(onClick = onNavigateUp)
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSource) {
                Icon(Icons.Outlined.Add, contentDescription = null)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current

        if (localSources.isEmpty() && !reorderState.isDragging) {
            StatusView(
                icon = Icons.TwoTone.CloudQueue,
                message = stringResource(R.string.source_manager_empty),
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = padding.calculateTopPadding(),
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection)
                    )
                    .reorderablePointerInput(
                        state = reorderState,
                        listState = listState,
                        localSources = localSources,
                        scope = scope
                    ),
                state = listState,
                contentPadding = PaddingValues(
                    top = 4.dp,
                    bottom = 4.dp + padding.calculateBottomPadding(),
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(localSources, key = { _, source -> source.id }) { _, source ->

                    val isItemDragging = reorderState.draggingItemKey == source.id

                    SourceManagerItem(
                        modifier = Modifier.widthIn(max = 600.dp),
                        source = source,
                        isDragging = isItemDragging,
                        isAnyDragging = reorderState.isDragging,
                        dragOffset = reorderState.dragOffset,
                        onEdit = { onEditSource(source) },
                        onDelete = { sourceToDelete = source }
                    )
                }
            }
        }
    }

    if (sourceToDelete != null) {
        AlertDialog(
            onDismissRequest = { sourceToDelete = null },
            title = { Text(stringResource(R.string.source_manager_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.source_manager_delete_message,
                        sourceToDelete?.name ?: ""
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sourceToDelete?.let { onIntent(SourceManagerIntent.DeleteSource(it)) }
                        sourceToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { sourceToDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun LazyItemScope.SourceManagerItem(
    modifier: Modifier = Modifier,
    source: RemoteSource,
    isDragging: Boolean,
    isAnyDragging: Boolean,
    dragOffset: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val mediaSource = MediaSource.Remote(source)
    val scale by animateFloatAsState(if (isDragging) 1.04f else 1f, label = "dragScale")
    val elevation = if (isDragging) 4.dp else 0.dp
    val progress = remember(source) {
        val total = source.totalSpace
        val free = source.freeSpace
        if (total != null && free != null && total > 0) {
            val used = total - free
            used.toFloat() / total.toFloat()
        } else 0f
    }

    Surface(
        modifier = modifier
            .animateItem(
                placementSpec = if (isDragging) null else spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else 0f
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceBright,
        tonalElevation = if (isDragging) 4.dp else 0.dp,
        shadowElevation = elevation,
        enabled = !isAnyDragging,
        onClick = { }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle & Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                SourceIcon(
                    source = mediaSource,
                    progress = progress,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title & URL
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // More Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.common_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StyledDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    StyledDropdownMenuItem(
                        text = stringResource(R.string.common_edit),
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = Icons.Outlined.Edit
                    )
                    StyledDropdownMenuItem(
                        text = stringResource(R.string.common_delete),
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = Icons.Outlined.Delete,
                        isDanger = true
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SourceManagerPreview() {
    val mockSources = listOf(
        RemoteSource(
            id = "1",
            name = "Home NAS",
            type = SourceType.Smb,
            host = "192.168.1.100",
            path = "movies"
        ),
        RemoteSource(
            id = "2",
            name = "Seedbox",
            type = SourceType.Ftp,
            host = "ftp.example.com",
            port = 21,
            username = "user"
        ),
        RemoteSource(
            id = "3",
            name = "Nextcloud",
            type = SourceType.WebDav,
            host = "cloud.mywebsite.com",
            useHttps = true,
            path = "remote.php/dav/files/user/"
        )
    )
    AppTheme {
        SourceManagerContent(
            state = SourceManagerState(
                remoteSources = mockSources,
                selectedSourceId = "1"
            ),
            onIntent = {},
            onEditSource = {},
            onAddSource = {},
            onNavigateUp = {}
        )
    }
}

private class ReorderableState(
    val onReorderSave: (List<RemoteSource>) -> Unit,
    val onHapticFeedback: () -> Unit
) {
    var isDragging by mutableStateOf(false)
    var draggingItemKey by mutableStateOf<String?>(null)
    var draggingItemInitialIndex by mutableIntStateOf(-1)
    var draggingItemCurrentIndex by mutableIntStateOf(-1)
    var dragOffset by mutableFloatStateOf(0f)
}

@Composable
private fun rememberReorderableState(
    onReorderSave: (List<RemoteSource>) -> Unit,
    onHapticFeedback: () -> Unit
) = remember {
    ReorderableState(onReorderSave, onHapticFeedback)
}

private fun Modifier.reorderablePointerInput(
    state: ReorderableState,
    listState: LazyListState,
    localSources: SnapshotStateList<RemoteSource>,
    scope: CoroutineScope
): Modifier = pointerInput(Unit) {
    detectDragGesturesAfterLongPress(
        onDragStart = { offset ->
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { item ->
                    offset.y.toInt() in item.offset..(item.offset + item.size)
                }
                ?.let {
                    val source = localSources.getOrNull(it.index)
                    if (source != null) {
                        state.onHapticFeedback()
                        state.isDragging = true
                        state.draggingItemKey = source.id
                        state.draggingItemInitialIndex = it.index
                        state.draggingItemCurrentIndex = it.index
                        state.dragOffset = 0f
                    }
                }
        },
        onDrag = { change, dragAmount ->
            change.consume()
            state.dragOffset += dragAmount.y

            val currentKey = state.draggingItemKey ?: return@detectDragGesturesAfterLongPress
            val draggingItemInfo = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == currentKey }
                ?: return@detectDragGesturesAfterLongPress

            val currentCenter =
                draggingItemInfo.offset + draggingItemInfo.size / 2f + state.dragOffset

            val targetItem = listState.layoutInfo.visibleItemsInfo
                .find { item ->
                    item.key != currentKey &&
                            currentCenter.toInt() in item.offset..(item.offset + item.size)
                }

            if (targetItem != null) {
                val newIndex = targetItem.index
                val currentIndex = state.draggingItemCurrentIndex
                if (newIndex != currentIndex) {
                    val item = localSources.removeAt(currentIndex)
                    localSources.add(newIndex, item)
                    state.dragOffset += draggingItemInfo.offset - targetItem.offset
                    state.draggingItemCurrentIndex = newIndex
                }
            }
        },
        onDragEnd = {
            if (state.draggingItemInitialIndex != state.draggingItemCurrentIndex) {
                state.onReorderSave(localSources.toList())
            }
            scope.launch {
                delay(100.milliseconds)
                state.isDragging = false
                state.draggingItemKey = null
            }
            state.dragOffset = 0f
        },
        onDragCancel = {
            scope.launch {
                delay(100.milliseconds)
                state.isDragging = false
                state.draggingItemKey = null
            }
            state.dragOffset = 0f
        }
    )
}
