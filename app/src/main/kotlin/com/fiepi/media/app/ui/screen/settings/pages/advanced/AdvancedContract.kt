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

package com.fiepi.media.app.ui.screen.settings.pages.advanced

sealed class AdvancedPrefsIntent {
    data class UpdateTlsVerify(val enabled: Boolean) : AdvancedPrefsIntent()
    data class UpdateUserAgent(val ua: String) : AdvancedPrefsIntent()
    data class UpdateNetworkTimeout(val timeout: Int) : AdvancedPrefsIntent()
    data class UpdateCacheSecs(val secs: Int) : AdvancedPrefsIntent()
    data class UpdateCacheBackSecs(val secs: Int) : AdvancedPrefsIntent()
    data class UpdateDemuxerMaxBytes(val bytes: Long) : AdvancedPrefsIntent()
    data class UpdateDemuxerMaxBackBytes(val bytes: Long) : AdvancedPrefsIntent()

    object ClearThumbnailCache : AdvancedPrefsIntent()
    object ClearVideoCache : AdvancedPrefsIntent()
    object ClearPlaybackHistory : AdvancedPrefsIntent()
}

sealed interface AdvancedEffect {
    data class ShowToast(val message: String) : AdvancedEffect
}
