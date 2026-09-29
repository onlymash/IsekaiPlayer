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

package com.fiepi.media.app.ui.utils

import android.view.View
import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

interface SystemBarsController {
    fun hide()
    fun show()
    fun setLight(isLightStatusBar: Boolean, isLightNavBar: Boolean)
}

@Composable
fun rememberSystemBarsController(): SystemBarsController {
    val activity = LocalActivity.current
    val window = activity?.window

    return remember(window) {
        if (window != null) {
            AndroidSystemBarsController(window, window.decorView)
        } else {
            object : SystemBarsController {
                override fun hide() {}
                override fun show() {}
                override fun setLight(isLightStatusBar: Boolean, isLightNavBar: Boolean) {}
            }
        }
    }
}

private class AndroidSystemBarsController(
    window: Window,
    view: View
) : SystemBarsController {

    private val insetsController = WindowInsetsControllerCompat(window, view)

    override fun hide() {
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun show() {
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun setLight(isLightStatusBar: Boolean, isLightNavBar: Boolean) {
        insetsController.isAppearanceLightStatusBars = isLightStatusBar
        insetsController.isAppearanceLightNavigationBars = isLightNavBar
    }
}
