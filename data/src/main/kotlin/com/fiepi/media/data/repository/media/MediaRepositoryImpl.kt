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

package com.fiepi.media.data.repository.media

import android.util.Log
import com.fiepi.media.data.repository.media.remote.FtpMediaRepositoryImpl
import com.fiepi.media.data.repository.media.remote.SmbMediaRepositoryImpl
import com.fiepi.media.data.repository.media.remote.WebDavMediaRepositoryImpl
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.preferences.SortOrder
import com.fiepi.media.domain.model.preferences.SortType
import com.fiepi.media.domain.model.source.MediaSource
import com.fiepi.media.domain.model.source.RemoteSource
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.media.LocalMediaRepository
import com.fiepi.media.domain.repository.media.MediaRepository
import com.fiepi.media.domain.repository.media.RemoteMediaRepository
import com.fiepi.media.domain.repository.source.SourceRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryImpl(
    private val sourceRepository: SourceRepository,
    private val localMediaRepository: LocalMediaRepository
) : MediaRepository {

    private val cleanupScope = CoroutineScope(
        SupervisorJob() +
                Dispatchers.IO +
                CoroutineExceptionHandler { _, throwable ->
                    Log.e("MediaRepository", "Background operation failed", throwable)
                }
    )
    private val remoteDispatcher = Dispatchers.IO.limitedParallelism(20)
    private var currentRemoteRepo: RemoteMediaRepository? = null
    private var currentSource: MediaSource.Remote? = null
    private val remoteFilesCache = ConcurrentHashMap<String, List<MediaFile>>()
    private val _remoteUpdateEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun getMediaFiles(
        path: String?,
        source: MediaSource,
        options: MediaOptions,
        forceRefresh: Boolean
    ): List<MediaFile> {
        return when (source) {
            is MediaSource.Local -> {
                withContext(Dispatchers.IO) {
                    cleanupOldRepo()
                    localMediaRepository.getMediaFiles(path, options, forceRefresh)
                }
            }

            is MediaSource.External -> emptyList()

            is MediaSource.Remote -> {
                // Isolate remote I/O and add a hard timeout to prevent app-wide hangs
                withContext(remoteDispatcher) {
                    withTimeout(AppConstants.REMOTE_OPERATION_TIMEOUT_MS.milliseconds) {
                        val repo = getOrUpdateRemoteRepo(source)

                        // Ensure connection, but don't disconnect/reconnect on every refresh
                        if (!repo.isConnected) {
                            repo.connect()
                            updateRemoteSpaceInfo(source, repo)
                        } else if (forceRefresh) {
                            // On manual refresh, also update space info to keep it fresh
                            updateRemoteSpaceInfo(source, repo)
                        }

                        if (forceRefresh) {
                            remoteFilesCache.clear()
                        }

                        val targetPath = path ?: ""
                        val rawItems =
                            if (!forceRefresh && remoteFilesCache.containsKey(targetPath)) {
                                remoteFilesCache[targetPath]!!
                            } else {
                                val fetched = repo.getMediaFiles(targetPath)
                                remoteFilesCache[targetPath] = fetched
                                fetched
                            }

                        val filteredItems = rawItems.filter { item ->
                            val passesHidden = options.showHidden || !item.name.startsWith(".")
                            val passesSubtitle =
                                options.showSubtitles || item !is MediaFile.Subtitle
                            passesHidden && passesSubtitle
                        }

                        // Remote results sorting is currently handled client-side since most basic
                        // protocols (FTP/WebDAV) don't support server-side sorting natively.
                        sortItems(filteredItems, options)
                    }
                }
            }
        }
    }

    override fun observeMediaFiles(
        path: String?,
        source: MediaSource,
        options: MediaOptions,
        forceRefresh: Boolean
    ): Flow<List<MediaFile>> {
        return when (source) {
            is MediaSource.Local -> localMediaRepository.observeMediaFiles(path, options, forceRefresh)
            is MediaSource.External -> flowOf(emptyList())
            is MediaSource.Remote -> flow {
                emit(getMediaFiles(path, source, options, forceRefresh))
                _remoteUpdateEvent.collect {
                    emit(getMediaFiles(path, source, options, forceRefresh = false))
                }
            }.flowOn(Dispatchers.IO)
        }
    }

    private fun sortItems(items: List<MediaFile>, options: MediaOptions): List<MediaFile> {
        val comparator = compareBy<MediaFile> {
            if (options.foldersFirst) {
                if (it is MediaFile.Folder) 0 else 1
            } else {
                0
            }
        }.then { item1, item2 ->
            val result = when (options.sortType) {
                SortType.Name -> item1.name.compareTo(item2.name, ignoreCase = true)
                SortType.Date -> {
                    val d1 = (item1 as? MediaFile.Video)?.lastModified ?: 0L
                    val d2 = (item2 as? MediaFile.Video)?.lastModified ?: 0L
                    d1.compareTo(d2)
                }

                SortType.Size -> {
                    val s1 = (item1 as? MediaFile.Video)?.size ?: 0L
                    val s2 = (item2 as? MediaFile.Video)?.size ?: 0L
                    s1.compareTo(s2)
                }

                SortType.Duration -> {
                    val dur1 = (item1 as? MediaFile.Video)?.duration ?: 0L
                    val dur2 = (item2 as? MediaFile.Video)?.duration ?: 0L
                    dur1.compareTo(dur2)
                }
            }

            if (options.sortOrder == SortOrder.Ascending) result else -result
        }

        return items.sortedWith(comparator)
    }

    override fun hasCachedData(path: String, source: MediaSource): Boolean {
        return when (source) {
            is MediaSource.Local -> localMediaRepository.isCached()
            is MediaSource.External -> false
            is MediaSource.Remote -> {
                // Ensure we are checking the cache for the currently active remote source
                val isSameSource = currentSource?.source?.isSameConnection(source.source) == true
                isSameSource && remoteFilesCache.containsKey(path)
            }
        }
    }

    override fun searchMediaFiles(
        query: String,
        path: String?,
        source: MediaSource
    ): Flow<MediaFile.Video> {
        return when (source) {
            is MediaSource.Local -> {
                localMediaRepository.searchMediaFiles(query, path)
            }

            is MediaSource.External -> emptyFlow()

            is MediaSource.Remote -> {
                flow {
                    val repo = getOrUpdateRemoteRepo(source)
                    if (!repo.isConnected) {
                        repo.connect()
                    }
                    repo.searchMediaFiles(query, path ?: "")
                        .collect { emit(it) }
                }.flowOn(remoteDispatcher)
            }
        }
    }

    private suspend fun updateRemoteSpaceInfo(
        source: MediaSource.Remote,
        repo: RemoteMediaRepository
    ) {
        val spaceInfo = repo.getSpaceInfo() ?: return
        if (spaceInfo.totalSpace != source.source.totalSpace || spaceInfo.freeSpace != source.source.freeSpace) {
            val updatedSource = source.source.copy(
                totalSpace = spaceInfo.totalSpace,
                freeSpace = spaceInfo.freeSpace
            )
            sourceRepository.saveRemoteSource(updatedSource)
        }
    }

    private fun getOrUpdateRemoteRepo(source: MediaSource.Remote): RemoteMediaRepository {
        synchronized(this) {
            val isSameSource = currentSource?.source?.isSameConnection(source.source) == true
            if (isSameSource && currentRemoteRepo != null) {
                // Update current source reference to keep metadata like name or space info fresh, 
                // but don't recreate the repository.
                currentSource = source
                return currentRemoteRepo!!
            }

            cleanupOldRepo()

            val newRepo = when (source.type) {
                SourceType.Smb -> SmbMediaRepositoryImpl(source.source)
                SourceType.Ftp -> FtpMediaRepositoryImpl(source.source)
                SourceType.WebDav -> WebDavMediaRepositoryImpl(source.source)
                else -> throw IllegalArgumentException("Unsupported SourceType")
            }

            currentRemoteRepo = newRepo
            currentSource = source
            return newRepo
        }
    }

    private fun cleanupOldRepo() {
        val oldRepo = currentRemoteRepo
        currentRemoteRepo = null
        currentSource = null
        remoteFilesCache.clear()
        // Cleanup old repository in a non-blocking way
        cleanupScope.launch(remoteDispatcher) {
            try {
                withTimeout(AppConstants.REMOTE_OPERATION_TIMEOUT_MS.milliseconds) {
                    oldRepo?.disconnect()
                }
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    override fun disconnectRemote() {
        synchronized(this) {
            val repo = currentRemoteRepo
            cleanupScope.launch(remoteDispatcher) {
                try {
                    withTimeout(AppConstants.REMOTE_OPERATION_TIMEOUT_MS.milliseconds) {
                        repo?.disconnect()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun connectRemote() {
        synchronized(this) {
            val repo = currentRemoteRepo
            cleanupScope.launch(remoteDispatcher) {
                try {
                    withTimeout(AppConstants.REMOTE_OPERATION_TIMEOUT_MS.milliseconds) {
                        repo?.connect()
                    }
                } catch (_: Exception) {
                    // Ignore background connection errors to prevent crashes.
                    // Real errors will be handled when getMediaFiles() is called.
                }
            }
        }
    }

    override suspend fun openRemoteFile(
        path: String,
        source: MediaSource.Remote,
        offset: Long
    ): InputStream {
        return withContext(remoteDispatcher) {
            withTimeout(AppConstants.REMOTE_OPERATION_TIMEOUT_MS.milliseconds) {
                val repo = getOrUpdateRemoteRepo(source)
                if (!repo.isConnected) {
                    repo.connect()
                }
                try {
                    repo.openFile(path, offset)
                } catch (e: Exception) {
                    if (!repo.isConnected) {
                        repo.connect()
                        repo.openFile(path, offset)
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    override suspend fun getRemoteFileSize(path: String, source: MediaSource.Remote): Long {
        return withContext(remoteDispatcher) {
            withTimeout(30_000.milliseconds) {
                val repo = getOrUpdateRemoteRepo(source)
                if (!repo.isConnected) {
                    repo.connect()
                }
                val size = repo.getFileSize(path)
                if (size == -1L && !repo.isConnected) {
                    // It likely disconnected during the call. Try one more time.
                    repo.connect()
                    repo.getFileSize(path)
                } else {
                    size
                }
            }
        }
    }

    override suspend fun readRemoteRange(
        path: String,
        source: MediaSource.Remote,
        range: LongRange
    ): ByteArray {
        return withContext(remoteDispatcher) {
            withTimeout(AppConstants.REMOTE_OPERATION_TIMEOUT_MS.milliseconds) {
                val repo = getOrUpdateRemoteRepo(source)
                repo.readRange(path, range)
            }
        }
    }

    override suspend fun validateRemoteSource(source: RemoteSource) {
        withContext(remoteDispatcher) {
            withTimeout(AppConstants.REMOTE_OPERATION_TIMEOUT_MS.milliseconds) {
                val repo = when (source.type) {
                    SourceType.Smb -> SmbMediaRepositoryImpl(source)
                    SourceType.Ftp -> FtpMediaRepositoryImpl(source)
                    SourceType.WebDav -> WebDavMediaRepositoryImpl(source)
                    else -> throw IllegalArgumentException("Unsupported SourceType: ${source.type}")
                }
                try {
                    repo.connect()
                } finally {
                    repo.disconnect()
                }
            }
        }
    }
}
