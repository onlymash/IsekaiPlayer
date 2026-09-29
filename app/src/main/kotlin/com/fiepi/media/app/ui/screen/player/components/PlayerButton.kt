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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.preferences.ButtonBorderStyle
import com.fiepi.media.domain.model.preferences.ButtonEffectOptions
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

val LocalBackdrop = staticCompositionLocalOf { emptyBackdrop() }
val LocalButtonEffectOptions = staticCompositionLocalOf { ButtonEffectOptions() }

@Composable
internal fun Modifier.playerButtonSurface(
    interactionSource: MutableInteractionSource,
    shape: Shape,
    scaleTarget: Float = 1.1f
): Modifier {
    val backdrop = LocalBackdrop.current
    val options = LocalButtonEffectOptions.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleTarget else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    return this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (options.blur || options.lens) {
                    vibrancy()
                }
                if (options.blur) {
                    blur(4f.dp.toPx())
                }
                if (options.lens) {
                    lens(16f.dp.toPx(), 32f.dp.toPx())
                }
            },
            highlight = {
                Highlight(
                    width = if (options.showBorder) 0.5.dp else 0.dp,
                    style = when (options.borderStyle) {
                        ButtonBorderStyle.Directional -> HighlightStyle.Default
                        ButtonBorderStyle.Ambient -> HighlightStyle.Ambient
                        ButtonBorderStyle.Plain -> HighlightStyle.Plain
                    }
                )
            },
            shadow = { Shadow() },
            innerShadow = { InnerShadow.Default },
            onDrawSurface = { drawRect(Color.Black.copy(alpha = options.alpha)) }
        )
}

@Composable
fun PlayerIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.playerButtonSurface(interactionSource, CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color.White
        ),
        content = content
    )
}

@Composable
fun PlayerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.playerButtonSurface(interactionSource, ButtonDefaults.shape),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
        ),
        content = content
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayerIconButtonPreview() {
    AppTheme {
        CompositionLocalProvider(
            LocalBackdrop provides emptyBackdrop(),
            LocalButtonEffectOptions provides ButtonEffectOptions()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerIconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                }
                PlayerIconButton(onClick = {}, enabled = false) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayerButtonPreview() {
    AppTheme {
        CompositionLocalProvider(
            LocalBackdrop provides emptyBackdrop(),
            LocalButtonEffectOptions provides ButtonEffectOptions()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerButton(onClick = {}) {
                    Text(
                        text = "HW+",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                PlayerButton(onClick = {}, enabled = false) {
                    Text(
                        text = "SW",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
