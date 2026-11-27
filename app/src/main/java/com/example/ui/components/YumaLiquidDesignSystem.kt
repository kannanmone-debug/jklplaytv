package com.example.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArtworkDisplayStyle
import com.example.ui.theme.ArtworkFitMode
import com.example.ui.theme.BnjArtworkPaletteExtractor
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ExtractedArtworkPalette
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPillBadge
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.artworkGradientPair
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveContextualAccent
import com.example.ui.theme.resolveDynamicGlassPalette
import com.example.ui.theme.resolveEffectiveConfigForComponent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * P4 Step 2: Output resolution tiers for the High-Clarity Album Artwork Engine.
 * - [THUMBNAIL]: Compact library thumbnails, mini player, and queue rows (~360px in High Clarity, 280px standard).
 * - [DETAIL]: Album / artist / playlist headers and featured cards (up to 1024px in High Clarity, 720px standard).
 * - [HERO]: Fullscreen music player hero artwork (up to 1440px in High Clarity, 960px standard).
 */
enum class ArtworkResolutionTier(
    val rank: Int,
    val standardMaxPx: Int,
    val highClarityMaxPx: Int
) {
    THUMBNAIL(rank = 1, standardMaxPx = 280, highClarityMaxPx = 360),
    DETAIL(rank = 2, standardMaxPx = 720, highClarityMaxPx = 1024),
    HERO(rank = 3, standardMaxPx = 960, highClarityMaxPx = 1440);

    fun targetMaxPx(highClarity: Boolean): Int =
        if (highClarity) highClarityMaxPx else standardMaxPx
}

/**
 * P4 High-Clarity Album Artwork Cache & Decoding Engine.
 *
 * Implements:
 * - 3 output resolution tiers: THUMBNAIL (~360px), DETAIL (up to 1024px), HERO (up to 1440px)
 * - Aspect-ratio preserving downsampling with ZERO upscaling of smaller original covers
 * - Predictable 5-step source priority:
 *   (1) Embedded audio artwork -> (2) Album-level MediaStore artwork ->
 *   (3) Scope-safe case-insensitive sibling folder cover -> (4) Resolution-aware cache ->
 *   (5) Deterministic Liquid Glass fallback
 * - Single-flight concurrent decode deduplication and source-change cache invalidation
 */
object BnjArtworkCache {
    const val THUMB_MAX_PX = 360
    const val DETAIL_MAX_PX = 1024
    const val HERO_MAX_PX = 1440

    private val FOLDER_COVER_NAMES_LOWER = listOf(
        "cover.jpg", "cover.jpeg", "cover.png",
        "folder.jpg", "folder.jpeg", "folder.png",
        "albumart.jpg", "albumart.jpeg", "albumart.png",
        "album.jpg", "album.jpeg", "album.png",
        "front.jpg", "front.jpeg", "front.png",
        "artwork.jpg", "artwork.jpeg", "artwork.png"
    )

    private val GENERIC_MULTI_ALBUM_FOLDER_NAMES = setOf(
        "music", "audio", "download", "downloads", "sdcard", "storage",
        "emulated", "0", "media", "podcasts", "ringtones", "notifications",
        "alarms", "shared", "files", "documents", "bluetooth", "whatsapp audio",
        "telegram", "telegram audio", "recordings"
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "flac", "m4a", "aac", "wav", "ogg", "opus", "alac", "wv", "aiff"
    )

    private val maxCacheBytes: Int = (Runtime.getRuntime().maxMemory() / 10L)
        .coerceIn(8L * 1024L * 1024L, 32L * 1024L * 1024L)
        .toInt()

