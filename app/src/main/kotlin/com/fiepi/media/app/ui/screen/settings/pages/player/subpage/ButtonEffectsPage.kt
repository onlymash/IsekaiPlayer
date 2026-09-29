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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.player.components.LocalBackdrop
import com.fiepi.media.app.ui.screen.player.components.LocalButtonEffectOptions
import com.fiepi.media.app.ui.screen.player.components.PlayerIconButton
import com.fiepi.media.app.ui.screen.settings.components.PreferencesGroup
import com.fiepi.media.app.ui.screen.settings.components.SingleChoicePreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SliderPreferencesItem
import com.fiepi.media.app.ui.screen.settings.components.SwitchPreferencesItem
import com.fiepi.media.app.ui.screen.settings.pages.player.PlayerPrefsIntent
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.ButtonBorderStyle
import com.fiepi.media.domain.model.preferences.ButtonEffectOptions
import com.fiepi.media.domain.model.preferences.PlayerOptions
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun ButtonEffectsPage(
    options: PlayerOptions,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    onIntent: (PlayerPrefsIntent) -> Unit,
) {
    LazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item {
            ButtonEffectsPreview(options)
        }

        item {
            PreferencesGroup(
                title = stringResource(R.string.settings_player_button_effects),
                items = arrayOf(
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_button_effects_blur),
                            summary = stringResource(R.string.settings_player_button_effects_blur_summary),
                            checked = options.buttonEffect.blur,
                            onCheckedChange = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateButtonEffectOptions(
                                        options.buttonEffect.copy(blur = it)
                                    )
                                )
                            }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_button_effects_lens),
                            summary = stringResource(R.string.settings_player_button_effects_lens_summary),
                            checked = options.buttonEffect.lens,
                            onCheckedChange = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateButtonEffectOptions(
                                        options.buttonEffect.copy(lens = it)
                                    )
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_button_effects_alpha),
                            summary = stringResource(R.string.settings_player_button_effects_alpha_summary),
                            value = options.buttonEffect.alpha,
                            onValueChange = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateButtonEffectOptions(
                                        options.buttonEffect.copy(alpha = it)
                                    )
                                )
                            },
                            onReset = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateButtonEffectOptions(
                                        options.buttonEffect.copy(
                                            alpha = 0.2f
                                        )
                                    )
                                )
                            },
                            range = 0f..1f,
                            steps = 100,
                            valueFormat = "%.2f"
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_button_effects_show_border),
                            summary = stringResource(R.string.settings_player_button_effects_show_border_summary),
                            checked = options.buttonEffect.showBorder,
                            onCheckedChange = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateButtonEffectOptions(
                                        options.buttonEffect.copy(showBorder = it)
                                    )
                                )
                            }
                        )
                    },
                    { shapes ->
                        val styleTextEntries = listOf(
                            R.string.settings_player_button_effects_border_style_directional,
                            R.string.settings_player_button_effects_border_style_ambient,
                            R.string.settings_player_button_effects_border_style_plain,
                        )
                        SingleChoicePreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_button_effects_border_style),
                            options = ButtonBorderStyle.entries,
                            currentValue = options.buttonEffect.borderStyle,
                            enabled = options.buttonEffect.showBorder,
                            onValueChange = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateButtonEffectOptions(
                                        options.buttonEffect.copy(borderStyle = it)
                                    )
                                )
                            },
                            optionToText = { stringResource(styleTextEntries[it.ordinal]) }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            shapes = shapes,
                            title = stringResource(R.string.settings_player_button_effects_use_texture_view),
                            summary = stringResource(R.string.settings_player_button_effects_use_texture_view_summary),
                            checked = options.buttonEffect.useTextureView,
                            enabled = options.buttonEffect.blur || options.buttonEffect.lens,
                            onCheckedChange = {
                                onIntent(
                                    PlayerPrefsIntent.UpdateButtonEffectOptions(
                                        options.buttonEffect.copy(useTextureView = it)
                                    )
                                )
                            }
                        )
                    }
                )
            )
        }

        item {
            val noteText = stringResource(R.string.settings_player_button_effects_note)
            val paragraphs = noteText.split("\n")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                paragraphs.forEachIndexed { index, text ->
                    when {
                        index == 0 -> {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp)
                            )
                        }

                        text.startsWith("•") -> {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }

                        else -> {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonEffectsPreview(options: PlayerOptions) {
    val infiniteTransition = rememberInfiniteTransition(label = "ButtonEffectPreview")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Time"
    )

    // Preview Area
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(200.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val backdrop = rememberLayerBackdrop()

        // Background Patterns / Icon
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            // Draw some patterns
            Canvas(modifier = Modifier.fillMaxSize()) {
                val time = animTime * 2 * Math.PI.toFloat()

                // Base shapes with floating animation
                drawCircle(
                    color = Color.Blue.copy(alpha = 0.5f),
                    radius = 100.dp.toPx(),
                    center = Offset(
                        x = center.x - 50.dp.toPx() + 20.dp.toPx() * sin(time),
                        y = center.y + 10.dp.toPx() * cos(time)
                    )
                )
                drawCircle(
                    color = Color.Red.copy(alpha = 0.5f),
                    radius = 80.dp.toPx(),
                    center = Offset(
                        x = center.x + 40.dp.toPx() + 15.dp.toPx() * cos(time * 0.7f),
                        y = center.y + 30.dp.toPx() + 20.dp.toPx() * sin(time * 0.7f)
                    )
                )
                drawCircle(
                    color = Color.Green.copy(alpha = 0.5f),
                    radius = 60.dp.toPx(),
                    center = Offset(
                        x = center.x + 10.dp.toPx() * sin(time * 1.3f),
                        y = center.y - 40.dp.toPx() + 15.dp.toPx() * cos(time * 1.3f)
                    )
                )

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

            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        // Preview Button
        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalButtonEffectOptions provides options.buttonEffect
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    16.dp,
                    Alignment.CenterHorizontally
                )
            ) {
                PlayerIconButton(
                    onClick = {},
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
                PlayerIconButton(
                    onClick = {},
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
                PlayerIconButton(
                    onClick = {},
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.common_preview),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ButtonEffectsPagePreview() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            ButtonEffectsPage(
                options = PlayerOptions(
                    buttonEffect = ButtonEffectOptions(
                        blur = true,
                        lens = true,
                        alpha = 0.1f,
                        useTextureView = false
                    )
                ),
                contentPadding = PaddingValues(16.dp),
                onIntent = {}
            )
        }
    }
}