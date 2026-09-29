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

import android.graphics.Typeface
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.fiepi.media.domain.model.preferences.ExoSubtitleOptions
import com.fiepi.media.domain.utils.FontScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Pure Jetpack Compose subtitle overlay for ExoPlayer.
 * Renders subtitle text cues over the video container with outline stroke and background.
 */
@Composable
fun ExoSubtitleOverlay(
    modifier: Modifier = Modifier,
    text: String?,
    options: ExoSubtitleOptions = ExoSubtitleOptions(),
) {
    val parsedTextColor = try {
        Color(options.textColorHex.toColorInt())
    } catch (_: Exception) {
        Color.White
    }

    val parsedOutlineColor = try {
        Color(options.outlineColorHex.toColorInt())
    } catch (_: Exception) {
        Color.Black
    }

    val parsedBgColor = try {
        val baseColor = options.bgColorHex.toColorInt()
        Color(baseColor).copy(alpha = options.bgOpacity.coerceIn(0f, 1f))
    } catch (_: Exception) {
        Color.Black.copy(alpha = options.bgOpacity.coerceIn(0f, 1f))
    }

    val parsedFontWeight = FontWeight(options.fontWeight.coerceIn(100, 900))

    val parsedFontFamily by produceState<FontFamily?>(
        initialValue = null,
        options.fontFamily,
        options.fontsDir
    ) {
        value = if (options.fontFamily.isBlank() || options.fontFamily == "sans-serif") {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val targetFontsDir = options.fontsDir.ifBlank {
                        File(Environment.getExternalStorageDirectory(), "Fonts").absolutePath
                    }
                    val matched = FontScanner.scanFontFamilies(targetFontsDir)
                        .firstOrNull { it.familyName == options.fontFamily }
                    if (matched != null) {
                        val file = File(matched.filePath)
                        if (file.exists() && file.canRead()) {
                            FontFamily(Typeface.createFromFile(file))
                        } else null
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    if (text.isNullOrBlank()) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = options.bottomPaddingDp.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        OutlineText(
            text = text,
            style = TextStyle(
                color = parsedTextColor,
                fontSize = options.fontSizeSp.sp,
                fontWeight = parsedFontWeight,
                fontFamily = parsedFontFamily,
                textAlign = TextAlign.Center
            ),
            outlineColor = parsedOutlineColor,
            outlineWidth = options.outlineWidthDp.coerceAtLeast(0).dp,
            modifier = Modifier
                .background(
                    color = parsedBgColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Text component that renders text with a stroke outline around it.
 * Layering a stroke-styled text beneath a fill-styled text creates an outline effect.
 */
@Composable
fun OutlineText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    outlineColor: Color = Color.Black,
    outlineWidth: Dp = 2.dp,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { outlineWidth.toPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Stroke outline layer
        if (strokeWidthPx > 0f && outlineColor != Color.Transparent && outlineColor.alpha > 0f) {
            BasicText(
                text = text,
                style = style.copy(
                    color = outlineColor,
                    textAlign = textAlign ?: style.textAlign,
                    drawStyle = Stroke(
                        width = strokeWidthPx,
                        join = StrokeJoin.Round
                    )
                ),
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines
            )
        }

        // Fill text layer
        BasicText(
            text = text,
            style = style.copy(
                textAlign = textAlign ?: style.textAlign,
                drawStyle = Fill
            ),
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines
        )
    }
}

@Preview(
    name = "ExoSubtitleOverlay Preview",
    heightDp = 480,
    widthDp = 600
)
@Composable
private fun ExoSubtitleOverlayPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCCCCCC))
    ) {
        ExoSubtitleOverlay(
            text = "This is a sample subtitle with outline text effect.",
            options = ExoSubtitleOptions(
                fontSizeSp = 20,
                textColorHex = "#FFFFFF",
                bgColorHex = "#000000",
                bgOpacity = 0f,
                bottomPaddingDp = 24
            )
        )
    }
}
