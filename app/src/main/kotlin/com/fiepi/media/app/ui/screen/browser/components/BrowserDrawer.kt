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

package com.fiepi.media.app.ui.screen.browser.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.PlaylistPlay
import androidx.compose.material.icons.twotone.Dns
import androidx.compose.material.icons.twotone.History
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.SourceIcon
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.app.ui.screen.browser.viewmodel.BrowserMode
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.utils.url

/**
 * A drawer sheet for selecting the active media source, using Modal style.
 */
@Composable
fun ModalBrowserDrawer(
    modifier: Modifier = Modifier,
    drawerState: DrawerState,
    availableSources: List<MediaSource>,
    currentSource: MediaSource,
    currentMode: BrowserMode,
    onSourceSelected: (MediaSource) -> Unit,
    onHistoryClick: () -> Unit,
    onPlaylistClick: () -> Unit = {},
    onManageSources: () -> Unit,
    onSettingsClick: () -> Unit,
) {

    val layoutDirection = LocalLayoutDirection.current
    val containerWidth = LocalWindowInfo.current.containerDpSize.width

    val startPaddingSize = DrawerDefaults.windowInsets
        .asPaddingValues()
        .calculateStartPadding(layoutDirection)

    val targetWidth = min(containerWidth * 0.9f, 400.dp) + startPaddingSize

    ModalDrawerSheet(
        modifier = modifier.requiredWidth(targetWidth),
        drawerState = drawerState,
        windowInsets = WindowInsets(),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BrowserDrawerContent(
            availableSources = availableSources,
            currentSource = currentSource,
            currentMode = currentMode,
            onSourceSelected = onSourceSelected,
            onHistoryClick = onHistoryClick,
            onPlaylistClick = onPlaylistClick,
            onManageSources = onManageSources,
            onSettingsClick = onSettingsClick
        )
    }
}

/**
 * A drawer sheet for selecting the active media source, using Permanent style.
 */
@Composable
fun PermanentBrowserDrawer(
    modifier: Modifier = Modifier,
    availableSources: List<MediaSource>,
    currentSource: MediaSource,
    currentMode: BrowserMode,
    onSourceSelected: (MediaSource) -> Unit,
    onHistoryClick: () -> Unit,
    onPlaylistClick: () -> Unit = {},
    onManageSources: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val containerWidth = LocalWindowInfo.current.containerDpSize.width

    val leftPaddingSize = DrawerDefaults.windowInsets
        .asPaddingValues()
        .calculateLeftPadding(layoutDirection)

    val targetWidth = min(containerWidth * 0.4f, 360.dp) + leftPaddingSize

    PermanentDrawerSheet(
        modifier = modifier.requiredWidth(targetWidth),
        windowInsets = WindowInsets(),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BrowserDrawerContent(
            availableSources = availableSources,
            currentSource = currentSource,
            currentMode = currentMode,
            onSourceSelected = onSourceSelected,
            onHistoryClick = onHistoryClick,
            onPlaylistClick = onPlaylistClick,
            onManageSources = onManageSources,
            onSettingsClick = onSettingsClick
        )
    }
}

@Composable
fun SplitPaneBrowserDrawer(
    modifier: Modifier = Modifier,
    availableSources: List<MediaSource>,
    currentSource: MediaSource,
    currentMode: BrowserMode,
    onSourceSelected: (MediaSource) -> Unit,
    onHistoryClick: () -> Unit,
    onPlaylistClick: () -> Unit = {},
    onManageSources: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        BrowserDrawerContent(
            availableSources = availableSources,
            currentSource = currentSource,
            currentMode = currentMode,
            onSourceSelected = onSourceSelected,
            onHistoryClick = onHistoryClick,
            onPlaylistClick = onPlaylistClick,
            onManageSources = onManageSources,
            onSettingsClick = onSettingsClick
        )
    }
}

/**
 * Common content for the media source picker drawer.
 */
