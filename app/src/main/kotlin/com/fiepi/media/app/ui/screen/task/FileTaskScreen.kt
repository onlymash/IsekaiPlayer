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

package com.fiepi.media.app.ui.screen.task

import android.text.format.Formatter
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Cancel
import androidx.compose.material.icons.twotone.DeleteSweep
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BackIconButtonBox
import com.fiepi.media.app.ui.components.StatusView
import com.fiepi.media.app.ui.screen.task.viewmodel.FileTaskIntent
import com.fiepi.media.app.ui.screen.task.viewmodel.FileTaskScreenState
import com.fiepi.media.app.ui.screen.task.viewmodel.FileTaskViewModel
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.app.ui.utils.formatDate
import com.fiepi.media.domain.model.media.FileOperationType
import com.fiepi.media.domain.model.media.FileTask
import com.fiepi.media.domain.model.media.FileTaskState
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point for the File Task Queue Screen.
 */
@Composable
fun FileTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: FileTaskViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    FileTaskScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateBack = onNavigateBack
    )
}

/**
 * Stateless content layout for the File Task Queue Screen.
 */
@Composable
fun FileTaskScreenContent(
    state: FileTaskScreenState,
    onIntent: (FileTaskIntent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val tasks = state.tasks
    val activeTask = tasks.firstOrNull { !it.isFinished }
    val pendingTasks =
        tasks.filter { it.status == FileTaskState.Pending && it.id != activeTask?.id }
            .sortedBy { it.createdAt }
    val historyTasks = tasks.filter { it.isFinished }
        .sortedByDescending { it.createdAt }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.file_task_queue_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    BackIconButtonBox(onClick = onNavigateBack)
                },
                actions = {
                    if (historyTasks.isNotEmpty()) {
                        IconButton(onClick = { onIntent(FileTaskIntent.ClearCompleted) }) {
                            Icon(
                                imageVector = Icons.TwoTone.DeleteSweep,
                                contentDescription = stringResource(R.string.file_task_clear_completed)
                            )
                        }
                    }
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
            if (tasks.isEmpty()) {
                StatusView(
                    icon = Icons.TwoTone.Folder,
                    message = stringResource(R.string.file_task_empty)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Active Task Section
                    if (activeTask != null) {
                        item(key = "active_header") {
                            SectionTitle(text = stringResource(R.string.file_task_section_active))
                        }
                        item(key = activeTask.id) {
                            ActiveTaskCard(
                                task = activeTask,
                                onCancel = { onIntent(FileTaskIntent.CancelTask(activeTask.id)) }
                            )
                        }
                    }

                    // Pending Tasks Section
                    if (pendingTasks.isNotEmpty()) {
                        item(key = "pending_header") {
                            SectionTitle(
                                text = stringResource(
                                    R.string.file_task_section_pending,
                                    pendingTasks.size
                                )
                            )
                        }
                        itemsIndexed(pendingTasks, key = { _, task -> task.id }) { index, task ->
                            PendingTaskCard(
                                task = task,
                                index = index,
                                count = pendingTasks.size,
                                onCancel = { onIntent(FileTaskIntent.CancelTask(task.id)) }
                            )
                        }
                    }

                    // History Tasks Section
                    if (historyTasks.isNotEmpty()) {
                        item(key = "history_header") {
                            SectionTitle(
                                text = stringResource(
                                    R.string.file_task_section_completed,
                                    historyTasks.size
                                )
                            )
                        }
                        itemsIndexed(historyTasks, key = { _, task -> task.id }) { index, task ->
                            CompletedTaskCard(
                                task = task,
                                index = index,
                                count = historyTasks.size
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

private fun formatSourcePaths(paths: List<String>): String {
    if (paths.isEmpty()) return ""
    return if (paths.size == 1) {
        paths.first()
    } else {
        "${paths.first()} (+${paths.size - 1})"
    }
}

@Composable
private fun ActiveTaskCard(
    task: FileTask,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val typeText = formatTaskType(task.type)
    val speedText = if (task.bytesPerSecond > 0) {
        "${Formatter.formatFileSize(context, task.bytesPerSecond)}/s"
    } else ""

    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        contentPadding = PaddingValues(16.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (speedText.isNotEmpty()) "$typeText • $speedText" else typeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.TwoTone.Cancel,
                            contentDescription = stringResource(R.string.file_task_cancel),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (task.currentFileName.isNotEmpty()) {
                    Text(
                        text = "${task.currentFileName} (${task.processedCount + 1}/${task.totalCount})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.sourcePaths.isNotEmpty()) {
                    Text(
                        text = formatSourcePaths(task.sourcePaths),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val targetDir = task.targetDirectory
                if (targetDir != null) {
                    Text(
                        text = "→ $targetDir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { task.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (task.totalBytes > 0) {
                        val formattedProcessed =
                            Formatter.formatFileSize(context, task.processedBytes)
                        val formattedTotal = Formatter.formatFileSize(context, task.totalBytes)
                        Text(
                            text = "$formattedProcessed / $formattedTotal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = "${task.progressPercent}% • ${formatDate(task.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

@Composable
private fun PendingTaskCard(
    task: FileTask,
    index: Int,
    count: Int,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val formattedSize = if (task.totalBytes > 0) {
        Formatter.formatFileSize(context, task.totalBytes)
    } else null

    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        contentPadding = PaddingValues(16.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTaskType(task.type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.TwoTone.Cancel,
                            contentDescription = stringResource(R.string.file_task_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (task.sourcePaths.isNotEmpty()) {
                    Text(
                        text = formatSourcePaths(task.sourcePaths),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val pendingTargetDir = task.targetDirectory
                if (pendingTargetDir != null) {
                    Text(
                        text = "→ $pendingTargetDir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val details = buildString {
                        append(
                            stringResource(
                                R.string.settings_advanced_history_count_format,
                                task.totalCount
                            )
                        )
                        if (formattedSize != null) {
                            append(" • ")
                            append(formattedSize)
                        }
                    }
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(task.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun CompletedTaskCard(
    task: FileTask,
    index: Int,
    count: Int
) {
    val context = LocalContext.current
    val statusText = when (task.status) {
        FileTaskState.Completed -> stringResource(R.string.file_task_status_completed)
        FileTaskState.Failed -> stringResource(R.string.file_task_status_failed)
        FileTaskState.Cancelled -> stringResource(R.string.file_task_status_cancelled)
        else -> ""
    }

    val statusColor = when (task.status) {
        FileTaskState.Completed -> MaterialTheme.colorScheme.primary
        FileTaskState.Failed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val formattedSize = if (task.totalBytes > 0) {
        Formatter.formatFileSize(context, task.totalBytes)
    } else null

    SegmentedListItem(
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        contentPadding = PaddingValues(16.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTaskType(task.type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (task.sourcePaths.isNotEmpty()) {
                    Text(
                        text = formatSourcePaths(task.sourcePaths),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val targetDir = task.targetDirectory
                if (targetDir != null) {
                    Text(
                        text = "→ $targetDir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val details = buildString {
                        append(
                            stringResource(
                                R.string.settings_advanced_history_count_format,
                                task.totalCount
                            )
                        )
                        if (formattedSize != null) {
                            append(" • ")
                            append(formattedSize)
                        }
                    }
                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(task.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

@Composable
private fun formatTaskType(type: FileOperationType): String {
    return when (type) {
        FileOperationType.Copy -> stringResource(R.string.file_action_copy)
        FileOperationType.Move -> stringResource(R.string.file_action_move)
        FileOperationType.Delete -> stringResource(R.string.file_action_delete)
    }
}

@Preview(showBackground = true)
@Composable
fun FileTaskScreenPreview() {
    val sampleTasks = listOf(
        // Active Running Task
        FileTask(
            id = "1",
            type = FileOperationType.Copy,
            sourcePaths = listOf(
                "/storage/emulated/0/Download/Interstellar.2014.2160p.UHD.mkv",
                "/storage/emulated/0/Download/Interstellar.2014.zh.ass"
            ),
            targetDirectory = "/storage/emulated/0/Movies/Sci-Fi",
            currentFileName = "Interstellar.2014.2160p.UHD.mkv",
            processedCount = 0,
            totalCount = 2,
            processedBytes = 1_500_000_000L,
            totalBytes = 4_500_000_000L,
            bytesPerSecond = 28_500_000L,
            progressPercent = 33,
            status = FileTaskState.Running,
            createdAt = System.currentTimeMillis() - 120_000L
        ),
        // Pending Task 1 (Top)
        FileTask(
            id = "2",
            type = FileOperationType.Move,
            sourcePaths = listOf("/storage/emulated/0/Download/Subtitle_Pack_01.zip"),
            targetDirectory = "/storage/emulated/0/Subtitles",
            totalCount = 1,
            totalBytes = 15_000_000L,
            status = FileTaskState.Pending,
            createdAt = System.currentTimeMillis() - 90_000L
        ),
        // Pending Task 2 (Middle)
        FileTask(
            id = "3",
            type = FileOperationType.Copy,
            sourcePaths = listOf(
                "/storage/emulated/0/Download/Anime_EP01.mp4",
                "/storage/emulated/0/Download/Anime_EP02.mp4",
                "/storage/emulated/0/Download/Anime_EP03.mp4"
            ),
            targetDirectory = "/storage/emulated/0/Anime/Season1",
            totalCount = 3,
            totalBytes = 1_200_000_000L,
            status = FileTaskState.Pending,
            createdAt = System.currentTimeMillis() - 60_000L
        ),
        // Pending Task 3 (Bottom)
        FileTask(
            id = "4",
            type = FileOperationType.Delete,
            sourcePaths = listOf("/storage/emulated/0/Download/temp_cache_file.tmp"),
            targetDirectory = null,
            totalCount = 1,
            totalBytes = 250_000_000L,
            status = FileTaskState.Pending,
            createdAt = System.currentTimeMillis() - 30_000L
        ),
        // Completed History 1 (Top - Completed)
        FileTask(
            id = "5",
            type = FileOperationType.Copy,
            sourcePaths = listOf("/storage/emulated/0/Download/Documentary.mp4"),
            targetDirectory = "/storage/emulated/0/Movies",
            totalCount = 1,
            totalBytes = 850_000_000L,
            status = FileTaskState.Completed,
            createdAt = System.currentTimeMillis() - 3_600_000L
        ),
        // Completed History 2 (Middle - Cancelled)
        FileTask(
            id = "6",
            type = FileOperationType.Move,
            sourcePaths = listOf("/storage/emulated/0/Download/Unfinished_Download.part"),
            targetDirectory = "/storage/emulated/0/Movies",
            totalCount = 1,
            totalBytes = 3_100_000_000L,
            status = FileTaskState.Cancelled,
            createdAt = System.currentTimeMillis() - 7_200_000L
        ),
        // Completed History 3 (Bottom - Failed)
        FileTask(
            id = "7",
            type = FileOperationType.Delete,
            sourcePaths = listOf("/storage/emulated/0/System/protected_file.sys"),
            targetDirectory = null,
            totalCount = 1,
            totalBytes = 42_000_000L,
            status = FileTaskState.Failed,
            createdAt = System.currentTimeMillis() - 86_400_000L
        )
    )
    AppTheme {
        FileTaskScreenContent(
            state = FileTaskScreenState(tasks = sampleTasks),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FileTaskScreenEmptyPreview() {
    AppTheme {
        FileTaskScreenContent(
            state = FileTaskScreenState(tasks = emptyList()),
            onIntent = {},
            onNavigateBack = {}
        )
    }
}
