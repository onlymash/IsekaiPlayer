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

package com.fiepi.media.app.service

import android.content.Context
import android.content.Intent
import com.fiepi.media.domain.model.media.ConflictDecision
import com.fiepi.media.domain.model.media.FileOperationType
import com.fiepi.media.domain.model.media.FileTask
import com.fiepi.media.domain.model.media.FileTaskState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class FileOperationManager {

    private val _tasks = MutableStateFlow<List<FileTask>>(emptyList())
    val tasks: StateFlow<List<FileTask>> = _tasks.asStateFlow()

    private val activeConflictDeferreds =
        ConcurrentHashMap<String, CompletableDeferred<ConflictDecision>>()

    fun enqueueCopy(context: Context, sourcePaths: List<String>, targetDirectory: String): String {
        return enqueue(context, FileOperationType.Copy, sourcePaths, targetDirectory)
    }

    fun enqueueMove(context: Context, sourcePaths: List<String>, targetDirectory: String): String {
        return enqueue(context, FileOperationType.Move, sourcePaths, targetDirectory)
    }

    fun enqueueDelete(context: Context, sourcePaths: List<String>): String {
        return enqueue(context, FileOperationType.Delete, sourcePaths, null)
    }

    private fun enqueue(
        context: Context,
        type: FileOperationType,
        sourcePaths: List<String>,
        targetDirectory: String?
    ): String {
        val newTask = FileTask(
            type = type,
            sourcePaths = sourcePaths,
            targetDirectory = targetDirectory,
            status = FileTaskState.Pending
        )

        _tasks.value += newTask

        val intent = Intent(context, FileOperationService::class.java).apply {
            action = FileOperationService.ACTION_PROCESS_QUEUE
        }
        context.startForegroundService(intent)
        return newTask.id
    }

    fun cancelTask(taskId: String) {
        val deferred = activeConflictDeferreds.remove(taskId)
        deferred?.cancel()

        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId && !task.isFinished) {
                task.copy(status = FileTaskState.Cancelled)
            } else {
                task
            }
        }
    }

    fun clearCompletedTasks() {
        _tasks.value = _tasks.value.filter { !it.isFinished }
    }

    fun submitConflictDecision(taskId: String, decision: ConflictDecision) {
        val deferred = activeConflictDeferreds.remove(taskId)
        deferred?.complete(decision)
    }

    internal fun getNextPendingTask(): FileTask? {
        return _tasks.value.firstOrNull { it.status == FileTaskState.Pending }
    }

    internal fun updateTask(taskId: String, transform: (FileTask) -> FileTask) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) transform(task) else task
        }
    }

    internal suspend fun requestConflictDecision(
        taskId: String,
        sourcePath: String,
        targetPath: String,
        conflictFileName: String
    ): ConflictDecision? {
        val deferred = CompletableDeferred<ConflictDecision>()
        activeConflictDeferreds[taskId] = deferred

        updateTask(taskId) {
            it.copy(
                status = FileTaskState.NeedConflict,
                conflictFileName = conflictFileName,
                conflictSourcePath = sourcePath,
                conflictTargetPath = targetPath
            )
        }

        return try {
            deferred.await()
        } catch (_: Exception) {
            null
        } finally {
            activeConflictDeferreds.remove(taskId)
        }
    }
}
