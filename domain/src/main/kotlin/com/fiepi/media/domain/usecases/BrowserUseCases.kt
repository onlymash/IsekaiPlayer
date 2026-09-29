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

package com.fiepi.media.domain.usecases

import com.fiepi.media.domain.usecase.history.ClearAllHistoryUseCase
import com.fiepi.media.domain.usecase.history.DeleteHistoryUseCase
import com.fiepi.media.domain.usecase.history.GetHistoryPagingUseCase
import com.fiepi.media.domain.usecase.history.GetLastPlayedInDirectoryUseCase
import com.fiepi.media.domain.usecase.history.SaveHistoryUseCase
import com.fiepi.media.domain.usecase.media.GetMediaFilesUseCase
import com.fiepi.media.domain.usecase.media.HasCachedMediaUseCase
import com.fiepi.media.domain.usecase.media.SearchMediaFilesUseCase
import com.fiepi.media.domain.usecase.preferences.GetDisplayFieldsUseCase
import com.fiepi.media.domain.usecase.preferences.GetInterceptBackNavigationUseCase
import com.fiepi.media.domain.usecase.preferences.GetSelectedSourceIdUseCase
import com.fiepi.media.domain.usecase.preferences.GetSortOptionsUseCase
import com.fiepi.media.domain.usecase.preferences.SaveSelectedSourceIdUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateDisplayFieldsUseCase
import com.fiepi.media.domain.usecase.preferences.UpdateSortOptionsUseCase
import com.fiepi.media.domain.usecase.source.DeleteRemoteSourceUseCase
import com.fiepi.media.domain.usecase.source.DisconnectRemoteSourceUseCase
import com.fiepi.media.domain.usecase.source.GetSourcesUseCase
import com.fiepi.media.domain.usecases.playlist.PlaylistUseCases

data class BrowserUseCases(
    val getMediaFiles: GetMediaFilesUseCase,
    val hasCachedMedia: HasCachedMediaUseCase,
    val searchMediaFiles: SearchMediaFilesUseCase,
    val getSortOptions: GetSortOptionsUseCase,
    val updateSortOptions: UpdateSortOptionsUseCase,
    val getDisplayFields: GetDisplayFieldsUseCase,
    val updateDisplayFields: UpdateDisplayFieldsUseCase,
    val getSources: GetSourcesUseCase,
    val deleteRemoteSource: DeleteRemoteSourceUseCase,
    val closeRemoteSource: DisconnectRemoteSourceUseCase,
    val getSelectedSourceId: GetSelectedSourceIdUseCase,
    val saveSelectedSourceId: SaveSelectedSourceIdUseCase,
    val getInterceptBackNavigation: GetInterceptBackNavigationUseCase,
    val getLastPlayedInDirectory: GetLastPlayedInDirectoryUseCase,
    val getHistoryPaging: GetHistoryPagingUseCase,
    val saveHistory: SaveHistoryUseCase,
    val deleteHistory: DeleteHistoryUseCase,
    val clearAllHistory: ClearAllHistoryUseCase,
    val playlist: PlaylistUseCases
)
