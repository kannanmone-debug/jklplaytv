package com.example.storage

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.example.codec.CodecCapabilityInspector
import com.example.data.local.MediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MediaDeletionOutcome(
    val deletedUris: List<String> = emptyList(),
    val failedUris: List<String> = emptyList(),
    val missingUris: List<String> = emptyList(),
    val pendingAuthorizationIntentSender: IntentSender? = null,
    val pendingAuthorizationItems: List<MediaEntity> = emptyList()
)

data class DirectoryListingResult(
    val entries: List<FileSystemEntry>,
    val errorMessage: String? = null
)

data class SafRootEntry(
    val label: String,
    val treeUriString: String
)

data class FileSystemEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedMs: Long,
    val createdTimeMs: Long = lastModifiedMs,
    val childCount: Int = 0,
    val mimeType: String = "",
    val codecBadge: String = "",
    val extension: String = name.substringAfterLast('.', "").uppercase(Locale.US),
    val isSafUri: Boolean = path.startsWith("content://")
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)

    val formattedDate: String
        get() = dateFormatThreadLocal.get()!!.format(Date(lastModifiedMs))

    val formattedCreatedDate: String
        get() = dateFormatThreadLocal.get()!!.format(Date(createdTimeMs))

    companion object {
        private val dateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US)
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

enum class FileSortOption(val label: String) {
    NAME_ASC("Name (A–Z)"),
    DATE_DESC("Newest First"),
    SIZE_DESC("Largest First"),
    TYPE("Type / Format")
}

enum class FileSortField(val label: String) {
    NAME("Name"),
    DATE_MODIFIED("Date Modified"),
    DATE_CREATED("Date Created"),
    FILE_SIZE("File Size"),
    FILE_TYPE("File Type");

    companion object {
        fun fromName(value: String): FileSortField =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NAME
    }
}

enum class FileCategoryFilter(val label: String) {
    AUDIO("Audio"),
    VIDEO("Video"),
    IMAGES("Images"),
    DOCUMENTS("Documents"),
    APK("APK"),
    LARGE_FILES("Large Files"),
    RECENT_FILES("Recent Files"),
    FOLDER("Folder");

    fun matches(entry: FileSystemEntry, nowMs: Long = System.currentTimeMillis()): Boolean {
        val ext = entry.extension.lowercase(Locale.US)
        return when (this) {
            FOLDER -> entry.isDirectory
            AUDIO -> !entry.isDirectory && (
                entry.mimeType.startsWith("audio/") ||
                    ext in CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS
                )
            VIDEO -> !entry.isDirectory && (
                entry.mimeType.startsWith("video/") ||
                    ext in CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS
                )
            IMAGES -> !entry.isDirectory && (
                entry.mimeType.startsWith("image/") ||
                    ext in setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp", "svg", "raw")
                )
            DOCUMENTS -> !entry.isDirectory && (
                entry.mimeType.startsWith("text/") ||
                    entry.mimeType.contains("pdf") ||
                    ext in setOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md", "json", "xml", "csv", "srt", "vtt", "ass", "lrc", "ttml")
                )
            APK -> !entry.isDirectory && (
                ext == "apk" || ext == "apks" || ext == "xapk" ||
                    entry.mimeType == "application/vnd.android.package-archive"
                )
            LARGE_FILES -> !entry.isDirectory && entry.sizeBytes >= 1_048_576L // >= 1 MB
            RECENT_FILES -> (nowMs - entry.lastModifiedMs) <= 7L * 24L * 3600L * 1000L
        }
    }
}

object ScopedFileManager {

