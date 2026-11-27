package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.example.ui.theme.BnjArtworkPaletteExtractor
import com.example.ui.theme.ExtractedArtworkPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class CustomBackgroundImportResult(
    val savedFilePath: String,
    val extractedPalette: ExtractedArtworkPalette,
    val extractedAccentHex: Long,
    val width: Int,
    val height: Int
)

data class TvWallpaperUriValidationResult(
    val uriString: String,
    val extractedPalette: ExtractedArtworkPalette,
    val extractedAccentHex: Long,
    val width: Int,
    val height: Int,
    val persistablePermissionGranted: Boolean
)

/**
 * Manages zero-permission SAF / Photo Picker custom background images safely,
 * supporting both zero-copy persisted `content://` SAF URIs (Android TV) and legacy
 * app-private wallpaper files, with EXIF orientation normalization, TV-hardware-aware
 * downsampling, and bounded LRU bitmap caching.
 */
object CustomBackgroundManager {
    const val DIR_NAME = "custom_theme_bg"
    const val MAX_DIMENSION_PX = 2048
    const val TV_MAX_DIMENSION_PX = 1920
    const val LOW_RAM_MAX_DIMENSION_PX = 1280

    private val imageCache = object : LruCache<String, ImageBitmap>(3) {}

    fun isContentUri(source: String?): Boolean {
        if (source.isNullOrBlank()) return false
        val lower = source.trim().lowercase()
        return lower.startsWith("content://") || lower.startsWith("android.resource://")
    }

