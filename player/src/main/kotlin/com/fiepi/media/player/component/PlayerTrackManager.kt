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
import com.fiepi.media.domain.player.model.ExternalSubtitle
import com.fiepi.media.domain.player.model.MediaTrack
import com.fiepi.media.domain.player.model.PlayerEngineType
import com.fiepi.media.domain.player.model.TrackType
import com.fiepi.media.domain.utils.SubtitleUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages audio/video/subtitle track selection, interlock defense (preventing simultaneous disable of both audio and video),
 * and automatic external subtitle language selection logic.
 *
 * Uses callbacks to trigger engine operations on [PlayerEngineManager] without holding direct references to [com.fiepi.media.domain.player.PlayerEngine].
 */
internal class PlayerTrackManager(
    private val getEngineType: () -> PlayerEngineType,
    private val onSelectTrack: (type: TrackType, id: Int) -> Unit,
    private val onAddSubtitleToEngine: (subtitle: ExternalSubtitle) -> Unit,
    private val onAddSubtitlesToEngine: (subtitles: List<ExternalSubtitle>) -> Unit,
    private val onAddSubtitleToState: ((subtitleFile: MediaFile.Subtitle) -> Unit)? = null
) {

    var lastSelectedAudioId: Int? = null
        private set

    var lastSelectedVideoId: Int? = null
        private set

    var expectedExternalSubtitlesCount: Int = 0
        private set

    var autoSubtitleSelectionDone: Boolean = false
        private set

    fun resetForNewVideo(expectedSubtitlesCount: Int) {
        lastSelectedAudioId = null
        lastSelectedVideoId = null
        expectedExternalSubtitlesCount = expectedSubtitlesCount
        autoSubtitleSelectionDone = false
    }

    /**
     * Validates track interlock defense rules before invoking [onSelectTrack] callback.
     * Returns true if track selection should proceed, false if blocked by interlock rule.
     */
    fun selectTrack(
        type: TrackType,
        id: Int
    ): Boolean {
        if (id == -1) {
            // [INTERLOCK DEFENSE] Prohibit disabling both audio and video simultaneously.
            if (type == TrackType.Video) {
                val hasAudio = (lastSelectedAudioId ?: -1) != -1
                if (!hasAudio) return false
            }
            if (type == TrackType.Audio) {
                val hasVideo = (lastSelectedVideoId ?: -1) != -1
                if (!hasVideo) return false
            }
        }

        // Optimistically sync tracked ID
        when (type) {
            TrackType.Audio -> lastSelectedAudioId = id
            TrackType.Video -> lastSelectedVideoId = id
            else -> {}
        }

        onSelectTrack(type, id)
        return true
    }

    fun onAudioTracksChanged(tracks: List<MediaTrack.Audio>) {
        if (tracks.isNotEmpty()) {
            lastSelectedAudioId = tracks.find { it.isSelected }?.id ?: -1
        }
    }

    fun onVideoTracksChanged(tracks: List<MediaTrack.Video>) {
        if (tracks.isNotEmpty()) {
            lastSelectedVideoId = tracks.find { it.isSelected }?.id ?: -1
        }
    }

    /**
     * Forces automatic subtitle track selection via [onSelectTrack] callback.
     * Switches track to 'None' (-1) then 'Auto' (-2) after 100ms delay.
     */
    fun triggerAutoSubtitleSelection(playerScope: CoroutineScope) {
        autoSubtitleSelectionDone = true
        playerScope.launch {
            onSelectTrack(TrackType.Subtitle, -1) // None
            delay(100.milliseconds)
            onSelectTrack(TrackType.Subtitle, -2) // Auto
        }
    }

    fun onSubtitleTracksChanged(
        tracks: List<MediaTrack.Subtitle>,
        currentSource: MediaSource?,
        playerScope: CoroutineScope
    ) {
        if (currentSource is MediaSource.Remote || currentSource == null) {
            // [EXTERNAL SUBTITLE AUTO-ACTIVATION LOGIC]
            val currentExternalCount = tracks.count { it.isExternal }
            val shouldTriggerAuto = !autoSubtitleSelectionDone &&
                    currentExternalCount > 0 &&
                    currentExternalCount >= expectedExternalSubtitlesCount

            if (shouldTriggerAuto) {
                triggerAutoSubtitleSelection(playerScope)
            }
        } else {
            autoSubtitleSelectionDone = true
        }
    }

    /**
     * Handles adding a single external subtitle via [onAddSubtitleToEngine] callback.
     */
    fun addSubtitle(
        subtitle: ExternalSubtitle,
        mediaSourceResolver: MediaSourceResolver,
        playerScope: CoroutineScope
    ) {
        val subtitleInfo = SubtitleUtils.parse(subtitle.url)
        val ext = subtitle.extension ?: subtitleInfo.extension
        val parsedLanguage = subtitle.language?.takeIf { it.isNotBlank() } ?: subtitleInfo.language

        // Notify playlist state update so external subtitle persists across engine switches
        val subtitleFile = MediaFile.Subtitle(
            path = subtitle.url,
            name = subtitleInfo.baseName,
            sourceType = SourceType.External,
            language = parsedLanguage,
            extension = ext,
            mimeType = subtitle.mimeType
        )

        val resolvedUrl = mediaSourceResolver.resolveExternalUrl(subtitleFile)

        onAddSubtitleToState?.invoke(subtitleFile)

        val resolvedSubtitle = subtitle.copy(url = resolvedUrl, language = parsedLanguage)
        onAddSubtitleToEngine(resolvedSubtitle)
        triggerAutoSubtitleSelection(playerScope)
    }

    /**
     * Handles adding a single external subtitle by URL string.
     */
    fun addSubtitle(
        url: String,
        mediaSourceResolver: MediaSourceResolver,
        playerScope: CoroutineScope
    ) {
        val parsedLanguage = try {
            SubtitleUtils.extractLanguageCode(url)
        } catch (_: Exception) {
            null
        }
        val subtitle = ExternalSubtitle(
            url = url,
            language = parsedLanguage
        )
        addSubtitle(
            subtitle = subtitle,
            mediaSourceResolver = mediaSourceResolver,
            playerScope = playerScope
        )
    }

    /**
     * Handles adding multiple external subtitles in batch.
     */
    fun addSubtitles(
        subtitles: List<ExternalSubtitle>,
        mediaSourceResolver: MediaSourceResolver,
        playerScope: CoroutineScope
    ) {
        subtitles.forEach { subtitle ->
            addSubtitle(
                subtitle = subtitle,
                mediaSourceResolver = mediaSourceResolver,
                playerScope = playerScope
            )
        }
    }

    /**
     * Handles delayed batch loading of external subtitles upon FileLoaded event via [onAddSubtitlesToEngine] callback.
     */
    fun loadExternalSubtitlesForVideo(
        video: MediaFile.Video?,
        source: MediaSource?,
        wasLoaded: Boolean,
        mediaSourceResolver: MediaSourceResolver,
        playerScope: CoroutineScope
    ) {
        if (wasLoaded || video == null || video.externalSubtitles.isEmpty()) return

        val engineType = getEngineType()

        val subsToLoad =
            if (engineType == PlayerEngineType.EXO_PLAYER || source is MediaSource.Remote) {
                video.externalSubtitles
            } else {
                // MPV auto-detects local same-directory file:// subs. Active load only manual SAF content:// subs.
                video.externalSubtitles.filter { sub ->
                    sub.path.startsWith("content://")
                }
            }

        if (subsToLoad.isNotEmpty()) {
            val subs = subsToLoad.map { sub ->
                val subUrl = mediaSourceResolver.resolveMediaUrl(source, sub)
                ExternalSubtitle(
                    url = subUrl,
                    language = sub.language,
                    extension = sub.extension ?: ExternalSubtitle.extractExtension(sub.path)
                    ?: ExternalSubtitle.extractExtension(sub.name),
                    mimeType = sub.mimeType ?: ExternalSubtitle.resolveMimeType(sub.extension)
                )
            }
            onAddSubtitlesToEngine(subs)
            triggerAutoSubtitleSelection(playerScope)
        }
    }
}
