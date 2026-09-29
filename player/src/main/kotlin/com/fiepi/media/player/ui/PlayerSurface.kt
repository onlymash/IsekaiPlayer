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

package com.fiepi.media.player.ui

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.viewinterop.AndroidView
import com.fiepi.media.domain.model.preferences.ExoSubtitleOptions
import com.fiepi.media.domain.model.preferences.VideoScaleMode
import com.fiepi.media.domain.player.model.MediaTrack
import com.fiepi.media.domain.player.model.PlayerEngineType
import com.fiepi.media.player.model.PlayerSurfaceEvent
import com.fiepi.media.player.ui.view.PlayerSurfaceView
import com.fiepi.media.player.ui.view.PlayerTextureView
import kotlin.math.roundToInt

/**
 * Universal Compose wrapper for media player surface (supports MPV, ExoPlayer, etc.).
 * Dispatches to engine-specific surfaces ([MpvPlayerSurface] and [ExoPlayerSurface]).
 */
@Composable
fun PlayerSurface(
    onEvent: (PlayerSurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
    useTextureView: Boolean = false,
    engineType: PlayerEngineType = PlayerEngineType.MPV,
    videoScaleMode: VideoScaleMode = VideoScaleMode.Fit,
    selectedVideoTrack: MediaTrack.Video? = null,
    subtitleText: String? = null,
    exoSubtitleOptions: ExoSubtitleOptions? = null,
    onSurfaceViewCreated: ((SurfaceView?) -> Unit)? = null
) {
    when (engineType) {
        PlayerEngineType.MPV -> {
            MpvPlayerSurface(
                onEvent = onEvent,
                useTextureView = useTextureView,
                onSurfaceViewCreated = onSurfaceViewCreated,
                modifier = modifier
            )
        }

        PlayerEngineType.EXO_PLAYER -> {
            ExoPlayerSurface(
                onEvent = onEvent,
                useTextureView = useTextureView,
                videoScaleMode = videoScaleMode,
                selectedVideoTrack = selectedVideoTrack,
                subtitleText = subtitleText,
                exoSubtitleOptions = exoSubtitleOptions,
                onSurfaceViewCreated = onSurfaceViewCreated,
                modifier = modifier
            )
        }
    }
}

/**
 * Surface layout for MPV engine.
 * Native GPU / libass renders video and subtitles directly onto the surface.
 */
@Composable
fun MpvPlayerSurface(
    onEvent: (PlayerSurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
    useTextureView: Boolean = false,
    onSurfaceViewCreated: ((SurfaceView?) -> Unit)? = null
) {
    key(useTextureView) {
        AndroidView(
            factory = { ctx ->
                if (useTextureView) {
                    PlayerTextureView(ctx).apply {
                        this.onSurfaceEvent = onEvent
                    }
                } else {
                    PlayerSurfaceView(ctx).apply {
                        onSurfaceViewCreated?.invoke(this)
                        this.onSurfaceEvent = { event ->
                            if (event is PlayerSurfaceEvent.Destroyed) {
                                onSurfaceViewCreated?.invoke(null)
                            }
                            onEvent(event)
                        }
                    }
                }
            },
            modifier = modifier
        )
    }
}

/**
 * Surface layout for ExoPlayer engine.
 * Displays video on SurfaceView/TextureView and renders subtitle cues using pure Compose [ExoSubtitleOverlay].
 */
@Composable
fun ExoPlayerSurface(
    onEvent: (PlayerSurfaceEvent) -> Unit,
    modifier: Modifier = Modifier,
    useTextureView: Boolean = false,
    videoScaleMode: VideoScaleMode = VideoScaleMode.Fit,
    selectedVideoTrack: MediaTrack.Video? = null,
    subtitleText: String? = null,
    exoSubtitleOptions: ExoSubtitleOptions? = null,
    onSurfaceViewCreated: ((SurfaceView?) -> Unit)? = null
) {
    val contentScale = when (videoScaleMode) {
        VideoScaleMode.Fit -> ContentScale.Fit
        VideoScaleMode.Fill -> ContentScale.Crop
        VideoScaleMode.Original -> ContentScale.None
    }

    val ratioModifier = if (selectedVideoTrack != null && selectedVideoTrack.width > 0 && selectedVideoTrack.height > 0) {
        Modifier.resizeWithContentScale(contentScale, selectedVideoTrack.width, selectedVideoTrack.height)
    } else {
        Modifier.fillMaxSize()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = ratioModifier
        ) {
            key(useTextureView) {
                AndroidView(
                    factory = { ctx ->
                        if (useTextureView) {
                            PlayerTextureView(ctx).apply {
                                this.onSurfaceEvent = onEvent
                            }
                        } else {
                            PlayerSurfaceView(ctx).apply {
                                onSurfaceViewCreated?.invoke(this)
                                this.onSurfaceEvent = { event ->
                                    if (event is PlayerSurfaceEvent.Destroyed) {
                                        onSurfaceViewCreated?.invoke(null)
                                    }
                                    onEvent(event)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Pure Jetpack Compose Subtitle Overlay
            ExoSubtitleOverlay(
                text = subtitleText,
                options = exoSubtitleOptions ?: ExoSubtitleOptions()
            )
        }
    }
}

/**
 * Media3-style layout modifier that resizes content according to [ContentScale] and source size.
 */
private fun Modifier.resizeWithContentScale(
    contentScale: ContentScale,
    videoWidth: Int,
    videoHeight: Int
): Modifier {
    if (videoWidth <= 0 || videoHeight <= 0) return this.fillMaxSize()

    return this
        .fillMaxSize()
        .wrapContentSize()
        .then(
            Modifier.layout { measurable, constraints ->
                val srcSizePx = Size(videoWidth.toFloat(), videoHeight.toFloat())
                val dstSizePx =
                    Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                val scaleFactor = contentScale.computeScaleFactor(srcSizePx, dstSizePx)
                val placeable = measurable.measure(
                    constraints.copy(
                        maxWidth = (srcSizePx.width * scaleFactor.scaleX).roundToInt(),
                        maxHeight = (srcSizePx.height * scaleFactor.scaleY).roundToInt()
                    )
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        )
}