@Composable
private fun BrowserDrawerContent(
    availableSources: List<MediaSource>,
    currentSource: MediaSource,
    currentMode: BrowserMode,
    onSourceSelected: (MediaSource) -> Unit,
    onHistoryClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onManageSources: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val insets = DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        DrawerHeader(
            onManageSources = onManageSources,
            onSettingsClick = onSettingsClick
        )

        HorizontalDivider()

        Text(
            text = stringResource(R.string.source_selection_devices_list_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .windowInsetsPadding(insets)
                .padding(top = 16.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
        )

        Column(
            modifier = Modifier
                .windowInsetsPadding(insets)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            availableSources.forEachIndexed { index, source ->
                SourcePickerItem(
                    source = source,
                    selected = currentMode == BrowserMode.Storage && currentSource.id == source.id,
                    onClick = { onSourceSelected(source) },
                    index = index,
                    count = availableSources.size
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = stringResource(R.string.drawer_library_section_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .windowInsetsPadding(insets)
                .padding(top = 8.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
        )

        Column(
            modifier = Modifier
                .windowInsetsPadding(insets)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DrawerCategoryItem(
                title = stringResource(R.string.playlist_title),
                subtitle = stringResource(R.string.playlist_subtitle),
                icon = Icons.AutoMirrored.TwoTone.PlaylistPlay,
                selected = currentMode == BrowserMode.Playlist,
                onClick = onPlaylistClick,
                index = 0,
                count = 2
            )
            DrawerCategoryItem(
                title = stringResource(R.string.playback_history_title),
                subtitle = stringResource(R.string.playback_history_subtitle),
                icon = Icons.TwoTone.History,
                selected = currentMode == BrowserMode.History,
                onClick = onHistoryClick,
                index = 1,
                count = 2
            )
        }

        Spacer(
            modifier = Modifier
                .windowInsetsPadding(DrawerDefaults.windowInsets.only(WindowInsetsSides.Bottom))
                .padding(8.dp)
        )
    }
}

/**
 * A visually appealing header for the navigation drawer.
 * Includes app branding and immersive background.
 */
@Composable
private fun DrawerHeader(
    onManageSources: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .headerBackgroundModifier()
            .windowInsetsPadding(DrawerDefaults.windowInsets.only(WindowInsetsSides.Start))
            .height(210.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.app_slogan_short),
            style = MaterialTheme.typography.labelLargeEmphasized,
            fontStyle = FontStyle.Italic,
            color = contentColor.copy(alpha = 0.7f),
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onManageSources) {
                Icon(
                    imageVector = Icons.TwoTone.Dns,
                    contentDescription = stringResource(R.string.source_selection_manage),
                    tint = contentColor
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.TwoTone.Settings,
                    contentDescription = stringResource(R.string.common_settings),
                    tint = contentColor
                )
            }
        }
    }
}

/**
 * A modifier that draws an immersive summoning magic array background for the drawer sheet,
 * inspired by the app icon's magic circle geometry.
 */
@Composable
private fun Modifier.headerBackgroundModifier(): Modifier {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val auraColor = tertiaryColor.copy(alpha = 0.12f)
    val strokeColor = contentColor.copy(alpha = 0.14f)
    val accentStrokeColor = tertiaryColor.copy(alpha = 0.22f)
    val fillColor = secondaryColor.copy(alpha = 0.08f)
    val coreColor = contentColor.copy(alpha = 0.25f)

    return this.drawBehind {
        // Background gradient covering potential overscroll and predictive back areas
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(containerColor, Color.Transparent)
            ),
            topLeft = Offset(-size.width * 5, 0f),
            size = Size(size.width * 10, size.height)
        )

        // Main summoning magic array center and scale
        val mainCenter = Offset(size.width * 0.82f, size.height * 0.48f)
        val mainRadius = size.height * 0.65f

        // Soft radial glow aura at the center of the magic array
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(auraColor, Color.Transparent),
                center = mainCenter,
                radius = mainRadius * 1.3f
            ),
            radius = mainRadius * 1.3f,
            center = mainCenter
        )

        // Outer primary track ring
        drawCircle(
            color = strokeColor,
            radius = mainRadius,
            center = mainCenter,
            style = Stroke(width = 2.dp.toPx())
        )

        // Outer fine boundary ring
        drawCircle(
            color = strokeColor.copy(alpha = strokeColor.alpha * 0.5f),
            radius = mainRadius * 1.06f,
            center = mainCenter,
            style = Stroke(width = 1.dp.toPx())
        )

        // Inner 8-direction rune notch arc ring
        val arcRadius = mainRadius * 0.85f
        val arcStrokeWidth = 2.5.dp.toPx()
        val arcAngles = listOf(
            70f to 25f,
            110f to 25f,
            160f to 25f,
            200f to 25f,
            250f to 25f,
            290f to 25f,
            340f to 25f,
            20f to 25f
        )
        val arcRect = Rect(
            left = mainCenter.x - arcRadius,
            top = mainCenter.y - arcRadius,
            right = mainCenter.x + arcRadius,
            bottom = mainCenter.y + arcRadius
        )
        for ((startAngle, sweepAngle) in arcAngles) {
            drawArc(
                color = accentStrokeColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(width = arcStrokeWidth, cap = StrokeCap.Round)
            )
        }

        // Middle track ring
        val midRadius = mainRadius * 0.68f
        drawCircle(
            color = strokeColor,
            radius = midRadius,
            center = mainCenter,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Radial tick marks surrounding the outer ring
        val tickCount = 24
        val innerTickR = mainRadius * 0.96f
        val outerTickR = mainRadius * 1.04f
        val tickPath = Path()
        for (i in 0 until tickCount) {
            val angleRad = Math.toRadians(i * 360.0 / tickCount).toFloat()
            val cosVal = cos(angleRad)
            val sinVal = sin(angleRad)
            tickPath.moveTo(mainCenter.x + innerTickR * cosVal, mainCenter.y + innerTickR * sinVal)
            tickPath.lineTo(mainCenter.x + outerTickR * cosVal, mainCenter.y + outerTickR * sinVal)
        }
        drawPath(
            path = tickPath,
            color = strokeColor.copy(alpha = strokeColor.alpha * 0.7f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Cardinal magic starburst points
        val starPath = Path()
        val starDistance = mainRadius * 1.0f
        val starLength = 24.dp.toPx()
        val starWidth = 8.dp.toPx()
        val angles = listOf(0f, 90f, 180f, 270f)
        for (angleDeg in angles) {
            val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val cosVal = cos(rad)
            val sinVal = sin(rad)
            val perpCos = -sinVal

            val basePos = Offset(mainCenter.x + starDistance * cosVal, mainCenter.y + starDistance * sinVal)
            val tipOuter = Offset(basePos.x + starLength * cosVal, basePos.y + starLength * sinVal)
            val tipInner = Offset(basePos.x - starLength * 0.5f * cosVal, basePos.y - starLength * 0.5f * sinVal)
            val side1 = Offset(basePos.x + starWidth * perpCos, basePos.y + starWidth * cosVal)
            val side2 = Offset(basePos.x - starWidth * perpCos, basePos.y - starWidth * cosVal)

            starPath.moveTo(tipOuter.x, tipOuter.y)
            starPath.lineTo(side1.x, side1.y)
            starPath.lineTo(tipInner.x, tipInner.y)
            starPath.lineTo(side2.x, side2.y)
            starPath.close()
        }
        drawPath(
            path = starPath,
            color = accentStrokeColor
        )

        // Interlocking geometric auxiliary construction triangle pointing left
        val auxRadius = mainRadius * 0.58f
        val auxTrianglePath = Path()
        val auxAngles = listOf(180f, 60f, 300f)
        for ((idx, a) in auxAngles.withIndex()) {
            val rad = Math.toRadians(a.toDouble()).toFloat()
            val pt = Offset(mainCenter.x + auxRadius * cos(rad), mainCenter.y + auxRadius * sin(rad))
            if (idx == 0) auxTrianglePath.moveTo(pt.x, pt.y) else auxTrianglePath.lineTo(pt.x, pt.y)
        }
        auxTrianglePath.close()
        drawPath(
            path = auxTrianglePath,
            color = strokeColor,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Core deconstructed play triangle
        val playRadius = mainRadius * 0.52f
        val playTrianglePath = Path()
        val playAngles = listOf(0f, 120f, 240f)
        for ((idx, a) in playAngles.withIndex()) {
            val rad = Math.toRadians(a.toDouble()).toFloat()
            val pt = Offset(mainCenter.x + playRadius * cos(rad), mainCenter.y + playRadius * sin(rad))
            if (idx == 0) playTrianglePath.moveTo(pt.x, pt.y) else playTrianglePath.lineTo(pt.x, pt.y)
        }
        playTrianglePath.close()
        drawPath(
            path = playTrianglePath,
            color = fillColor
        )
        drawPath(
            path = playTrianglePath,
            color = accentStrokeColor,
            style = Stroke(width = 2.dp.toPx())
        )

        // Core highlight kernel
        val coreRadius = playRadius * 0.45f
        val coreTrianglePath = Path()
        for ((idx, a) in playAngles.withIndex()) {
            val rad = Math.toRadians(a.toDouble()).toFloat()
            val pt = Offset(mainCenter.x + coreRadius * cos(rad), mainCenter.y + coreRadius * sin(rad))
            if (idx == 0) coreTrianglePath.moveTo(pt.x, pt.y) else coreTrianglePath.lineTo(pt.x, pt.y)
        }
        coreTrianglePath.close()
        drawPath(
            path = coreTrianglePath,
            color = coreColor
        )

        // Secondary orbiting magic circle in top-left background
        val secCenter = Offset(size.width * 0.12f, size.height * 0.20f)
        val secRadius = size.height * 0.25f
        drawCircle(
            color = strokeColor.copy(alpha = strokeColor.alpha * 0.5f),
            radius = secRadius,
            center = secCenter,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = strokeColor.copy(alpha = strokeColor.alpha * 0.3f),
            radius = secRadius * 0.7f,
            center = secCenter,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
private fun SourcePickerItem(
    source: MediaSource,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val subtitle = when (source) {
        is MediaSource.Local, is MediaSource.External -> {
            val total = source.totalSpace
            val free = source.freeSpace
            if (total != null && free != null && total > 0) {
                val totalStr = Formatter.formatShortFileSize(context, total)
                val freeStr = Formatter.formatShortFileSize(context, free)
                stringResource(R.string.source_storage_usage, freeStr, totalStr)
            } else {
                source.rootPath
            }
        }

        is MediaSource.Remote -> {
            val total = source.totalSpace
            val free = source.freeSpace
            if (total != null && free != null && total > 0) {
                val totalStr = Formatter.formatShortFileSize(context, total)
                val freeStr = Formatter.formatShortFileSize(context, free)
                stringResource(R.string.source_storage_usage, freeStr, totalStr)
            } else {
                source.source.url
            }
        }
    }

    val progress = remember(source) {
        val total = source.totalSpace
        val free = source.freeSpace
        if (total != null && free != null && total > 0) {
            val used = total - free
            used.toFloat() / total.toFloat()
        } else 0f
    }

    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val colors = ListItemDefaults.segmentedColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLeadingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        containerColor = MaterialTheme.colorScheme.surfaceBright
    )

    SegmentedListItem(
        selected = selected,
        onClick = rememberHapticClickHandler(onClick),
        shapes = shapes,
        colors = colors,
        modifier = modifier,
        leadingContent = {
            SourceIcon(
                source = source,
                progress = progress,
                selected = selected
            )
        },
        content = {
            Column {
                Text(
                    text = source.name,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        contentPadding = PaddingValues(12.dp)
    )
}

/**
 * A generic navigation item for drawer category sections (e.g. Playback History, Playlists).
 */
@Composable
private fun DrawerCategoryItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val colors = ListItemDefaults.segmentedColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        selectedLeadingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        containerColor = MaterialTheme.colorScheme.surfaceBright
    )

    SegmentedListItem(
        selected = selected,
        onClick = rememberHapticClickHandler(onClick),
        shapes = shapes,
        colors = colors,
        modifier = modifier,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        },
        content = {
            Column {
                Text(
                    text = title,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailingContent = if (badgeText != null) {
            {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        } else null,
        contentPadding = PaddingValues(12.dp)
    )
}


@Preview(device = Devices.PIXEL_4, showBackground = true)
@Composable
fun ModalBrowserDrawerPreview() {
    val mockSources = listOf(
        MediaSource.Local(
            MediaSource.INTERNAL_STORAGE_ID,
            "Internal Storage",
            "/root",
            totalSpace = 128L * 1024 * 1024 * 1024,
            freeSpace = 50L * 1024 * 1024 * 1024
        ),
        MediaSource.Remote(
            RemoteSource(
                id = "id1",
                name = "Share",
                type = SourceType.Smb,
                host = "192.168.0.1",
                path = "share"
            )
        )
    )
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            ModalBrowserDrawer(
                drawerState = rememberDrawerState(DrawerValue.Open),
                availableSources = mockSources,
                currentSource = mockSources.first(),
                currentMode = BrowserMode.Storage,
                onSourceSelected = {},
                onHistoryClick = {},
                onManageSources = {},
                onSettingsClick = {}
            )
        }
    }
}

@Preview(device = Devices.TABLET, showBackground = true)
@Composable
fun PermanentBrowserDrawerPreview() {
    val mockSources = listOf(
        MediaSource.Local(
            MediaSource.INTERNAL_STORAGE_ID,
            "Internal Storage",
            "/root",
            totalSpace = 128L * 1024 * 1024 * 1024,
            freeSpace = 50L * 1024 * 1024 * 1024
        ),
        MediaSource.Remote(
            RemoteSource(
                id = "id1",
                name = "Share",
                type = SourceType.Smb,
                host = "192.168.0.1",
                path = "share"
            )
        )
    )
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            PermanentBrowserDrawer(
                availableSources = mockSources,
                currentSource = mockSources.first(),
                currentMode = BrowserMode.Storage,
                onSourceSelected = {},
                onHistoryClick = {},
                onManageSources = {},
                onSettingsClick = {}
            )
        }
    }
}
