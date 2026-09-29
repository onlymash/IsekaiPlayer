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

package com.fiepi.media.app.ui.screen.player

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.paging.compose.collectAsLazyPagingItems
import com.fiepi.media.app.ui.components.AppBackHandler
import com.fiepi.media.app.ui.screen.player.components.LocalBackdrop
import com.fiepi.media.app.ui.screen.player.components.LocalButtonEffectOptions
import com.fiepi.media.app.ui.screen.player.components.PlayerControls
import com.fiepi.media.app.ui.screen.player.components.PlayerDrawer
import com.fiepi.media.app.ui.screen.player.components.PlayerSideEffects
import com.fiepi.media.app.ui.screen.player.components.PlayerSystemUiEffect
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerControlMode
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerIntent
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerViewModel
import com.fiepi.media.domain.player.model.PlayerEngineType
import com.fiepi.media.player.ui.PlayerSurface
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Stateful entry point for the Player Screen.
 * Hosts the root layout including the Video surface and the Navigation Drawer for tracks.
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onEnterPiP: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val playlistPagingItems = viewModel.playlistItemPagingFlow.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val backdrop = rememberLayerBackdrop()
    val isExoPlayer = state.playbackOptions.engineType == PlayerEngineType.EXO_PLAYER
    val isBackdropEnabled = if (isExoPlayer) {
        (state.uiOptions.buttonEffect.blur || state.uiOptions.buttonEffect.lens) && state.uiOptions.buttonEffect.useTextureView
    } else {
        (state.uiOptions.buttonEffect.blur || state.uiOptions.buttonEffect.lens)
    }
    val useTextureView = isBackdropEnabled && state.uiOptions.buttonEffect.useTextureView
    val isAnyOverlayVisible =
        state.ui.isControlsVisible || state.ui.controlMode != PlayerControlMode.Normal
    var surfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }

    // Block system back gesture when locked; Close drawer when opened
    AppBackHandler(enabled = state.ui.isLocked || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }
    }

    // Global Side Effects (PiP, Orientation, Brightness, etc.)
    PlayerSideEffects(
        state = state,
        effectFlow = viewModel.effect,
        onEnterPiP = onEnterPiP,
        onFinish = onBack
    )

    // System UI Management (Immersive mode)
    // Keep system bars visible if the player controls are shown and "always full screen" is disabled.
    PlayerSystemUiEffect(
        systemBarsVisible = state.ui.isControlsVisible && !state.uiOptions.alwaysFullScreen && drawerState.isClosed
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // PixelCopy Backdrop Bridge Layer (Active when backdrop is enabled, NOT using TextureView, and UI overlay shown)
        if (!state.ui.isPiP && isBackdropEnabled && !useTextureView && isAnyOverlayVisible && surfaceViewRef != null) {
            surfaceViewRef?.let { surfaceView ->
                PixelCopyBackdropCanvas(
                    surfaceView = surfaceView,
                    isFrameUpdating = state.playback.isPlaying || state.playback.isSeekingOrScrubbing,
                    isLoaded = state.playback.isLoaded,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Native Video Surface
        PlayerSurface(
            onEvent = { event ->
                viewModel.sendIntent(PlayerIntent.HandleSurfaceEvent(event))
            },
            onSurfaceViewCreated = { surfaceView ->
                surfaceViewRef = surfaceView
            },
            engineType = state.playbackOptions.engineType,
            videoScaleMode = state.uiOptions.videoScaleMode,
            selectedVideoTrack = state.playback.selectedVideoTrack,
            subtitleText = state.playback.subtitleText,
            exoSubtitleOptions = state.exoSubtitleOptions,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (state.playback.isLoaded) 1f else 0f
                }
                .then(
                    if (useTextureView && state.playback.isLoaded) {
                        Modifier.layerBackdrop(backdrop)
                    } else {
                        Modifier
                    }
                ),
            useTextureView = useTextureView
        )

        // Loading/Buffering Indicator
        if (!state.playback.isLoaded || state.playback.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Compose Player Controls
        if (!state.ui.isPiP) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                // Disable gestures when closed to prevent accidental activation during playback
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    PlayerDrawer(
                        state = state,
                        playlistItems = playlistPagingItems,
                        onIntent = viewModel::sendIntent,
                        onClose = {
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                }
            ) {
                CompositionLocalProvider(
                    LocalBackdrop provides if (isBackdropEnabled && state.playback.isLoaded) backdrop else emptyBackdrop(),
                    LocalButtonEffectOptions provides state.uiOptions.buttonEffect
                ) {
                    PlayerControls(
                        state = state,
                        onIntent = viewModel::sendIntent,
                        onBack = onBack,
                        onOpenDrawer = { type ->
                            scope.launch {
                                viewModel.sendIntent(PlayerIntent.SetActiveDrawer(type))
                                viewModel.sendIntent(PlayerIntent.SetControlsVisible(false))
                                drawerState.open()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelCopyBackdropCanvas(
    surfaceView: SurfaceView,
    isFrameUpdating: Boolean,
    isLoaded: Boolean,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier
) {
    var activeBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(isLoaded) {
        if (!isLoaded) {
            activeBitmap = null
        }
    }

    val (copyThread, copyHandler) = remember {
        val thread = android.os.HandlerThread(
            "PixelCopyBackdropThread",
            android.os.Process.THREAD_PRIORITY_DISPLAY
        ).apply { start() }
        thread to Handler(thread.looper)
    }

    DisposableEffect(Unit) {
        onDispose {
            copyThread.quitSafely()
        }
    }

    val isFrameUpdatingState = rememberUpdatedState(isFrameUpdating)

    LaunchedEffect(surfaceView) {
        var bmpA: Bitmap? = null
        var bmpB: Bitmap? = null
        var useBmpA = true
        val isCopying = java.util.concurrent.atomic.AtomicBoolean(false)
        var capturedWhileStatic = false

        while (isActive) {
            // Compose VSync alignment
            // Guarantees each loop aligns precisely with system refresh rate, eliminating unnecessary wait latency
            withFrameNanos { /* VSync alignment */ }

            val sw = surfaceView.width
            val sh = surfaceView.height
            val surface = surfaceView.holder.surface
            val frameUpdating = isFrameUpdatingState.value

            if (!frameUpdating && capturedWhileStatic) {
                snapshotFlow { isFrameUpdatingState.value }.first { it }
                capturedWhileStatic = false
                continue
            }

            if (surface?.isValid == true && sw > 0 && sh > 0 && isCopying.compareAndSet(
                    false,
                    true
                )
            ) {
                val targetW = (sw / 2).coerceAtLeast(1)
                val targetH = (sh / 2).coerceAtLeast(1)

                if (bmpA == null || bmpA.width != targetW || bmpA.height != targetH) {
                    bmpA = createBitmap(targetW, targetH)
                    bmpB = createBitmap(targetW, targetH)
                }

                val targetBmp = if (useBmpA) bmpA else bmpB!!
                val rect = Rect(0, 0, sw, sh)

                PixelCopy.request(
                    surfaceView,
                    rect,
                    targetBmp,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            val newImage = targetBmp.asImageBitmap()
                            // Directly assign State; Compose safely receives snapshot sync on multi-core CPUs, eliminating Handler.post queue latency
                            activeBitmap = newImage
                            useBmpA = !useBmpA

                            if (!frameUpdating) {
                                capturedWhileStatic = true
                            }
                        }
                        isCopying.set(false)
                    },
                    copyHandler
                )
            }
        }
    }

    activeBitmap?.let { imageBitmap ->
        Canvas(
            modifier = modifier
                .graphicsLayer { alpha = 0f }
                .layerBackdrop(backdrop)
        ) {
            drawImage(
                image = imageBitmap,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }
    }
}