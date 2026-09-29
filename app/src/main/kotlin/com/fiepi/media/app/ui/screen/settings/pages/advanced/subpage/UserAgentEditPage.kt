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

package com.fiepi.media.app.ui.screen.settings.pages.advanced.subpage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import com.fiepi.media.app.ui.hooks.rememberHapticClickHandler
import com.fiepi.media.app.ui.screen.settings.components.PreferencesLabel
import com.fiepi.media.app.ui.screen.settings.pages.advanced.AdvancedPrefsIntent
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.preferences.AdvancedOptions


@Composable
fun UserAgentEditPage(
    options: AdvancedOptions,
    onIntent: (AdvancedPrefsIntent) -> Unit,
    contentPadding: PaddingValues,
    scrollState: LazyListState = rememberLazyListState(),
    onBack: () -> Unit = {}
) {
    var text by remember(options.userAgent) { mutableStateOf(options.userAgent) }
    var isError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        state = scrollState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {
            PreferencesLabel(
                title = stringResource(R.string.settings_advanced_user_agent),
                horizontalPadding = 16.dp
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = text,
                onValueChange = {
                    text = it
                    isError = false
                },
                minLines = 3,
                isError = isError,
                supportingText = if (isError) {
                    { Text(stringResource(R.string.settings_advanced_user_agent_empty_error)) }
                } else null
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalIconButton(
                    onClick = rememberHapticClickHandler {
                        text = AppConstants.DEFAULT_USER_AGENT
                    },
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.RestartAlt,
                        contentDescription = stringResource(R.string.common_reset)
                    )
                }

                Button(
                    onClick = rememberHapticClickHandler {
                        if (text.isBlank()) {
                            isError = true
                        } else {
                            onIntent(AdvancedPrefsIntent.UpdateUserAgent(text))
                            onBack()
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Save,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MinHeight)),
                    )
                    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MinHeight)))
                    Text(stringResource(R.string.source_editor_save))
                }
            }
        }
    }
}