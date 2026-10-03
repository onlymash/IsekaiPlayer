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

/**
 * Singleton manager responsible for enqueuing, tracking, and coordinating file operation tasks
 * (Copy, Move, Delete). Maintains a FIFO sequential queue and handles async conflict resolution.
 */
class FileOperationManager {

    private val _tasks = MutableStateFlow<List<FileTask>>(emptyList())

    /** Observable list of all file tasks in the queue (pending, running, and completed). */
    val tasks: StateFlow<List<FileTask>> = _tasks.asStateFlow()

    /** Map of active completable deferreds waiting for user conflict decisions per task ID. */
    private val activeConflictDeferreds =
        ConcurrentHashMap<String, CompletableDeferred<ConflictDecision>>()

    /**
     * Enqueues a batch file copy operation and starts the background execution service.
     *
     * @param context Application context.
     * @param sourcePaths List of absolute paths of source files/folders to copy.
     * @param targetDirectory Destination directory absolute path.
     * @return Unique ID of the created task.
     */
    fun enqueueCopy(context: Context, sourcePaths: List<String>, targetDirectory: String): String {
        return enqueue(context, FileOperationType.Copy, sourcePaths, targetDirectory)
    }

    /**
     * Enqueues a batch file move operation and starts the background execution service.
     *
     * @param context Application context.
     * @param sourcePaths List of absolute paths of source files/folders to move.
     * @param targetDirectory Destination directory absolute path.
     * @return Unique ID of the created task.
     */
    fun enqueueMove(context: Context, sourcePaths: List<String>, targetDirectory: String): String {
        return enqueue(context, FileOperationType.Move, sourcePaths, targetDirectory)
    }

    /**
     * Enqueues a batch file delete operation and starts the background execution service.
     *
     * @param context Application context.
     * @param sourcePaths List of absolute paths of source files/folders to delete.
     * @return Unique ID of the created task.
     */
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

    /**
     * Cancels an active or pending task by its unique ID.
     *
     * @param taskId Unique identifier of the task to cancel.
     */
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

    /** Clears all finished (completed, failed, or cancelled) task records from the queue. */
    fun clearCompletedTasks() {
        _tasks.value = _tasks.value.filter { !it.isFinished }
    }

    /**
     * Submits the user's conflict decision (Overwrite/Skip) for a duplicate file conflict.
     *
     * @param taskId Task ID encountering the conflict.
     * @param decision The resolution decision made by the user.
     */
    fun submitConflictDecision(taskId: String, decision: ConflictDecision) {
        val deferred = activeConflictDeferreds.remove(taskId)
        deferred?.complete(decision)
    }

    /** Gets the next pending task in the FIFO queue, or null if empty. */
    internal fun getNextPendingTask(): FileTask? {
        return _tasks.value.firstOrNull { it.status == FileTaskState.Pending }
    }

    /** Mutates and updates the state of a specific task in the queue. */
    internal fun updateTask(taskId: String, transform: (FileTask) -> FileTask) {
        _tasks.value = _tasks.value.map { task ->
            if (task.id == taskId) transform(task) else task
        }
    }

    /** Suspends service execution until user resolves a file name collision conflict. */
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
