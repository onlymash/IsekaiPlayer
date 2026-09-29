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

package com.fiepi.media.app.ui.screen.settings.pages.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.StyledDropdownMenu
import com.fiepi.media.app.ui.components.StyledDropdownMenuItem
import com.fiepi.media.app.ui.screen.settings.components.PreferencesLabel
import com.fiepi.media.domain.model.preferences.AudioOptions


@Composable
fun AudioOutputOrderPage(
    options: AudioOptions,
    onIntent: (AudioPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    val backends = options.audioOutputBackends

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        state = scrollState
    ) {
        item {
            PreferencesLabel(stringResource(R.string.settings_audio_output_order_summary))
        }

        itemsIndexed(backends, key = { _, it -> it }) { index, backend ->
            val shapes = ListItemDefaults.segmentedShapes(
                index = index,
                count = backends.size
            )
            var menuExpanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SegmentedListItem(
                    selected = false,
                    onClick = { },
                    shapes = shapes,
                    verticalAlignment = Alignment.CenterVertically,
                    supportingContent = {
                        Text(
                            when (backend) {
                                "aaudio" -> stringResource(R.string.settings_audio_output_aaudio_summary)
                                "audiotrack" -> stringResource(R.string.settings_audio_output_audiotrack_summary)
                                "opensles" -> stringResource(R.string.settings_audio_output_opensles_summary)
                                else -> ""
                            }
                        )
                    },
                    leadingContent = {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = null)
                            }
                            StyledDropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                StyledDropdownMenuItem(
                                    text = stringResource(R.string.common_move_up),
                                    leadingIcon = Icons.Outlined.ArrowUpward,
                                    enabled = index > 0,
                                    onClick = {
                                        menuExpanded = false
                                        val newList = backends.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index - 1, item)
                                        onIntent(AudioPrefsIntent.UpdateAudioOutputBackends(newList))
                                    }
                                )
                                StyledDropdownMenuItem(
                                    text = stringResource(R.string.common_move_down),
                                    leadingIcon = Icons.Outlined.ArrowDownward,
                                    enabled = index < backends.size - 1,
                                    onClick = {
                                        menuExpanded = false
                                        val newList = backends.toMutableList()
                                        val item = newList.removeAt(index)
                                        newList.add(index + 1, item)
                                        onIntent(AudioPrefsIntent.UpdateAudioOutputBackends(newList))
                                    }
                                )
                            }
                        }
                    },
                    colors = ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    )
                ) {
                    Text(
                        when (backend) {
                            "aaudio" -> "AAudio"
                            "audiotrack" -> "AudioTrack"
                            "opensles" -> "OpenSL ES"
                            else -> ""
                        }
                    )
                }
            }
        }
    }
}
