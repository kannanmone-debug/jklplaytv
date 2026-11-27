package com.example.scanner

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.codec.CodecCapabilityInspector
import com.example.data.local.MediaEntity
import com.example.storage.ScopedFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.sin

data class StorageOverviewInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val audioCount: Int,
    val videoCount: Int,
    val folderCount: Int
) {
    val usedFraction: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class DuplicateMediaGroup(
    val signatureKey: String,
    val title: String,
    val sizeBytes: Long,
    val items: List<MediaEntity>
)

data class MediaScanResult(
    val discoveredItems: List<MediaEntity>,
    val staleUrisToRemove: List<String>,
    val duplicateGroups: List<DuplicateMediaGroup>
)

object LocalMediaScanner {

    fun requiredMediaPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun hasMediaPermissions(context: Context): Boolean {
        return requiredMediaPermissions().any { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPartialVisualAccessOnly(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val partialGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED
        val fullVideoGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
        return partialGranted && !fullVideoGranted
    }

    fun queryStorageOverview(
        audioCount: Int,
        videoCount: Int,
        folderCount: Int
    ): StorageOverviewInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = (total - free).coerceAtLeast(0L)
            StorageOverviewInfo(
                totalBytes = total,
                usedBytes = used,
                freeBytes = free,
                audioCount = audioCount,
                videoCount = videoCount,
                folderCount = folderCount
            )
        } catch (_: Throwable) {
            StorageOverviewInfo(
                totalBytes = 128L * 1024 * 1024 * 1024,
                usedBytes = 42L * 1024 * 1024 * 1024,
                freeBytes = 86L * 1024 * 1024 * 1024,
                audioCount = audioCount,
                videoCount = videoCount,
                folderCount = folderCount
            )
        }
    }

    /**
     * Checks whether a MediaEntity's resolved extension is enabled under the user's current
     * audio/video format preferences (case-insensitive).
     */
    fun isMediaEntityExtensionEnabled(
        entity: MediaEntity,
        enabledAudioExtensions: Set<String>,
        enabledVideoExtensions: Set<String>
    ): Boolean {
        val normAudio = enabledAudioExtensions.map { CodecCapabilityInspector.normalizeExtension(it) }.toSet()
        val normVideo = enabledVideoExtensions.map { CodecCapabilityInspector.normalizeExtension(it) }.toSet()
        return isMediaEntityExtensionEnabledNormalized(entity, normAudio, normVideo)
    }

    fun isMediaEntityExtensionEnabledNormalized(
        entity: MediaEntity,
        normalizedAudioExtensions: Set<String>,
        normalizedVideoExtensions: Set<String>
    ): Boolean {
        val ext = CodecCapabilityInspector.resolveCanonicalExtension(
            uriOrPath = entity.uri,
            containerFormat = entity.containerFormat,
            mimeType = entity.mimeType,
            title = entity.title,
            isVideo = entity.isVideo
        )
        return if (entity.isVideo) {
            ext in normalizedVideoExtensions
        } else {
            ext in normalizedAudioExtensions
        }
    }

    suspend fun scanDeviceMediaDetailed(
        context: Context,
        excludedFolders: Set<String>,
        existingMap: Map<String, MediaEntity>,
        safRootsRaw: String = "",
        enabledAudioExtensions: Set<String> = CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS,
        enabledVideoExtensions: Set<String> = CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS
    ): MediaScanResult = withContext(Dispatchers.IO) {
        val normAudioExts = enabledAudioExtensions
            .map { CodecCapabilityInspector.normalizeExtension(it) }
            .filter { it in CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS }
            .toSet()
        val normVideoExts = enabledVideoExtensions
            .map { CodecCapabilityInspector.normalizeExtension(it) }
            .filter { it in CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS }
            .toSet()

        val discovered = mutableListOf<MediaEntity>()

        if (hasMediaPermissions(context)) {
            if (normAudioExts.isNotEmpty()) {
                discovered.addAll(
                    scanAudioMediaStore(
                        context = context,
                        excludedFolders = excludedFolders,
                        existingMap = existingMap,
                        enabledAudioExtensions = normAudioExts
                    )
                )
            }
            if (normVideoExts.isNotEmpty()) {
                discovered.addAll(
                    scanVideoMediaStore(
                        context = context,
                        excludedFolders = excludedFolders,
                        existingMap = existingMap,
                        enabledVideoExtensions = normVideoExts
                    )
                )
            }
        }

        // Scan mounted SAF document trees (e.g., SD cards, USB OTG drives, user-granted folders)
        for (safRoot in ScopedFileManager.parseSafRoots(safRootsRaw)) {
            discovered.addAll(
                scanSafTreeRecursive(
                    context = context,
                    treeUri = Uri.parse(safRoot.treeUriString),
                    rootLabel = safRoot.label,
                    excludedFolders = excludedFolders,
                    existingMap = existingMap,
                    enabledAudioExtensions = normAudioExts,
                    enabledVideoExtensions = normVideoExts
                )
            )
        }

        // App local media library folder (never generates fake sample media files)
        val localStudioDir = File(context.filesDir, "local_media_library").apply { mkdirs() }
        discovered.addAll(
            scanLocalFolderFiles(
                rootDir = localStudioDir,
                excludedFolders = excludedFolders,
                existingMap = existingMap,
                enabledAudioExtensions = normAudioExts,
                enabledVideoExtensions = normVideoExts
            )
        )

        val uniqueDiscovered = discovered.distinctBy { it.uri }
        val discoveredUriSet = uniqueDiscovered.map { it.uri }.toSet()

        // Existing media records must NEVER be deleted when their extension is disabled!
        val staleUris = existingMap.entries
            .filter { (existingUri, existingEntity) ->
                existingUri !in discoveredUriSet &&
                    isMediaEntityExtensionEnabled(existingEntity, normAudioExts, normVideoExts) &&
                    !isSafOrTempUriStillAccessible(context, existingUri)
            }
            .map { it.key }

        val duplicates = detectDuplicateMedia(uniqueDiscovered)
        MediaScanResult(
            discoveredItems = uniqueDiscovered,
            staleUrisToRemove = staleUris,
            duplicateGroups = duplicates
        )
    }

    suspend fun scanDeviceMedia(
        context: Context,
        excludedFolders: Set<String>,
        existingMap: Map<String, MediaEntity>,
        enabledAudioExtensions: Set<String> = CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS,
        enabledVideoExtensions: Set<String> = CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS
    ): List<MediaEntity> = scanDeviceMediaDetailed(
        context = context,
        excludedFolders = excludedFolders,
        existingMap = existingMap,
        enabledAudioExtensions = enabledAudioExtensions,
        enabledVideoExtensions = enabledVideoExtensions
    ).discoveredItems

    fun detectDuplicateMedia(items: List<MediaEntity>): List<DuplicateMediaGroup> {
        return items
            .filter { it.sizeBytes > 0L }
            .groupBy { item ->
                val cleanTitle = item.title.trim().lowercase(Locale.US)
                "${item.isVideo}|$cleanTitle|${item.sizeBytes}"
            }
            .filter { it.value.size > 1 }
            .map { (sig, group) ->
                val head = group.first()
                DuplicateMediaGroup(
                    signatureKey = sig,
                    title = head.title,
                    sizeBytes = head.sizeBytes,
                    items = group
                )
            }
    }

    private fun isSafOrTempUriStillAccessible(context: Context, uriString: String): Boolean {
        return try {
            if (uriString.startsWith("file://")) {
                val f = File(Uri.parse(uriString).path ?: return false)
                f.exists() && f.length() > 0L
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun scanAudioMediaStore(
        context: Context,
        excludedFolders: Set<String>,
        existingMap: Map<String, MediaEntity>,
        enabledAudioExtensions: Set<String>
    ): List<MediaEntity> {
        val results = mutableListOf<MediaEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val hasGenreCol = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATA)
            if (hasGenreCol) add(MediaStore.Audio.Media.GENRE)
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val genreCol = if (hasGenreCol) cursor.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val folderPath = if (rawPath.contains("/")) rawPath.substringBeforeLast("/") else "/storage/emulated/0/Music"
                    if (isFolderExcluded(folderPath, excludedFolders)) continue

                    val folderName = folderPath.substringAfterLast("/").ifBlank { "Music" }
                    val title = cursor.getString(titleCol)?.ifBlank { "Untitled Track" } ?: "Untitled Track"
                    val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Unknown Artist"
                    val album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Unknown Album"
                    val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val rawMime = cursor.getString(mimeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val mediaStoreGenre = if (genreCol >= 0) cursor.getString(genreCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } else null

                    val canonicalExt = CodecCapabilityInspector.resolveCanonicalExtension(
                        uriOrPath = rawPath.ifBlank { contentUri },
                        containerFormat = rawPath.substringAfterLast('.', ""),
                        mimeType = rawMime ?: "",
                        title = title,
                        isVideo = false
                    )
                    if (canonicalExt !in enabledAudioExtensions) continue

                    val (resolvedMime, codecBadge) = CodecCapabilityInspector.resolveMimeAndCodecBadge(
                        fileNameOrPath = rawPath.ifBlank { "$title.$canonicalExt" },
                        rawMimeType = rawMime,
                        isVideoHint = false
                    )

                    val existing = existingMap[contentUri]
                    val needsProbe = existing == null || existing.sizeBytes != size || existing.bitrate == 0
                    val probed = if (needsProbe) {
                        CodecCapabilityInspector.probeMediaMetadata(context, contentUri, isVideo = false, titleHint = title)
                    } else null

                    val localLrc = loadSidecarLyricsIfAvailable(rawPath)
                    val resolvedGenre = mediaStoreGenre
                        ?: probed?.genre?.takeIf { it.isNotBlank() }
                        ?: existing?.genre?.takeIf { it.isNotBlank() && it != "General" }
                        ?: "Music"

                    results.add(
                        MediaEntity(
                            uri = contentUri,
                            mediaStoreId = id,
                            title = existing?.title?.takeIf { it.isNotBlank() && it != title } ?: title,
                            artist = existing?.artist?.takeIf { it.isNotBlank() && it != "Unknown Artist" } ?: artist,
                            album = existing?.album?.takeIf { it.isNotBlank() && it != "Unknown Album" } ?: album,
                            genre = resolvedGenre,
                            durationMs = duration.takeIf { it > 0L } ?: probed?.durationMs ?: 0L,
                            sizeBytes = size,
                            mimeType = resolvedMime,
                            codecName = codecBadge,
                            containerFormat = canonicalExt.uppercase(Locale.US),
                            isVideo = false,
                            bitrate = probed?.bitrate ?: existing?.bitrate ?: 0,
                            sampleRateHz = probed?.sampleRateHz ?: existing?.sampleRateHz ?: 44100,
                            channelCount = probed?.channelCount ?: existing?.channelCount ?: 2,
                            folderPath = folderPath,
                            folderName = folderName,
                            dateAddedSec = dateAdded,
                            isFavorite = existing?.isFavorite ?: false,
                            lastPlayedTimestamp = existing?.lastPlayedTimestamp ?: 0L,
                            lastPositionMs = existing?.lastPositionMs ?: 0L,
                            playCount = existing?.playCount ?: 0,
                            subtitleDelayMs = existing?.subtitleDelayMs ?: 0L,
                            audioDelayMs = existing?.audioDelayMs ?: 0L,
                            preferredSpeed = existing?.preferredSpeed ?: 1.0f,
                            localLyrics = localLrc.ifBlank { existing?.localLyrics ?: "" }
                        )
                    )
                }
            }
        } catch (_: Throwable) {}
        return results
    }

    private fun scanVideoMediaStore(
        context: Context,
        excludedFolders: Set<String>,
        existingMap: Map<String, MediaEntity>,
        enabledVideoExtensions: Set<String>
    ): List<MediaEntity> {
        val results = mutableListOf<MediaEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.ALBUM,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val rawPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val folderPath = if (rawPath.contains("/")) rawPath.substringBeforeLast("/") else "/storage/emulated/0/Movies"
                    if (isFolderExcluded(folderPath, excludedFolders)) continue

                    val folderName = folderPath.substringAfterLast("/").ifBlank { "Movies" }
                    val title = cursor.getString(titleCol)?.ifBlank { "Untitled Video" } ?: "Untitled Video"
                    val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: "Local Video"
                    val album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" && it.isNotBlank() } ?: folderName
                    val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val size = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val rawMime = cursor.getString(mimeCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val dateAdded = cursor.getLong(dateCol)

                    val canonicalExt = CodecCapabilityInspector.resolveCanonicalExtension(
                        uriOrPath = rawPath.ifBlank { contentUri },
                        containerFormat = rawPath.substringAfterLast('.', ""),
                        mimeType = rawMime ?: "",
                        title = title,
                        isVideo = true
                    )
                    if (canonicalExt !in enabledVideoExtensions) continue

                    val (resolvedMime, codecBadge) = CodecCapabilityInspector.resolveMimeAndCodecBadge(
                        fileNameOrPath = rawPath.ifBlank { "$title.$canonicalExt" },
                        rawMimeType = rawMime,
                        isVideoHint = true
                    )

                    val existing = existingMap[contentUri]
                    val needsProbe = existing == null || existing.sizeBytes != size || existing.bitrate == 0
                    val probed = if (needsProbe) {
                        CodecCapabilityInspector.probeMediaMetadata(context, contentUri, isVideo = true, titleHint = title)
                    } else null

                    val actualHdr = probed?.hdrFormat ?: existing?.hdrFormat ?: "SDR"

                    results.add(
                        MediaEntity(
                            uri = contentUri,
                            mediaStoreId = id,
                            title = title,
                            artist = artist,
                            album = album,
                            genre = "Video",
                            durationMs = duration.takeIf { it > 0L } ?: probed?.durationMs ?: 0L,
                            sizeBytes = size,
                            mimeType = resolvedMime,
                            codecName = codecBadge,
                            containerFormat = canonicalExt.uppercase(Locale.US),
                            isVideo = true,
                            width = width.takeIf { it > 0 } ?: probed?.width ?: 0,
                            height = height.takeIf { it > 0 } ?: probed?.height ?: 0,
                            frameRate = probed?.frameRate ?: existing?.frameRate ?: 0f,
                            bitrate = probed?.bitrate ?: existing?.bitrate ?: 0,
                            hdrFormat = actualHdr,
                            sampleRateHz = probed?.sampleRateHz ?: existing?.sampleRateHz ?: 48000,
                            channelCount = probed?.channelCount ?: existing?.channelCount ?: 2,
                            folderPath = folderPath,
                            folderName = folderName,
                            dateAddedSec = dateAdded,
                            isFavorite = existing?.isFavorite ?: false,
                            lastPlayedTimestamp = existing?.lastPlayedTimestamp ?: 0L,
                            lastPositionMs = existing?.lastPositionMs ?: 0L,
                            playCount = existing?.playCount ?: 0,
                            subtitleDelayMs = existing?.subtitleDelayMs ?: 0L,
                            audioDelayMs = existing?.audioDelayMs ?: 0L,
                            preferredSpeed = existing?.preferredSpeed ?: 1.0f
                        )
                    )
                }
            }
        } catch (_: Throwable) {}
        return results
    }

    private fun classifyExtensionCategory(
        ext: String,
        rawMime: String,
        fileNameOrPath: String,
        enabledAudioExtensions: Set<String>,
        enabledVideoExtensions: Set<String>
    ): Pair<Boolean, Boolean> {
        val normExt = ext.lowercase(Locale.US)
        if (normExt == "3gp") {
            val lowerMime = rawMime.lowercase(Locale.US)
            val lowerName = fileNameOrPath.lowercase(Locale.US)
            val audioEnabled = "3gp" in enabledAudioExtensions
            val videoEnabled = "3gp" in enabledVideoExtensions
            return when {
                lowerMime.startsWith("audio/") || lowerName.contains("audio") || lowerName.contains("voice") -> {
                    audioEnabled to false
                }
                lowerMime.startsWith("video/") || lowerName.contains("video") || lowerName.contains("movie") -> {
                    false to videoEnabled
                }
                videoEnabled && !audioEnabled -> false to true
                audioEnabled && !videoEnabled -> true to false
                videoEnabled -> false to true
                else -> false to false
            }
        }
        val isAudio = normExt in enabledAudioExtensions
        val isVideo = normExt in enabledVideoExtensions && !isAudio
        return isAudio to isVideo
    }

    private fun scanSafTreeRecursive(
        context: Context,
        treeUri: Uri,
        rootLabel: String,
        excludedFolders: Set<String>,
        existingMap: Map<String, MediaEntity>,
        enabledAudioExtensions: Set<String>,
        enabledVideoExtensions: Set<String>,
        depth: Int = 0
    ): List<MediaEntity> {
        if (depth > 4) return emptyList()
        val list = mutableListOf<MediaEntity>()
        try {
            val docId = if (DocumentsContract.isDocumentUri(context, treeUri)) {
                DocumentsContract.getDocumentId(treeUri)
            } else {
                DocumentsContract.getTreeDocumentId(treeUri)
            }
            val docUriStr = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
            if (isFolderExcluded(docUriStr, excludedFolders)) return emptyList()

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
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

                while (cursor.moveToNext()) {
                    val childId = if (idCol >= 0) cursor.getString(idCol) else continue
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: continue else continue
                    val rawMime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    val childDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)

                    if (rawMime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        list.addAll(
                            scanSafTreeRecursive(
                                context = context,
                                treeUri = childDocUri,
                                rootLabel = name,
                                excludedFolders = excludedFolders,
                                existingMap = existingMap,
                                enabledAudioExtensions = enabledAudioExtensions,
                                enabledVideoExtensions = enabledVideoExtensions,
                                depth = depth + 1
                            )
                        )
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
                        val (isAudio, isVideo) = classifyExtensionCategory(
                            ext = ext,
                            rawMime = rawMime,
                            fileNameOrPath = name,
                            enabledAudioExtensions = enabledAudioExtensions,
                            enabledVideoExtensions = enabledVideoExtensions
                        )
                        if (!isVideo && !isAudio) continue

                        val uriStr = childDocUri.toString()
                        val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                        val modSec = if (modCol >= 0) cursor.getLong(modCol) / 1000L else System.currentTimeMillis() / 1000L
                        val (mime, badge) = CodecCapabilityInspector.resolveMimeAndCodecBadge(name, rawMime, isVideoHint = isVideo)

                        val existing = existingMap[uriStr]
                        val probed = if (existing == null || existing.sizeBytes != size) {
                            CodecCapabilityInspector.probeMediaMetadata(context, uriStr, isVideo = isVideo, titleHint = name)
                        } else null

                        list.add(
                            MediaEntity(
                                uri = uriStr,
                                mediaStoreId = uriStr.hashCode().toLong(),
                                title = name.substringBeforeLast('.'),
                                artist = existing?.artist ?: rootLabel,
                                album = existing?.album ?: rootLabel,
                                genre = probed?.genre?.ifBlank { null } ?: if (isVideo) "Video" else "Music",
                                durationMs = probed?.durationMs ?: existing?.durationMs ?: 0L,
                                sizeBytes = size,
                                mimeType = mime,
                                codecName = badge,
                                containerFormat = ext.uppercase(Locale.US),
                                isVideo = isVideo,
                                width = probed?.width ?: existing?.width ?: 0,
                                height = probed?.height ?: existing?.height ?: 0,
                                frameRate = probed?.frameRate ?: existing?.frameRate ?: 0f,
                                bitrate = probed?.bitrate ?: existing?.bitrate ?: 0,
                                hdrFormat = probed?.hdrFormat ?: existing?.hdrFormat ?: "SDR",
                                sampleRateHz = probed?.sampleRateHz ?: existing?.sampleRateHz ?: 44100,
                                channelCount = probed?.channelCount ?: existing?.channelCount ?: 2,
                                folderPath = docUriStr,
                                folderName = rootLabel,
                                dateAddedSec = modSec,
                                isFavorite = existing?.isFavorite ?: false,
                                lastPlayedTimestamp = existing?.lastPlayedTimestamp ?: 0L,
                                lastPositionMs = existing?.lastPositionMs ?: 0L,
                                playCount = existing?.playCount ?: 0,
                                subtitleDelayMs = existing?.subtitleDelayMs ?: 0L,
                                audioDelayMs = existing?.audioDelayMs ?: 0L,
                                preferredSpeed = existing?.preferredSpeed ?: 1.0f
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    fun isFolderExcluded(folderPath: String, excludedFolders: Set<String>): Boolean {
        val normalized = folderPath.trimEnd('/')
        return excludedFolders.any { excluded ->
            val cleanExcluded = excluded.trimEnd('/')
            normalized.equals(cleanExcluded, ignoreCase = true) ||
                normalized.startsWith("$cleanExcluded/", ignoreCase = true)
        }
    }

    private fun loadSidecarLyricsIfAvailable(mediaFilePath: String): String {
        if (mediaFilePath.isBlank()) return ""
        return try {
            val base = mediaFilePath.substringBeforeLast('.')
            val lrcFile = File("$base.lrc")
            if (lrcFile.exists() && lrcFile.canRead() && lrcFile.length() < 256_000) {
                lrcFile.readText()
            } else {
                ""
            }
        } catch (_: Throwable) {
            ""
        }
    }

    fun scanLocalFolderFiles(
        rootDir: File,
        excludedFolders: Set<String>,
        existingMap: Map<String, MediaEntity>,
        enabledAudioExtensions: Set<String> = CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS,
        enabledVideoExtensions: Set<String> = CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS
    ): List<MediaEntity> {
        val normAudioExts = enabledAudioExtensions
            .map { CodecCapabilityInspector.normalizeExtension(it) }
            .filter { it in CodecCapabilityInspector.ALL_AUDIO_EXTENSIONS }
            .toSet()
        val normVideoExts = enabledVideoExtensions
            .map { CodecCapabilityInspector.normalizeExtension(it) }
            .filter { it in CodecCapabilityInspector.ALL_VIDEO_EXTENSIONS }
            .toSet()

        val list = mutableListOf<MediaEntity>()
        if (!rootDir.exists() || !rootDir.isDirectory) return emptyList()
        if (isFolderExcluded(rootDir.absolutePath, excludedFolders)) return emptyList()

        val files = rootDir.listFiles() ?: return emptyList()
        for (file in files) {
            if (file.isDirectory) {
                if (!isFolderExcluded(file.absolutePath, excludedFolders)) {
                    list.addAll(
                        scanLocalFolderFiles(
                            rootDir = file,
                            excludedFolders = excludedFolders,
                            existingMap = existingMap,
                            enabledAudioExtensions = normAudioExts,
                            enabledVideoExtensions = normVideoExts
                        )
                    )
                }
            } else {
                val ext = file.extension.lowercase(Locale.US)
                if (ext.isEmpty()) continue
                val (isAudio, isVideo) = classifyExtensionCategory(
                    ext = ext,
                    rawMime = "",
                    fileNameOrPath = file.absolutePath,
                    enabledAudioExtensions = normAudioExts,
                    enabledVideoExtensions = normVideoExts
                )
                if (!isAudio && !isVideo) continue

                val uriStr = Uri.fromFile(file).toString()
                val (mime, badge) = CodecCapabilityInspector.resolveMimeAndCodecBadge(
                    fileNameOrPath = file.name,
                    rawMimeType = null,
                    isVideoHint = isVideo
                )
                val existing = existingMap[uriStr]
                val lrc = loadSidecarLyricsIfAvailable(file.absolutePath)
                list.add(
                    MediaEntity(
                        uri = uriStr,
                        mediaStoreId = uriStr.hashCode().toLong(),
                        title = existing?.title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension.replace('_', ' '),
                        artist = existing?.artist ?: if (isVideo) "Local Video" else "Local Audio",
                        album = existing?.album ?: (file.parentFile?.name ?: "Local Media"),
                        genre = existing?.genre ?: if (isVideo) "Video" else "Music",
                        durationMs = existing?.durationMs ?: 12_000L,
                        sizeBytes = file.length(),
                        mimeType = mime,
                        codecName = badge,
                        containerFormat = ext.uppercase(Locale.US),
                        isVideo = isVideo,
                        width = existing?.width ?: if (isVideo) 1920 else 0,
                        height = existing?.height ?: if (isVideo) 1080 else 0,
                        hdrFormat = existing?.hdrFormat ?: "SDR",
                        sampleRateHz = existing?.sampleRateHz ?: 44100,
                        channelCount = existing?.channelCount ?: 2,
                        folderPath = file.parentFile?.absolutePath ?: rootDir.absolutePath,
                        folderName = file.parentFile?.name ?: "Local Media",
                        dateAddedSec = file.lastModified() / 1000L,
                        isFavorite = existing?.isFavorite ?: false,
                        lastPlayedTimestamp = existing?.lastPlayedTimestamp ?: 0L,
                        lastPositionMs = existing?.lastPositionMs ?: 0L,
                        playCount = existing?.playCount ?: 0,
                        subtitleDelayMs = existing?.subtitleDelayMs ?: 0L,
                        audioDelayMs = existing?.audioDelayMs ?: 0L,
                        preferredSpeed = existing?.preferredSpeed ?: 1.0f,
                        localLyrics = lrc.ifBlank { existing?.localLyrics ?: "" }
                    )
                )
            }
        }
        return list.distinctBy { it.uri }
    }

    fun writeSynthesizedStereoWavFile(
        outputFile: File,
        durationSeconds: Int = 6,
        sampleRate: Int = 22050,
        freqLeft: Double = 440.0,
        freqRight: Double = 554.37
    ) {
        try {
            val numChannels = 2
            val bitsPerSample = 16
            val numSamples = sampleRate * durationSeconds
            val dataSize = numSamples * numChannels * (bitsPerSample / 8)
            val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
            val blockAlign = (numChannels * (bitsPerSample / 8)).toShort()

            FileOutputStream(outputFile).use { fos ->
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray(Charsets.US_ASCII))
                header.putInt(36 + dataSize)
                header.put("WAVE".toByteArray(Charsets.US_ASCII))
                header.put("fmt ".toByteArray(Charsets.US_ASCII))
                header.putInt(16)
                header.putShort(1)
                header.putShort(numChannels.toShort())
                header.putInt(sampleRate)
                header.putInt(byteRate)
                header.putShort(blockAlign)
                header.putShort(bitsPerSample.toShort())
                header.put("data".toByteArray(Charsets.US_ASCII))
                header.putInt(dataSize)
                fos.write(header.array())

                val frameBuffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate.toDouble()
                    val envelope = if (t < 0.25) (t / 0.25) else if (t > durationSeconds - 0.5) ((durationSeconds - t) / 0.5).coerceAtLeast(0.0) else 1.0
                    val sampleL = (sin(2.0 * Math.PI * freqLeft * t) * 0.35 * envelope * Short.MAX_VALUE).toInt().toShort()
                    val sampleR = (sin(2.0 * Math.PI * freqRight * t) * 0.35 * envelope * Short.MAX_VALUE).toInt().toShort()
                    if (frameBuffer.remaining() < 4) {
                        fos.write(frameBuffer.array(), 0, frameBuffer.position())
                        frameBuffer.clear()
                    }
                    frameBuffer.putShort(sampleL)
                    frameBuffer.putShort(sampleR)
                }
                if (frameBuffer.position() > 0) {
                    fos.write(frameBuffer.array(), 0, frameBuffer.position())
                }
            }
        } catch (_: Throwable) {}
    }
}
