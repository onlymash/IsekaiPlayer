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

package com.fiepi.media.app.ui.screen.settings.pages.player.subpage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ArtTrack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.DynamicForm
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.player.components.LocalBackdrop
import com.fiepi.media.app.ui.screen.player.components.LocalButtonEffectOptions
import com.fiepi.media.app.ui.screen.player.components.PlayerActionButton
import com.fiepi.media.app.ui.screen.player.viewmodel.PlayerState
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.PreferencesLabel
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerPrefsIntent
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.ButtonEffectOptions
import com.fiepi.media.domain.model.preferences.PlayerAction
import com.fiepi.media.domain.model.preferences.PlayerLayoutOptions
import com.fiepi.media.domain.model.preferences.PlayerOptions
import com.fiepi.media.domain.model.preferences.PlayerUiOptions
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.cos
import kotlin.math.sin


enum class LayoutCorner {
    TopLeft, TopRight, BottomLeft, BottomRight;
}

@Composable
fun PlayerLayoutPage(
    options: PlayerOptions,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    onIntent: (PlayerPrefsIntent) -> Unit,
) {
    val layoutOptions = options.layoutOptions
    var selectedCorner by remember { mutableStateOf(LayoutCorner.TopLeft) }

    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            PlayerLayoutPreview(
                layoutOptions = layoutOptions,
                buttonEffectOptions = options.buttonEffect,
                selectedCorner = selectedCorner,
                onCornerSelect = { selectedCorner = it },
                onActionRemove = { corner, action ->
                    val newActions = when (corner) {
                        LayoutCorner.TopLeft -> layoutOptions.topLeftActions - action
                        LayoutCorner.TopRight -> layoutOptions.topRightActions - action
                        LayoutCorner.BottomLeft -> layoutOptions.bottomLeftActions - action
                        LayoutCorner.BottomRight -> layoutOptions.bottomRightActions - action
                    }
                    val newLayout = when (corner) {
                        LayoutCorner.TopLeft -> layoutOptions.copy(topLeftActions = newActions)
                        LayoutCorner.TopRight -> layoutOptions.copy(topRightActions = newActions)
                        LayoutCorner.BottomLeft -> layoutOptions.copy(bottomLeftActions = newActions)
                        LayoutCorner.BottomRight -> layoutOptions.copy(bottomRightActions = newActions)
                    }
                    onIntent(PlayerPrefsIntent.UpdateLayoutOptions(newLayout))
                }
            )
        }

        item {
            PreferencesGroup(
                items = arrayOf(
                    { shapes: ListItemShapes ->
                        SwitchPreferencesItem(
                            title = stringResource(R.string.settings_player_layout_show_title_bar),
                            checked = layoutOptions.showTitleBar,
                            onCheckedChange = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateLayoutOptions(
                                        layoutOptions.copy(
                                            showTitleBar = it
                                        )
                                    )
                                )
                            },
                            shapes = shapes,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Title,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                )
            )
        }

        item {
            PreferencesLabel(
                title = stringResource(R.string.settings_player_layout_all_buttons)
            )
        }

        item {
            ActionBank(
                layoutOptions = layoutOptions,
                onActionAdd = { action ->
                    val currentActions = when (selectedCorner) {
                        LayoutCorner.TopLeft -> layoutOptions.topLeftActions
                        LayoutCorner.TopRight -> layoutOptions.topRightActions
                        LayoutCorner.BottomLeft -> layoutOptions.bottomLeftActions
                        LayoutCorner.BottomRight -> layoutOptions.bottomRightActions
                    }
                    if (action !in currentActions) {
                        val newActions = currentActions + action
                        val newLayout = when (selectedCorner) {
                            LayoutCorner.TopLeft -> layoutOptions.copy(topLeftActions = newActions)
                            LayoutCorner.TopRight -> layoutOptions.copy(topRightActions = newActions)
                            LayoutCorner.BottomLeft -> layoutOptions.copy(bottomLeftActions = newActions)
                            LayoutCorner.BottomRight -> layoutOptions.copy(bottomRightActions = newActions)
                        }
                        onIntent(PlayerPrefsIntent.UpdateLayoutOptions(newLayout))
                    }
                }
            )
        }
    }
}

