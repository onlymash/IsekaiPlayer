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

import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.usecase.history.GetHistoryUseCase
import com.fiepi.media.domain.usecase.history.SaveHistoryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles playback history persistence and position resume calculation.
 * Obtains current [MediaPlaybackState] and [MediaSource] dynamically via callbacks.
 */
internal class PlayerHistoryTracker(
    private val getHistory: GetHistoryUseCase,
    private val saveHistory: SaveHistoryUseCase,
    private val getCurrentState: () -> MediaPlaybackState,
    private val getCurrentSource: () -> MediaSource?
) {

    /**
     * Calculates the initial resume position (in ms) and duration for a video based on saved history.
     */
    suspend fun getResumePosition(
        sourceId: String,
        videoPath: String,
        currentDuration: Long
    ): Pair<Long, Long> {
        val history = getHistory(sourceId, videoPath) ?: return Pair(0L, currentDuration)
        // If recorded position is too short (< 5s), treat as starting from beginning
        val initialPos = history.positionMs.takeIf { it > 5000L } ?: 0L
        val duration = if (currentDuration <= 0) history.durationMs else currentDuration
        return Pair(initialPos, duration)
    }

    /**
     * Captures current playback state snapshot and triggers async save on Dispatchers.IO.
     */
    fun triggerHistorySave(coroutineScope: CoroutineScope) {
        val source = getCurrentSource() ?: return
        if (source.type == SourceType.External) return

        val state = getCurrentState()
        if (!state.isLoaded || state.isEnded) return
        val video = state.playlist.getOrNull(state.currentIndex) ?: return

        val position = state.currentPosition
        val duration = state.duration

        coroutineScope.launch(Dispatchers.IO) {
            saveHistoryInternal(
                sourceId = source.id,
                path = video.path,
                positionMs = position,
                durationMs = duration,
                title = video.name,
                thumbnailUrl = video.thumbnailUrl,
                sourceType = source.type
            )
        }
    }

    /**
     * Saves playback history immediately (e.g. on completion or force reset).
     */
    suspend fun saveHistoryImmediate(
        forceReset: Boolean = false,
        video: MediaFile.Video? = null,
        targetSource: MediaSource? = null,
        isCompleted: Boolean? = null
    ) {
        val state = getCurrentState()
        val source = getCurrentSource()

        val currentVideo = video ?: state.playlist.getOrNull(state.currentIndex) ?: return
        val currentSource = targetSource ?: source ?: return
        val position = if (forceReset) 0L else state.currentPosition
        val duration = state.duration

        saveHistoryInternal(
            sourceId = currentSource.id,
            path = currentVideo.path,
            positionMs = position,
            durationMs = duration,
            title = currentVideo.name,
            thumbnailUrl = currentVideo.thumbnailUrl,
            sourceType = currentSource.type,
            isCompleted = isCompleted
        )
    }

    /**
     * Saves playback history internal logic with position threshold validation.
     */
    suspend fun saveHistoryInternal(
        sourceId: String,
        path: String,
        positionMs: Long,
        durationMs: Long,
        title: String = "",
        thumbnailUrl: String? = null,
        sourceType: SourceType = SourceType.Local,
        isCompleted: Boolean? = null
    ) {
        if (positionMs == 0L || (positionMs > 5000L && durationMs > 0L)) {
            saveHistory(
                sourceId = sourceId,
                path = path,
                positionMs = positionMs,
                durationMs = durationMs,
                title = title,
                thumbnailUrl = thumbnailUrl,
                sourceType = sourceType,
                isCompleted = isCompleted
            )
        }
    }
}
