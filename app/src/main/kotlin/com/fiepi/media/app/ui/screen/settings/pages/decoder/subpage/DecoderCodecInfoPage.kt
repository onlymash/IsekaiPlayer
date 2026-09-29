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

package com.fiepi.media.app.ui.screen.settings.pages.decoder.subpage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.screen.settings.components.PreferencesLabel
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.domain.model.decoder.CodecInfo
import org.koin.compose.viewmodel.koinViewModel

enum class CodecInfoGroup(val labelResId: Int) {
    ALL(R.string.settings_decoder_codec_group_all),
    VIDEO(R.string.settings_decoder_codec_group_video),
    AUDIO(R.string.settings_decoder_codec_group_audio),
    HARDWARE(R.string.settings_decoder_codec_group_hardware)
}

val codecInfoGroupSaver = Saver<MutableState<CodecInfoGroup>, Int>(
    save = { info ->
        info.value.ordinal
    },
    restore = { ordinal ->
        mutableStateOf(CodecInfoGroup.entries[ordinal])
    }
)

@Composable
fun DecoderCodecInfoPage(
    contentPadding: PaddingValues,
    viewModel: DecoderCodecInfoViewModel = koinViewModel(),
    scrollState: LazyListState = rememberLazyListState()
) {
    val codecs by viewModel.codecs.collectAsState()
    var selectedGroup by rememberSaveable(saver = codecInfoGroupSaver) {
        mutableStateOf(
            CodecInfoGroup.ALL
        )
    }

    DecoderCodecInfoPageContent(
        codecs = codecs,
        selectedGroup = selectedGroup,
        onGroupSelected = { selectedGroup = it },
        contentPadding = contentPadding,
        scrollState = scrollState
    )
}

@Composable
fun DecoderCodecInfoPageContent(
    codecs: List<CodecInfo>,
    selectedGroup: CodecInfoGroup,
    onGroupSelected: (CodecInfoGroup) -> Unit,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState()
) {
    LaunchedEffect(selectedGroup) {
        scrollState.scrollToItem(0)
    }

    val filteredDecoders = remember(selectedGroup, codecs) {
        when (selectedGroup) {
            CodecInfoGroup.ALL -> codecs
            CodecInfoGroup.VIDEO -> codecs.filter { it.isVideo }
            CodecInfoGroup.AUDIO -> codecs.filter { it.isAudio }
            CodecInfoGroup.HARDWARE -> codecs.filter { it.isHardware }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        state = scrollState
    ) {
        item(key = "codec_flow_row_group") {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CodecInfoGroup.entries.forEachIndexed { index, group ->
                    val isSelected = selectedGroup == group
                    OutlinedToggleButton(
                        checked = isSelected,
                        onCheckedChange = { if (!isSelected) onGroupSelected(group) },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            CodecInfoGroup.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = OutlinedToggleButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    ) {
                        Text(text = stringResource(group.labelResId))
                    }
                }
            }
        }

        item(key = "codec_count_label") {
            PreferencesLabel(
                title = stringResource(
                    R.string.settings_decoder_codec_count,
                    filteredDecoders.size
                )
            )
        }

        if (filteredDecoders.isEmpty()) {
            item(key = "codec_empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.settings_decoder_codec_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        itemsIndexed(
            items = filteredDecoders,
            key = { index, item -> "${selectedGroup.name}_${item.name}_$index" }
        ) { index, decoder ->
            val shapes = ListItemDefaults.segmentedShapes(
                index = index,
                count = filteredDecoders.size
            )

            Surface(
                shape = shapes.shape,
                color = MaterialTheme.colorScheme.surfaceBright,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = decoder.shortFormatName.ifEmpty { decoder.name },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        val isHw = decoder.isHardware
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (isHw) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (isHw) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ) {
                            Text(
                                text = if (isHw) {
                                    stringResource(R.string.settings_decoder_hw_decoding)
                                } else {
                                    stringResource(R.string.settings_decoder_sw_decoding)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (decoder.shortFormatName.isNotEmpty()) {
                        Text(
                            text = decoder.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = decoder.mimeTypes.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        val maxRes = decoder.maxResolution
                        if (decoder.isVideo && decoder.isHardware && maxRes != null) {
                            Text(
                                text = maxRes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DecoderCodecInfoPagePreview() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            DecoderCodecInfoPageContent(
                codecs = listOf(
                    CodecInfo(
                        name = "c2.android.avc.decoder",
                        canonicalName = "c2.android.avc.decoder",
                        shortFormatName = "H.264 / AVC",
                        maxResolution = "3840x2160",
                        isHardware = true,
                        isSoftwareOnly = false,
                        isVendor = true,
                        mimeTypes = listOf("video/avc"),
                        isVideo = true,
                        isAudio = false
                    ),
                    CodecInfo(
                        name = "c2.android.mp3.decoder",
                        canonicalName = "c2.android.mp3.decoder",
                        shortFormatName = "MP3",
                        maxResolution = null,
                        isHardware = false,
                        isSoftwareOnly = true,
                        isVendor = false,
                        mimeTypes = listOf("audio/mpeg"),
                        isVideo = false,
                        isAudio = true
                    ),
                    CodecInfo(
                        name = "c2.android.hevc.decoder",
                        canonicalName = "c2.android.hevc.decoder",
                        shortFormatName = "H.265 / HEVC",
                        maxResolution = "7680x4320",
                        isHardware = true,
                        isSoftwareOnly = false,
                        isVendor = true,
                        mimeTypes = listOf("video/hevc"),
                        isVideo = true,
                        isAudio = false
                    )
                ),
                selectedGroup = CodecInfoGroup.ALL,
                onGroupSelected = {},
                contentPadding = PaddingValues(16.dp)
            )
        }
    }
}
