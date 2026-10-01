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
 * Represents the current status or progress of a file operation task.
 */
sealed interface FileOperationStatus {
    data object Idle : FileOperationStatus

    data class Running(
        val type: FileOperationType,
        val currentFileName: String,
        val processedCount: Int,
        val totalCount: Int,
        val progressPercent: Int
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
