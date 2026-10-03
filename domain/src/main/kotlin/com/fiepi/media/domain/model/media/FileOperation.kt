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

package com.fiepi.media.domain.model.media

import java.util.UUID

/**
 * Strategy to adopt when encountering a duplicate filename during copy/move operations.
 */
enum class ConflictResolution {
    Overwrite,
    Skip
}

/**
 * Encapsulates the user's decision for a filename conflict.
 */
data class ConflictDecision(
    val resolution: ConflictResolution,
    val applyToAll: Boolean = false
)

/**
 * Type of file system operation.
 */
enum class FileOperationType {
    Copy,
    Move,
    Delete
}

/**
 * Execution state of a file task in the queue.
 */
enum class FileTaskState {
    Pending,
    Running,
    NeedConflict,
    Completed,
    Failed,
    Cancelled
}

/**
 * Encapsulates a file operation task in the queue.
 */
data class FileTask(
    val id: String = UUID.randomUUID().toString(),
    val type: FileOperationType,
    val sourcePaths: List<String>,
    val targetDirectory: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val currentFileName: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = sourcePaths.size,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val progressPercent: Int = 0,
    val status: FileTaskState = FileTaskState.Pending,
    val conflictFileName: String? = null,
    val conflictSourcePath: String? = null,
    val conflictTargetPath: String? = null,
    val errorMessage: String? = null,
    val successCount: Int = 0,
    val failCount: Int = 0,
    val skippedCount: Int = 0
) {
    val isFinished: Boolean
        get() = status == FileTaskState.Completed || status == FileTaskState.Failed || status == FileTaskState.Cancelled
}

/**
 * Represents the current status or progress of a file operation task.
 */
sealed interface FileOperationStatus {
    data object Idle : FileOperationStatus

    data class Running(
        val type: FileOperationType,
        val currentFileName: String,
        val processedCount: Int,
        val totalCount: Int,
        val processedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val bytesPerSecond: Long = 0L,
        val progressPercent: Int = 0
    ) : FileOperationStatus

    data class NeedConflictDecision(
        val type: FileOperationType,
        val sourcePath: String,
        val targetPath: String,
        val conflictFileName: String
    ) : FileOperationStatus

    data class Completed(
        val type: FileOperationType,
        val successCount: Int,
        val failCount: Int,
        val skippedCount: Int = 0
    ) : FileOperationStatus

    data class Failed(
        val type: FileOperationType,
        val errorMessage: String
    ) : FileOperationStatus
}
