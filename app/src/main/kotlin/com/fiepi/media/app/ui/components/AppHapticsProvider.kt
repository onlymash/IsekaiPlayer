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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.fiepi.media.app.ui.hooks.AppHaptics
import com.fiepi.media.app.ui.hooks.LocalAppHaptics
import com.fiepi.media.domain.usecase.preferences.GetHapticsEnabledUseCase
import org.koin.compose.koinInject

@Composable
fun AppHapticsProvider(
    getHapticsEnabled: GetHapticsEnabledUseCase = koinInject(),
    content: @Composable () -> Unit
) {
    val systemHapticFeedback = LocalHapticFeedback.current
    val isHapticsEnabled = getHapticsEnabled().collectAsState(true)
    val appHaptics = remember(systemHapticFeedback, isHapticsEnabled) {
        object : AppHaptics {
            override fun performClick() {
                if (isHapticsEnabled.value) {
                    systemHapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                }
            }

            override fun performLongClick() {
                if (isHapticsEnabled.value) {
                    systemHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            override fun performTick() {
                if (isHapticsEnabled.value) {
                    systemHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            }

            override fun performFrequentTick() {
                if (isHapticsEnabled.value) {
                    systemHapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
            }
        }
    }
    CompositionLocalProvider(LocalAppHaptics provides appHaptics) {
        content()
    }
}