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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of [LocalMediaRepository] using MediaStore and local file system access.
 * Manages video metadata caching, external subtitle discovery, reactive media updates,
 * and file management operations including file copying, moving, renaming, and deletion.
 */
class LocalMediaRepositoryImpl(
    private val context: Context
) : LocalMediaRepository {

    // Primary memory caches for video items and folder directory structures
    private var allVideosCache: List<MediaFile.Video>? = null
    private var videosByParentCache: Map<String, List<MediaFile.Video>> = emptyMap()
    private var allParentPathsCache: Set<String> = emptySet()

    // Secondary memory caches for expensive disk-based metadata
    private val subtitleCache = ConcurrentHashMap<String, List<MediaFile.Subtitle>>()

    // Key format: folderPath + "|" + showSubtitles + "|" + showHidden
    private val folderItemCountCache = ConcurrentHashMap<String, Int>()

    @Volatile
    private var isCacheDirty = true

    // Shared flow used to notify observers when local media cache is invalidated
    private val _updateEvent = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        // Register ContentObserver to listen for MediaStore video changes on external storage
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

    /**
     * Clears all in-memory caches and notifies active reactive streams to re-emit fresh data.
     */
    private fun invalidateCache() {
        isCacheDirty = true
        allVideosCache = null
        videosByParentCache = emptyMap()
        allParentPathsCache = emptySet()
        subtitleCache.clear()
        folderItemCountCache.clear()
        _updateEvent.tryEmit(Unit)
    }

    /**
     * Ensures all local video files are loaded from MediaStore and indexed into memory caches.
     */
    private suspend fun ensureVideosLoaded(force: Boolean = false): List<MediaFile.Video> =
        withContext(Dispatchers.IO) {
            if (force || isCacheDirty || allVideosCache == null) {
                val all = queryVideos()
                allVideosCache = all
                val grouped = all.groupBy { File(it.path).parent ?: "" }
                videosByParentCache = grouped
                allParentPathsCache = grouped.keys

                if (force) {
                    subtitleCache.clear()
                    folderItemCountCache.clear()
                }

                isCacheDirty = false
            }
            allVideosCache!!
        }

    /**
     * Fetches media items (videos, subfolders, and subtitles) for a specified local path.
     */
    override suspend fun getMediaFiles(
        path: String?,
        options: MediaOptions,
        forceRefresh: Boolean
    ): List<MediaFile> {
        ensureVideosLoaded(forceRefresh)

        val items = if (path == null) {
            // Root view: Return parent folders that contain indexed video files
            allParentPathsCache.asSequence()
                .filter { it.isNotEmpty() }
                .map { folderPath -> createFolderItem(folderPath, options) }
                .toList()
        } else {
            val folderItems = mutableListOf<MediaFile>()

            // Add video files located directly in this folder
            videosByParentCache[path]?.let { folderItems.addAll(it) }

            // Discover and add immediate sub-folders derived from indexed video paths
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

            // Add external subtitle files via disk scan if enabled
            val subtitles = getOrScanSubtitles(path)
            if (options.showSubtitles) {
                folderItems.addAll(subtitles)
            }

            // Associate matching subtitles with corresponding videos in this directory
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

        return sortItems(items, options)
    }

    /**
     * Observes media files at a specific path as a reactive flow, re-emitting whenever caches invalidate.
     */
    override fun observeMediaFiles(
        path: String?,
        options: MediaOptions,
        forceRefresh: Boolean
    ): Flow<List<MediaFile>> = flow {
        emit(getMediaFiles(path, options, forceRefresh))
        _updateEvent.collect {
            emit(getMediaFiles(path, options, false))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Sorts media files based on current user options such as folder order, sort type, and direction.
     */
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

    /**
     * Searches for video files matching the search query under the specified path prefix.
     */
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

    /**
     * Constructs a folder item with cached item counts.
     */
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

    /**
     * Calculates the total item count (subfolders, videos, subtitles) for a folder with caching.
     */
    private fun getOrCalculateItemCount(folderPath: String, options: MediaOptions): Int {
        val cacheKey = "$folderPath|${options.showSubtitles}|${options.showHidden}"
        return folderItemCountCache.getOrPut(cacheKey) {
            val subFoldersCount = allParentPathsCache.count {
                it.startsWith(folderPath) && it != folderPath &&
                        it.removePrefix(folderPath).removePrefix("/")
                            .substringBefore('/') == it.removePrefix(folderPath).removePrefix("/")
            }

            val videosCount = videosByParentCache[folderPath]?.size ?: 0

            val subtitlesCount = if (options.showSubtitles) {
                val subs = getOrScanSubtitles(folderPath)
                if (options.showHidden) subs.size else subs.count { !it.name.startsWith(".") }
            } else 0

            subFoldersCount + videosCount + subtitlesCount
        }
    }

    /**
     * Scans local directory for subtitle files matching the base names of videos in that directory.
     */
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

    /**
     * Queries local video items directly from MediaStore ContentProvider.
     */
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

    /**
     * Renames a file or directory on local disk and updates system MediaScanner.
     * Supports case-sensitive renaming and handles case-only renames (e.g., "file.txt" to "File.txt").
     */
    override suspend fun renameFile(path: String, newName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val oldFile = File(path)
                if (!oldFile.exists()) {
                    throw NoSuchFileException(oldFile, null, "File does not exist")
                }

                val parent = oldFile.parentFile ?: throw IllegalArgumentException("Invalid path")
                val oldExt = FileUtils.extractExtension(oldFile.name)
                val newExt = FileUtils.extractExtension(newName)

                // Preserve original extension if newName does not specify an extension for files
                val finalName =
                    if (oldFile.isFile && !oldExt.isNullOrEmpty() && newExt.isNullOrEmpty()) {
                        "$newName.$oldExt"
                    } else {
                        newName
                    }

                val newFile = File(parent, finalName)
                val isCaseOnlyRename =
                    oldFile.name.equals(finalName, ignoreCase = true) && oldFile.name != finalName

                if (newFile.exists() && !isCaseOnlyRename) {
                    throw FileAlreadyExistsException(newFile, null, "Target name already exists")
                }

                var success = false
                if (isCaseOnlyRename) {
                    // Force a two-step rename for case-only changes to bypass FUSE/case-folding filesystem no-ops
                    val tempFile = File(parent, "${oldFile.name}.tmp_${System.currentTimeMillis()}")
                    if (oldFile.renameTo(tempFile)) {
                        success = tempFile.renameTo(newFile)
                        if (!success) {
                            tempFile.renameTo(oldFile)
                        }
                    }
                } else {
                    success = oldFile.renameTo(newFile)
                }

                if (!success) {
                    throw IllegalStateException("Failed to rename file")
                }

                scanMediaFiles(arrayOf(oldFile.absolutePath, newFile.absolutePath))
                invalidateCache()
            }
        }

    /**
     * Recursively deletes specified files or directories from local disk.
     */
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

    /**
     * Copies a source file or directory to a target directory.
     */
    override suspend fun copyFile(
        sourcePath: String,
        targetDirectory: String,
        overwrite: Boolean,
        onProgress: ((bytesWritten: Long) -> Unit)?
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
            copyFileWithProgress(sourceFile, targetFile, overwrite, onProgress)

            scanMediaFiles(arrayOf(targetFile.absolutePath))
            invalidateCache()
        }
    }

    /**
     * Moves a source file or directory to a target directory.
     * Merges directory contents if target directory already exists.
     */
    override suspend fun moveFile(
        sourcePath: String,
        targetDirectory: String,
        overwrite: Boolean,
        onProgress: ((bytesWritten: Long) -> Unit)?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                throw NoSuchFileException(sourceFile, null, "Source file does not exist")
            }

            val targetDir = File(targetDirectory)
            if (sourceFile.isDirectory) {
                val sourceCanonical =
                    runCatching { sourceFile.canonicalPath }.getOrDefault(sourceFile.absolutePath)
                val targetDirCanonical =
                    runCatching { targetDir.canonicalPath }.getOrDefault(targetDir.absolutePath)
                if (targetDirCanonical == sourceCanonical || targetDirCanonical.startsWith(
                        sourceCanonical + File.separator
                    )
                ) {
                    throw IllegalArgumentException("Cannot move a directory into itself or one of its subdirectories")
                }
            }

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetFile = File(targetDir, sourceFile.name)

            if (sourceFile.isDirectory && targetFile.exists() && targetFile.isDirectory) {
                if (!overwrite) {
                    throw FileAlreadyExistsException(targetFile, null, "Target file already exists")
                }
                moveDirectoryContents(sourceFile, targetFile, true, onProgress)
            } else {
                if (targetFile.exists()) {
                    if (!overwrite) {
                        throw FileAlreadyExistsException(
                            targetFile,
                            null,
                            "Target file already exists"
                        )
                    }
                    if (targetFile.isDirectory) {
                        targetFile.deleteRecursively()
                    } else {
                        targetFile.delete()
                    }
                }

                val moved = sourceFile.renameTo(targetFile)
                if (moved) {
                    val size = if (sourceFile.isFile) sourceFile.length() else sourceFile.walk()
                        .filter { it.isFile }.sumOf { it.length() }
                    onProgress?.invoke(size)
                } else {
                    copyFileWithProgress(
                        sourceFile,
                        targetFile,
                        overwrite = true,
                        onProgress = onProgress
                    )
                    if (sourceFile.isDirectory) {
                        sourceFile.deleteRecursively()
                    } else {
                        sourceFile.delete()
                    }
                }
            }

            scanMediaFiles(arrayOf(sourceFile.absolutePath, targetFile.absolutePath))
            invalidateCache()
        }
    }

    /**
     * Recursively moves the contents of a directory into a target directory,
     * merging subfolders and overwriting individual file conflicts.
     */
    private fun moveDirectoryContents(
        sourceDir: File,
        targetDir: File,
        overwrite: Boolean,
        onProgress: ((bytesWritten: Long) -> Unit)?
    ) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val files = sourceDir.listFiles() ?: return
        for (file in files) {
            val dest = File(targetDir, file.name)
            if (file.isDirectory) {
                if (dest.exists()) {
                    if (dest.isDirectory) {
                        moveDirectoryContents(file, dest, overwrite, onProgress)
                    } else {
                        if (!overwrite) {
                            throw FileAlreadyExistsException(dest, null, "Target already exists")
                        }
                        dest.delete()
                        val moved = file.renameTo(dest)
                        if (!moved) {
                            copyFileWithProgress(
                                file,
                                dest,
                                overwrite = true,
                                onProgress = onProgress
                            )
                            file.deleteRecursively()
                        }
                    }
                } else {
                    val moved = file.renameTo(dest)
                    if (moved) {
                        val size = file.walk().filter { it.isFile }.sumOf { it.length() }
                        onProgress?.invoke(size)
                    } else {
                        copyFileWithProgress(file, dest, overwrite = true, onProgress = onProgress)
                        file.deleteRecursively()
                    }
                }
            } else {
                if (dest.exists()) {
                    if (!overwrite) {
                        throw FileAlreadyExistsException(dest, null, "Target file already exists")
                    }
                    if (dest.isDirectory) {
                        dest.deleteRecursively()
                    } else {
                        dest.delete()
                    }
                }
                val moved = file.renameTo(dest)
                if (moved) {
                    onProgress?.invoke(file.length())
                } else {
                    copyFileWithProgress(file, dest, overwrite = true, onProgress = onProgress)
                    file.delete()
                }
            }
        }

        val remaining = sourceDir.listFiles()
        if (remaining.isNullOrEmpty()) {
            sourceDir.delete()
        }
    }

    /**
     * Copies a file or directory with progress reporting.
     * Guarantees prevention of infinite recursion when copying a directory into its descendant.
     */
    private fun copyFileWithProgress(
        source: File,
        target: File,
        overwrite: Boolean,
        onProgress: ((bytesWritten: Long) -> Unit)?,
        rootTarget: File = target
    ) {
        val sourceCanonical = runCatching { source.canonicalPath }.getOrDefault(source.absolutePath)
        val rootTargetCanonical =
            runCatching { rootTarget.canonicalPath }.getOrDefault(rootTarget.absolutePath)

        if (sourceCanonical == rootTargetCanonical || sourceCanonical.startsWith(rootTargetCanonical + File.separator)) {
            return
        }

        if (target.exists()) {
            if (!overwrite) {
                throw FileAlreadyExistsException(target, null, "Target file already exists")
            }
            if (source.isFile || target.isFile) {
                if (target.isDirectory) {
                    target.deleteRecursively()
                } else {
                    target.delete()
                }
            }
        }

        if (source.isDirectory) {
            target.mkdirs()
            val files = source.listFiles() ?: return
            for (file in files) {
                val subTarget = File(target, file.name)
                copyFileWithProgress(file, subTarget, overwrite, onProgress, rootTarget)
            }
        } else {
            val parent = target.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            try {
                source.inputStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(128 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } >= 0) {
                            onProgress?.invoke(bytesRead.toLong())
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                target.setLastModified(source.lastModified())
            } catch (e: Exception) {
                if (target.exists()) {
                    target.delete()
                }
                throw e
            }
        }
    }

    /**
     * Creates a new subfolder under the specified parent directory.
     */
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

    /**
     * Triggers MediaScannerConnection to update Android's MediaStore database for modified paths.
     */
    private fun scanMediaFiles(paths: Array<String>) {
        if (paths.isEmpty()) return
        try {
            MediaScannerConnection.scanFile(context, paths, null, null)
        } catch (_: Exception) {
            // Ignore scan failure
        }
    }
}
