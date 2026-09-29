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

package com.fiepi.media.player.component

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.preferences.ScreenshotFormat
import com.fiepi.media.domain.player.model.PlayerEngineType
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles capturing video screenshots, saving image files into system Pictures/[AppConstants.APP_NAME] directory,
 * polling for file generation, and triggering system media scanner.
 */
internal class PlayerScreenshotHelper(
    private val context: Context,
    private val takeEngineScreenshot: suspend (filePath: String) -> Unit
) {

    suspend fun takeScreenshot(
        isLoaded: Boolean,
        engineType: PlayerEngineType,
        format: ScreenshotFormat = ScreenshotFormat.JPG
    ): File? {
        if (!isLoaded) return null

        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val playerDir = File(picturesDir, AppConstants.APP_NAME)
        if (!playerDir.exists()) {
            playerDir.mkdirs()
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val prefix = if (engineType == PlayerEngineType.EXO_PLAYER) "exo-screenshot" else "mpv-screenshot"
        val fileName = "${prefix}_${timestamp}.${format.extension}"
        val file = File(playerDir, fileName)

        takeEngineScreenshot(file.absolutePath)

        // Wait for file generation
        var retry = 0
        while (!file.exists() && retry < 20) {
            delay(50.milliseconds)
            retry++
        }

        return if (file.exists()) {
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
            file
        } else null
    }
}