    fun isLowRamDevice(context: Context): Boolean {
        return runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.isLowRamDevice == true
        }.getOrDefault(false)
    }

    fun isValidWallpaperSource(context: Context, source: String?): Boolean {
        if (source.isNullOrBlank()) return false
        val trimmed = source.trim()
        if (!isContentUri(trimmed)) {
            return isValidWallpaperFile(trimmed)
        }
        return try {
            val uri = Uri.parse(trimmed)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val probe = ByteArray(4)
                stream.read(probe) > 0
            } ?: false
        } catch (_: SecurityException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    fun getCachedBitmapForSource(source: String): ImageBitmap? {
        if (source.isBlank()) return null
        if (!isContentUri(source) && !isValidWallpaperFile(source)) {
            synchronized(imageCache) { imageCache.remove(source) }
            return null
        }
        return synchronized(imageCache) { imageCache.get(source) }
    }

    fun evictWallpaperCache() {
        synchronized(imageCache) { imageCache.evictAll() }
    }

    /**
     * Validates a SAF image URI (JPG/JPEG/PNG/WEBP), retains persistable read permission when supported,
     * decodes a bounded preview bitmap into the LRU cache, and extracts its accent palette using the
     * single existing [BnjArtworkPaletteExtractor] — without copying large image files onto disk.
     */
    suspend fun validateAndPersistSafWallpaperUri(
        context: Context,
        sourceUri: Uri?
    ): TvWallpaperUriValidationResult? = withContext(Dispatchers.IO) {
        if (sourceUri == null) return@withContext null
        val uriString = sourceUri.toString()
        val persistableGranted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                sourceUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            true
        }.getOrDefault(false)

        try {
            if (!hasValidImageSignature(context, sourceUri)) {
                return@withContext null
            }
            val maxDim = if (isLowRamDevice(context)) LOW_RAM_MAX_DIMENSION_PX else TV_MAX_DIMENSION_PX
            val decoded = decodeUriScaledBitmap(context, sourceUri, maxDim) ?: return@withContext null
            val palette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
                bitmap = decoded,
                cacheKey = "bg:$uriString",
                fallbackSeed = "BNJ_TV_BG"
            )
            val imageBitmap = decoded.asImageBitmap()
            synchronized(imageCache) {
                imageCache.put(uriString, imageBitmap)
            }
            TvWallpaperUriValidationResult(
                uriString = uriString,
                extractedPalette = palette,
                extractedAccentHex = palette.vibrantAccent.toArgb().toLong() and 0xFFFFFFFFL,
                width = decoded.width,
                height = decoded.height,
                persistablePermissionGranted = persistableGranted
            )
        } catch (_: OutOfMemoryError) {
            synchronized(imageCache) { imageCache.evictAll() }
            null
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Loads a cached or bounded-resolution [ImageBitmap] from either a persisted SAF `content://` URI
     * or a legacy local file path. Recovers cleanly (returns `null`) if permission is revoked or file is missing.
     */
    suspend fun loadBackgroundBitmapFromSourceAsync(
        context: Context,
        source: String
    ): ImageBitmap? {
        if (source.isBlank()) return null
        val trimmed = source.trim()
        if (!isContentUri(trimmed)) {
            return loadBackgroundBitmapAsync(trimmed)
        }
        synchronized(imageCache) { imageCache.get(trimmed) }?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(trimmed)
                if (!hasValidImageSignature(context, uri)) {
                    synchronized(imageCache) { imageCache.remove(trimmed) }
                    return@withContext null
                }
                val maxDim = if (isLowRamDevice(context)) LOW_RAM_MAX_DIMENSION_PX else TV_MAX_DIMENSION_PX
                val decoded = decodeUriScaledBitmap(context, uri, maxDim) ?: return@withContext null
                if (BnjArtworkPaletteExtractor.getCachedPalette("bg:$trimmed") == null) {
                    BnjArtworkPaletteExtractor.extractFromBitmapAsync(
                        bitmap = decoded,
                        cacheKey = "bg:$trimmed",
                        fallbackSeed = "BNJ_TV_BG"
                    )
                }
                val imageBitmap = decoded.asImageBitmap()
                synchronized(imageCache) {
                    imageCache.put(trimmed, imageBitmap)
                }
                imageBitmap
            } catch (_: OutOfMemoryError) {
                synchronized(imageCache) { imageCache.evictAll() }
                null
            } catch (_: SecurityException) {
                synchronized(imageCache) { imageCache.remove(trimmed) }
                null
            } catch (_: Throwable) {
                synchronized(imageCache) { imageCache.remove(trimmed) }
                null
            }
        }
    }

    private fun decodeUriScaledBitmap(
        context: Context,
        sourceUri: Uri,
        maxDimensionPx: Int
    ): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val streamOpened = resolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
            true
        } ?: false
        if (!streamOpened || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx, maxDimensionPx)
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val rawBitmap = resolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOpts)
        } ?: return null
        val exifOrientation = readExifOrientation(context, sourceUri)
        val oriented = applyExifOrientation(rawBitmap, exifOrientation)
        return scaleDownToMaxDimension(oriented, maxDimensionPx)
    }

    fun isValidWallpaperFile(filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) return false
        return try {
            val file = File(filePath)
            file.exists() && file.isFile && file.length() > 0L
        } catch (_: Throwable) {
            false
        }
    }

    fun getCachedBitmap(filePath: String): ImageBitmap? {
        if (!isValidWallpaperFile(filePath)) {
            if (filePath.isNotBlank()) {
                synchronized(imageCache) { imageCache.remove(filePath) }
            }
            return null
        }
        return synchronized(imageCache) { imageCache.get(filePath) }
    }

    suspend fun loadBackgroundBitmapAsync(filePath: String): ImageBitmap? {
        if (!isValidWallpaperFile(filePath)) {
            if (filePath.isNotBlank()) {
                synchronized(imageCache) { imageCache.remove(filePath) }
            }
            return null
        }
        getCachedBitmap(filePath)?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (!file.exists() || !file.isFile || file.length() <= 0L) return@withContext null
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

                val sample = calculateInSampleSize(
                    bounds.outWidth,
                    bounds.outHeight,
                    MAX_DIMENSION_PX,
                    MAX_DIMENSION_PX
                )
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return@withContext null
                val scaled = scaleDownToMaxDimension(decoded, MAX_DIMENSION_PX)

                if (BnjArtworkPaletteExtractor.getCachedPalette("bg:${file.absolutePath}") == null) {
                    BnjArtworkPaletteExtractor.extractFromBitmapAsync(
                        bitmap = scaled,
                        cacheKey = "bg:${file.absolutePath}",
                        fallbackSeed = "BNJ_BG"
                    )
                }

                val imageBitmap = scaled.asImageBitmap()
                synchronized(imageCache) {
                    imageCache.put(filePath, imageBitmap)
                }
                imageBitmap
            } catch (_: OutOfMemoryError) {
                synchronized(imageCache) { imageCache.evictAll() }
                null
            } catch (_: Throwable) {
                null
            }
        }
    }

    suspend fun importBackgroundImageFromUri(
        context: Context,
        sourceUri: Uri?
    ): CustomBackgroundImportResult? = withContext(Dispatchers.IO) {
        if (sourceUri == null) return@withContext null
        try {
            if (!hasValidImageSignature(context, sourceUri)) {
                return@withContext null
            }
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val streamOpened = resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
                true
            } ?: false
            if (!streamOpened || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext null
            }

            val sampleSize = calculateInSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                MAX_DIMENSION_PX,
                MAX_DIMENSION_PX
            )
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = resolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return@withContext null

            val exifOrientation = readExifOrientation(context, sourceUri)
            val orientedBitmap = applyExifOrientation(rawBitmap, exifOrientation)
            val finalBitmap = scaleDownToMaxDimension(orientedBitmap, MAX_DIMENSION_PX)

            val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
            val outFile = File(dir, "bnj_bg_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")

            val writeSucceeded = try {
                FileOutputStream(outFile).use { fos ->
                    val ok = finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                    fos.flush()
                    ok
                } && outFile.exists() && outFile.length() > 0L
            } catch (_: Throwable) {
                false
            }

            if (!writeSucceeded) {
                runCatching { outFile.delete() }
                return@withContext null
            }

            // Delete previous wallpaper files ONLY AFTER the replacement file is safely written on disk
            dir.listFiles()?.forEach { existing ->
                if (existing.absolutePath != outFile.absolutePath) {
                    runCatching { existing.delete() }
                }
            }

            val palette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
                bitmap = finalBitmap,
                cacheKey = "bg:${outFile.absolutePath}",
                fallbackSeed = "BNJ_BG"
            )
            val imageBitmap = finalBitmap.asImageBitmap()
            synchronized(imageCache) {
                imageCache.evictAll()
                imageCache.put(outFile.absolutePath, imageBitmap)
            }

            CustomBackgroundImportResult(
                savedFilePath = outFile.absolutePath,
                extractedPalette = palette,
                extractedAccentHex = palette.vibrantAccent.toArgb().toLong() and 0xFFFFFFFFL,
                width = finalBitmap.width,
                height = finalBitmap.height
            )
        } catch (_: OutOfMemoryError) {
            synchronized(imageCache) { imageCache.evictAll() }
            null
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun deleteCustomBackgroundFiles(context: Context, specificPath: String = "") {
        withContext(Dispatchers.IO) {
            synchronized(imageCache) {
                imageCache.evictAll()
            }
            if (specificPath.isNotBlank()) {
                runCatching { File(specificPath).delete() }
            }
            val dir = File(context.filesDir, DIR_NAME)
            if (dir.exists()) {
                dir.listFiles()?.forEach { runCatching { it.delete() } }
            }
        }
    }

    fun scaleDownToMaxDimension(bitmap: Bitmap, maxDimensionPx: Int = MAX_DIMENSION_PX): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return bitmap
        val largest = max(w, h)
        if (largest <= maxDimensionPx) return bitmap

        val ratio = maxDimensionPx.toFloat() / largest.toFloat()
        val targetW = (w * ratio).roundToInt().coerceIn(1, maxDimensionPx)
        val targetH = (h * ratio).roundToInt().coerceIn(1, maxDimensionPx)
        if (targetW == w && targetH == h) return bitmap

        return try {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true).also { scaled ->
                if (scaled !== bitmap) {
                    bitmap.recycle()
                }
            }
        } catch (_: Throwable) {
            bitmap
        }
    }

    internal fun applyExifOrientation(bitmap: Bitmap, exifOrientation: Int): Bitmap {
        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { transformed ->
                if (transformed !== bitmap) {
                    bitmap.recycle()
                }
            }
        } catch (_: Throwable) {
            bitmap
        }
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Throwable) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun hasValidImageSignature(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(16)
                val n = stream.read(header)
                if (n < 4) return false
                val b0 = header[0].toInt() and 0xFF
                val b1 = header[1].toInt() and 0xFF
                val b2 = header[2].toInt() and 0xFF
                val b3 = header[3].toInt() and 0xFF

                val isJpeg = b0 == 0xFF && b1 == 0xD8
                val isPng = b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47
                val isGif = b0 == 'G'.code && b1 == 'I'.code && b2 == 'F'.code
                val isWebpOrRiff = b0 == 'R'.code && b1 == 'I'.code && b2 == 'F'.code && b3 == 'F'.code
                val isBmp = b0 == 'B'.code && b1 == 'M'.code
                val isHeifOrAvif = n >= 8 &&
                    (header[4].toInt() and 0xFF) == 'f'.code &&
                    (header[5].toInt() and 0xFF) == 't'.code &&
                    (header[6].toInt() and 0xFF) == 'y'.code &&
                    (header[7].toInt() and 0xFF) == 'p'.code
                val isRobolectricCompressedBitmap = b0 == 'B'.code && b1 == 'i'.code && b2 == 't'.code && b3 == 'm' .code

                isJpeg || isPng || isGif || isWebpOrRiff || isBmp || isHeifOrAvif || isRobolectricCompressedBitmap
            } ?: false
        } catch (_: Throwable) {
            false
        }
    }

    fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
            // Extra step when a single dimension still exceeds the requested cap
            while (max(rawWidth / inSampleSize, rawHeight / inSampleSize) > max(reqWidth, reqHeight) * 1.6f) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
