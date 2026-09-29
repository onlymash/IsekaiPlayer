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

package com.fiepi.media.app.ui.utils

import android.text.format.DateFormat
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.model.MediaTrack
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatTime(millis: Long, referenceMillis: Long = millis): String {
    val totalSeconds = maxOf(millis, referenceMillis) / 1000
    val showHours = totalSeconds >= 3600

    val s = (millis / 1000) % 60
    val m = (millis / (1000 * 60)) % 60
    val h = (millis / (1000 * 60 * 60))

    return if (showHours) {
        val refHours = totalSeconds / 3600
        val hourDigits = if (refHours < 10) 1 else refHours.toString().length
        val format = "%0${hourDigits}d:%02d:%02d"
        String.format(Locale.getDefault(), format, h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}


val MediaTrack.displayedName: String
    get() = if (id != -1 && (this is MediaTrack.Subtitle || title.isBlank())) {
        var str = "Track $id"
        if (!language.isNullOrBlank()) {
            str += " (${language})"
        }
        if (isExternal) {
            str += " (External)"
        }
        str
    } else title

fun getResolutionLabel(width: Int, height: Int): String {
    val h = minOf(width, height)
    return when {
        h >= 2160 -> "2160P"
        h >= 1440 -> "1440P"
        h >= 1080 -> "1080P"
        h >= 720 -> "720P"
        h >= 480 -> "480P"
        h >= 320 -> "320P"
        else -> "${width}x$height"
    }
}

fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatDate(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val zone = ZoneId.systemDefault()
    val dateTime = instant.atZone(zone)
    val now = ZonedDateTime.now(zone)

    val pattern = if (dateTime.year == now.year) {
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMdHHmm")
    } else {
        DateFormat.getBestDateTimePattern(Locale.getDefault(), "yyyyMMMd")
    }

    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return formatter.format(dateTime)
}


val SourceType.displayName: String
    get() = when (this) {
        SourceType.Ftp -> "FTP"
        SourceType.Smb -> "SMB"
        SourceType.WebDav -> "WebDAV"
        SourceType.Local -> "Local"
        SourceType.External -> "External"
        SourceType.Stream -> "Stream"
    }