    private val bitmapCache = object : LruCache<String, ImageBitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            val bytes = value.width.toLong() * value.height.toLong() * 4L
            return bytes.coerceIn(4096L, Int.MAX_VALUE.toLong()).toInt()
        }
    }
    private val rawBitmapRefCache = object : LruCache<String, Bitmap>(24) {}
    private val negativeLookupCache = object : LruCache<String, Boolean>(256) {}
    private val folderCoverPathCache = object : LruCache<String, String>(128) {}
    private val sourceVersionTracker = ConcurrentHashMap<String, Long>()
    private val inFlightDecodes = ConcurrentHashMap<String, CompletableDeferred<ImageBitmap?>>()

    private const val NO_FOLDER_COVER_SENTINEL = "__NONE__"

    fun targetPixelDimension(tier: ArtworkResolutionTier, highClarity: Boolean): Int =
        tier.targetMaxPx(highClarity)

    fun buildCacheKey(
        uri: String,
        tier: ArtworkResolutionTier,
        highClarity: Boolean = true
    ): String {
        val modToken = resolveSourceModificationToken(uri)
        return "${tier.name}::hc=$highClarity::v=$modToken::$uri"
    }

    fun buildPaletteCacheKey(uri: String): String {
        val modToken = resolveSourceModificationToken(uri)
        return "media::$uri::v=$modToken"
    }

    fun getCachedPaletteForUri(uri: String): ExtractedArtworkPalette? {
        if (uri.isBlank()) return null
        ensureFreshSourceVersion(uri)
        return BnjArtworkPaletteExtractor.getCachedPalette(buildPaletteCacheKey(uri))
            ?: BnjArtworkPaletteExtractor.getCachedPalette("media::$uri")
    }

    private fun resolveSourceModificationToken(uriString: String): Long {
        if (uriString.isBlank()) return 0L
        return try {
            val path = when {
                uriString.startsWith("file://") -> Uri.parse(uriString).path
                uriString.startsWith("/") -> uriString
                else -> null
            }
            if (!path.isNullOrBlank()) {
                val f = File(path)
                if (f.exists()) f.lastModified() else 0L
            } else {
                0L
            }
        } catch (_: Throwable) {
            0L
        }
    }

    /**
     * Checks if source file timestamp changed since last cache entry; if so, invalidates stale entries.
     */
    private fun ensureFreshSourceVersion(uri: String) {
        val currentMod = resolveSourceModificationToken(uri)
        val previousMod = sourceVersionTracker.put(uri, currentMod)
        if (previousMod != null && previousMod != currentMod) {
            invalidateMedia(uri)
            sourceVersionTracker[uri] = currentMod
        }
    }

    /**
     * Zero-I/O, zero-scaling fast in-memory cache lookup safe for Compose composition on Main thread.
     */
    fun getCachedFastInMemory(
        uri: String,
        highClarity: Boolean = true,
        tier: ArtworkResolutionTier = if (highClarity) ArtworkResolutionTier.HERO else ArtworkResolutionTier.THUMBNAIL
    ): ImageBitmap? {
        if (uri.isBlank()) return null
        val modToken = sourceVersionTracker[uri] ?: return null
        val exactKey = "${tier.name}::hc=$highClarity::v=$modToken::$uri"
        synchronized(bitmapCache) {
            bitmapCache.get(exactKey)?.let { return it }
            for (higherTier in ArtworkResolutionTier.entries) {
                if (higherTier.rank > tier.rank) {
                    val higherKey = "${higherTier.name}::hc=$highClarity::v=$modToken::$uri"
                    bitmapCache.get(higherKey)?.let { return it }
                }
            }
            return null
        }
    }

    /**
     * P4 Step 7: Resolution-aware cache lookup.
     * - Never reuses a low-resolution THUMBNAIL (360px) as DETAIL (1024px) or HERO (1440px).
     * - Never reuses DETAIL (1024px) as HERO (1440px).
     * - Can satisfy a smaller tier request from an already-cached higher tier for the same URI & clarity setting.
     */
    fun getCached(
        uri: String,
        highClarity: Boolean = true,
        tier: ArtworkResolutionTier = if (highClarity) ArtworkResolutionTier.HERO else ArtworkResolutionTier.THUMBNAIL
    ): ImageBitmap? {
        if (uri.isBlank()) return null
        ensureFreshSourceVersion(uri)
        val exactKey = buildCacheKey(uri, tier, highClarity)
        synchronized(bitmapCache) {
            bitmapCache.get(exactKey)?.let { return it }
            // Check higher-rank tiers for the same URI & highClarity (never lower-rank tiers!)
            ArtworkResolutionTier.entries
                .filter { it.rank > tier.rank }
                .sortedBy { it.rank }
                .forEach { higherTier ->
                    val higherKey = buildCacheKey(uri, higherTier, highClarity)
                    val higherRaw = rawBitmapRefCache.get(higherKey)
                    if (higherRaw != null && !higherRaw.isRecycled) {
                        val downscaled = scaleBitmapDownPreservingAspect(
                            higherRaw,
                            tier.targetMaxPx(highClarity)
                        )
                        val downscaledImg = downscaled.asImageBitmap()
                        bitmapCache.put(exactKey, downscaledImg)
                        rawBitmapRefCache.put(exactKey, downscaled)
                        return downscaledImg
                    }
                    bitmapCache.get(higherKey)?.let { return it }
                }
            return null
        }
    }

    /**
     * Stores a decoded [Bitmap] for a specific [ArtworkResolutionTier] and [highClarity] setting,
     * applying exact tier downsampling without upscaling.
     */
    fun putBitmapForTier(
        uri: String,
        bitmap: Bitmap,
        tier: ArtworkResolutionTier,
        highClarity: Boolean = true
    ): ImageBitmap {
        ensureFreshSourceVersion(uri)
        val scaled = scaleBitmapDownPreservingAspect(bitmap, tier.targetMaxPx(highClarity))
        val imageBitmap = scaled.asImageBitmap()
        val key = buildCacheKey(uri, tier, highClarity)
        synchronized(bitmapCache) {
            bitmapCache.put(key, imageBitmap)
            rawBitmapRefCache.put(key, scaled)
            negativeLookupCache.remove(key)
        }
        val palette = BnjArtworkPaletteExtractor.extractQuantizedPalette(scaled, uri)
        BnjArtworkPaletteExtractor.putCachedPalette(buildPaletteCacheKey(uri), palette)
        BnjArtworkPaletteExtractor.putCachedPalette("media::$uri", palette)
        return imageBitmap
    }

    /**
     * P4 Step 7: Invalidates all cached resolution tiers and palette entries for [uri].
     */
    fun invalidateMedia(uri: String) {
        if (uri.isBlank()) return
        synchronized(bitmapCache) {
            val snapshotKeys = bitmapCache.snapshot().keys.filter { it.endsWith("::$uri") }
            snapshotKeys.forEach { bitmapCache.remove(it) }
            val rawKeys = rawBitmapRefCache.snapshot().keys.filter { it.endsWith("::$uri") }
            rawKeys.forEach { rawBitmapRefCache.remove(it) }
            val negKeys = negativeLookupCache.snapshot().keys.filter { it.endsWith("::$uri") }
            negKeys.forEach { negativeLookupCache.remove(it) }
        }
        BnjArtworkPaletteExtractor.invalidateByPrefix("media::$uri")
        sourceVersionTracker.remove(uri)
    }

    suspend fun loadOrExtractArtwork(
        context: Context,
        uriString: String,
        isVideo: Boolean,
        highClarity: Boolean = true,
        tier: ArtworkResolutionTier = if (highClarity) ArtworkResolutionTier.HERO else ArtworkResolutionTier.THUMBNAIL,
        extractPalette: Boolean = true
    ): ImageBitmap? {
        if (uriString.isBlank()) return null

        val inFlightKey = "${tier.name}::hc=$highClarity::$uriString"
        // Single-flight concurrent decode deduplication
        val newDeferred = CompletableDeferred<ImageBitmap?>()
        val existingDeferred = inFlightDecodes.putIfAbsent(inFlightKey, newDeferred)
        if (existingDeferred != null) {
            return existingDeferred.await()
        }

        val maxDim = tier.targetMaxPx(highClarity)
        return try {
            val result = withContext(Dispatchers.IO) {
                getCached(uriString, highClarity, tier)?.let { return@withContext it }

                val key = buildCacheKey(uriString, tier, highClarity)
                synchronized(bitmapCache) {
                    if (negativeLookupCache.get(key) == true) return@withContext null
                }

                val decodedBitmap = decodeArtworkBySourcePriority(
                    context = context,
                    uriString = uriString,
                    isVideo = isVideo,
                    maxDim = maxDim
                )

                if (decodedBitmap != null) {
                    if (extractPalette) {
                        val paletteKey = buildPaletteCacheKey(uriString)
                        val palette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
                            bitmap = decodedBitmap,
                            cacheKey = paletteKey,
                            fallbackSeed = uriString
                        )
                        BnjArtworkPaletteExtractor.putCachedPalette("media::$uriString", palette)
                    }
                    val imageBitmap = decodedBitmap.asImageBitmap()
                    synchronized(bitmapCache) {
                        bitmapCache.put(key, imageBitmap)
                        rawBitmapRefCache.put(key, decodedBitmap)
                    }
                    imageBitmap
                } else {
                    synchronized(bitmapCache) {
                        negativeLookupCache.put(key, true)
                    }
                    null
                }
            }
            newDeferred.complete(result)
            result
        } catch (t: Throwable) {
            newDeferred.complete(null)
            null
        } finally {
            inFlightDecodes.remove(inFlightKey, newDeferred)
        }
    }

    /**
     * P5 Step 2 & Step 8: Extracts the dynamic artwork palette using an already-cached bitmap or
     * the lightweight THUMBNAIL tier (~360px) so palette extraction NEVER forces a 1440px HERO decode.
     */
    suspend fun extractPaletteWithoutHeroDecode(
        context: Context,
        uriString: String,
        isVideo: Boolean,
        titleSeed: String,
        highClarity: Boolean = true
    ): ExtractedArtworkPalette {
        if (uriString.isBlank()) {
            return BnjArtworkPaletteExtractor.fallbackPalette(titleSeed)
        }
        getCachedPaletteForUri(uriString)?.let { return it }

        // Check if any tier's raw bitmap is already in memory for this URI
        val existingBitmap = synchronized(bitmapCache) {
            rawBitmapRefCache.snapshot().entries
                .firstOrNull { it.key.endsWith("::$uriString") && !it.value.isRecycled }
                ?.value
        }
        if (existingBitmap != null) {
            val paletteKey = buildPaletteCacheKey(uriString)
            val palette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
                bitmap = existingBitmap,
                cacheKey = paletteKey,
                fallbackSeed = titleSeed
            )
            BnjArtworkPaletteExtractor.putCachedPalette("media::$uriString", palette)
            return palette
        }

        // Otherwise load only the lightweight THUMBNAIL tier (360px / 280px)
        loadOrExtractArtwork(
            context = context,
            uriString = uriString,
            isVideo = isVideo,
            highClarity = highClarity,
            tier = ArtworkResolutionTier.THUMBNAIL
        )
        getCachedPaletteForUri(uriString)?.let { return it }

        val thumbKey = buildCacheKey(uriString, ArtworkResolutionTier.THUMBNAIL, highClarity)
        val bitmapForPalette = synchronized(bitmapCache) {
            rawBitmapRefCache.get(thumbKey)
                ?: rawBitmapRefCache.snapshot().entries
                    .firstOrNull { it.key.endsWith("::$uriString") && !it.value.isRecycled }
                    ?.value
        }
        val paletteKey = buildPaletteCacheKey(uriString)
        val palette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
            bitmap = bitmapForPalette,
            cacheKey = paletteKey,
            fallbackSeed = titleSeed
        )
        BnjArtworkPaletteExtractor.putCachedPalette("media::$uriString", palette)
        return palette
    }

    suspend fun loadArtworkAndPalette(
        context: Context,
        uriString: String,
        isVideo: Boolean,
        highClarity: Boolean,
        titleSeed: String,
        tier: ArtworkResolutionTier = if (highClarity) ArtworkResolutionTier.HERO else ArtworkResolutionTier.THUMBNAIL
    ): Pair<ImageBitmap?, ExtractedArtworkPalette> {
        val artwork = loadOrExtractArtwork(context, uriString, isVideo, highClarity, tier)
        getCachedPaletteForUri(uriString)?.let { return artwork to it }
        val exactKey = buildCacheKey(uriString, tier, highClarity)
        val bitmapForPalette = synchronized(bitmapCache) {
            rawBitmapRefCache.get(exactKey)
                ?: rawBitmapRefCache.snapshot().entries.firstOrNull { it.key.endsWith("::$uriString") }?.value
        }
        val paletteKey = buildPaletteCacheKey(uriString)
        val palette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
            bitmap = bitmapForPalette,
            cacheKey = paletteKey,
            fallbackSeed = titleSeed
        )
        BnjArtworkPaletteExtractor.putCachedPalette("media::$uriString", palette)
        return artwork to palette
    }

    /**
     * P4 Step 3: Predictable Artwork Source Priority
     * 1. Valid embedded artwork from the audio file (or video frame for video items).
     * 2. Valid album-level artwork associated with the MediaStore library.
     * 3. Valid sibling-folder artwork (case-insensitive, cached, scope-safe).
     * (4. Cached artwork is checked prior to calling this method; 5. Default fallback is rendered when null).
     */
    private fun decodeArtworkBySourcePriority(
        context: Context,
        uriString: String,
        isVideo: Boolean,
        maxDim: Int
    ): Bitmap? {
        val parsedUri = try {
            if (uriString.startsWith("/")) Uri.fromFile(File(uriString)) else Uri.parse(uriString)
        } catch (_: Throwable) {
            return null
        }

        // Priority 1: Embedded artwork in audio/video file
        val embeddedOrFrame = extractEmbeddedOrVideoFrame(context, parsedUri, isVideo, maxDim)
        if (embeddedOrFrame != null) return embeddedOrFrame

        if (isVideo) return null

        // Priority 2: Album-level MediaStore artwork
        val albumLevelArt = extractAlbumLevelMediaStoreArtwork(context, parsedUri, maxDim)
        if (albumLevelArt != null) return albumLevelArt

        // Priority 3: Scope-safe sibling folder artwork
        return findSiblingFolderCover(context, parsedUri, maxDim)
    }

    private fun extractEmbeddedOrVideoFrame(
        context: Context,
        parsedUri: Uri,
        isVideo: Boolean,
        maxDim: Int
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (parsedUri.scheme == "file" || parsedUri.scheme.isNullOrBlank()) {
                val path = parsedUri.path ?: return null
                val file = File(path)
                if (!file.exists() || !file.isFile || file.length() <= 0L) return null
                retriever.setDataSource(file.absolutePath)
            } else {
                retriever.setDataSource(context, parsedUri)
            }

            val embeddedBytes = retriever.embeddedPicture
            when {
                embeddedBytes != null && embeddedBytes.isNotEmpty() -> {
                    decodeByteArrayHighClarity(embeddedBytes, maxDim)
                }
                isVideo -> {
                    val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    frame?.let { scaleBitmapDownPreservingAspect(it, maxDim) }
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }
    }

    /**
     * P4 Step 3 (Source #2): Album-level MediaStore artwork lookup.
     * Queries MediaStore for the audio item's ALBUM_ID and decodes the album art stream
     * (or uses ContentResolver.loadThumbnail on API 29+) at the requested resolution tier.
     */
    private fun extractAlbumLevelMediaStoreArtwork(
        context: Context,
        parsedUri: Uri,
        maxDim: Int
    ): Bitmap? {
        if (parsedUri.scheme != "content") return null
        return try {
            val resolver = context.contentResolver
            var albumId: Long? = null
            resolver.query(
                parsedUri,
                arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                    if (idx >= 0 && !cursor.isNull(idx)) {
                        albumId = cursor.getLong(idx)
                    }
                }
            }

            val validAlbumId = albumId?.takeIf { it > 0L }
            if (validAlbumId != null) {
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    validAlbumId
                )
                resolver.openInputStream(albumArtUri)?.use { stream ->
                    val bytes = stream.readBytes()
                    decodeByteArrayHighClarity(bytes, maxDim)?.let { return it }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val thumb = resolver.loadThumbnail(parsedUri, Size(maxDim, maxDim), null)
                if (thumb.width > 0 && thumb.height > 0) {
                    return scaleBitmapDownPreservingAspect(thumb, maxDim)
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Validates magic-byte headers for JPEG, PNG, WebP, GIF, and BMP images so corrupted
     * or non-image byte arrays fail fast and safely without native decoder exceptions.
     */
    fun hasValidImageSignature(bytes: ByteArray): Boolean {
        if (bytes.size < 8) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF
        // JPEG: FF D8 FF
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return true
        // PNG: 89 50 4E 47
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return true
        // GIF: 'G' 'I' 'F' '8'
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return true
        // BMP: 'B' 'M'
        if (b0 == 0x42 && b1 == 0x4D) return true
        // WebP: 'R' 'I' 'F' 'F' .... 'W' 'E' 'B' 'P'
        if (bytes.size >= 12 &&
            b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46 &&
            (bytes[8].toInt() and 0xFF) == 0x57 &&
            (bytes[9].toInt() and 0xFF) == 0x45 &&
            (bytes[10].toInt() and 0xFF) == 0x42 &&
            (bytes[11].toInt() and 0xFF) == 0x50
        ) {
            return true
        }
        return false
    }

    fun decodeArtworkBytesForTier(
        bytes: ByteArray,
        tier: ArtworkResolutionTier,
        highClarity: Boolean = true
    ): Bitmap? = decodeByteArrayHighClarity(bytes, tier.targetMaxPx(highClarity))

    fun decodeArtworkFileForTier(
        file: File,
        tier: ArtworkResolutionTier,
        highClarity: Boolean = true
    ): Bitmap? = decodeFileHighClarity(file, tier.targetMaxPx(highClarity))

    /**
     * Computes the largest power-of-2 [BitmapFactory.Options.inSampleSize] such that the decoded
     * bitmap's longest edge remains >= [targetMaxPx] (when the source is >= [targetMaxPx]),
     * allowing [scaleBitmapDownPreservingAspect] to produce the exact target tier resolution
     * without ever undershooting or upscaling.
     */
    fun calculateArtworkInSampleSize(rawWidth: Int, rawHeight: Int, targetMaxPx: Int): Int {
        if (rawWidth <= 0 || rawHeight <= 0 || targetMaxPx <= 0) return 1
        val longest = max(rawWidth, rawHeight)
        var inSampleSize = 1
        while (longest / (inSampleSize * 2) >= targetMaxPx) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Decodes [bytes] close to [maxDim] while preserving original aspect ratio and
     * NEVER upscaling smaller original artwork.
     */
    fun decodeByteArrayHighClarity(bytes: ByteArray, maxDim: Int): Bitmap? {
        if (!hasValidImageSignature(bytes)) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sample = calculateArtworkInSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            maxDim
        )
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
        return scaleBitmapDownPreservingAspect(decoded, maxDim)
    }

    /**
     * Decodes an external image [file] close to [maxDim] while preserving original aspect ratio and
     * NEVER upscaling smaller original artwork.
     */
    fun decodeFileHighClarity(file: File, maxDim: Int): Bitmap? {
        if (!file.exists() || !file.isFile) return null
        val len = file.length()
        if (len !in 16L..(25L * 1024L * 1024L)) return null

        val header = ByteArray(16)
        val readCount = try {
            FileInputStream(file).use { it.read(header) }
        } catch (_: Throwable) {
            -1
        }
        if (readCount < 8 || !hasValidImageSignature(header.copyOf(readCount))) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sample = calculateArtworkInSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            maxDim
        )
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        return scaleBitmapDownPreservingAspect(decoded, maxDim)
    }

    /**
     * P4 Step 2: Preserves original aspect ratio and NEVER upscales a smaller source image.
     * If either dimension exceeds [maxDim], scales down smoothly with bilinear filtering.
     */
    fun scaleBitmapDownPreservingAspect(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0 || maxDim <= 0) return bitmap
        val longest = max(w, h)
        if (longest <= maxDim) {
            // Never upscale smaller original artwork
            return bitmap
        }
        val scale = maxDim.toDouble() / longest.toDouble()
        val targetW = (w * scale).roundToInt().coerceAtLeast(1)
        val targetH = (h * scale).roundToInt().coerceAtLeast(1)
        if (targetW == w && targetH == h) return bitmap
        return try {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        } catch (_: Throwable) {
            bitmap
        }
    }

    /**
     * P4 Step 8: Discovers a valid sibling-folder cover file for [audioFileOrUri] with:
     * - Case-insensitive matching against standard album cover filenames
     * - Folder lookup caching keyed by directory path + lastModified timestamp
     * - Scope safety so generic multi-album dump folders (e.g. /Music, /Download, or folders
     *   containing multiple album subdirectories) do not cross-contaminate unrelated albums.
     */
    fun discoverSiblingFolderCoverFile(
        audioFileOrUri: String,
        context: Context? = null
    ): File? {
        if (audioFileOrUri.isBlank()) return null
        val parsedUri = try {
            if (audioFileOrUri.startsWith("/")) Uri.fromFile(File(audioFileOrUri)) else Uri.parse(audioFileOrUri)
        } catch (_: Throwable) {
            return null
        }
        val audioFilePath: String? = when {
            parsedUri.scheme == "file" || parsedUri.scheme.isNullOrBlank() -> parsedUri.path
            parsedUri.scheme == "content" && context != null -> resolveLocalPathFromContentUri(context, parsedUri)
            else -> null
        }
        val parentDir = audioFilePath?.let { File(it).parentFile } ?: return null
        if (!parentDir.exists() || !parentDir.isDirectory) return null

        val dirCacheKey = "${parentDir.absolutePath}::${parentDir.lastModified()}"
        synchronized(folderCoverPathCache) {
            val cachedPath = folderCoverPathCache.get(dirCacheKey)
            if (cachedPath != null) {
                if (cachedPath == NO_FOLDER_COVER_SENTINEL) return null
                val cachedFile = File(cachedPath)
                if (cachedFile.exists() && cachedFile.isFile) return cachedFile
            }
        }

        if (!isDedicatedAlbumDirectory(parentDir)) {
            synchronized(folderCoverPathCache) {
                folderCoverPathCache.put(dirCacheKey, NO_FOLDER_COVER_SENTINEL)
            }
            return null
        }

        val children = parentDir.listFiles() ?: return null
        val filesByLowerName = children
            .filter { it.isFile && it.length() in 32L..(25L * 1024L * 1024L) }
            .associateBy { it.name.lowercase(Locale.US) }

        val matchedFile = FOLDER_COVER_NAMES_LOWER
            .firstNotNullOfOrNull { candidateName -> filesByLowerName[candidateName] }

        synchronized(folderCoverPathCache) {
            folderCoverPathCache.put(dirCacheKey, matchedFile?.absolutePath ?: NO_FOLDER_COVER_SENTINEL)
        }
        return matchedFile
    }

    /**
     * Ensures sibling folder covers are only applied when [dir] represents a dedicated album
     * directory rather than a generic top-level multi-album dump folder (e.g., `/Music`, `/Download`).
     */
    private fun isDedicatedAlbumDirectory(dir: File): Boolean {
        val dirNameLower = dir.name.trim().lowercase(Locale.US)
        if (dirNameLower in GENERIC_MULTI_ALBUM_FOLDER_NAMES) {
            return false
        }
        val children = dir.listFiles() ?: return false
        // If a directory contains multiple subdirectories that themselves contain audio tracks,
        // treat the parent as a multi-album collection root and do not apply its loose image across albums.
        val audioSubdirs = children.count { sub ->
            sub.isDirectory && (sub.listFiles()?.any { f ->
                f.isFile && f.extension.lowercase(Locale.US) in AUDIO_EXTENSIONS
            } == true)
        }
        return audioSubdirs <= 1
    }

    private fun resolveLocalPathFromContentUri(context: Context, uri: Uri): String? {
        return try {
            @Suppress("DEPRECATION")
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (idx >= 0 && !cursor.isNull(idx)) cursor.getString(idx) else null
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun findSiblingFolderCover(
        context: Context,
        uri: Uri,
        maxDim: Int
    ): Bitmap? {
        val coverFile = discoverSiblingFolderCoverFile(uri.toString(), context) ?: return null
        return decodeFileHighClarity(coverFile, maxDim)
    }

    fun clearCache() {
        synchronized(bitmapCache) {
            bitmapCache.evictAll()
            rawBitmapRefCache.evictAll()
            negativeLookupCache.evictAll()
            folderCoverPathCache.evictAll()
        }
        sourceVersionTracker.clear()
        inFlightDecodes.clear()
    }
}

/**
 * YumaPlayer-inspired Artwork Surface that renders embedded/album/folder artwork at the appropriate
 * P4 [ArtworkResolutionTier] (THUMBNAIL ~360px, DETAIL up to 1024px, HERO up to 1440px),
 * respects centralized High Clarity and Crop Fill / Smart Fit settings, and falls back cleanly
 * to a deterministic Liquid Glass gradient + vector icon.
 */
@Composable
fun BnjArtworkSurface(
    titleSeed: String,
    mediaUri: String = "",
    isVideo: Boolean = false,
    fallbackIcon: ImageVector = if (isVideo) BnjIcons.VideoSelected else BnjIcons.MusicSelected,
    shape: Shape = RoundedCornerShape(16.dp),
    badgeText: String? = null,
    showPlayOverlay: Boolean = false,
    iconSize: Dp = 28.dp,
    highClarity: Boolean = LocalLiquidGlassConfig.current.artworkHighClarityEnabled,
    resolutionTier: ArtworkResolutionTier = ArtworkResolutionTier.THUMBNAIL,
    contentScale: ContentScale = if (LocalLiquidGlassConfig.current.artworkFitMode == ArtworkFitMode.SMART_FIT) {
        ContentScale.Fit
    } else {
        ContentScale.Crop
    },
    onPaletteExtracted: ((ExtractedArtworkPalette) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveTier = remember(resolutionTier, highClarity) {
        // Preserve backward compatibility for callers passing highClarity = true expecting HERO tier
        if (resolutionTier == ArtworkResolutionTier.THUMBNAIL && highClarity && onPaletteExtracted != null) {
            ArtworkResolutionTier.HERO
        } else {
            resolutionTier
        }
    }

    var artworkBitmap by remember(mediaUri, highClarity, effectiveTier) {
        mutableStateOf(BnjArtworkCache.getCachedFastInMemory(mediaUri, highClarity, effectiveTier))
    }

    LaunchedEffect(mediaUri, isVideo, highClarity, effectiveTier, titleSeed, onPaletteExtracted != null) {
        if (mediaUri.isNotBlank()) {
            val callback = onPaletteExtracted
            if (callback != null) {
                val (bmp, palette) = BnjArtworkCache.loadArtworkAndPalette(
                    context = context,
                    uriString = mediaUri,
                    isVideo = isVideo,
                    highClarity = highClarity,
                    titleSeed = titleSeed,
                    tier = effectiveTier
                )
                artworkBitmap = bmp
                callback.invoke(palette)
            } else {
                val bmp = BnjArtworkCache.loadOrExtractArtwork(
                    context = context,
                    uriString = mediaUri,
                    isVideo = isVideo,
                    highClarity = highClarity,
                    tier = effectiveTier,
                    extractPalette = false
                )
                artworkBitmap = bmp
            }
        } else {
            artworkBitmap = null
            onPaletteExtracted?.invoke(BnjArtworkPaletteExtractor.fallbackPalette(titleSeed))
        }
    }

    val (gradStart, gradEnd) = remember(titleSeed) { artworkGradientPair(titleSeed) }
    val imageFilterQuality = if (highClarity) FilterQuality.High else FilterQuality.Medium

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        gradStart.copy(alpha = 0.85f),
                        gradEnd.copy(alpha = 0.65f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = artworkBitmap
        if (bmp != null) {
            // P4 Step 5: In Smart Fit (ContentScale.Fit) mode, provide a subtle blurred ambient
            // backdrop extension when artwork is non-square so complete artwork is preserved without harsh bars.
            if (contentScale == ContentScale.Fit && bmp.width != bmp.height) {
                Image(
                    bitmap = bmp,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(18.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.45f,
                    filterQuality = FilterQuality.Low
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.24f))
                )
            }
            Image(
                bitmap = bmp,
                contentDescription = titleSeed,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                filterQuality = imageFilterQuality
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(iconSize)
            )
        }

        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.58f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }

        if (showPlayOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = BnjIcons.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * YumaPlayer-style Square Artwork Card for horizontal carousels and album/playlist grids.
 * Supports [resolutionTier] (defaults to THUMBNAIL for compact cards, or DETAIL for featured cards).
 */
@Composable
fun BnjSquareArtworkCard(
    title: String,
    subtitle: String,
    mediaUri: String = "",
    isVideo: Boolean = false,
    fallbackIcon: ImageVector = if (isVideo) BnjIcons.VideoSelected else BnjIcons.MusicSelected,
    badgeText: String? = null,
    progressFraction: Float? = null,
    width: Dp = 136.dp,
    resolutionTier: ArtworkResolutionTier = ArtworkResolutionTier.THUMBNAIL,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val artCorner = (glassConfig.artworkCornerRadiusDp * 0.62f).coerceIn(8f, 20f).dp
    val cardShape = if (glassConfig.artworkDisplayStyle == ArtworkDisplayStyle.VINYL_DISC && !isVideo) {
        RoundedCornerShape(18.dp)
    } else {
        RoundedCornerShape(artCorner)
    }

    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BnjArtworkSurface(
            titleSeed = title,
            mediaUri = mediaUri,
            isVideo = isVideo,
            fallbackIcon = fallbackIcon,
            shape = cardShape,
            badgeText = badgeText,
            showPlayOverlay = true,
            iconSize = 26.dp,
            highClarity = glassConfig.artworkHighClarityEnabled,
            resolutionTier = resolutionTier,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )

        if (progressFraction != null && progressFraction > 0f) {
            val config = LocalLiquidGlassConfig.current
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0.04f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass),
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * OTT-inspired Cinematic Featured Video Hero Banner driven strictly by real local video metadata.
 */
@Composable
fun BnjFeaturedVideoHeroCard(
    title: String,
    subtitle: String,
    mediaUri: String,
    resolutionBadge: String? = null,
    hdrBadge: String? = null,
    durationBadge: String? = null,
    progressFraction: Float? = null,
    isResumable: Boolean = false,
    isFavorite: Boolean = false,
    onPlayOrResume: () -> Unit,
    onPlayFloating: (() -> Unit)? = null,
    onOpenDetails: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val accent = adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass)
    val heroCorner = (config.glassCornerRadiusDp + 2f).coerceIn(10f, 32f).dp
    val rimAlpha = config.borderGlowAlpha.coerceIn(0f, 1f)
    val borderColor = if (config.isDarkGlass) {
        accent.copy(alpha = (0.14f + 0.38f * rimAlpha).coerceIn(0.06f, 0.72f))
    } else {
        Color(0xFF94A3B8).copy(alpha = (0.28f + 0.42f * rimAlpha).coerceIn(0.12f, 0.78f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 196.dp, max = 224.dp)
            .clip(RoundedCornerShape(heroCorner))
            .then(
                if (rimAlpha > 0.02f) {
                    Modifier.border((0.8f + 0.8f * rimAlpha).dp, borderColor, RoundedCornerShape(heroCorner))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onPlayOrResume)
            .testTag("video_hero_banner")
    ) {
        BnjArtworkSurface(
            titleSeed = title,
            mediaUri = mediaUri,
            isVideo = true,
            fallbackIcon = BnjIcons.VideoSelected,
            shape = RoundedCornerShape(heroCorner),
            badgeText = null,
            showPlayOverlay = false,
            iconSize = 38.dp,
            modifier = Modifier.fillMaxSize()
        )

        // Cinematic dark vignette + bottom scrim so text/actions stay legible on any video frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.45f),
                        0.32f to Color.Black.copy(alpha = 0.14f),
                        0.62f to Color(0xFF05070B).copy(alpha = 0.72f),
                        1.0f to Color(0xFF05070B).copy(alpha = 0.94f)
                    )
                )
        )

        // Top badge bar: Featured / Continue Watching pill + HDR / Resolution / Duration
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.24f))
                    .border(0.8.dp, accent.copy(alpha = 0.70f), CircleShape)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isResumable) "Continue Watching" else "Featured",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!hdrBadge.isNullOrBlank() && !hdrBadge.equals("SDR", ignoreCase = true)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(0.8.dp, GlassAmberTint.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = hdrBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GlassAmberTint,
                            maxLines = 1
                        )
                    }
                }
                if (!resolutionBadge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = resolutionBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
                if (!durationBadge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = durationBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Bottom content overlay: Title, metadata, optional progress, and OTT action row
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isResumable && progressFraction != null && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0.04f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = config.dynamicAccent,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary OTT Play / Resume CTA
                Row(
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable(onClick = onPlayOrResume)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("video_hero_play_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = BnjIcons.Play,
                        contentDescription = null,
                        tint = Color(0xFF05070B),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isResumable) "Resume" else "Play",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF05070B),
                        maxLines = 1
                    )
                }

                if (onPlayFloating != null) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                            .clickable(onClick = onPlayFloating)
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                            .testTag("video_hero_floating_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = BnjIcons.PictureInPicture,
                            contentDescription = "Open Floating Video Player",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Floating",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onToggleFavorite != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                            .clickable(onClick = onToggleFavorite)
                            .testTag("video_hero_favorite_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                            contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                            tint = if (isFavorite) GlassRoseTint else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onOpenDetails != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                            .clickable(onClick = onOpenDetails)
                            .testTag("video_hero_details_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = BnjIcons.MoreOptions,
                            contentDescription = "Video Details & Options",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 16:9 OTT Widescreen Video Card for horizontal carousels ("Continue Watching", "Recently Played",
 * "Recently Added", "4K UHD", "HDR Videos", etc.) with natural cinema proportions, bottom scrim,
 * sleek progress bar, and non-overlapping HDR, resolution, and duration badges.
 */
@Composable
fun BnjWidescreenVideoCard(
    title: String,
    subtitle: String,
    mediaUri: String = "",
    resolutionBadge: String? = null,
    hdrBadge: String? = null,
    durationBadge: String? = null,
    progressFraction: Float? = null,
    width: Dp = 188.dp,
    isFavorite: Boolean = false,
    onMoreOptions: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val cardCorner = config.glassCornerRadiusDp.coerceIn(8f, 32f).dp
    val thumbCorner = (config.glassCornerRadiusDp - 4f).coerceIn(6f, 24f).dp

    Column(
        modifier = modifier
            .width(width)
            .liquidGlassSurface(
                shape = RoundedCornerShape(cardCorner),
                elevated = false,
                tier = GlassTier.FROSTED,
                componentTarget = GlassComponentTarget.CARDS
            )
            .clickable(onClick = onClick)
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(thumbCorner))
        ) {
            BnjArtworkSurface(
                titleSeed = title,
                mediaUri = mediaUri,
                isVideo = true,
                fallbackIcon = BnjIcons.VideoSelected,
                shape = RoundedCornerShape(thumbCorner),
                badgeText = null,
                showPlayOverlay = false,
                iconSize = 24.dp,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom subtle cinema gradient overlay for crisp duration & progress legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.72f)
                        )
                    )
            )

            // Top-start subtle favorite indicator when favorited
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = BnjIcons.FavoriteFilled,
                        contentDescription = "Favorite",
                        tint = GlassRoseTint,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // Top-right unified Resolution + HDR badge strip (never overlapping)
            if (!resolutionBadge.isNullOrBlank() || (!hdrBadge.isNullOrBlank() && !hdrBadge.equals("SDR", ignoreCase = true))) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!hdrBadge.isNullOrBlank() && !hdrBadge.equals("SDR", ignoreCase = true)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.Black.copy(alpha = 0.78f))
                                .border(0.8.dp, config.dynamicAccent.copy(alpha = 0.68f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = hdrBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = config.dynamicAccent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                    if (!resolutionBadge.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.Black.copy(alpha = 0.78f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = resolutionBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bottom-start compact play cue
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 6.dp, bottom = if (progressFraction != null && progressFraction > 0f) 7.dp else 6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.68f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.38f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = BnjIcons.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }

            // Bottom-right duration badge
            if (!durationBadge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = if (progressFraction != null && progressFraction > 0f) 7.dp else 6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.Black.copy(alpha = 0.80f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = durationBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Bottom edge OTT playback progress bar overlaid cleanly
            if (progressFraction != null && progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0.04f, 1f) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = config.dynamicAccent,
                    trackColor = Color.White.copy(alpha = 0.24f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onMoreOptions != null) {
                IconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = BnjIcons.MoreOptions,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * YumaPlayer-style Compact Song / Media Row showing only essential information:
 * Artwork, Title, Artist, optional compact format badge, Favorite toggle, and More Options.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BnjCompactMediaRow(
    title: String,
    artist: String,
    mediaUri: String = "",
    isVideo: Boolean = false,
    formatBadge: String? = null,
    isFavorite: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onMoreOptions: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val palette = resolveDynamicGlassPalette(
        config = resolveEffectiveConfigForComponent(config, GlassComponentTarget.CARDS),
        tier = if (isSelected) GlassTier.ACTIVE else GlassTier.CLEAR,
        elevated = isSelected
    )
    val resolvedCyan = palette.effectiveAccent
    val rowCorner = (config.glassCornerRadiusDp - 2f).coerceIn(8f, 28f).dp
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { currentOnClick() },
            onLongClick = { currentOnLongClick?.invoke() }
        )
    } else {
        Modifier.clickable(onClick = { currentOnClick() })
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { selected = isSelected }
            .liquidGlassSurface(
                shape = RoundedCornerShape(rowCorner),
                elevated = isSelected,
                tier = if (isSelected) GlassTier.ACTIVE else GlassTier.CLEAR,
                componentTarget = GlassComponentTarget.CARDS
            )
            .then(
                if (isSelected) {
                    Modifier
                        .background(resolvedCyan.copy(alpha = 0.12f), RoundedCornerShape(rowCorner))
                        .border(1.2.dp, resolvedCyan.copy(alpha = 0.65f), RoundedCornerShape(rowCorner))
                } else {
                    Modifier
                }
            )
            .then(clickModifier)
            .padding(start = 8.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                BnjArtworkSurface(
                    titleSeed = title,
                    mediaUri = mediaUri,
                    isVideo = isVideo,
                    shape = RoundedCornerShape(10.dp),
                    iconSize = 18.dp,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected || isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) resolvedCyan else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            )
                            .border(
                                width = 1.2.dp,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else resolvedCyan.copy(alpha = 0.65f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = BnjIcons.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!formatBadge.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(resolvedCyan.copy(alpha = 0.14f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = formatBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = resolvedCyan,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onToggleFavorite != null) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) BnjIcons.FavoriteFilled else BnjIcons.FavoriteOutline,
                        contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        tint = if (isFavorite) adaptiveGlassAccent(GlassRoseTint, config.isDarkGlass) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (onMoreOptions != null) {
                IconButton(
                    onClick = onMoreOptions,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = BnjIcons.MoreOptions,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact YumaPlayer-inspired Section Header without verbose descriptive subtitles.
 */
@Composable
fun BnjSectionHeader(
    title: String,
    countBadge: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val accent = adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!countBadge.isNullOrBlank()) {
                LiquidGlassPillBadge(
                    text = countBadge,
                    color = accent
                )
            }
        }

        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Horizontally scrollable YumaPlayer-style category chips row.
 */
@Composable
fun <T> BnjCategoryChipsBar(
    categories: List<T>,
    selectedCategory: T,
    labelProvider: (T) -> String,
    iconProvider: ((T) -> ImageVector?)? = null,
    onSelectCategory: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = resolveEffectiveConfigForComponent(LocalLiquidGlassConfig.current, GlassComponentTarget.CARDS)
    val palette = resolveDynamicGlassPalette(
        config = config,
        tier = GlassTier.CLEAR,
        elevated = false
    )
    val unselectedBg = palette.chipUnselectedColor
    val accent = palette.effectiveAccent
    val showBorder = config.glassBorderEnabled && config.borderGlowAlpha > 0.01f
    val borderWidth = config.glassRimThicknessDp.coerceIn(0.5f, 3f).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { cat ->
            val selected = cat == selectedCategory
            val bgColor by animateColorAsState(
                targetValue = if (selected) palette.selectedSurfaceColor else unselectedBg,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "chipBg"
            )
            val borderColor = if (selected) {
                palette.selectedBorderColor
            } else {
                palette.borderColor
            }
            val textColor = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            val icon = iconProvider?.invoke(cat)

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bgColor)
                    .then(
                        if (selected || showBorder) {
                            Modifier.border(borderWidth, borderColor, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectCategory(cat) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = labelProvider(cat),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

/**
 * Reusable Liquid Glass Segmented Control.
 */
@Composable
fun <T> BnjSegmentedControl(
    options: List<T>,
    selectedOption: T,
    labelProvider: (T) -> String,
    onSelectOption: (T) -> Unit,
    accentColor: Color = LocalLiquidGlassConfig.current.dynamicAccent,
    modifier: Modifier = Modifier
) {
    val config = resolveEffectiveConfigForComponent(LocalLiquidGlassConfig.current, GlassComponentTarget.CARDS)
    val palette = resolveDynamicGlassPalette(
        config = config,
        tier = GlassTier.CLEAR,
        accentTint = accentColor,
        elevated = false
    )
    val trackBg = palette.chipUnselectedColor
    val resolvedAccent = palette.effectiveAccent
    val outerCorner = (config.glassCornerRadiusDp * 0.72f).coerceIn(8f, 20f).dp
    val innerCorner = (config.glassCornerRadiusDp * 0.60f).coerceIn(6f, 18f).dp
    val showBorder = config.glassBorderEnabled && config.borderGlowAlpha > 0.01f
    val borderWidth = config.glassRimThicknessDp.coerceIn(0.5f, 3f).dp

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(outerCorner))
            .background(trackBg)
            .then(
                if (showBorder) {
                    Modifier.border(borderWidth, palette.borderColor, RoundedCornerShape(outerCorner))
                } else {
                    Modifier
                }
            )
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val selected = option == selectedOption
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(innerCorner))
                    .background(
                        if (selected) palette.selectedSurfaceColor else Color.Transparent
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) palette.selectedBorderColor else Color.Transparent,
                        shape = RoundedCornerShape(innerCorner)
                    )
                    .clickable { onSelectOption(option) }
                    .padding(vertical = 5.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(option),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Minimal, friendly Liquid Glass Empty State Card.
 */
@Composable
fun BnjEmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val resolvedAccent = adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass)
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        accentTint = config.secondaryAccent,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(resolvedAccent.copy(alpha = 0.14f))
                    .border(1.dp, resolvedAccent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                Spacer(Modifier.height(4.dp))
                LiquidGlassPillButton(
                    onClick = onActionClick,
                    selected = true,
                    accentColor = config.dynamicAccent,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * Determines whether an external state update ([externalValue]) should overwrite the local
 * slider position ([localValue]).
 *
 * Prevents slider thumb jitter/vibration caused by asynchronous DataStore/ViewModel emissions
 * arriving mid-drag or immediately after finger release.
 */
fun shouldSyncExternalSliderValue(
    localValue: Float,
    externalValue: Float,
    isDragging: Boolean,
    nowTimestampMs: Long = 0L,
    lastReleaseTimestampMs: Long = 0L,
    postReleaseSettleWindowMs: Long = 240L,
    epsilon: Float = 0.0001f
): Boolean {
    if (isDragging) return false
    if (kotlin.math.abs(localValue - externalValue) <= epsilon) return false
    if (lastReleaseTimestampMs > 0L && nowTimestampMs >= lastReleaseTimestampMs) {
        val elapsed = nowTimestampMs - lastReleaseTimestampMs
        if (elapsed < postReleaseSettleWindowMs) {
            return false
        }
    }
    return true
}

/**
 * Resolves the stable playback position to display on a Video or Music Player seek bar.
 *
 * - While [isDragging] is true, strictly follows [dragPositionMs] so periodic ExoPlayer ticks
 *   never fight with the user's finger.
 * - Immediately after finger release ([elapsedSinceSeekReleaseMs] < [settleWindowMs]), holds
 *   [pendingSeekTargetMs] until ExoPlayer's reported [livePositionMs] catches up within
 *   [catchUpToleranceMs], preventing 1-2 frame post-release thumb snapback.
 */
fun resolveStableSeekPositionMs(
    livePositionMs: Long,
    dragPositionMs: Long,
    durationMs: Long,
    isDragging: Boolean,
    pendingSeekTargetMs: Long = -1L,
    elapsedSinceSeekReleaseMs: Long = Long.MAX_VALUE,
    settleWindowMs: Long = 450L,
    catchUpToleranceMs: Long = 1200L
): Long {
    val safeDuration = durationMs.coerceAtLeast(1000L)
    if (isDragging) {
        return dragPositionMs.coerceIn(0L, safeDuration)
    }
    if (pendingSeekTargetMs >= 0L && elapsedSinceSeekReleaseMs in 0L until settleWindowMs) {
        val targetClamped = pendingSeekTargetMs.coerceIn(0L, safeDuration)
        val liveClamped = livePositionMs.coerceIn(0L, safeDuration)
        if (kotlin.math.abs(liveClamped - targetClamped) > catchUpToleranceMs) {
            return targetClamped
        }
    }
    return livePositionMs.coerceIn(0L, safeDuration)
}

/**
 * Unified, jitter-free compact slider for BNJ MEDIA PRO Settings, Equalizer/DSP, and Quick Controls.
 *
 * Features:
 * - Decouples active finger drag state ([localValue]) from asynchronous DataStore/ViewModel
 *   recomposition loops so the thumb never jumps, vibrates, or resets mid-drag.
 * - Renders a slim 3.5dp rounded track (4.2dp while dragging) and a compact 13dp circular thumb
 *   (15dp while dragging) without Material 3's oversized vertical handle bar or track-gap cutout jitter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BnjCompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    accentColor: Color = LocalLiquidGlassConfig.current.dynamicAccent,
    inactiveTrackColor: Color? = null,
    bipolarCenter: Boolean = false
) {
    val config = LocalLiquidGlassConfig.current
    val resolvedAccent = adaptiveGlassAccent(
        resolveContextualAccent(accentColor, config),
        config.isDarkGlass
    )
    val resolvedInactiveTrack = inactiveTrackColor ?: if (config.isDarkGlass) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.12f)
    }

    val clampedExternal = value.coerceIn(valueRange.start, valueRange.endInclusive)
    var localValue by remember { mutableFloatStateOf(clampedExternal) }
    var isDragging by remember { mutableStateOf(false) }
    var lastReleaseTimestampMs by remember { mutableLongStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(clampedExternal, isDragging, lastReleaseTimestampMs, valueRange.start, valueRange.endInclusive) {
        if (isDragging) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (!shouldSyncExternalSliderValue(
                localValue = localValue,
                externalValue = clampedExternal,
                isDragging = false,
                nowTimestampMs = now,
                lastReleaseTimestampMs = lastReleaseTimestampMs,
                postReleaseSettleWindowMs = 240L
            )
        ) {
            val remaining = (240L - (now - lastReleaseTimestampMs)).coerceIn(16L, 240L)
            kotlinx.coroutines.delay(remaining)
        }
        if (!isDragging) {
            val latestClamped = value.coerceIn(valueRange.start, valueRange.endInclusive)
            if (kotlin.math.abs(localValue - latestClamped) > 0.0001f) {
                localValue = latestClamped
            }
        }
    }

    val displayValue = localValue.coerceIn(valueRange.start, valueRange.endInclusive)
    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val normalizedFraction = ((displayValue - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    Slider(
        value = displayValue,
        onValueChange = { newVal ->
            val clamped = newVal.coerceIn(valueRange.start, valueRange.endInclusive)
            isDragging = true
            localValue = clamped
            onValueChange(clamped)
        },
        onValueChangeFinished = {
            lastReleaseTimestampMs = System.currentTimeMillis()
            isDragging = false
            onValueChangeFinished?.invoke()
        },
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp),
        colors = SliderDefaults.colors(
            thumbColor = if (enabled) resolvedAccent else resolvedAccent.copy(alpha = 0.45f),
            activeTrackColor = if (enabled) resolvedAccent else resolvedAccent.copy(alpha = 0.45f),
            inactiveTrackColor = resolvedInactiveTrack
        ),
        thumb = {
            val thumbDiameter = if (isDragging) 15.dp else 13.dp
            val haloDiameter = if (isDragging) 21.dp else 15.dp
            val thumbFill = if (enabled) resolvedAccent else resolvedAccent.copy(alpha = 0.45f)
            val rimColor = if (config.isDarkGlass) {
                Color.White.copy(alpha = if (isDragging) 0.92f else 0.75f)
            } else {
                Color.White.copy(alpha = 0.95f)
            }
            Box(
                modifier = Modifier.size(haloDiameter),
                contentAlignment = Alignment.Center
            ) {
                if (isDragging && enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(thumbFill.copy(alpha = 0.24f), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(thumbDiameter)
                        .clip(CircleShape)
                        .background(thumbFill, CircleShape)
                        .border(1.2.dp, rimColor, CircleShape)
                )
            }
        },
        track = {
            val activeColor = if (enabled) resolvedAccent else resolvedAccent.copy(alpha = 0.42f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            ) {
                val trackHeightPx = (if (isDragging) 4.2.dp else 3.4.dp).toPx()
                val cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                val topY = (size.height - trackHeightPx) / 2f

                // 1. Unplayed / inactive full track
                drawRoundRect(
                    color = resolvedInactiveTrack,
                    topLeft = Offset(0f, topY),
                    size = ComposeSize(size.width, trackHeightPx),
                    cornerRadius = cornerRadius
                )

                // 2. Active track segment
                val isBipolar = bipolarCenter && valueRange.start < 0f && valueRange.endInclusive > 0f
                if (isBipolar) {
                    val zeroFraction = ((0f - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
                    val centerX = size.width * zeroFraction
                    val currentX = size.width * normalizedFraction
                    val startX = kotlin.math.min(centerX, currentX)
                    val segWidth = kotlin.math.abs(currentX - centerX)
                    if (segWidth > 0.5f) {
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(startX, topY),
                            size = ComposeSize(segWidth, trackHeightPx),
                            cornerRadius = cornerRadius
                        )
                    }
                    // Subtle center notch
                    drawCircle(
                        color = activeColor.copy(alpha = 0.65f),
                        radius = trackHeightPx * 0.55f,
                        center = Offset(centerX, size.height / 2f)
                    )
                } else {
                    val activeWidth = (size.width * normalizedFraction).coerceIn(0f, size.width)
                    if (activeWidth > 0.5f) {
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(0f, topY),
                            size = ComposeSize(activeWidth, trackHeightPx),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }
    )
}

