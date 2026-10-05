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

package com.fiepi.media.app.ui.screen.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerDrawerType
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.PlayerAction
import com.fiepi.media.domain.model.preferences.PlayerLayoutOptions
import com.fiepi.media.domain.model.preferences.PlayerUiOptions
import com.fiepi.media.domain.player.model.MediaMetadata
import com.fiepi.media.domain.player.model.MediaPlaybackState
import com.fiepi.media.domain.player.model.MediaTrack
import java.util.Locale
import kotlin.math.abs


@Composable
fun PlayerTopBar(
    modifier: Modifier = Modifier,
    state: PlayerState,
    onIntent: (PlayerIntent) -> Unit,
    onBack: () -> Unit,
    onOpenDrawer: (PlayerDrawerType) -> Unit
) {
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Top
    )
    Column(
        modifier = modifier
            .windowInsetsPadding(insets)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block background click */ }
    ) {
        if (state.uiOptions.layoutOptions.showTitleBar) {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        text = state.playback.mediaTitle
                            ?: state.playback.mediaId.substringAfterLast(
                                '/'
                            ),
                        style = MaterialTheme.typography.titleMediumEmphasized.copy(
                            color = Color.White,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = Offset(1f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    val selectedVideoTrack = state.playback.selectedVideoTrack
                    val videoInfoSubtitle = remember(selectedVideoTrack) {
                        buildVideoInfoSubtitle(selectedVideoTrack)
                    }
                    AnimatedVisibility(
                        modifier = Modifier.fillMaxWidth(),
                        visible = videoInfoSubtitle.isNotBlank(),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            text = videoInfoSubtitle,
                            style = MaterialTheme.typography.bodySmallEmphasized.copy(
                                color = Color.White.copy(alpha = 0.95f),
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.8f),
                                    offset = Offset(1f, 1f),
                                    blurRadius = 1f
                                )
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    PlayerActionButton(
                        action = PlayerAction.Back,
                        state = state,
                        onIntent = onIntent,
                        onOpenDrawer = onOpenDrawer,
                        onBack = onBack
                    )
                },
                actions = {
                    PlayerActionButton(
                        action = PlayerAction.DecoderSettings,
                        state = state,
                        onIntent = onIntent,
                        onOpenDrawer = onOpenDrawer
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                windowInsets = WindowInsets()
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Top Left Corner (Under TopAppBar)
            PlayerCornerActions(
                modifier = Modifier.weight(1f),
                actions = state.uiOptions.layoutOptions.topLeftActions,
                state = state,
                onIntent = onIntent,
                onOpenDrawer = onOpenDrawer,
                onBack = onBack,
                horizontalAlignment = Alignment.Start
            )

            // Top Right Corner (Under TopAppBar)
            PlayerCornerActions(
                modifier = Modifier.weight(1f),
                actions = state.uiOptions.layoutOptions.topRightActions,
                state = state,
                onIntent = onIntent,
                onOpenDrawer = onOpenDrawer,
                onBack = onBack,
                horizontalAlignment = Alignment.End
            )
        }
    }
}

private fun buildVideoInfoSubtitle(track: MediaTrack.Video?): String {
    if (track == null) return ""
    val resolution =
        if (track.width > 0 && track.height > 0) "${track.width}×${track.height}" else null
    val fps = if (track.fps > 0.0) {
        val rounded = track.fps.toInt()
        if (abs(track.fps - rounded) < 0.01) "$rounded fps" else String.format(
            Locale.US,
            "%.2f fps",
            track.fps
        )
    } else null
    val codec = track.codec?.takeIf { it.isNotBlank() }?.let { formatCodec(it) }
    val bitrate = if (track.bitrate > 0) {
        if (track.bitrate >= 1_000_000) {
            String.format(Locale.getDefault(), "%.1f Mbps", track.bitrate / 1_000_000.0)
        } else {
            "${track.bitrate / 1000} kbps"
        }
    } else null

    return listOfNotNull(resolution, fps, codec, bitrate).joinToString(" • ")
}

private fun formatCodec(codec: String): String {
    val cleaned = codec.lowercase()
        .removePrefix("video/")
        .removePrefix("video/x-vnd.on2.")
    return when {
        cleaned.contains("avc") || cleaned.contains("h264") || cleaned.contains("h.264") -> "H.264"
        cleaned.contains("hevc") || cleaned.contains("h265") || cleaned.contains("h.265") -> "HEVC"
        cleaned.contains("av01") || cleaned.contains("av1") -> "AV1"
        cleaned.contains("vp9") -> "VP9"
        cleaned.contains("vp8") -> "VP8"
        cleaned.contains("mpeg4") || cleaned.contains("mp4v") -> "MPEG-4"
        else -> cleaned.uppercase()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF555555)
@Composable
private fun PlayerTopBarPreview() {
    AppTheme {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            PlayerTopBar(
                state = PlayerState(
                    playback = MediaPlaybackState(
                        mediaMetadata = MediaMetadata(title = "Sample Video Title"),
                        videoTracks = listOf(
                            MediaTrack.Video(
                                id = 1,
                                title = "1080p",
                                language = null,
                                isSelected = true,
                                width = 1920,
                                height = 1080,
                                fps = 60.0,
                                codec = "hevc",
                                bitrate = 12_500_000L
                            )
                        )
                    ),
                    uiOptions = PlayerUiOptions(
                        layoutOptions = PlayerLayoutOptions(
                            topLeftActions = listOf(
                                PlayerAction.Screenshot,
                                PlayerAction.PiP,
                                PlayerAction.Osd,
                                PlayerAction.PlayerSettings
                            ),
                            topRightActions = listOf(
                                PlayerAction.Lock,
                                PlayerAction.PlaybackSpeed
                            )
                        ),
                    )
                ),
                onIntent = {},
                onBack = {},
                onOpenDrawer = {}
            )
        }
    }
}
