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

package com.fiepi.media.app.ui.screen.source

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fiepi.media.app.R
import com.fiepi.media.app.ui.components.BackIconButtonBox
import com.fiepi.media.app.ui.screen.source.viewmodel.SourceEditorIntent
import com.fiepi.media.app.ui.screen.source.viewmodel.SourceEditorState
import com.fiepi.media.app.ui.screen.source.viewmodel.SourceEditorViewModel
import com.fiepi.media.app.ui.theme.AppTheme
import com.fiepi.media.app.ui.utils.displayName
import com.fiepi.media.domain.model.source.SourceType
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.UUID

@Composable
fun SourceEditorScreen(
    sourceId: String?,
    onNavigateUp: () -> Unit,
) {
    // We use a session key to ensure a fresh ViewModel if needed, 
    // but for a dedicated screen, standard ViewModel lifecycle is usually fine.
    // However, to keep it consistent with previous logic:
    val sessionKey = remember { UUID.randomUUID().toString() }
    val viewModel: SourceEditorViewModel =
        koinViewModel(key = sessionKey) { parametersOf(sourceId) }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateUp()
        }
    }

    SourceEditorContent(
        state = state,
        onIntent = viewModel::onIntent,
        onNavigateUp = onNavigateUp
    )
}

@Composable
private fun SourceEditorContent(
    state: SourceEditorState,
    onIntent: (SourceEditorIntent) -> Unit,
    onNavigateUp: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val passwordState = rememberTextFieldState(initialText = state.password)

    LaunchedEffect(state.password) {
        if (passwordState.text.toString() != state.password) {
            passwordState.setTextAndPlaceCursorAtEnd(state.password)
        }
    }

    LaunchedEffect(passwordState) {
        snapshotFlow { passwordState.text.toString() }
            .collect { onIntent(SourceEditorIntent.UpdatePassword(it)) }
    }

    val context = LocalContext.current
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onIntent(SourceEditorIntent.Save)
        }
    }

    val onSaveClick = {
        val host = state.host.trim()
        val isTargetSdk37OrHigher = context.applicationInfo.targetSdkVersion >= 37
        val needsLocalNetworkPermission = Build.VERSION.SDK_INT >= 37 &&
                isTargetSdk37OrHigher &&
                isLocalNetworkHost(host)

        if (needsLocalNetworkPermission) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            } else {
                onIntent(SourceEditorIntent.Save)
            }
        } else {
            onIntent(SourceEditorIntent.Save)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = if (state.id == null) {
                            stringResource(R.string.source_editor_title_add)
                        } else {
                            stringResource(R.string.source_editor_title_edit)
                        }
                    )
                },
                navigationIcon = {
                    BackIconButtonBox(onClick = onNavigateUp)
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onSaveClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(stringResource(R.string.source_editor_save))
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { contentPadding ->
        val contentShape = MaterialTheme.shapes.large.copy(
            bottomStart = CornerSize(0f),
            bottomEnd = CornerSize(0f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .clip(contentShape)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = contentShape
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onIntent(SourceEditorIntent.UpdateName(it)) },
                    label = { Text(stringResource(R.string.source_editor_label_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.source_editor_label_protocol),
                    style = MaterialTheme.typography.labelLarge
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val sourceTypes =
                        listOf(SourceType.Ftp, SourceType.Smb, SourceType.WebDav)
                    sourceTypes.forEachIndexed { index, sourceType ->
                        SegmentedButton(
                            selected = state.sourceType == sourceType,
                            onClick = { onIntent(SourceEditorIntent.UpdateProtocol(sourceType)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = sourceTypes.size
                            ),
                            label = { Text(sourceType.displayName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.host,
                    onValueChange = { onIntent(SourceEditorIntent.UpdateHost(it)) },
                    label = { Text(stringResource(R.string.source_editor_label_host)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.port,
                    onValueChange = { onIntent(SourceEditorIntent.UpdatePort(it)) },
                    label = { Text(stringResource(R.string.source_editor_label_port)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.path,
                    onValueChange = { onIntent(SourceEditorIntent.UpdatePath(it)) },
                    label = {
                        Text(
                            when (state.sourceType) {
                                SourceType.Smb -> stringResource(R.string.source_editor_label_path_smb)
                                else -> stringResource(R.string.source_editor_label_path_default)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                AnimatedVisibility(
                    visible = state.sourceType == SourceType.WebDav,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.source_editor_label_https),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = state.useHttps,
                                onCheckedChange = { onIntent(SourceEditorIntent.UpdateUseHttps(it)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.username,
                    onValueChange = { onIntent(SourceEditorIntent.UpdateUsername(it)) },
                    label = { Text(stringResource(R.string.source_editor_label_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedSecureTextField(
                    state = passwordState,
                    label = { Text(stringResource(R.string.source_editor_label_password)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.errorResId != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(state.errorResId),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (state.detailedError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.detailedError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun isLocalNetworkHost(host: String): Boolean {
    if (host.isBlank()) return false
    val h = host.lowercase()
    if (h == "localhost" || h.endsWith(".local") || h.endsWith(".lan") || h.endsWith(".home") || !h.contains(
            "."
        )
    ) {
        return true
    }
    val parts = h.split(".")
    if (parts.size == 4 && parts.all { it.toIntOrNull() != null }) {
        val p0 = parts[0].toIntOrNull() ?: return false
        val p1 = parts[1].toIntOrNull() ?: return false
        if (p0 == 10) return true
        if (p0 == 172 && p1 in 16..31) return true
        if (p0 == 192 && p1 == 168) return true
        if (p0 == 169 && p1 == 254) return true
        if (p0 == 100 && p1 in 64..127) return true
        if (p0 == 127) return true
    }
    return false
}

@Preview(showBackground = true)
@Composable
fun SourceEditorContentPreview() {
    AppTheme {
        SourceEditorContent(
            state = SourceEditorState(name = "My Server", host = "192.168.1.1"),
            onIntent = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, device = Devices.TABLET)
@Composable
fun SourceEditorContentTabletPreview() {
    AppTheme {
        SourceEditorContent(
            state = SourceEditorState(name = "My Server", host = "192.168.1.1"),
            onIntent = {},
            onNavigateUp = {}
        )
    }
}