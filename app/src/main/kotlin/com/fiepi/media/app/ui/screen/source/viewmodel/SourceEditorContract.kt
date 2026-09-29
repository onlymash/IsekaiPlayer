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

package com.fiepi.media.app.ui.screen.source.viewmodel

import com.fiepi.media.domain.model.source.SourceType

data class SourceEditorState(
    val id: String? = null,
    val name: String = "",
    val sourceType: SourceType = SourceType.Smb,
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val path: String = "",
    val useHttps: Boolean = true,
    val sortOrder: Int = 0,
    val isSaving: Boolean = false,
    val isSuccess: Boolean = false,
    val errorResId: Int? = null,
    val detailedError: String? = null
)

sealed class SourceEditorIntent {
    data class UpdateName(val name: String) : SourceEditorIntent()
    data class UpdateProtocol(val sourceType: SourceType) : SourceEditorIntent()
    data class UpdateHost(val host: String) : SourceEditorIntent()
    data class UpdatePort(val port: String) : SourceEditorIntent()
    data class UpdateUsername(val username: String) : SourceEditorIntent()
    data class UpdatePassword(val password: String) : SourceEditorIntent()
    data class UpdatePath(val path: String) : SourceEditorIntent()
    data class UpdateUseHttps(val useHttps: Boolean) : SourceEditorIntent()
    data object Save : SourceEditorIntent()
}
