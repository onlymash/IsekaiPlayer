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

package com.fiepi.media.app.ui.hooks

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Abstracts haptic feedback behaviors.
 */
interface AppHaptics {
    fun performClick()
    fun performLongClick()
    fun performTick()
    fun performFrequentTick()
}

/**
 * A CompositionLocal used to pass AppHaptics instance down Composable tree.
 */
val LocalAppHaptics = staticCompositionLocalOf<AppHaptics> {
    // Provides a default empty implementation to prevent crashes when not provided
    object : AppHaptics {
        override fun performClick() {}
        override fun performLongClick() {}
        override fun performTick() {}
        override fun performFrequentTick() {}
    }
}

/**
 * A custom Modifier that wraps clickable and automatically adds haptic feedback.
 */
@Composable
fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val appHaptics = LocalAppHaptics.current
    return this.then(
        Modifier.clickable(
            enabled = enabled,
            onClick = {
                appHaptics.performClick()
                onClick()
            }
        )
    )
}


/**
 * A Composable function returning a new click event lambda that executes haptic feedback before calling original click event.
 * Suitable for components with their own onClick parameter (Button, Card, ListItem, etc.)
 *
 * @param onClick Your original click event lambda.
 * @return A new lambda containing haptic feedback logic.
 */
@Composable
fun rememberHapticClickHandler(onClick: () -> Unit): () -> Unit {
    val appHaptics = LocalAppHaptics.current
    // Use remember to ensure returned lambda instance is stable
    return remember(appHaptics, onClick) {
        {
            appHaptics.performClick()
            onClick()
        }
    }
}