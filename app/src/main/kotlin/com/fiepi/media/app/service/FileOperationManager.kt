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
import com.fiepi.media.domain.model.media.FileOperationStatus
import com.fiepi.media.domain.model.media.FileOperationType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FileOperationManager {

    private val _status = MutableStateFlow<FileOperationStatus>(FileOperationStatus.Idle)
    val status: StateFlow<FileOperationStatus> = _status.asStateFlow()

    @Volatile
    private var activeConflictDeferred: CompletableDeferred<ConflictDecision>? = null

    @Volatile
    private var isCancelRequested: Boolean = false

    fun startCopy(context: Context, sourcePaths: List<String>, targetDirectory: String) {
        startService(context, FileOperationType.Copy, sourcePaths, targetDirectory)
    }

    fun startMove(context: Context, sourcePaths: List<String>, targetDirectory: String) {
        startService(context, FileOperationType.Move, sourcePaths, targetDirectory)
    }

    fun startDelete(context: Context, sourcePaths: List<String>) {
        startService(context, FileOperationType.Delete, sourcePaths, null)
    }

    private fun startService(
        context: Context,
        type: FileOperationType,
        sourcePaths: List<String>,
        targetDirectory: String?
    ) {
        isCancelRequested = false
        activeConflictDeferred = null
        _status.value = FileOperationStatus.Running(
            type = type,
            currentFileName = "",
            processedCount = 0,
            totalCount = sourcePaths.size,
            progressPercent = 0
        )

        val intent = Intent(context, FileOperationService::class.java).apply {
            action = FileOperationService.ACTION_START
            putExtra(FileOperationService.EXTRA_TYPE, type.name)
            putStringArrayListExtra(FileOperationService.EXTRA_SOURCE_PATHS, ArrayList(sourcePaths))
            putExtra(FileOperationService.EXTRA_TARGET_DIR, targetDirectory)
        }
        context.startForegroundService(intent)
    }

    fun cancelOperation() {
        isCancelRequested = true
        activeConflictDeferred?.cancel()
        activeConflictDeferred = null
    }

    fun submitConflictDecision(decision: ConflictDecision) {
        val deferred = activeConflictDeferred
        activeConflictDeferred = null
        deferred?.complete(decision)
    }

    internal fun isCancelled(): Boolean = isCancelRequested

    internal fun updateStatus(newStatus: FileOperationStatus) {
        _status.value = newStatus
    }

    internal suspend fun requestConflictDecision(
        type: FileOperationType,
        sourcePath: String,
        targetPath: String,
        conflictFileName: String
    ): ConflictDecision? {
        val deferred = CompletableDeferred<ConflictDecision>()
        activeConflictDeferred = deferred
        _status.value = FileOperationStatus.NeedConflictDecision(
            type = type,
            sourcePath = sourcePath,
            targetPath = targetPath,
            conflictFileName = conflictFileName
        )

        return try {
            deferred.await()
        } catch (_: Exception) {
            null
        }
    }
}
