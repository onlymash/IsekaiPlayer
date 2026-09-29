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

package com.fiepi.media.domain.usecases.playlist

import com.fiepi.media.domain.usecase.playlist.AddMediaToPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.CreatePlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.DeletePlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistItemByIndexUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistItemCountUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistItemIndexByMediaIdUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistItemPagingUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.GetPlaylistsUseCase
import com.fiepi.media.domain.usecase.playlist.ImportM3uPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.RefreshPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.RemoveMediaFromPlaylistUseCase
import com.fiepi.media.domain.usecase.playlist.ReorderPlaylistItemsUseCase
import com.fiepi.media.domain.usecase.playlist.ReorderPlaylistsUseCase
import com.fiepi.media.domain.usecase.playlist.UpdatePlaylistTitleUseCase

data class PlaylistUseCases(
    val getPlaylists: GetPlaylistsUseCase,
    val getPlaylist: GetPlaylistUseCase,
    val createPlaylist: CreatePlaylistUseCase,
    val updatePlaylistTitle: UpdatePlaylistTitleUseCase,
    val deletePlaylist: DeletePlaylistUseCase,
    val addMediaToPlaylist: AddMediaToPlaylistUseCase,
    val removeMediaFromPlaylist: RemoveMediaFromPlaylistUseCase,
    val reorderPlaylistItems: ReorderPlaylistItemsUseCase,
    val importM3uPlaylist: ImportM3uPlaylistUseCase,
    val refreshPlaylist: RefreshPlaylistUseCase,
    val getPlaylistItemPaging: GetPlaylistItemPagingUseCase,
    val getPlaylistItemByIndex: GetPlaylistItemByIndexUseCase,
    val getPlaylistItemCount: GetPlaylistItemCountUseCase,
    val getPlaylistItemIndexByMediaId: GetPlaylistItemIndexByMediaIdUseCase,
    val reorderPlaylists: ReorderPlaylistsUseCase
)
