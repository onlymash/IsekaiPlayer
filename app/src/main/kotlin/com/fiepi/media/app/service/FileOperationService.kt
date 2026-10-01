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
import com.fiepi.media.domain.model.media.FileOperationStatus
import com.fiepi.media.domain.model.media.FileOperationType
import com.fiepi.media.domain.usecase.media.FileManagementUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File

class FileOperationService : Service() {

    private val fileOperationManager: FileOperationManager by inject()
    private val fileManagementUseCases: FileManagementUseCases by inject()
    private val notificationController: NotificationController by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var activeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        if (action == ACTION_CANCEL) {
            fileOperationManager.cancelOperation()
            activeJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START) {
            val typeName = intent.getStringExtra(EXTRA_TYPE) ?: FileOperationType.Copy.name
            val type = runCatching { FileOperationType.valueOf(typeName) }.getOrDefault(FileOperationType.Copy)
            val sourcePaths = intent.getStringArrayListExtra(EXTRA_SOURCE_PATHS) ?: emptyList<String>()
            val targetDir = intent.getStringExtra(EXTRA_TARGET_DIR) ?: ""

            startForeground(
                NotificationController.NOTIFICATION_ID_FILE_OPERATION,
                buildNotification(type, 0, sourcePaths.size, ""),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )

            activeJob?.cancel()
            activeJob = serviceScope.launch {
                executeOperation(type, sourcePaths, targetDir)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun executeOperation(
        type: FileOperationType,
        sourcePaths: List<String>,
        targetDir: String
    ) {
        var successCount = 0
        var failCount = 0
        var skippedCount = 0
        var applyToAllDecision: ConflictDecision? = null

        val total = sourcePaths.size

        if (type == FileOperationType.Delete) {
            fileOperationManager.updateStatus(
                FileOperationStatus.Running(
                    type = type,
                    currentFileName = "",
                    processedCount = 0,
                    totalCount = total,
                    progressPercent = 0
                )
            )
            val result = fileManagementUseCases.deleteFiles(sourcePaths)
            if (result.isSuccess) {
                successCount = total
            } else {
                failCount = total
            }
        } else {
            for ((index, sourcePath) in sourcePaths.withIndex()) {
                if (fileOperationManager.isCancelled()) {
                    break
                }

                val sourceFile = File(sourcePath)
                val fileName = sourceFile.name
                val percent = ((index.toFloat() / total) * 100).toInt()

                fileOperationManager.updateStatus(
                    FileOperationStatus.Running(
                        type = type,
                        currentFileName = fileName,
                        processedCount = index,
                        totalCount = total,
                        progressPercent = percent
                    )
                )
                updateNotification(type, index, total, fileName)

                val targetFile = File(targetDir, fileName)
                var overwrite = false

                if (targetFile.exists()) {
                    val decision = applyToAllDecision ?: fileOperationManager.requestConflictDecision(
                        type = type,
                        sourcePath = sourcePath,
                        targetPath = targetDir,
                        conflictFileName = fileName
                    )

                    if (decision == null || fileOperationManager.isCancelled()) {
                        break
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

        val finalStatus = FileOperationStatus.Completed(
            type = type,
            successCount = successCount,
            failCount = failCount,
            skippedCount = skippedCount
        )
        fileOperationManager.updateStatus(finalStatus)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        val cancelIntent = Intent(this, FileOperationService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return notificationController.buildFileOperationNotification(
            context = this,
            type = type,
            current = current,
            total = total,
            fileName = fileName,
            cancelPendingIntent = cancelPendingIntent
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.fiepi.media.app.action.FILE_OP_START"
        const val ACTION_CANCEL = "com.fiepi.media.app.action.FILE_OP_CANCEL"

        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_SOURCE_PATHS = "extra_source_paths"
        const val EXTRA_TARGET_DIR = "extra_target_dir"
    }
}
