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

package com.fiepi.media.app.ui.screen.player.components.page

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Audiotrack
import androidx.compose.material.icons.twotone.AvTimer
import androidx.compose.material.icons.twotone.ClosedCaption
import androidx.compose.material.icons.twotone.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerControlMode
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.app.ui.utils.displayedName
import com.fiepi.media.domain.player.model.MediaTrack
import com.fiepi.media.domain.player.model.TrackType


@Composable
fun TracksDrawerPage(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onClose: () -> Unit = {},
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            onIntent(PlayerIntent.AddSubtitle(it))
        }
    }

    val videoTracksTitle = stringResource(R.string.player_drawer_video_tracks)
    val audioTracksTitle = stringResource(R.string.player_drawer_audio_tracks)
    val subtitleTracksTitle = stringResource(R.string.player_drawer_subtitle_tracks)
    val noneText = stringResource(R.string.common_none)

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = contentPadding,
    ) {
        trackDrawerContent(
            state = state,
            onIntent = onIntent,
            onClose = onClose,
            videoTracksTitle = videoTracksTitle,
            audioTracksTitle = audioTracksTitle,
            subtitleTracksTitle = subtitleTracksTitle,
            noneText = noneText,
            onAddSubtitle = {
                subtitlePickerLauncher.launch(arrayOf("*/*"))
            }
        )
    }
}

private fun LazyListScope.trackDrawerContent(
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onClose: () -> Unit,
    videoTracksTitle: String,
    audioTracksTitle: String,
    subtitleTracksTitle: String,
    noneText: String,
    onAddSubtitle: () -> Unit
) {
    // Video Tracks
    trackGroup(
        title = videoTracksTitle,
        icon = Icons.TwoTone.Movie,
        tracks = state.playback.videoTracks,
        type = TrackType.Video,
        onSelect = { onIntent(PlayerIntent.SelectTrack(TrackType.Video, it)) },
        noneText = noneText
    )
    item(key = "spacer_video") {
        Spacer(
            modifier = Modifier
                .height(12.dp)
                .animateItem()
        )
    }
    // Audio Tracks
    trackGroup(
        title = audioTracksTitle,
        icon = Icons.TwoTone.Audiotrack,
        tracks = state.playback.audioTracks,
        type = TrackType.Audio,
        onSelect = { onIntent(PlayerIntent.SelectTrack(TrackType.Audio, it)) },
        noneText = noneText,
        trailingAction = if (state.playback.capabilities.supportsAudioDelay) {
            {
                IconButton(
                    onClick = {
                        onIntent(PlayerIntent.SetControlMode(PlayerControlMode.AudioDelay))
                        onClose()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.AvTimer,
                        contentDescription = stringResource(R.string.player_audio_delay),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else null
    )
    item(key = "spacer_audio") {
        Spacer(
            modifier = Modifier
                .height(12.dp)
                .animateItem()
        )
    }
    // Subtitle Tracks
    trackGroup(
        title = subtitleTracksTitle,
        icon = Icons.TwoTone.ClosedCaption,
        tracks = state.playback.subtitleTracks,
        type = TrackType.Subtitle,
        onSelect = { onIntent(PlayerIntent.SelectTrack(TrackType.Subtitle, it)) },
        noneText = noneText,
        allowDisable = true,
        trailingAction = if (state.playback.capabilities.supportsSubtitleDelay) {
            {
                IconButton(
                    onClick = {
                        onIntent(PlayerIntent.SetControlMode(PlayerControlMode.SubtitleDelay))
                        onClose()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = stringResource(R.string.player_subtitle_delay),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else null,
        onAddSubtitle = onAddSubtitle
    )
}

private fun LazyListScope.trackGroup(
    title: String,
    icon: ImageVector,
    tracks: List<MediaTrack>,
    type: TrackType,
    onSelect: (Int) -> Unit,
    noneText: String,
    allowDisable: Boolean = false,
    trailingAction: @Composable (() -> Unit)? = null,
    onAddSubtitle: (() -> Unit)? = null
) {
    item(key = "header_${type.name}") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .animateItem()
                .padding(top = 8.dp, bottom = 4.dp, start = 24.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            trailingAction?.invoke()
        }
    }

    val displayTracks = if (allowDisable || tracks.isEmpty()) {
        val noneTrack: MediaTrack = when (type) {
            TrackType.Audio -> MediaTrack.Audio(
                id = -1,
                title = noneText,
                language = null,
                isSelected = tracks.none { it.isSelected }
            )

            TrackType.Subtitle -> MediaTrack.Subtitle(
                id = -1,
                title = noneText,
                language = null,
                isSelected = tracks.none { it.isSelected }
            )

            TrackType.Video -> MediaTrack.Video(
                id = -1,
                title = noneText,
                language = null,
                isSelected = tracks.none { it.isSelected }
            )
        }
        listOf(noneTrack) + tracks
    } else {
        tracks
    }

    itemsIndexed(displayTracks, key = { _, track -> "${type.name}_${track.id}" }) { index, track ->
        SegmentedListItem(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .animateItem(),
            selected = track.isSelected,
            onClick = { onSelect(track.id) },
            colors = ListItemDefaults.segmentedColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.6f),
                containerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f),

                selectedLeadingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedSupportingContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                    alpha = 0.8f
                ),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shapes = ListItemDefaults.segmentedShapes(index, displayTracks.size),
            supportingContent = track.codec?.let { codec ->
                { Text(text = codec) }
            },
            content = { Text(text = track.displayedName) }
        )
    }

    if (onAddSubtitle != null) {
        item(key = "add_subtitle_button") {
            OutlinedButton(
                onClick = onAddSubtitle,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .animateItem()
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.player_add_external_subtitle))
            }
        }
    }
}

@Preview(showBackground = true, name = "Tracks Page")
@Composable
fun TracksDrawerPagePreview() {
    val mockAudioTracks = listOf(
        MediaTrack.Audio(1, "English (Default)", "eng", true, codec = "aac"),
        MediaTrack.Audio(2, "Japanese", "jpn", false, codec = "flac")
    )
    val mockSubtitleTracks = listOf(
        MediaTrack.Subtitle(1, "English (SRT)", "eng", false, codec = "subrip"),
        MediaTrack.Subtitle(2, "Chinese (Simplified)", "chi", true, codec = "ass")
    )
    val mockVideoTracks = listOf(
        MediaTrack.Video(1, "1080p (AVC)", null, true, codec = "h264")
    )

    val state = PlayerState(
        playback = com.fiepi.media.domain.player.model.MediaPlaybackState(
            audioTracks = mockAudioTracks,
            subtitleTracks = mockSubtitleTracks,
            videoTracks = mockVideoTracks
        )
    )
    AppTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f))
                .width(400.dp)
                .fillMaxHeight()
        ) {
            TracksDrawerPage(
                state = state,
                onIntent = {},
                contentPadding = PaddingValues(16.dp)
            )
        }
    }
}