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

package com.fiepi.media.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.ui.utils.formatDuration

@Composable
fun BrowserItem(
    modifier: Modifier = Modifier,
    title: Any,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    leadingContent: (@Composable () -> Unit)? = null,
    tailingContent: (@Composable () -> Unit)? = null,
    isHighlight: Boolean = false,
    supportingContent: (@Composable () -> Unit)? = null,
    tags: @Composable (RowScope.() -> Unit) = {},
    contentPadding: PaddingValues = PaddingValues(
        start = 8.dp,
        end = 4.dp,
        top = 8.dp,
        bottom = 8.dp
    )
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
    } else {
        Color.Transparent
    }
    val contentAlpha = if (enabled) 1f else 0.38f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .alpha(contentAlpha)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = containerColor,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Thumbnail/Icon
            if (leadingContent != null) {
                leadingContent()
                Spacer(modifier = Modifier.width(16.dp))
            }
            // Center: Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                val titleText = title as? AnnotatedString ?: AnnotatedString(title.toString())
                Text(
                    text = titleText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isHighlight) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    fontWeight = FontWeight.Medium
                )

                supportingContent?.invoke()

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = tags
                )
            }
            // Right: Tailing Content
            if (tailingContent != null) {
                Spacer(modifier = Modifier.width(8.dp))
                tailingContent()
            }
        }
    }
}

/**
 * Creates and remembers a reusable linear gradient shimmer brush for skeleton loading placeholders.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)

    val shimmerColors = listOf(
        baseColor,
        highlightColor,
        baseColor
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}

/**
 * A reusable shimmer-animated skeleton placeholder matching the layout of [BrowserItem].
 *
 * @param modifier Layout modifier applied as the first parameter.
 * @param brush Shimmer gradient brush. Defaults to [rememberShimmerBrush].
 * @param contentPadding Padding applied inside the item container.
 */
@Composable
fun BrowserItemPlaceholder(
    modifier: Modifier = Modifier,
    brush: Brush = rememberShimmerBrush(),
    contentPadding: PaddingValues = PaddingValues(
        start = 8.dp,
        end = 4.dp,
        top = 8.dp,
        bottom = 8.dp
    )
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail skeleton placeholder (80x60dp)
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 60.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(brush)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            // Title skeleton placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )

            // Tags skeleton placeholders
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun DurationTag(
    duration: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .background(
                Color.Black.copy(alpha = 0.5f),
                MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = formatDuration(duration),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
fun MetadataTag(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProgressMetadataTag(
    text: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.extraSmall
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    val progressColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier
            .clip(shape)
            .drawWithContent {
                drawRect(color = trackColor)
                if (progress > 0f) {
                    drawRect(
                        color = progressColor,
                        size = size.copy(width = size.width * progress.coerceIn(0f, 1f))
                    )
                }
                drawContent()
            }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
