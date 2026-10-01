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

package com.fiepi.media.data.repository.media.local

import android.content.Context
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.fiepi.media.data.utils.resolveMimeType
import com.fiepi.media.domain.config.AppConstants
import com.fiepi.media.domain.model.media.MediaFile
import com.fiepi.media.domain.model.preferences.MediaOptions
import com.fiepi.media.domain.model.preferences.SortOrder
import com.fiepi.media.domain.model.preferences.SortType
import com.fiepi.media.domain.model.source.SourceType
import com.fiepi.media.domain.repository.media.LocalMediaRepository
import com.fiepi.media.domain.utils.FileUtils
import com.fiepi.media.domain.utils.SubtitleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class LocalMediaRepositoryImpl(
    private val context: Context
) : LocalMediaRepository {

    // Primary caches for video data
    private var allVideosCache: List<MediaFile.Video>? = null
    private var videosByParentCache: Map<String, List<MediaFile.Video>> = emptyMap()
    private var allParentPathsCache: Set<String> = emptySet()

    // Secondary caches for expensive disk-based metadata
    private val subtitleCache = ConcurrentHashMap<String, List<MediaFile.Subtitle>>()

    // Key: folderPath + "|" + showSubtitles + "|" + showHidden
    private val folderItemCountCache = ConcurrentHashMap<String, Int>()

    @Volatile
    private var isCacheDirty = true

    init {
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    invalidateCache()
                }
            }
        )
    }

    private fun invalidateCache() {
        isCacheDirty = true
        allVideosCache = null
        videosByParentCache = emptyMap()
        allParentPathsCache = emptySet()
        subtitleCache.clear()
        folderItemCountCache.clear()
    }

    private suspend fun ensureVideosLoaded(force: Boolean = false): List<MediaFile.Video> =
        withContext(Dispatchers.IO) {
            if (force || isCacheDirty || allVideosCache == null) {
                val all = queryVideos()
                allVideosCache = all
                val grouped = all.groupBy { File(it.path).parent ?: "" }
                videosByParentCache = grouped
                allParentPathsCache = grouped.keys

                // If forced, we clear everything to ensure a fresh scan
                if (force) {
                    subtitleCache.clear()
                    folderItemCountCache.clear()
                }

                isCacheDirty = false
            }
            allVideosCache!!
        }

    override suspend fun getMediaFiles(
        path: String?,
        options: MediaOptions,
        forceRefresh: Boolean
    ): List<MediaFile> {
        ensureVideosLoaded(forceRefresh)

        val items = if (path == null) {
            // Show root folders that contain videos
            allParentPathsCache.asSequence()
                .filter { it.isNotEmpty() }
                .map { folderPath -> createFolderItem(folderPath, options) }
                .toList()
        } else {
            val folderItems = mutableListOf<MediaFile>()

            // Add direct videos from memory cache
            videosByParentCache[path]?.let { folderItems.addAll(it) }

            // Add direct sub-folders discovered from indexed video paths
            val searchPrefix = if (path.endsWith("/")) path else "$path/"
            val directSubFolders = allParentPathsCache.asSequence()
                .filter { it.startsWith(searchPrefix) }
                .map { fullPath ->
                    val relative = fullPath.removePrefix(searchPrefix)
                    val directSubName = relative.substringBefore('/')
                    if (path.endsWith("/")) path + directSubName else "$path/$directSubName"
                }
                .distinct()
                .map { subFolderPath -> createFolderItem(subFolderPath, options) }
            folderItems.addAll(directSubFolders)

            // Add subtitles (lazy cached disk scan)
            val subtitles = getOrScanSubtitles(path)
            if (options.showSubtitles) {
                folderItems.addAll(subtitles)
            }

            // Associate subtitles with videos in this folder
            folderItems.indices.forEach { i ->
                val item = folderItems[i]
                if (item is MediaFile.Video) {
                    val videoBaseName = item.name
                    val associated = subtitles.filter {
                        SubtitleUtils.isAssociatedWithVideo(it.name, videoBaseName)
                    }
                    folderItems[i] = item.copy(externalSubtitles = associated)
                }
            }
            folderItems
        }

        // Perform optimized sorting
        return sortItems(items, options)
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

    override fun searchMediaFiles(query: String, path: String?): Flow<MediaFile.Video> = flow {
        val allVideos = ensureVideosLoaded()
        allVideos.forEach { video ->
            val matchesQuery = video.name.contains(query, ignoreCase = true)
            val matchesPath = path == null || video.path.startsWith(path)
            if (matchesQuery && matchesPath) {
                val parentPath = File(video.path).parent ?: ""
                val videoBaseName = video.name
                val associated = getOrScanSubtitles(parentPath).filter {
                    SubtitleUtils.isAssociatedWithVideo(it.name, videoBaseName)
                }
                emit(video.copy(externalSubtitles = associated))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun isCached(): Boolean = !isCacheDirty && allVideosCache != null

    private fun createFolderItem(folderPath: String, options: MediaOptions): MediaFile.Folder {
        val folderFile = File(folderPath)
        return MediaFile.Folder(
            path = folderPath,
            name = folderPath.substringAfterLast('/'),
            sourceType = SourceType.Local,
            lastModified = folderFile.lastModified().takeIf { it > 0 },
            itemCount = getOrCalculateItemCount(folderPath, options)
        )
    }

    private fun getOrCalculateItemCount(folderPath: String, options: MediaOptions): Int {
        val cacheKey = "$folderPath|${options.showSubtitles}|${options.showHidden}"
        return folderItemCountCache.getOrPut(cacheKey) {
            // A. Count sub-folders that contain videos (from memory)
            val subFoldersCount = allParentPathsCache.count {
                it.startsWith(folderPath) && it != folderPath &&
                        it.removePrefix(folderPath).removePrefix("/")
                            .substringBefore('/') == it.removePrefix(folderPath).removePrefix("/")
            }

            // B. Count videos in this folder (from memory)
            val videosCount = videosByParentCache[folderPath]?.size ?: 0

            // C. Count subtitles (minimal disk probe, then cached)
            val subtitlesCount = if (options.showSubtitles) {
                val subs = getOrScanSubtitles(folderPath)
                if (options.showHidden) subs.size else subs.count { !it.name.startsWith(".") }
            } else 0

            subFoldersCount + videosCount + subtitlesCount
        }
    }

    private fun getOrScanSubtitles(path: String): List<MediaFile.Subtitle> {
        return subtitleCache.getOrPut(path) {
            val videos = videosByParentCache[path]
            if (videos.isNullOrEmpty()) {
                return@getOrPut emptyList()
            }

            val videoBaseNames = videos.map { it.name }
            val subtitles = mutableListOf<MediaFile.Subtitle>()
            try {
                val dirPath = Paths.get(path)
                if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                    Files.newDirectoryStream(dirPath) { entry ->
                        val fileName = entry.fileName.toString()
                        val subtitleInfo = SubtitleUtils.parse(fileName)
                        val ext = subtitleInfo.extension ?: ""
                        ext in AppConstants.SUBTITLE_EXTENSIONS &&
                                videoBaseNames.any {
                                    SubtitleUtils.isAssociatedWithVideo(
                                        subtitleInfo.baseName,
                                        it
                                    )
                                } &&
                                Files.isRegularFile(entry)
                    }.use { stream ->
                        for (entry in stream) {
                            val file = entry.toFile()
                            val fileName = file.name
                            val subtitleInfo = SubtitleUtils.parse(fileName)
                            val mimeType = resolveMimeType(subtitleInfo.extension)

                            subtitles.add(
                                MediaFile.Subtitle(
                                    path = file.absolutePath,
                                    name = subtitleInfo.baseName,
                                    sourceType = SourceType.Local,
                                    size = file.length(),
                                    lastModified = file.lastModified(),
                                    language = subtitleInfo.language,
                                    extension = subtitleInfo.extension,
                                    mimeType = mimeType
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore or log error
            }
            subtitles
        }
    }

    private fun queryVideos(): List<MediaFile.Video> {
        val videos = mutableListOf<MediaFile.Video>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val videoId = cursor.getLong(idColumn)
                val path = cursor.getString(pathColumn)
                val rawName = cursor.getString(nameColumn)
                val ext = FileUtils.extractExtension(rawName)
                val nameWithoutExt = FileUtils.extractNameWithoutExtension(rawName)
                val mimeType = cursor.getString(mimeColumn) ?: resolveMimeType(ext)

                videos.add(
                    MediaFile.Video(
                        id = videoId.toString(),
                        name = nameWithoutExt,
                        path = path,
                        sourceType = SourceType.Local,
                        duration = cursor.getLong(durationColumn),
                        size = cursor.getLong(sizeColumn),
                        lastModified = cursor.getLong(dateColumn) * 1000,
                        mimeType = mimeType,
                        extension = ext,
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        thumbnailUrl = Uri.Builder()
                            .scheme(AppConstants.MEDIA_FRAME_SCHEME)
                            .authority("local")
                            .path(path)
                            .build()
                            .toString()
                    )
                )
            }
        }
        return videos
    }

    override suspend fun renameFile(path: String, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val oldFile = File(path)
                if (!oldFile.exists()) {
                    throw NoSuchFileException(oldFile, null, "File does not exist")
                }

                val parent = oldFile.parentFile ?: throw IllegalArgumentException("Invalid path")
                val oldExt = FileUtils.extractExtension(oldFile.name)
                val finalName = if (oldFile.isFile && !oldExt.isNullOrEmpty() && !newName.endsWith(".$oldExt", ignoreCase = true)) {
                    "$newName.$oldExt"
                } else {
                    newName
                }

                val newFile = File(parent, finalName)
                if (newFile.exists()) {
                    throw FileAlreadyExistsException(newFile, null, "Target name already exists")
                }

                if (!oldFile.renameTo(newFile)) {
                    throw IllegalStateException("Failed to rename file")
                }

                scanMediaFiles(arrayOf(oldFile.absolutePath, newFile.absolutePath))
                invalidateCache()
            }
        }

    override suspend fun deleteFiles(paths: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val scannedPaths = mutableListOf<String>()
                for (path in paths) {
                    val file = File(path)
                    if (file.exists()) {
                        scannedPaths.add(file.absolutePath)
                        if (file.isDirectory) {
                            file.deleteRecursively()
                        } else {
                            file.delete()
                        }
                    }
                }
                scanMediaFiles(scannedPaths.toTypedArray())
                invalidateCache()
            }
        }

    override suspend fun copyFile(
        sourcePath: String,
        targetDirectory: String,
        overwrite: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                throw NoSuchFileException(sourceFile, null, "Source file does not exist")
            }

            val targetDir = File(targetDirectory)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = File(targetDir, sourceFile.name)
            if (targetFile.exists()) {
                if (!overwrite) {
                    throw FileAlreadyExistsException(targetFile, null, "Target file already exists")
                }
            }

            if (sourceFile.isDirectory) {
                sourceFile.copyRecursively(targetFile, overwrite = overwrite)
            } else {
                sourceFile.copyTo(targetFile, overwrite = overwrite)
            }

            scanMediaFiles(arrayOf(targetFile.absolutePath))
            invalidateCache()
        }
    }

    override suspend fun moveFile(
        sourcePath: String,
        targetDirectory: String,
        overwrite: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                throw NoSuchFileException(sourceFile, null, "Source file does not exist")
            }

            val targetDir = File(targetDirectory)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = File(targetDir, sourceFile.name)
            if (targetFile.exists()) {
                if (!overwrite) {
                    throw FileAlreadyExistsException(targetFile, null, "Target file already exists")
                }
                if (targetFile.isDirectory) {
                    targetFile.deleteRecursively()
                } else {
                    targetFile.delete()
                }
            }

            val moved = sourceFile.renameTo(targetFile)
            if (!moved) {
                // Fallback to copy and delete if cross-filesystem move
                if (sourceFile.isDirectory) {
                    sourceFile.copyRecursively(targetFile, overwrite = true)
                    sourceFile.deleteRecursively()
                } else {
                    sourceFile.copyTo(targetFile, overwrite = true)
                    sourceFile.delete()
                }
            }

            scanMediaFiles(arrayOf(sourceFile.absolutePath, targetFile.absolutePath))
            invalidateCache()
        }
    }

    override suspend fun createDirectory(parentPath: String, folderName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val targetDir = File(parentPath, folderName)
                if (targetDir.exists()) {
                    throw FileAlreadyExistsException(targetDir, null, "Folder already exists")
                }
                if (!targetDir.mkdirs()) {
                    throw IllegalStateException("Failed to create directory")
                }
                invalidateCache()
            }
        }

    private fun scanMediaFiles(paths: Array<String>) {
        if (paths.isEmpty()) return
        try {
            MediaScannerConnection.scanFile(context, paths, null, null)
        } catch (_: Exception) {
            // Ignore scan failure
        }
    }
}
