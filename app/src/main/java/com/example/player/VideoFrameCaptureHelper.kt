package com.example.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.media3.ui.PlayerView
import com.example.data.local.MediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object VideoFrameCaptureHelper {

    suspend fun captureAndSaveFrame(
        context: Context,
        playerView: PlayerView?,
        media: MediaEntity,
        positionMs: Long
    ): Result<String> {
        val bitmap: Bitmap = captureBitmapFromSurface(playerView)
            ?: withContext(Dispatchers.IO) {
                captureBitmapWithRetriever(context, media.uri, positionMs)
            }
            ?: return Result.failure(
                IllegalStateException("Could not capture a decoded video frame for the active stream.")
            )

        return withContext(Dispatchers.IO) {
            try {
                val safeTitle = media.title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(32)
                    .ifBlank { "Video" }
                val fileName = "BNJ_Frame_${safeTitle}_${positionMs}ms.png"

                val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/JAAS PLAY TV"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("MediaStore failed to allocate output image URI.")
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    uri.toString()
                } else {
                    val picturesDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "JAAS PLAY TV"
                    ).apply { mkdirs() }
                    val outFile = File(picturesDir, fileName)
                    FileOutputStream(outFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    outFile.absolutePath
                }

                Result.success("Captured frame saved: $fileName ($savedUri)")
            } catch (t: Throwable) {
                Result.failure(t)
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    private suspend fun captureBitmapFromSurface(playerView: PlayerView?): Bitmap? {
        val surface = playerView?.videoSurfaceView ?: return null
        if (surface is TextureView) {
            return surface.bitmap
        }
        if (surface is SurfaceView && surface.width > 0 && surface.height > 0 && surface.holder.surface.isValid) {
            return withTimeoutOrNull(1500L) {
                suspendCancellableCoroutine { cont ->
                    try {
                        val bmp = Bitmap.createBitmap(
                            surface.width.coerceAtMost(3840),
                            surface.height.coerceAtMost(2160),
                            Bitmap.Config.ARGB_8888
                        )
                        PixelCopy.request(
                            surface,
                            bmp,
                            { copyResult ->
                                if (copyResult == PixelCopy.SUCCESS) {
                                    cont.resume(bmp)
                                } else {
                                    bmp.recycle()
                                    cont.resume(null)
                                }
                            },
                            Handler(Looper.getMainLooper())
                        )
                    } catch (_: Throwable) {
                        cont.resume(null)
                    }
                }
            }
        }
        return null
    }

    private fun captureBitmapWithRetriever(context: Context, uriString: String, positionMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val parsed = Uri.parse(uriString)
            if (parsed.scheme == "file" && !parsed.path.isNullOrBlank()) {
                retriever.setDataSource(parsed.path)
            } else {
                retriever.setDataSource(context, parsed)
            }
            retriever.getFrameAtTime(
                (positionMs.coerceAtLeast(0L)) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.frameAtTime
        } catch (_: Throwable) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }
    }
}
