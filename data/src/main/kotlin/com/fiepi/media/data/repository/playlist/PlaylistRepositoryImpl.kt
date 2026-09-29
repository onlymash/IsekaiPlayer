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

package com.fiepi.media.data.repository.playlist

import android.content.Context
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.fiepi.media.data.database.dao.PlaylistDao
import com.fiepi.media.data.database.entity.PlaylistEntity
import com.fiepi.media.data.database.entity.PlaylistItemEntity
import com.fiepi.media.data.repository.playlist.parser.M3uParser
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.playlist.M3uEntry
import com.fiepi.media.domain.model.playlist.Playlist
import com.fiepi.media.domain.model.playlist.PlaylistItem
import com.fiepi.media.domain.model.playlist.PlaylistType
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.playlist.PlaylistRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

class PlaylistRepositoryImpl(
    private val dao: PlaylistDao,
    private val context: Context,
    private val httpClient: HttpClient
) : PlaylistRepository {

    override fun getPlaylistsFlow(): Flow<List<Playlist>> {
        return dao.getPlaylistsWithCountFlow().map { list ->
            list.map { it.playlist.toDomain(itemCount = it.itemCount) }
        }
    }

    override fun getPlaylistFlow(playlistId: String): Flow<Playlist?> {
        return dao.getPlaylistWithCountFlow(playlistId).map { result ->
            result?.playlist?.toDomain(itemCount = result.itemCount)
        }
    }

    override fun getPlaylistItemsFlow(playlistId: String): Flow<List<PlaylistItem>> {
        return dao.getPlaylistItemsFlow(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPlaylistItemsPagingFlow(
        playlistId: String?,
        query: String?,
        initialKey: Int?
    ): Flow<PagingData<PlaylistItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                prefetchDistance = 15,
                enablePlaceholders = true
            ),
            initialKey = initialKey,
            pagingSourceFactory = { dao.getPlaylistItemsPagingSource(playlistId, query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun getPlaylistItemByIndex(playlistId: String, index: Int): PlaylistItem? {
        return dao.getPlaylistItemByIndex(playlistId, index)?.toDomain()
    }

    override suspend fun getPlaylistItemCount(playlistId: String): Int {
        return dao.getPlaylistItemCount(playlistId)
    }

    override suspend fun getPlaylistItemIndexByMediaId(playlistId: String, mediaId: String): Int? {
        return dao.getPlaylistItemIndexByMediaId(playlistId, mediaId)
    }

    private suspend fun createPlaylistInternal(
        title: String,
        coverUrl: String? = null,
        type: PlaylistType = PlaylistType.NORMAL,
        sourceUrl: String? = null
    ): String {
        val maxSort = dao.getPlaylistMaxSortOrder() ?: -1
        val entity = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            coverUrl = coverUrl,
            type = type.value,
            sourceUrl = sourceUrl,
            sortOrder = maxSort + 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertPlaylist(entity)
        return entity.id
    }

    override suspend fun createPlaylist(title: String, coverUrl: String?): String =
        withContext(Dispatchers.IO) {
            createPlaylistInternal(title = title, coverUrl = coverUrl, type = PlaylistType.NORMAL)
        }

    override suspend fun updatePlaylistTitle(playlistId: String, newTitle: String) =
        withContext(Dispatchers.IO) {
            dao.updatePlaylistTitle(playlistId, newTitle)
        }

    override suspend fun reorderPlaylists(playlistIds: List<String>) =
        withContext(Dispatchers.IO) {
            playlistIds.forEachIndexed { index, id ->
                dao.updatePlaylistSortOrder(id, index)
            }
        }

    override suspend fun deletePlaylist(playlistId: String) =
        withContext(Dispatchers.IO) {
            dao.deletePlaylistById(playlistId)
        }

    override suspend fun addMediaToPlaylist(playlistId: String, media: MediaFile.Video): String =
        withContext(Dispatchers.IO) {
            val maxSort = dao.getMaxSortOrder(playlistId) ?: -1
            val newSortOrder = maxSort + 1
            val entity = media.toEntity(playlistId = playlistId, sortOrder = newSortOrder)
            dao.insertPlaylistItem(entity)
            dao.updatePlaylistTimestamp(playlistId)
            entity.id
        }

    override suspend fun addMediaListToPlaylist(
        playlistId: String,
        mediaList: List<MediaFile.Video>
    ) = withContext(Dispatchers.IO) {
        if (mediaList.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        val currentSort = (dao.getMaxSortOrder(playlistId) ?: -1) + 1
        val entities = mediaList.mapIndexed { index, media ->
            media.toEntity(playlistId = playlistId, sortOrder = currentSort + index)
                .copy(addedAt = now + index)
        }
        dao.insertPlaylistItems(entities)
        dao.updatePlaylistTimestamp(playlistId)
    }

    override suspend fun swapPlaylistItems(playlistId: String, itemId1: String, itemId2: String) =
        withContext(Dispatchers.IO) {
            dao.swapPlaylistItems(itemId1, itemId2)
            dao.updatePlaylistTimestamp(playlistId)
        }

    override suspend fun removePlaylistItem(itemId: String) =
        withContext(Dispatchers.IO) {
            dao.deletePlaylistItemById(itemId)
        }

    override suspend fun removeMediaFromPlaylist(
        playlistId: String,
        sourceId: String,
        path: String
    ) =
        withContext(Dispatchers.IO) {
            dao.deletePlaylistItemByPath(playlistId, sourceId, path)
        }

    override suspend fun reorderPlaylistItems(playlistId: String, itemIds: List<String>) =
        withContext(Dispatchers.IO) {
            if (itemIds.isEmpty()) return@withContext
            val existingItems = dao.getPlaylistItemsSync(playlistId).associateBy { it.id }
            val reorderedEntities = itemIds.mapIndexedNotNull { index, itemId ->
                existingItems[itemId]?.copy(sortOrder = index)
            }
            if (reorderedEntities.isNotEmpty()) {
                dao.updatePlaylistItems(reorderedEntities)
                dao.updatePlaylistTimestamp(playlistId)
            }
        }

    override suspend fun clearPlaylistItems(playlistId: String) =
        withContext(Dispatchers.IO) {
            dao.clearPlaylistItems(playlistId)
            dao.updatePlaylistTimestamp(playlistId)
        }

    override suspend fun importM3uFromUrl(title: String, url: String): String =
        withContext(Dispatchers.IO) {
            val trimmedUrl = url.trim()
            require(
                trimmedUrl.startsWith("http://", ignoreCase = true) ||
                        trimmedUrl.startsWith("https://", ignoreCase = true)
            ) { "Invalid URL scheme" }

            val response = try {
                httpClient.get(trimmedUrl)
            } catch (e: Exception) {
                throw IllegalStateException("Network request failed", e)
            }

            val entries = try {
                val inputStream = response.bodyAsChannel().toInputStream()
                val finalBaseUrl = response.call.request.url.toString().ifBlank { trimmedUrl }
                M3uParser.parse(inputStream, finalBaseUrl)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse M3U content", e)
            }

            val mediaList = entries.toMediaList()
            if (mediaList.isEmpty()) {
                throw IllegalArgumentException("Playlist contains no valid media items")
            }
            val playlistTitle = title.ifBlank { "M3U Playlist" }
            val playlistId = createPlaylistInternal(
                title = playlistTitle,
                type = PlaylistType.M3U_LINK,
                sourceUrl = trimmedUrl
            )
            addMediaListToPlaylist(playlistId, mediaList)
            playlistId
        }

    override suspend fun importM3uFromUri(title: String, uriString: String): String =
        withContext(Dispatchers.IO) {
            val uri = uriString.toUri()
            val inputStream = try {
                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open file stream")
            } catch (e: Exception) {
                throw IllegalStateException("File read failed", e)
            }

            val entries = try {
                M3uParser.parse(inputStream)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse M3U content", e)
            }

            val mediaList = entries.toMediaList()
            if (mediaList.isEmpty()) {
                throw IllegalArgumentException("Playlist contains no valid media items")
            }
            val playlistTitle = title.ifBlank { "M3U Playlist" }
            val playlistId = createPlaylistInternal(
                title = playlistTitle,
                type = PlaylistType.M3U_FILE,
                sourceUrl = null
            )
            addMediaListToPlaylist(playlistId, mediaList)
            playlistId
        }

    override suspend fun refreshPlaylist(playlistId: String): Unit =
        withContext(Dispatchers.IO) {
            val entity = dao.getPlaylistByIdSync(playlistId) ?: return@withContext
            val sourceUrl = entity.sourceUrl ?: return@withContext
            if (PlaylistType.fromValue(entity.type) != PlaylistType.M3U_LINK) return@withContext

            val response = try {
                httpClient.get(sourceUrl)
            } catch (e: Exception) {
                throw IllegalStateException("Network request failed", e)
            }

            val entries = try {
                val inputStream = response.bodyAsChannel().toInputStream()
                val finalBaseUrl = response.call.request.url.toString().ifBlank { sourceUrl }
                M3uParser.parse(inputStream, finalBaseUrl)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse M3U content", e)
            }

            val mediaList = entries.toMediaList()

            dao.clearPlaylistItems(playlistId)
            if (mediaList.isNotEmpty()) {
                val entities = mediaList.mapIndexed { index, media ->
                    media.toEntity(playlistId = playlistId, sortOrder = index)
                }
                dao.insertPlaylistItems(entities)
            }
            dao.updatePlaylistTimestamp(playlistId)
        }

    override suspend fun refreshM3uFilePlaylist(playlistId: String, uriString: String): Unit =
        withContext(Dispatchers.IO) {
            val uri = uriString.toUri()
            val inputStream = try {
                context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open file stream")
            } catch (e: Exception) {
                throw IllegalStateException("File read failed", e)
            }

            val entries = try {
                M3uParser.parse(inputStream)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse M3U content", e)
            }

            val mediaList = entries.toMediaList()

            dao.clearPlaylistItems(playlistId)
            if (mediaList.isNotEmpty()) {
                val entities = mediaList.mapIndexed { index, media ->
                    media.toEntity(playlistId = playlistId, sortOrder = index)
                }
                dao.insertPlaylistItems(entities)
            }
            dao.updatePlaylistTimestamp(playlistId)
        }

    private val json = Json { ignoreUnknownKeys = true }

    private fun Map<String, String>.toJsonString(): String? =
        if (isEmpty()) null else runCatching { json.encodeToString(this) }.getOrNull()

    private fun String?.toMap(): Map<String, String> =
        if (isNullOrBlank()) emptyMap() else runCatching {
            json.decodeFromString<Map<String, String>>(
                this
            )
        }.getOrDefault(emptyMap())

    @Serializable
    private data class SubtitleDto(
        @SerialName("path")
        val path: String,
        @SerialName("name")
        val name: String,
        @SerialName("source_type")
        val sourceType: String,
        @SerialName("source_id")
        val sourceId: String? = null,
        @SerialName("size")
        val size: Long? = null,
        @SerialName("last_modified")
        val lastModified: Long? = null,
        @SerialName("extension")
        val extension: String? = null,
        @SerialName("language")
        val language: String? = null,
        @SerialName("mime_type")
        val mimeType: String? = null
    ) {
        fun toDomain(): MediaFile.Subtitle = MediaFile.Subtitle(
            path = path,
            name = name,
            sourceType = SourceType.fromValue(sourceType),
            sourceId = sourceId,
            size = size,
            lastModified = lastModified,
            extension = extension,
            language = language,
            mimeType = mimeType
        )
    }

    private fun MediaFile.Subtitle.toDto(): SubtitleDto = SubtitleDto(
        path = path,
        name = name,
        sourceType = sourceType.value,
        sourceId = sourceId,
        size = size,
        lastModified = lastModified,
        extension = extension,
        language = language,
        mimeType = mimeType
    )

    private fun List<MediaFile.Subtitle>.toSubtitlesJsonString(): String? =
        if (isEmpty()) null else runCatching { json.encodeToString(map { it.toDto() }) }.getOrNull()

    private fun String?.toSubtitlesList(): List<MediaFile.Subtitle> =
        if (isNullOrBlank()) emptyList() else runCatching {
            json.decodeFromString<List<SubtitleDto>>(this).map { it.toDomain() }
        }.getOrDefault(emptyList())

    private fun List<M3uEntry>.toMediaList(): List<MediaFile.Video> = map { entry ->
        val isNetworkStream = entry.url.startsWith("http://", ignoreCase = true) ||
                entry.url.startsWith("https://", ignoreCase = true) ||
                entry.url.startsWith("rtmp://", ignoreCase = true) ||
                entry.url.startsWith("rtsp://", ignoreCase = true)
        MediaFile.Video(
            id = UUID.randomUUID().toString(),
            path = entry.url,
            name = entry.title,
            sourceType = if (isNetworkStream) SourceType.Stream else SourceType.Local,
            duration = if (entry.durationSeconds > 0) entry.durationSeconds * 1000 else null,
            thumbnailUrl = entry.logoUrl,
            extraHeaders = entry.headers
        )
    }

    private fun PlaylistEntity.toDomain(itemCount: Int = 0): Playlist = Playlist(
        id = id,
        title = title,
        coverUrl = coverUrl,
        itemCount = itemCount,
        type = PlaylistType.fromValue(type),
        sourceUrl = sourceUrl,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun PlaylistItemEntity.toDomain(): PlaylistItem = PlaylistItem(
        id = id,
        playlistId = playlistId,
        media = MediaFile.Video(
            path = path,
            name = name,
            sourceType = SourceType.fromValue(sourceType),
            size = size,
            lastModified = lastModified,
            extension = extension,
            id = mediaId,
            sourceId = sourceId,
            duration = duration,
            mimeType = mimeType,
            width = width,
            height = height,
            thumbnailUrl = thumbnailUrl,
            externalSubtitles = externalSubtitles.toSubtitlesList(),
            extraHeaders = extraHeaders.toMap()
        ),
        sortOrder = sortOrder,
        addedAt = addedAt
    )

    private fun MediaFile.Video.toEntity(playlistId: String, sortOrder: Int): PlaylistItemEntity =
        PlaylistItemEntity(
            id = UUID.randomUUID().toString(),
            playlistId = playlistId,
            path = path,
            name = name,
            sourceId = sourceId ?: sourceType.value,
            sourceType = sourceType.value,
            size = size,
            lastModified = lastModified,
            extension = extension,
            mediaId = id,
            duration = duration,
            mimeType = mimeType,
            width = width,
            height = height,
            thumbnailUrl = thumbnailUrl,
            extraHeaders = extraHeaders.toJsonString(),
            externalSubtitles = externalSubtitles.toSubtitlesJsonString(),
            sortOrder = sortOrder,
            addedAt = System.currentTimeMillis()
        )
}
