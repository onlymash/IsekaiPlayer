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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Cancel
import androidx.compose.material.icons.twotone.DeleteSweep
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
@OptIn(ExperimentalMaterial3Api::class)
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
    val historyTasks = tasks.filter { it.isFinished }
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        items(pendingTasks, key = { it.id }) { task ->
                            PendingTaskCard(
                                task = task,
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
                        items(historyTasks, key = { it.id }) { task ->
                            CompletedTaskCard(task = task)
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
            .padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ActiveTaskCard(
    task: FileTask,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTaskType(task.type),
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
                    text = "${task.currentFileName} (${task.processedCount}/${task.totalCount})",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val targetDir = task.targetDirectory
            if (targetDir != null) {
                Text(
                    text = targetDir,
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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${task.progressPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun PendingTaskCard(
    task: FileTask,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTaskType(task.type),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(
                        R.string.settings_advanced_history_count_format,
                        task.totalCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val pendingTargetDir = task.targetDirectory
                if (pendingTargetDir != null) {
                    Text(
                        text = pendingTargetDir,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.TwoTone.Cancel,
                    contentDescription = stringResource(R.string.file_task_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompletedTaskCard(task: FileTask) {
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTaskType(task.type),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.settings_advanced_history_count_format,
                        task.totalCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
        }
    }
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
        FileTask(
            id = "1",
            type = FileOperationType.Copy,
            sourcePaths = listOf(
                "/storage/emulated/0/Download/movie1.mp4",
                "/storage/emulated/0/Download/movie2.mp4"
            ),
            targetDirectory = "/storage/emulated/0/Movies",
            currentFileName = "movie1.mp4",
            processedCount = 1,
            totalCount = 2,
            progressPercent = 50,
            status = FileTaskState.Running
        ),
        FileTask(
            id = "2",
            type = FileOperationType.Move,
            sourcePaths = listOf("/storage/emulated/0/Download/sub.srt"),
            targetDirectory = "/storage/emulated/0/Subtitles",
            totalCount = 1,
            status = FileTaskState.Pending
        ),
        FileTask(
            id = "3",
            type = FileOperationType.Delete,
            sourcePaths = listOf("/storage/emulated/0/Download/old.mp4"),
            targetDirectory = null,
            totalCount = 1,
            status = FileTaskState.Completed
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
