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

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.fiepi.media.domain.model.media.ConflictDecision
import com.fiepi.media.domain.model.media.ConflictResolution
import com.fiepi.media.domain.model.media.FileOperationType
import com.fiepi.media.domain.model.media.FileTask
import com.fiepi.media.domain.model.media.FileTaskState
import com.fiepi.media.domain.usecase.media.FileManagementUseCases
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

class FileOperationService : Service() {

    private val fileOperationManager: FileOperationManager by inject()
    private val fileManagementUseCases: FileManagementUseCases by inject()
    private val notificationController: NotificationController by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var processJob: Job? = null
    private val queueMutex = Mutex()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        if (action == ACTION_CANCEL) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            if (taskId != null) {
                fileOperationManager.cancelTask(taskId)
            } else {
                val activeTask = fileOperationManager.tasks.value.find { !it.isFinished }
                if (activeTask != null) {
                    fileOperationManager.cancelTask(activeTask.id)
                }
            }
            return START_NOT_STICKY
        }

        if (action == ACTION_PROCESS_QUEUE) {
            triggerQueueProcessing()
        }

        return START_NOT_STICKY
    }

    private fun triggerQueueProcessing() {
        if (processJob?.isActive == true) return

        processJob = serviceScope.launch {
            queueMutex.withLock {
                var currentTask = fileOperationManager.getNextPendingTask()
                while (currentTask != null) {
                    val task = currentTask
                    startForegroundWithNotification(task.id, task.type)
                    try {
                        executeSingleTask(task)
                    } catch (_: CancellationException) {
                        fileOperationManager.updateTask(task.id) {
                            it.copy(status = FileTaskState.Cancelled)
                        }
                    }
                    currentTask = fileOperationManager.getNextPendingTask()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationController.cancelNotification(NotificationController.NOTIFICATION_ID_FILE_OPERATION)
                stopSelf()
            }
        }
    }

    private suspend fun executeSingleTask(task: FileTask) {
        fileOperationManager.updateTask(task.id) {
            it.copy(status = FileTaskState.Running)
        }

        val type = task.type
        val sourcePaths = task.sourcePaths
        val targetDir = task.targetDirectory ?: ""
        val totalCount = task.totalCount

        var successCount = 0
        var failCount = 0
        var skippedCount = 0
        var applyToAllDecision: ConflictDecision? = null

        val totalBytes = withContext(Dispatchers.IO) {
            sourcePaths.sumOf { path ->
                val file = File(path)
                if (!file.exists()) 0L
                else if (file.isFile) file.length()
                else file.walk().filter { it.isFile }.sumOf { it.length() }
            }
        }

        fileOperationManager.updateTask(task.id) {
            it.copy(
                totalBytes = totalBytes,
                processedBytes = 0L,
                bytesPerSecond = 0L,
                progressPercent = 0
            )
        }

        if (type == FileOperationType.Delete) {
            val result = fileManagementUseCases.deleteFiles(sourcePaths)
            if (result.isSuccess) {
                successCount = totalCount
            } else {
                failCount = totalCount
            }
        } else {
            var totalProcessedBytes = 0L
            var lastNotificationTime = 0L
            var lastSpeedSampleTime = System.currentTimeMillis()
            var lastSpeedSampleBytes = 0L
            var currentBytesPerSecond = 0L

            for ((index, sourcePath) in sourcePaths.withIndex()) {
                val currentTaskState =
                    fileOperationManager.tasks.value.find { it.id == task.id }?.status
                if (currentTaskState == FileTaskState.Cancelled) {
                    throw CancellationException("Task was cancelled")
                }

                val sourceFile = File(sourcePath)
                val fileName = sourceFile.name

                fileOperationManager.updateTask(task.id) {
                    it.copy(
                        currentFileName = fileName,
                        processedCount = index
                    )
                }

                val targetFile = File(targetDir, fileName)
                var overwrite = false

                if (targetFile.exists()) {
                    val decision =
                        applyToAllDecision ?: fileOperationManager.requestConflictDecision(
                            taskId = task.id,
                            sourcePath = sourcePath,
                            targetPath = targetDir,
                            conflictFileName = fileName
                        )

                    val updatedStatus =
                        fileOperationManager.tasks.value.find { it.id == task.id }?.status
                    if (decision == null || updatedStatus == FileTaskState.Cancelled) {
                        fileOperationManager.updateTask(task.id) {
                            it.copy(status = FileTaskState.Cancelled)
                        }
                        throw CancellationException("Task was cancelled during conflict decision")
                    }

                    fileOperationManager.updateTask(task.id) {
                        it.copy(status = FileTaskState.Running)
                    }

                    if (decision.applyToAll && applyToAllDecision == null) {
                        applyToAllDecision = decision
                    }

                    if (decision.resolution == ConflictResolution.Skip) {
                        skippedCount++
                        val skippedBytes =
                            if (sourceFile.isFile) sourceFile.length() else sourceFile.walk()
                                .filter { it.isFile }.sumOf { it.length() }
                        totalProcessedBytes += skippedBytes
                        continue
                    } else if (decision.resolution == ConflictResolution.Overwrite) {
                        overwrite = true
                    }
                }

                val onChunkProgress: (Long) -> Unit = { chunkSize ->
                    val isCancelled =
                        fileOperationManager.tasks.value.find { it.id == task.id }?.status == FileTaskState.Cancelled
                    if (isCancelled) {
                        throw CancellationException("Task was cancelled during byte streaming")
                    }

                    totalProcessedBytes += chunkSize
                    val now = System.currentTimeMillis()

                    val speedTimeDiff = now - lastSpeedSampleTime
                    if (speedTimeDiff >= 300) {
                        val bytesDiff = totalProcessedBytes - lastSpeedSampleBytes
                        currentBytesPerSecond = (bytesDiff * 1000L) / speedTimeDiff
                        lastSpeedSampleTime = now
                        lastSpeedSampleBytes = totalProcessedBytes
                    }

                    if (now - lastNotificationTime >= 200 || totalProcessedBytes >= totalBytes) {
                        lastNotificationTime = now
                        val percent = if (totalBytes > 0) {
                            ((totalProcessedBytes.toDouble() / totalBytes) * 100).toInt()
                                .coerceIn(0, 100)
                        } else 0

                        fileOperationManager.updateTask(task.id) {
                            it.copy(
                                processedBytes = totalProcessedBytes,
                                bytesPerSecond = currentBytesPerSecond,
                                progressPercent = percent
                            )
                        }

                        updateNotification(
                            taskId = task.id,
                            type = type,
                            processedBytes = totalProcessedBytes,
                            totalBytes = totalBytes,
                            bytesPerSecond = currentBytesPerSecond,
                            fileName = fileName
                        )
                    }
                }

                val opResult = if (type == FileOperationType.Copy) {
                    fileManagementUseCases.copyFile(
                        sourcePath,
                        targetDir,
                        overwrite,
                        onProgress = onChunkProgress
                    )
                } else {
                    fileManagementUseCases.moveFile(
                        sourcePath,
                        targetDir,
                        overwrite,
                        onProgress = onChunkProgress
                    )
                }

                if (opResult.isFailure && opResult.exceptionOrNull() is CancellationException) {
                    throw CancellationException("Task was cancelled")
                }

                if (opResult.isSuccess) {
                    successCount++
                } else {
                    failCount++
                }
            }
        }

        val finalTaskState = fileOperationManager.tasks.value.find { it.id == task.id }?.status
        if (finalTaskState != FileTaskState.Cancelled) {
            fileOperationManager.updateTask(task.id) {
                it.copy(
                    status = FileTaskState.Completed,
                    successCount = successCount,
                    failCount = failCount,
                    skippedCount = skippedCount,
                    processedBytes = totalBytes,
                    bytesPerSecond = 0L,
                    progressPercent = 100
                )
            }
        }
    }

    private fun startForegroundWithNotification(taskId: String, type: FileOperationType) {
        val notification = buildNotification(
            taskId = taskId,
            type = type,
            processedBytes = 0L,
            totalBytes = 0L,
            bytesPerSecond = 0L,
            fileName = ""
        )
        startForeground(
            NotificationController.NOTIFICATION_ID_FILE_OPERATION,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun updateNotification(
        taskId: String,
        type: FileOperationType,
        processedBytes: Long,
        totalBytes: Long,
        bytesPerSecond: Long,
        fileName: String
    ) {
        val notification = buildNotification(
            taskId = taskId,
            type = type,
            processedBytes = processedBytes,
            totalBytes = totalBytes,
            bytesPerSecond = bytesPerSecond,
            fileName = fileName
        )
        notificationController.showNotification(
            NotificationController.NOTIFICATION_ID_FILE_OPERATION,
            notification
        )
    }

    private fun buildNotification(
        taskId: String,
        type: FileOperationType,
        processedBytes: Long,
        totalBytes: Long,
        bytesPerSecond: Long,
        fileName: String
    ): Notification {
        val cancelIntent = Intent(this, FileOperationService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            taskId.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return notificationController.buildFileOperationNotification(
            context = this,
            type = type,
            processedBytes = processedBytes,
            totalBytes = totalBytes,
            bytesPerSecond = bytesPerSecond,
            fileName = fileName,
            cancelPendingIntent = cancelPendingIntent
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PROCESS_QUEUE = "com.fiepi.media.app.action.FILE_OP_PROCESS_QUEUE"
        const val ACTION_CANCEL = "com.fiepi.media.app.action.FILE_OP_CANCEL"

        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
