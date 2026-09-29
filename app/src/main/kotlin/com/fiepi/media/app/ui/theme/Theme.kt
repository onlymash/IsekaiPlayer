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

package com.fiepi.media.app.ui.theme

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import com.fiepi.media.app.ui.utils.rememberSystemBarsController
import com.fiepi.media.domain.model.preferences.ThemeMode
import com.fiepi.media.domain.model.preferences.ThemeOptions

interface EnvironmentState {
    val isDarkTheme: Boolean
}

private class AndroidEnvironmentState(override val isDarkTheme: Boolean) : EnvironmentState

val LocalEnvironmentState = staticCompositionLocalOf<EnvironmentState> {
    error("No EnvironmentState provided")
}

object AppEnvironment {
    val current: EnvironmentState
        @Composable
        @ReadOnlyComposable
        get() = LocalEnvironmentState.current
}

@Composable
fun AppTheme(
    themeOptions: ThemeOptions = ThemeOptions(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemBarsController = rememberSystemBarsController()

    val systemIsDark = isSystemInDarkTheme()

    val useDarkTheme = remember(themeOptions.themeMode, systemIsDark) {
        when (themeOptions.themeMode) {
            ThemeMode.System -> systemIsDark
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
    }

    DisposableEffect(useDarkTheme) {
        systemBarsController.setLight(
            isLightStatusBar = !useDarkTheme,
            isLightNavBar = !useDarkTheme
        )
        onDispose { }
    }

    val environmentState = remember(useDarkTheme) { AndroidEnvironmentState(useDarkTheme) }

    val colorScheme = if (useDarkTheme) {
        val baseScheme = dynamicDarkColorScheme(context)
        if (themeOptions.highContrastDarkThemeEnabled) {
            baseScheme.toDynamicHighContrastDark()
        } else {
            baseScheme
        }
    } else {
        dynamicLightColorScheme(context)
    }

    val window = LocalActivity.current?.window

    if (window != null) {
        val backgroundColor = remember(colorScheme.background) { colorScheme.background.toArgb() }
        DisposableEffect(backgroundColor) {
            window.setBackgroundDrawable(backgroundColor.toDrawable())
            onDispose { }
        }
    }

    CompositionLocalProvider(LocalEnvironmentState provides environmentState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun ColorScheme.toDynamicHighContrastDark(): ColorScheme {
    // Container darkening factor in dynamic theme (closer to 0 is darker)
    val containerDarkenFactor = 0.4f

    return this.copy(
        // Drop background to pure black for maximum contrast
        background = Color.Black,
        surface = Color(0xFF080808),

        // Dynamically darken core containers (preserve wallpaper hue while reducing brightness)
        primaryContainer = this.primaryContainer.darken(containerDarkenFactor),
        secondaryContainer = this.secondaryContainer.darken(containerDarkenFactor),
        tertiaryContainer = this.tertiaryContainer.darken(containerDarkenFactor),
        errorContainer = this.errorContainer.darken(containerDarkenFactor),

        // Dynamically darken Surface containers across different elevation tiers
        surfaceContainerLowest = this.surfaceContainerLowest.darken(0.2f),
        surfaceContainerLow = this.surfaceContainerLow.darken(0.4f),
        surfaceContainer = this.surfaceContainer.darken(0.5f),
        surfaceContainerHigh = this.surfaceContainerHigh.darken(0.6f),
        surfaceContainerHighest = this.surfaceContainerHighest.darken(0.7f),
        surfaceBright = this.surfaceBright.darken(0.7f),
        surfaceVariant = this.surfaceVariant.darken(0.7f)
    )
}

private fun Color.darken(factor: Float): Color {
    return Color(
        red = (this.red * factor).coerceIn(0f, 1f),
        green = (this.green * factor).coerceIn(0f, 1f),
        blue = (this.blue * factor).coerceIn(0f, 1f),
        alpha = this.alpha
    )
}