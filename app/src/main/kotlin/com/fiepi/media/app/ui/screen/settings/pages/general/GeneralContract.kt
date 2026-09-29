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

package com.fiepi.media.app.ui.screen.settings.pages.general

sealed class GeneralPrefsIntent {
    data class SetInterceptBackNavigation(val enabled: Boolean) : GeneralPrefsIntent()
    data class SetHapticsEnabled(val enabled: Boolean) : GeneralPrefsIntent()
    data class ExportBackup(val uriString: String) : GeneralPrefsIntent()
    data class ImportBackup(val uriString: String) : GeneralPrefsIntent()
}

sealed interface GeneralEffect {
    data class ShowToast(val message: String) : GeneralEffect
}

data class GeneralUiState(
    val interceptBackNavigation: Boolean = true,
    val isHapticsEnabled: Boolean = true
)