    fun parseSafRoots(raw: String): List<SafRootEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split("\n")
            .mapNotNull { line ->
                val parts = line.split("::", limit = 2)
                if (parts.size == 2 && parts[1].isNotBlank()) {
                    SafRootEntry(parts[0].ifBlank { "SAF Folder" }, parts[1])
                } else null
            }
            .distinctBy { it.treeUriString }
    }

    fun encodeSafRoots(roots: List<SafRootEntry>): String {
        return roots.distinctBy { it.treeUriString }
            .joinToString("\n") { "${it.label.replace("::", " ")}::${it.treeUriString}" }
    }

    fun resolveSafTreeLabel(treeUri: Uri): String {
        val last = treeUri.lastPathSegment ?: return "External / USB Drive"
        val clean = last.substringAfterLast(':').substringAfterLast('/').ifBlank { last }
        return if (last.contains("primary", ignoreCase = true)) {
            "SAF: $clean"
        } else {
            "USB / SD: $clean"
        }
    }

    fun defaultRootRoots(context: Context, safRootsRaw: String = ""): List<Pair<String, String>> {
        val roots = mutableListOf<Pair<String, String>>()
        try {
            val extRoot = Environment.getExternalStorageDirectory()
            if (extRoot != null && extRoot.exists()) {
                roots.add("Internal Storage" to normalizePath(extRoot.absolutePath))
            }
        } catch (_: Throwable) {}

        val studioDir = File(context.filesDir, "local_media_library").apply { mkdirs() }
        roots.add("App Studio Media" to normalizePath(studioDir.absolutePath))

        try {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (musicDir != null && musicDir.exists()) {
                roots.add("Music Folder" to normalizePath(musicDir.absolutePath))
            }
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (moviesDir != null && moviesDir.exists()) {
                roots.add("Movies Folder" to normalizePath(moviesDir.absolutePath))
            }
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists()) {
                roots.add("Downloads" to normalizePath(downloadsDir.absolutePath))
            }
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir != null && documentsDir.exists()) {
                roots.add("Documents" to normalizePath(documentsDir.absolutePath))
            }
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (dcimDir != null && dcimDir.exists()) {
                roots.add("DCIM" to normalizePath(dcimDir.absolutePath))
            }
        } catch (_: Throwable) {}

        parseSafRoots(safRootsRaw).forEach { saf ->
            val rootDocUri = buildSafRootDocumentUri(Uri.parse(saf.treeUriString))
            roots.add(saf.label to rootDocUri.toString())
        }

        return roots.distinctBy { it.second }
    }

    fun normalizePath(path: String): String {
        if (path.startsWith("content://")) return path
        val trimmed = path.trim().trimEnd('/')
        return if (trimmed.isEmpty()) "/" else trimmed
    }

    fun buildSafRootDocumentUri(treeUri: Uri): Uri {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        } catch (_: Throwable) {
            treeUri
        }
    }

    suspend fun probeFileDurationMs(context: Context, pathOrUri: String): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            if (pathOrUri.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(pathOrUri))
            } else {
                retriever.setDataSource(pathOrUri.removePrefix("file://"))
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }
    }

    private val systemDirectoryNames = setOf(
        "android", "proc", "sys", "system", "recovery", "cache", "lost+found", "lost.dir"
    )

    private fun buildMediaCountBadge(audio: Int, video: Int): String {
        return when {
            audio > 0 && video > 0 -> "$audio audio, $video video"
            audio > 0 -> "$audio audio"
            video > 0 -> "$video video"
            else -> "Media"
        }
    }

    private fun resolveNonMediaMimeAndBadge(fileName: String): Pair<String, String> {
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        if (ext in CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS || ext in CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS) {
            return CodecCapabilityInspector.resolveMimeAndCodecBadge(fileName, null)
        }
        val guessed = try {
            URLConnection.guessContentTypeFromName(fileName).orEmpty()
        } catch (_: Throwable) {
            ""
        }
        val mime = when {
            guessed.isNotBlank() -> guessed
            ext in setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "bmp", "svg") -> "image/$ext"
            ext == "pdf" -> "application/pdf"
            ext in setOf("txt", "md", "json", "xml", "csv", "srt", "vtt", "ass", "lrc", "ttml", "log") -> "text/plain"
            ext in setOf("apk", "apks", "xapk") -> "application/vnd.android.package-archive"
            else -> "application/octet-stream"
        }
        val badge = ext.uppercase(Locale.US).ifBlank { "FILE" }
        return mime to badge
    }

    private fun resolveIndexedMediaFileName(media: MediaEntity): String {
        val uriTail = if (media.uri.startsWith("file://")) {
            media.uri.removePrefix("file://").substringAfterLast('/')
        } else ""
        if (uriTail.contains('.') && uriTail.isNotBlank()) return uriTail
        val cleanTitle = media.title.ifBlank { if (media.isVideo) "Video" else "Audio" }
        val ext = media.containerFormat.lowercase(Locale.US)
            .takeIf { it.isNotBlank() && it != "AUDIO" && it != "VIDEO" }
            ?: if (media.isVideo) "mp4" else "mp3"
        return if (cleanTitle.endsWith(".$ext", ignoreCase = true)) cleanTitle else "$cleanTitle.$ext"
    }

    /**
     * Synthesizes immediate child files and subdirectories for [directoryPath] using already-indexed
     * MediaStore / Room [indexedMedia] records in O(N_media) memory without slow disk tree crawls.
     */
    fun buildMediaStoreEntriesForDirectory(
        directoryPath: String,
        indexedMedia: List<MediaEntity>,
        showHiddenFiles: Boolean = false,
        sortOption: FileSortOption = FileSortOption.NAME_ASC
    ): List<FileSystemEntry> {
        if (directoryPath.startsWith("content://") || indexedMedia.isEmpty()) return emptyList()
        val targetDir = normalizePath(directoryPath)
        val childPrefix = if (targetDir == "/") "/" else "$targetDir/"

        val directFiles = mutableListOf<FileSystemEntry>()
        // Immediate child folder name -> (audioCount, videoCount, latestTimestampMs)
        val childFolderStats = LinkedHashMap<String, Triple<Int, Int, Long>>()

        for (item in indexedMedia) {
            val rawFolder = item.folderPath.trim()
            if (rawFolder.isEmpty()) continue
            val itemFolder = normalizePath(rawFolder)
            val timestampMs = if (item.dateAddedSec > 0L) item.dateAddedSec * 1000L else item.lastPlayedTimestamp

            if (itemFolder == targetDir) {
                val displayName = resolveIndexedMediaFileName(item)
                if (!showHiddenFiles && displayName.startsWith(".")) continue
                val filePath = if (item.uri.startsWith("file://")) {
                    item.uri.removePrefix("file://")
                } else {
                    item.uri
                }
                val ext = displayName.substringAfterLast('.', item.containerFormat).uppercase(Locale.US)
                directFiles.add(
                    FileSystemEntry(
                        path = filePath,
                        name = displayName,
                        isDirectory = false,
                        sizeBytes = item.sizeBytes,
                        lastModifiedMs = timestampMs,
                        createdTimeMs = timestampMs,
                        mimeType = item.mimeType,
                        codecBadge = item.codecName.ifBlank { ext },
                        extension = ext
                    )
                )
            } else if (itemFolder.startsWith(childPrefix)) {
                val remainder = itemFolder.removePrefix(childPrefix)
                val childName = remainder.substringBefore('/')
                if (childName.isBlank()) continue
                if (!showHiddenFiles && childName.startsWith(".")) continue
                if (childName.lowercase(Locale.US) in systemDirectoryNames) continue

                val prev = childFolderStats[childName]
                val nextAudio = (prev?.first ?: 0) + if (!item.isVideo) 1 else 0
                val nextVideo = (prev?.second ?: 0) + if (item.isVideo) 1 else 0
                val nextTs = maxOf(prev?.third ?: 0L, timestampMs)
                childFolderStats[childName] = Triple(nextAudio, nextVideo, nextTs)
            }
        }

        val childDirs = childFolderStats.map { (folderName, stats) ->
            val (audioCount, videoCount, latestTs) = stats
            val folderPath = if (targetDir == "/") "/$folderName" else "$targetDir/$folderName"
            FileSystemEntry(
                path = folderPath,
                name = folderName,
                isDirectory = true,
                sizeBytes = 0L,
                lastModifiedMs = latestTs,
                createdTimeMs = latestTs,
                childCount = audioCount + videoCount,
                codecBadge = buildMediaCountBadge(audioCount, videoCount),
                extension = "FOLDER"
            )
        }

        return sortEntries((childDirs + directFiles).distinctBy { it.path }, sortOption)
    }

    suspend fun listDirectory(
        directoryPath: String,
        searchQuery: String = "",
        sortOption: FileSortOption = FileSortOption.NAME_ASC,
        showHiddenFiles: Boolean = false,
        context: Context? = null,
        indexedMedia: List<MediaEntity> = emptyList()
    ): List<FileSystemEntry> = listDirectoryWithStatus(
        directoryPath = directoryPath,
        searchQuery = searchQuery,
        sortOption = sortOption,
        showHiddenFiles = showHiddenFiles,
        context = context,
        indexedMedia = indexedMedia
    ).entries

    suspend fun listDirectoryWithStatus(
        directoryPath: String,
        searchQuery: String = "",
        sortOption: FileSortOption = FileSortOption.NAME_ASC,
        showHiddenFiles: Boolean = false,
        context: Context? = null,
        indexedMedia: List<MediaEntity> = emptyList()
    ): DirectoryListingResult = withContext(Dispatchers.IO) {
        currentCoroutineContext().ensureActive()

        if (directoryPath.startsWith("content://") && context != null) {
            return@withContext listSafDirectoryWithStatus(
                context = context,
                directoryUriString = directoryPath,
                searchQuery = searchQuery,
                sortOption = sortOption,
                showHiddenFiles = showHiddenFiles
            )
        }

        val normalizedDir = normalizePath(directoryPath)
        val indexedEntries = buildMediaStoreEntriesForDirectory(
            directoryPath = normalizedDir,
            indexedMedia = indexedMedia,
            showHiddenFiles = showHiddenFiles,
            sortOption = sortOption
        )

        // Precompute immediate subfolder media stats in memory for fast O(1) directory badge lookup
        val childPrefix = if (normalizedDir == "/") "/" else "$normalizedDir/"
        val indexedSubfolderCounts = HashMap<String, Pair<Int, Int>>()
        for (media in indexedMedia) {
            val fPath = normalizePath(media.folderPath)
            if (fPath.startsWith(childPrefix)) {
                val childName = fPath.removePrefix(childPrefix).substringBefore('/')
                if (childName.isNotBlank()) {
                    val childFull = if (normalizedDir == "/") "/$childName" else "$normalizedDir/$childName"
                    val prev = indexedSubfolderCounts[childFull] ?: (0 to 0)
                    indexedSubfolderCounts[childFull] = if (media.isVideo) {
                        prev.first to (prev.second + 1)
                    } else {
                        (prev.first + 1) to prev.second
                    }
                }
            }
        }

        val dir = File(normalizedDir)
        if (!dir.exists() || !dir.isDirectory) {
            val filteredFallback = applyListingSearch(indexedEntries, searchQuery, sortOption)
            return@withContext DirectoryListingResult(entries = filteredFallback, errorMessage = null)
        }

        val rawFiles: Array<File>? = try {
            dir.listFiles()
        } catch (se: SecurityException) {
            if (indexedEntries.isNotEmpty()) {
                return@withContext DirectoryListingResult(
                    entries = applyListingSearch(indexedEntries, searchQuery, sortOption),
                    errorMessage = null
                )
            }
            return@withContext DirectoryListingResult(
                entries = emptyList(),
                errorMessage = se.localizedMessage ?: "Permission denied reading $normalizedDir"
            )
        } catch (t: Throwable) {
            if (indexedEntries.isNotEmpty()) {
                return@withContext DirectoryListingResult(
                    entries = applyListingSearch(indexedEntries, searchQuery, sortOption),
                    errorMessage = null
                )
            }
            return@withContext DirectoryListingResult(
                entries = emptyList(),
                errorMessage = t.localizedMessage ?: "Failed to read folder $normalizedDir"
            )
        }

        if (rawFiles == null) {
            val filteredFallback = applyListingSearch(indexedEntries, searchQuery, sortOption)
            return@withContext DirectoryListingResult(entries = filteredFallback, errorMessage = null)
        }

        val entries = mutableListOf<FileSystemEntry>()
        val seenDiskDirPaths = HashSet<String>()
        val seenDiskFileNamesLower = HashSet<String>()

        for ((index, file) in rawFiles.withIndex()) {
            if (index % 64 == 0) {
                currentCoroutineContext().ensureActive()
            }
            val name = file.name
            if (!showHiddenFiles && name.startsWith(".")) continue

            val modified = file.lastModified()
            if (file.isDirectory) {
                if (name.lowercase(Locale.US) in systemDirectoryNames) continue
                val normChild = normalizePath(file.absolutePath)
                seenDiskDirPaths.add(normChild)

                val indexedCounts = indexedSubfolderCounts[normChild]
                val msAudio = indexedCounts?.first ?: 0
                val msVideo = indexedCounts?.second ?: 0
                val directVisibleCount = try {
                    file.list()?.count { showHiddenFiles || !it.startsWith(".") } ?: 0
                } catch (_: Throwable) {
                    0
                }
                val badge = when {
                    msAudio > 0 || msVideo > 0 -> buildMediaCountBadge(msAudio, msVideo)
                    directVisibleCount > 0 -> "$directVisibleCount items"
                    else -> "Empty"
                }

                entries.add(
                    FileSystemEntry(
                        path = normChild,
                        name = name,
                        isDirectory = true,
                        sizeBytes = 0L,
                        lastModifiedMs = modified,
                        createdTimeMs = modified,
                        childCount = maxOf(directVisibleCount, msAudio + msVideo),
                        codecBadge = badge,
                        extension = "FOLDER"
                    )
                )
            } else if (file.isFile) {
                seenDiskFileNamesLower.add(name.lowercase(Locale.US))
                val (mime, badge) = resolveNonMediaMimeAndBadge(name)
                val ext = name.substringAfterLast('.', badge).uppercase(Locale.US)
                entries.add(
                    FileSystemEntry(
                        path = file.absolutePath,
                        name = name,
                        isDirectory = false,
                        sizeBytes = file.length(),
                        lastModifiedMs = modified,
                        createdTimeMs = modified,
                        mimeType = mime,
                        codecBadge = badge,
                        extension = ext
                    )
                )
            }
        }

        // Merge MediaStore-indexed items/folders not already present on direct disk listing
        for (indexed in indexedEntries) {
            if (indexed.isDirectory) {
                if (normalizePath(indexed.path) !in seenDiskDirPaths) {
                    entries.add(indexed)
                }
            } else {
                if (indexed.name.lowercase(Locale.US) !in seenDiskFileNamesLower) {
                    entries.add(indexed)
                }
            }
        }

        DirectoryListingResult(
            entries = applyListingSearch(entries.distinctBy { it.path }, searchQuery, sortOption),
            errorMessage = null
        )
    }

    private fun applyListingSearch(
        entries: List<FileSystemEntry>,
        searchQuery: String,
        sortOption: FileSortOption
    ): List<FileSystemEntry> {
        val filtered = if (searchQuery.isBlank()) {
            entries
        } else {
            entries.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        return sortEntries(filtered, sortOption)
    }

    private suspend fun listSafDirectoryWithStatus(
        context: Context,
        directoryUriString: String,
        searchQuery: String,
        sortOption: FileSortOption,
        showHiddenFiles: Boolean
    ): DirectoryListingResult {
        val entries = mutableListOf<FileSystemEntry>()
        try {
            currentCoroutineContext().ensureActive()
            val dirUri = Uri.parse(directoryUriString)
            val docId = if (DocumentsContract.isDocumentUri(context, dirUri)) {
                DocumentsContract.getDocumentId(dirUri)
            } else {
                DocumentsContract.getTreeDocumentId(dirUri)
            }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                var rowIdx = 0
                while (cursor.moveToNext()) {
                    if (rowIdx++ % 64 == 0) {
                        currentCoroutineContext().ensureActive()
                    }
                    val childId = if (idCol >= 0) cursor.getString(idCol) else continue
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: childId else childId
                    if (!showHiddenFiles && name.startsWith(".")) continue
                    if (searchQuery.isNotBlank() && !name.contains(searchQuery, ignoreCase = true)) continue

                    val rawMime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    val isDir = rawMime == DocumentsContract.Document.MIME_TYPE_DIR
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val modified = if (modCol >= 0) cursor.getLong(modCol).coerceAtLeast(0L) else System.currentTimeMillis()
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(dirUri, childId).toString()

                    if (isDir) {
                        entries.add(
                            FileSystemEntry(
                                path = childUri,
                                name = name,
                                isDirectory = true,
                                sizeBytes = 0L,
                                lastModifiedMs = modified,
                                createdTimeMs = modified,
                                childCount = 0,
                                codecBadge = "SAF DIR",
                                extension = "FOLDER",
                                isSafUri = true
                            )
                        )
                    } else {
                        val (mime, badge) = CodecCapabilityInspector.resolveMimeAndCodecBadge(name, rawMime)
                        entries.add(
                            FileSystemEntry(
                                path = childUri,
                                name = name,
                                isDirectory = false,
                                sizeBytes = size,
                                lastModifiedMs = modified,
                                createdTimeMs = modified,
                                mimeType = mime,
                                codecBadge = badge,
                                extension = name.substringAfterLast('.', badge).uppercase(Locale.US),
                                isSafUri = true
                            )
                        )
                    }
                }
            }
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            return DirectoryListingResult(
                entries = emptyList(),
                errorMessage = t.localizedMessage ?: "Unable to read SAF folder"
            )
        }
        return DirectoryListingResult(entries = sortEntries(entries, sortOption), errorMessage = null)
    }

    private fun sortEntries(
        entries: List<FileSystemEntry>,
        sortOption: FileSortOption
    ): List<FileSystemEntry> {
        return when (sortOption) {
            FileSortOption.NAME_ASC -> entries.sortedWith(
                compareByDescending<FileSystemEntry> { it.isDirectory }.thenBy { it.name.lowercase() }
            )
            FileSortOption.DATE_DESC -> entries.sortedWith(
                compareByDescending<FileSystemEntry> { it.isDirectory }.thenByDescending { it.lastModifiedMs }
            )
            FileSortOption.SIZE_DESC -> entries.sortedWith(
                compareByDescending<FileSystemEntry> { it.isDirectory }.thenByDescending { it.sizeBytes }
            )
            FileSortOption.TYPE -> entries.sortedWith(
                compareByDescending<FileSystemEntry> { it.isDirectory }.thenBy { it.codecBadge }.thenBy { it.name.lowercase() }
            )
        }
    }

    fun filterAndSortEntries(
        entries: List<FileSystemEntry>,
        searchQuery: String,
        activeCategories: Set<FileCategoryFilter>,
        sortField: FileSortField,
        ascending: Boolean
    ): List<FileSystemEntry> {
        val nowMs = System.currentTimeMillis()
        val filtered = entries.filter { entry ->
            val matchesQuery = searchQuery.isBlank() ||
                entry.name.contains(searchQuery, ignoreCase = true) ||
                entry.codecBadge.contains(searchQuery, ignoreCase = true) ||
                entry.extension.contains(searchQuery, ignoreCase = true)
            val matchesCategory = if (activeCategories.isEmpty()) {
                true
            } else {
                activeCategories.any { it.matches(entry, nowMs) }
            }
            matchesQuery && matchesCategory
        }

        val fieldComparator: Comparator<FileSystemEntry> = when (sortField) {
            FileSortField.NAME -> compareBy { it.name.lowercase(Locale.US) }
            FileSortField.DATE_MODIFIED -> compareBy<FileSystemEntry> { it.lastModifiedMs }.thenBy { it.name.lowercase(Locale.US) }
            FileSortField.DATE_CREATED -> compareBy<FileSystemEntry> { it.createdTimeMs }.thenBy { it.name.lowercase(Locale.US) }
            FileSortField.FILE_SIZE -> compareBy<FileSystemEntry> { it.sizeBytes }.thenBy { it.name.lowercase(Locale.US) }
            FileSortField.FILE_TYPE -> compareBy<FileSystemEntry> { it.codecBadge.lowercase(Locale.US) }
                .thenBy { it.extension.lowercase(Locale.US) }
                .thenBy { it.name.lowercase(Locale.US) }
        }

        val directionalComparator = if (ascending) fieldComparator else fieldComparator.reversed()
        return filtered.sortedWith(
            compareByDescending<FileSystemEntry> { it.isDirectory }.then(directionalComparator)
        )
    }

    suspend fun renameFileOrFolder(
        sourcePath: String,
        newName: String,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanName = newName.trim()
            if (cleanName.isEmpty() || cleanName.contains("/")) return@withContext false
            if (sourcePath.startsWith("content://") && context != null) {
                val renamed = DocumentsContract.renameDocument(
                    context.contentResolver,
                    Uri.parse(sourcePath),
                    cleanName
                )
                return@withContext renamed != null
            }
            val src = File(sourcePath)
            if (!src.exists()) return@withContext false
            val dest = File(src.parentFile, cleanName)
            src.renameTo(dest)
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun deletePaths(
        paths: Collection<String>,
        context: Context? = null
    ): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        for (p in paths) {
            try {
                if (p.startsWith("content://") && context != null) {
                    if (DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(p))) {
                        deletedCount++
                    }
                } else {
                    val target = File(p)
                    if (target.exists() && target.deleteRecursively()) {
                        deletedCount++
                    }
                }
            } catch (_: Throwable) {}
        }
        deletedCount
    }

    suspend fun copyOrMoveFile(
        sourcePath: String,
        targetDirectoryPath: String,
        moveInsteadOfCopy: Boolean,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val isSrcSaf = sourcePath.startsWith("content://")
            val isDestSaf = targetDirectoryPath.startsWith("content://")

            if (!isSrcSaf && !isDestSaf) {
                val src = File(sourcePath)
                val targetDir = File(targetDirectoryPath).apply { mkdirs() }
                if (!src.exists() || !targetDir.isDirectory) return@withContext false
                val dest = File(targetDir, src.name)
                if (src.absolutePath == dest.absolutePath) return@withContext true

                src.copyTo(dest, overwrite = true)
                if (moveInsteadOfCopy) {
                    src.delete()
                }
                return@withContext true
            }

            if (context == null) return@withContext false
            val resolver = context.contentResolver
            val srcName = if (isSrcSaf) {
                querySafDisplayName(context, Uri.parse(sourcePath)) ?: "copied_file"
            } else {
                File(sourcePath).name
            }
            val (mime, _) = CodecCapabilityInspector.resolveMimeAndCodecBadge(srcName, null)

            val inputStream = if (isSrcSaf) {
                resolver.openInputStream(Uri.parse(sourcePath))
            } else {
                File(sourcePath).takeIf { it.exists() && it.isFile }?.inputStream()
            } ?: return@withContext false

            val copied = if (isDestSaf) {
                val destParentUri = Uri.parse(targetDirectoryPath)
                val createdUri = DocumentsContract.createDocument(resolver, destParentUri, mime, srcName)
                    ?: return@withContext false
                inputStream.use { ins ->
                    resolver.openOutputStream(createdUri)?.use { outs ->
                        ins.copyTo(outs)
                    }
                }
                true
            } else {
                val destDir = File(targetDirectoryPath).apply { mkdirs() }
                val destFile = File(destDir, srcName)
                inputStream.use { ins ->
                    destFile.outputStream().use { outs ->
                        ins.copyTo(outs)
                    }
                }
                true
            }

            if (copied && moveInsteadOfCopy) {
                if (isSrcSaf) {
                    DocumentsContract.deleteDocument(resolver, Uri.parse(sourcePath))
                } else {
                    File(sourcePath).delete()
                }
            }
            copied
        } catch (_: Throwable) {
            false
        }
    }

    private fun querySafDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun createSubfolder(
        parentPath: String,
        folderName: String,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val clean = folderName.trim()
            if (clean.isEmpty()) return@withContext false
            if (parentPath.startsWith("content://") && context != null) {
                val created = DocumentsContract.createDocument(
                    context.contentResolver,
                    Uri.parse(parentPath),
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    clean
                )
                return@withContext created != null
            }
            val folder = File(parentPath, clean)
            folder.mkdirs() || folder.exists()
        } catch (_: Throwable) {
            false
        }
    }

    @Volatile
    var customDeleteRequestFactory: ((ContentResolver, List<Uri>) -> IntentSender?)? = null

    @Volatile
    var customDirectDeleteInterceptor: ((Context, MediaEntity, Uri?) -> Int)? = null

    private fun invalidateCache(folderPath: String) {
        // Folder entries cache is refreshed in BnjMainViewModel after deletion
    }

    fun resolvePhysicalFile(media: MediaEntity): File? {
        val raw = media.uri.trim()
        if (raw.startsWith("file://")) {
            val path = Uri.parse(raw).path
            if (!path.isNullOrBlank()) return File(path)
        } else if (raw.startsWith("/")) {
            return File(raw)
        }
        return null
    }

    fun resolveMediaStoreAudioUri(context: Context, media: MediaEntity): Uri? {
        val raw = media.uri.trim()
        if (raw.startsWith("content://")) {
            return Uri.parse(raw)
        }
        if (media.mediaStoreId > 0L) {
            val baseUri = if (media.isVideo) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            return ContentUris.withAppendedId(baseUri, media.mediaStoreId)
        }
        val physical = resolvePhysicalFile(media)
        if (physical != null) {
            try {
                val collection = if (media.isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                context.contentResolver.query(
                    collection,
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.DATA} = ?",
                    arrayOf(physical.absolutePath),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(0)
                        if (id > 0L) {
                            return ContentUris.withAppendedId(collection, id)
                        }
                    }
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    fun existsOnDevice(context: Context, media: MediaEntity): Boolean {
        val physical = resolvePhysicalFile(media)
        if (physical != null) {
            return physical.exists()
        }
        val contentUri = resolveMediaStoreAudioUri(context, media) ?: return false
        return try {
            context.contentResolver.query(
                contentUri,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (_: Throwable) {
            false
        }
    }

    fun createMediaStoreDeleteIntentSender(
        context: Context,
        uris: List<Uri>
    ): IntentSender? {
        if (uris.isEmpty()) return null
        customDeleteRequestFactory?.invoke(context.contentResolver, uris)?.let { return it }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            } catch (_: Throwable) {
                try {
                    val fallbackIntent = Intent("android.provider.action.MANAGE_MEDIA_DELETE").apply {
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    }
                    PendingIntent.getActivity(
                        context,
                        2048,
                        fallbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )?.intentSender
                } catch (_: Throwable) {
                    null
                }
            }
        } else {
            null
        }
    }

    suspend fun deleteMediaItemsFromDevice(
        context: Context,
        items: List<MediaEntity>,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): MediaDeletionOutcome = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext MediaDeletionOutcome()

        val deletedUris = mutableListOf<String>()
        val failedUris = mutableListOf<String>()
        val missingUris = mutableListOf<String>()
        val needsRDeleteRequest = mutableListOf<Pair<MediaEntity, Uri>>()
        var recoverableIntentSender: IntentSender? = null
        val pendingRecoverableItems = mutableListOf<MediaEntity>()

        for (media in items) {
            val physical = resolvePhysicalFile(media)
            val contentUri = resolveMediaStoreAudioUri(context, media)

            val interceptor = customDirectDeleteInterceptor
            if (interceptor != null) {
                try {
                    val rows = interceptor(context, media, contentUri)
                    if (rows > 0) {
                        physical?.takeIf { it.exists() }?.delete()
                        deletedUris.add(media.uri)
                        invalidateCache(media.folderPath)
                    } else if (rows == 0) {
                        if (!existsOnDevice(context, media)) {
                            missingUris.add(media.uri)
                            deletedUris.add(media.uri)
                        } else if (sdkInt >= Build.VERSION_CODES.R && contentUri != null) {
                            needsRDeleteRequest.add(media to contentUri)
                        } else {
                            failedUris.add(media.uri)
                        }
                    } else {
                        failedUris.add(media.uri)
                    }
                    continue
                } catch (sec: SecurityException) {
                    if (sdkInt >= Build.VERSION_CODES.R && contentUri != null) {
                        needsRDeleteRequest.add(media to contentUri)
                    } else if (sdkInt >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
                        if (recoverableIntentSender == null) {
                            recoverableIntentSender = sec.userAction.actionIntent.intentSender
                        }
                        pendingRecoverableItems.add(media)
                    } else {
                        failedUris.add(media.uri)
                    }
                    continue
                }
            }

            if (physical != null) {
                if (!physical.exists()) {
                    if (contentUri != null) {
                        try {
                            context.contentResolver.delete(contentUri, null, null)
                        } catch (_: Throwable) {}
                    }
                    missingUris.add(media.uri)
                    deletedUris.add(media.uri)
                    invalidateCache(media.folderPath)
                    continue
                }

                var securityErr: SecurityException? = null
                if (contentUri != null && media.mediaStoreId > 0L) {
                    try {
                        context.contentResolver.delete(contentUri, null, null)
                    } catch (sec: SecurityException) {
                        securityErr = sec
                    } catch (_: Throwable) {}
                }

                val fileDeleted = try {
                    physical.delete() || !physical.exists()
                } catch (_: Throwable) {
                    false
                }

                if (fileDeleted && !physical.exists()) {
                    deletedUris.add(media.uri)
                    invalidateCache(media.folderPath)
                    invalidateCache(physical.parent ?: "")
                } else if (securityErr != null) {
                    if (sdkInt >= Build.VERSION_CODES.R && contentUri != null) {
                        needsRDeleteRequest.add(media to contentUri)
                    } else if (sdkInt >= Build.VERSION_CODES.Q && securityErr is RecoverableSecurityException) {
                        if (recoverableIntentSender == null) {
                            recoverableIntentSender = securityErr.userAction.actionIntent.intentSender
                        }
                        pendingRecoverableItems.add(media)
                    } else {
                        failedUris.add(media.uri)
                    }
                } else if (sdkInt >= Build.VERSION_CODES.R && contentUri != null) {
                    needsRDeleteRequest.add(media to contentUri)
                } else {
                    failedUris.add(media.uri)
                }
                continue
            }

            if (contentUri != null) {
                if (DocumentsContract.isDocumentUri(context, contentUri)) {
                    val docDeleted = try {
                        DocumentsContract.deleteDocument(context.contentResolver, contentUri)
                    } catch (_: Throwable) {
                        false
                    }
                    if (docDeleted) {
                        deletedUris.add(media.uri)
                        invalidateCache(media.folderPath)
                    } else {
                        failedUris.add(media.uri)
                    }
                    continue
                }

                try {
                    val rows = context.contentResolver.delete(contentUri, null, null)
                    if (rows > 0) {
                        deletedUris.add(media.uri)
                        invalidateCache(media.folderPath)
                    } else {
                        val stillExists = existsOnDevice(context, media)
                        if (!stillExists) {
                            missingUris.add(media.uri)
                            deletedUris.add(media.uri)
                            invalidateCache(media.folderPath)
                        } else if (sdkInt >= Build.VERSION_CODES.R) {
                            needsRDeleteRequest.add(media to contentUri)
                        } else {
                            failedUris.add(media.uri)
                        }
                    }
                } catch (sec: SecurityException) {
                    if (sdkInt >= Build.VERSION_CODES.R) {
                        needsRDeleteRequest.add(media to contentUri)
                    } else if (sdkInt >= Build.VERSION_CODES.Q && sec is RecoverableSecurityException) {
                        if (recoverableIntentSender == null) {
                            recoverableIntentSender = sec.userAction.actionIntent.intentSender
                        }
                        pendingRecoverableItems.add(media)
                    } else {
                        failedUris.add(media.uri)
                    }
                } catch (_: Throwable) {
                    failedUris.add(media.uri)
                }
            } else {
                missingUris.add(media.uri)
                deletedUris.add(media.uri)
            }
        }

        if (needsRDeleteRequest.isNotEmpty() && sdkInt >= Build.VERSION_CODES.R) {
            val uris = needsRDeleteRequest.map { it.second }
            val sender = createMediaStoreDeleteIntentSender(context, uris)
            if (sender != null) {
                return@withContext MediaDeletionOutcome(
                    deletedUris = deletedUris,
                    failedUris = failedUris,
                    missingUris = missingUris,
                    pendingAuthorizationIntentSender = sender,
                    pendingAuthorizationItems = needsRDeleteRequest.map { it.first }
                )
            } else {
                failedUris.addAll(needsRDeleteRequest.map { it.first.uri })
            }
        } else if (recoverableIntentSender != null && pendingRecoverableItems.isNotEmpty()) {
            return@withContext MediaDeletionOutcome(
                deletedUris = deletedUris,
                failedUris = failedUris,
                missingUris = missingUris,
                pendingAuthorizationIntentSender = recoverableIntentSender,
                pendingAuthorizationItems = pendingRecoverableItems
            )
        }

        MediaDeletionOutcome(
            deletedUris = deletedUris,
            failedUris = failedUris,
            missingUris = missingUris
        )
    }

    suspend fun finalizeAuthorizedMediaDeletion(
        context: Context,
        authorizedItems: List<MediaEntity>
    ): MediaDeletionOutcome = withContext(Dispatchers.IO) {
        val deletedUris = mutableListOf<String>()
        val failedUris = mutableListOf<String>()
        val missingUris = mutableListOf<String>()

        for (media in authorizedItems) {
            val physical = resolvePhysicalFile(media)
            val contentUri = resolveMediaStoreAudioUri(context, media)
            try {
                if (contentUri != null) {
                    try {
                        context.contentResolver.delete(contentUri, null, null)
                    } catch (_: Throwable) {}
                }
                if (physical != null && physical.exists()) {
                    try {
                        physical.delete()
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {}

            if (!existsOnDevice(context, media)) {
                deletedUris.add(media.uri)
                invalidateCache(media.folderPath)
                physical?.parent?.let { invalidateCache(it) }
            } else {
                failedUris.add(media.uri)
            }
        }

        MediaDeletionOutcome(
            deletedUris = deletedUris,
            failedUris = failedUris,
            missingUris = missingUris
        )
    }
}