@Composable
private fun PlayerLayoutPreview(
    layoutOptions: PlayerLayoutOptions,
    buttonEffectOptions: ButtonEffectOptions,
    selectedCorner: LayoutCorner,
    onCornerSelect: (LayoutCorner) -> Unit,
    onActionRemove: (LayoutCorner, PlayerAction) -> Unit
) {
    val mockState = PlayerState(
        uiOptions = PlayerUiOptions(layoutOptions = layoutOptions)
    )
    val infiniteTransition = rememberInfiniteTransition(label = "PlayerLayoutPreview")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Time"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(320.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val backdrop = rememberLayerBackdrop()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            val time = animTime * 2 * Math.PI.toFloat()

            // Add high-frequency particles (snowflakes/dust) with movement
            val random = java.util.Random(42) // Fixed seed for stable preview
            repeat(300) {
                val startX = random.nextFloat() * size.width
                val startY = random.nextFloat() * size.height
                val speed = random.nextFloat() * 20.dp.toPx() + 10.dp.toPx()

                drawCircle(
                    color = Color.White.copy(alpha = random.nextFloat() * 0.4f + 0.2f),
                    radius = (random.nextFloat() * 1.5f + 0.5f).dp.toPx(),
                    center = Offset(
                        x = (startX + speed * sin(time + startX)) % size.width,
                        y = (startY + speed * cos(time + startY)) % size.height
                    )
                )
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalButtonEffectOptions provides buttonEffectOptions
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = layoutOptions.showTitleBar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlayerActionButton(
                            action = PlayerAction.Back,
                            state = mockState,
                            onIntent = {},
                            onOpenDrawer = {}
                        )
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.common_preview),
                            color = Color.White,
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
                        PlayerActionButton(
                            action = PlayerAction.DecoderSettings,
                            state = mockState,
                            onIntent = {},
                            onOpenDrawer = {}
                        )
                    }
                }

                // Top Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    PreviewCorner(
                        modifier = Modifier.weight(1f),
                        cornerType = LayoutCorner.TopLeft,
                        actions = layoutOptions.topLeftActions,
                        isSelected = selectedCorner == LayoutCorner.TopLeft,
                        state = mockState,
                        onClick = { onCornerSelect(LayoutCorner.TopLeft) },
                        onRemove = { onActionRemove(LayoutCorner.TopLeft, it) },
                    )
                    PreviewCorner(
                        modifier = Modifier.weight(1f),
                        cornerType = LayoutCorner.TopRight,
                        actions = layoutOptions.topRightActions,
                        isSelected = selectedCorner == LayoutCorner.TopRight,
                        state = mockState,
                        onClick = { onCornerSelect(LayoutCorner.TopRight) },
                        onRemove = { onActionRemove(LayoutCorner.TopRight, it) },
                    )
                }

                // Bottom Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    PreviewCorner(
                        modifier = Modifier.weight(1f),
                        cornerType = LayoutCorner.BottomLeft,
                        actions = layoutOptions.bottomLeftActions,
                        isSelected = selectedCorner == LayoutCorner.BottomLeft,
                        state = mockState,
                        onClick = { onCornerSelect(LayoutCorner.BottomLeft) },
                        onRemove = { onActionRemove(LayoutCorner.BottomLeft, it) },
                    )
                    PreviewCorner(
                        modifier = Modifier.weight(1f),
                        cornerType = LayoutCorner.BottomRight,
                        actions = layoutOptions.bottomRightActions,
                        isSelected = selectedCorner == LayoutCorner.BottomRight,
                        state = mockState,
                        onClick = { onCornerSelect(LayoutCorner.BottomRight) },
                        onRemove = { onActionRemove(LayoutCorner.BottomRight, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewCorner(
    modifier: Modifier = Modifier,
    cornerType: LayoutCorner,
    actions: List<PlayerAction>,
    isSelected: Boolean,
    state: PlayerState,
    onClick: () -> Unit,
    onRemove: (PlayerAction) -> Unit
) {
    val isRightAligned =
        cornerType == LayoutCorner.TopRight || cornerType == LayoutCorner.BottomRight
    val isBottomAligned =
        cornerType == LayoutCorner.BottomLeft || cornerType == LayoutCorner.BottomRight

    FlowRow(
        modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .graphicsLayer {
                scaleX = if (isRightAligned) -1f else 1f
                scaleY = if (isBottomAligned) -1f else 1f
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
    ) {
        actions.forEach { action ->
            Box(
                modifier = Modifier.graphicsLayer {
                    // Double flip to keep content oriented correctly
                    scaleX = if (isRightAligned) -1f else 1f
                    scaleY = if (isBottomAligned) -1f else 1f
                }
            ) {
                PlayerActionButton(
                    action = action,
                    state = state,
                    onIntent = {},
                    onOpenDrawer = {}
                )
                // Remove Button
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clickable { onRemove(action) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionBank(
    modifier: Modifier = Modifier,
    layoutOptions: PlayerLayoutOptions,
    onActionAdd: (PlayerAction) -> Unit
) {
    val usedActions = remember(layoutOptions) {
        layoutOptions.topLeftActions + layoutOptions.topRightActions +
                layoutOptions.bottomLeftActions + layoutOptions.bottomRightActions
    }

    val allActions = PlayerAction.entries.filter { it != PlayerAction.None }

    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            allActions.forEach { action ->
                val isUsed = action in usedActions
                ActionBankItem(
                    modifier = Modifier.width(80.dp),
                    action = action,
                    isUsed = isUsed,
                    onClick = { if (!isUsed) onActionAdd(action) }
                )
            }
        }
    }
}

@Composable
private fun ActionBankItem(
    modifier: Modifier = Modifier,
    action: PlayerAction,
    isUsed: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButtonDefaults.filledIconButtonColors()
        FilledTonalIconButton(
            modifier = Modifier.size(48.dp),
            enabled = !isUsed,
            onClick = onClick,
        ) {
            Icon(
                imageVector = action.getIcon(),
                contentDescription = null
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = action.getLabel(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = if (isUsed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun PlayerAction.getIcon(): ImageVector {
    return when (this) {
        PlayerAction.None -> Icons.Outlined.Block
        PlayerAction.Lock -> Icons.Outlined.Lock
        PlayerAction.Screenshot -> Icons.Outlined.PhotoCamera
        PlayerAction.PiP -> Icons.Outlined.PictureInPicture
        PlayerAction.BackgroundAudio -> Icons.Outlined.Headset
        PlayerAction.Osd -> Icons.Outlined.Info
        PlayerAction.Playlist -> Icons.Outlined.VideoLibrary
        PlayerAction.Tracks -> Icons.Outlined.ArtTrack
        PlayerAction.LoopModeToggle -> Icons.Outlined.Repeat
        PlayerAction.ScaleModeToggle -> Icons.Outlined.AspectRatio
        PlayerAction.PlaybackSpeed -> Icons.Outlined.Speed
        PlayerAction.PlayerSettings -> Icons.Outlined.DisplaySettings
        PlayerAction.Back -> Icons.AutoMirrored.Rounded.ArrowBack
        PlayerAction.DecoderSettings -> Icons.Outlined.DynamicForm
        PlayerAction.AudioSettings -> Icons.Outlined.Audiotrack
        PlayerAction.SubtitleSettings -> Icons.Outlined.Subtitles
        PlayerAction.GestureSettings -> Icons.Outlined.Gesture
        PlayerAction.AudioDelay -> Icons.Outlined.AvTimer
        PlayerAction.SubtitleDelay -> Icons.Outlined.Timer
    }
}

@Composable
private fun PlayerAction.getLabel(): String {
    return when (this) {
        PlayerAction.None -> ""
        PlayerAction.Lock -> stringResource(R.string.player_control_lock)
        PlayerAction.Screenshot -> stringResource(R.string.player_control_screenshot)
        PlayerAction.PiP -> stringResource(R.string.player_control_enter_pip)
        PlayerAction.BackgroundAudio -> stringResource(R.string.settings_audio_enable_background_audio)
        PlayerAction.Osd -> stringResource(R.string.player_control_osd)
        PlayerAction.Playlist -> stringResource(R.string.player_drawer_playlist_button)
        PlayerAction.Tracks -> stringResource(R.string.player_drawer_tracks_button)
        PlayerAction.LoopModeToggle -> stringResource(R.string.settings_player_loop_mode)
        PlayerAction.ScaleModeToggle -> stringResource(R.string.settings_player_scale_mode)
        PlayerAction.PlaybackSpeed -> stringResource(R.string.player_control_playback_speed)
        PlayerAction.PlayerSettings -> stringResource(R.string.player_settings_player)
        PlayerAction.Back -> stringResource(R.string.common_back)
        PlayerAction.DecoderSettings -> stringResource(R.string.player_settings_decoder)
        PlayerAction.AudioSettings -> stringResource(R.string.player_settings_audio)
        PlayerAction.SubtitleSettings -> stringResource(R.string.player_settings_subtitle)
        PlayerAction.GestureSettings -> stringResource(R.string.player_settings_gestures)
        PlayerAction.AudioDelay -> stringResource(R.string.player_audio_delay)
        PlayerAction.SubtitleDelay -> stringResource(R.string.player_subtitle_delay)
    }
}


@Preview
@Composable
fun PlayerLayoutPagePreview() {
    AppTheme {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            PlayerLayoutPage(
                options = PlayerOptions(
                    layoutOptions = PlayerLayoutOptions(
                        topLeftActions = listOf(
                            PlayerAction.Screenshot
                        ),
                        topRightActions = listOf(
                            PlayerAction.Lock,
                            PlayerAction.PlaybackSpeed
                        ),
                        bottomRightActions = listOf(
                            PlayerAction.ScaleModeToggle,
                            PlayerAction.LoopModeToggle,
                            PlayerAction.PiP,
                            PlayerAction.Osd
                        ),
                        bottomLeftActions = listOf()
                    )
                ),
                contentPadding = PaddingValues(0.dp),
                onIntent = {}
            )
        }
    }
}