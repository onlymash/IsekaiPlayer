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
import com.fiepi.media.app.activity.MainActivity
import com.fiepi.media.domain.model.media.ConflictDecision
import com.fiepi.media.domain.model.media.ConflictResolution
import com.fiepi.media.domain.model.media.FileOperationType
import com.fiepi.media.domain.model.media.FileTask
import com.fiepi.media.domain.model.media.FileTaskState
import com.fiepi.media.domain.usecase.media.FileManagementUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
                    startForegroundWithNotification(task.type, task.totalCount)
                    executeSingleTask(task)
                    currentTask = fileOperationManager.getNextPendingTask()
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
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
        val total = task.totalCount

        var successCount = 0
        var failCount = 0
        var skippedCount = 0
        var applyToAllDecision: ConflictDecision? = null

        if (type == FileOperationType.Delete) {
            fileOperationManager.updateTask(task.id) {
                it.copy(
                    currentFileName = "",
                    processedCount = 0,
                    progressPercent = 0
                )
            }
            val result = fileManagementUseCases.deleteFiles(sourcePaths)
            if (result.isSuccess) {
                successCount = total
            } else {
                failCount = total
            }
        } else {
            for ((index, sourcePath) in sourcePaths.withIndex()) {
                val currentTaskState =
                    fileOperationManager.tasks.value.find { it.id == task.id }?.status
                if (currentTaskState == FileTaskState.Cancelled) {
                    break
                }

                val sourceFile = File(sourcePath)
                val fileName = sourceFile.name
                val percent = ((index.toFloat() / total) * 100).toInt()

                fileOperationManager.updateTask(task.id) {
                    it.copy(
                        currentFileName = fileName,
                        processedCount = index,
                        progressPercent = percent
                    )
                }
                updateNotification(type, index, total, fileName)

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
                        break
                    }

                    fileOperationManager.updateTask(task.id) {
                        it.copy(status = FileTaskState.Running)
                    }

                    if (decision.applyToAll && applyToAllDecision == null) {
                        applyToAllDecision = decision
                    }

                    if (decision.resolution == ConflictResolution.Skip) {
                        skippedCount++
                        continue
                    } else if (decision.resolution == ConflictResolution.Overwrite) {
                        overwrite = true
                    }
                }

                val opResult = if (type == FileOperationType.Copy) {
                    fileManagementUseCases.copyFile(sourcePath, targetDir, overwrite)
                } else {
                    fileManagementUseCases.moveFile(sourcePath, targetDir, overwrite)
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
                    progressPercent = 100
                )
            }
        }
    }

    private fun startForegroundWithNotification(
        type: FileOperationType,
        total: Int
    ) {
        val notification = buildNotification(type, 0, total, "")
        startForeground(
            NotificationController.NOTIFICATION_ID_FILE_OPERATION,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun updateNotification(
        type: FileOperationType,
        current: Int,
        total: Int,
        fileName: String
    ) {
        val notification = buildNotification(type, current, total, fileName)
        notificationController.showNotification(
            NotificationController.NOTIFICATION_ID_FILE_OPERATION,
            notification
        )
    }

    private fun buildNotification(
        type: FileOperationType,
        current: Int,
        total: Int,
        fileName: String
    ): Notification {
        val openActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return notificationController.buildFileOperationNotification(
            context = this,
            type = type,
            current = current,
            total = total,
            fileName = fileName,
            cancelPendingIntent = openActivityPendingIntent
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
