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

package com.fiepi.media.app.ui.screen.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fiepi.media.app.ui.theme.AppTheme


@Preview(showBackground = true)
@Composable
fun PreferencesPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            var isDarkMode by remember { mutableStateOf(true) }
            var imageQuality by remember { mutableFloatStateOf(50f) }
            var segmentedValue by rememberSaveable { mutableStateOf("Small") }
            val segmentedOptions = listOf("Small", "Medium", "Large")

            PreferencesGroup(
                title = "Appearance",
                items = arrayOf(
                    { shapes ->
                        SwitchPreferencesItem(
                            title = "Dark mode",
                            summary = "Enable dark mode for the app",
                            checked = isDarkMode,
                            onCheckedChange = { isDarkMode = it },
                            shapes = shapes,
                            icon = { Icon(Icons.Outlined.Palette, contentDescription = null) }
                        )
                    },
                    { shapes ->
                        SingleChoicePreferencesItem(
                            title = "Display Size",
                            summary = "Adjust the size of items on screen",
                            options = segmentedOptions,
                            currentValue = segmentedValue,
                            onValueChange = { segmentedValue = it },
                            shapes = shapes,
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = "Display Size"
                                )
                            }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            title = "Image Quality",
                            value = imageQuality,
                            summary = "Adjust the quality of images",
                            onValueChange = { imageQuality = it },
                            shapes = shapes,
                            onReset = { },
                            icon = { Icon(Icons.Outlined.Image, contentDescription = null) }
                        )
                    },
                    { shapes ->
                        SliderPreferencesItem(
                            title = "Disabled Slider",
                            value = 25f,
                            summary = "This slider is disabled",
                            onValueChange = {},
                            onReset = { },
                            enabled = false,
                            shapes = shapes,
                            icon = { Icon(Icons.Outlined.Image, contentDescription = null) }
                        )
                    },
                    { shapes ->
                        SwitchPreferencesItem(
                            title = "Disabled Switch",
                            summary = "This switch is disabled",
                            checked = true,
                            onCheckedChange = {},
                            enabled = false,
                            shapes = shapes
                        )
                    }
                )
            )
        }
    }
}