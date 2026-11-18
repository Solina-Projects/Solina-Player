package app.solina.player.presentation.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import android.util.LruCache
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import app.solina.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val screenshotScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private val mainHandler = Handler(Looper.getMainLooper())

/**
 * Captures the active video frame off the main thread, writes the JPEG into
 * `Pictures/Solina Player`, recycles temporary bitmaps cleanly, and invokes [onDone] on Main.
 */
@OptIn(UnstableApi::class)
fun takeVideoScreenshotAsync(
    context: Context,
    playerView: PlayerView?,
    videoUri: String,
    positionMs: Long,
    onDone: (Boolean) -> Unit
) {
    val appContext = context.applicationContext
    if (playerView == null) {
        showScreenshotToast(appContext, false)
        onDone(false)
        return
    }
    when (val surfaceView = playerView.videoSurfaceView) {
        is SurfaceView -> {
            val width = surfaceView.width
            val height = surfaceView.height
            if (width <= 0 || height <= 0 || !surfaceView.holder.surface.isValid) {
                captureFrameWithRetrieverAsync(appContext, videoUri, positionMs, onDone)
                return
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val thread = HandlerThread("SolinaPixelCopy").apply { start() }
            val threadHandler = Handler(thread.looper)
            try {
                PixelCopy.request(
                    surfaceView,
                    bitmap,
                    { result ->
                        thread.quitSafely()
                        if (result == PixelCopy.SUCCESS) {
                            screenshotScope.launch {
                                val saved = try {
                                    saveScreenshotBitmap(appContext, bitmap)
                                } finally {
                                    if (!bitmap.isRecycled) bitmap.recycle()
                                }
                                withContext(Dispatchers.Main) {
                                    showScreenshotToast(appContext, saved)
                                    onDone(saved)
                                }
                            }
                        } else {
                            if (!bitmap.isRecycled) bitmap.recycle()
                            captureFrameWithRetrieverAsync(appContext, videoUri, positionMs, onDone)
                        }
                    },
                    threadHandler
                )
            } catch (_: Throwable) {
                thread.quitSafely()
                if (!bitmap.isRecycled) bitmap.recycle()
                captureFrameWithRetrieverAsync(appContext, videoUri, positionMs, onDone)
            }
        }
        is TextureView -> {
            val bitmap = surfaceView.bitmap
            if (bitmap != null) {
                screenshotScope.launch {
                    val saved = try {
                        saveScreenshotBitmap(appContext, bitmap)
                    } finally {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                    withContext(Dispatchers.Main) {
                        showScreenshotToast(appContext, saved)
                        onDone(saved)
                    }
                }
            } else {
                captureFrameWithRetrieverAsync(appContext, videoUri, positionMs, onDone)
            }
        }
        else -> captureFrameWithRetrieverAsync(appContext, videoUri, positionMs, onDone)
    }
}

private fun captureFrameWithRetrieverAsync(
    appContext: Context,
    videoUri: String,
    positionMs: Long,
    onDone: (Boolean) -> Unit
) {
    screenshotScope.launch {
        val retriever = MediaMetadataRetriever()
        val bitmap = try {
            retriever.setDataSource(appContext, Uri.parse(videoUri))
            retriever.getFrameAtTime(
                positionMs.coerceAtLeast(0L) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
        val saved = if (bitmap != null) {
            try {
                saveScreenshotBitmap(appContext, bitmap)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        } else {
            false
        }
        withContext(Dispatchers.Main) {
            showScreenshotToast(appContext, saved)
            onDone(saved)
        }
    }
}

private fun saveScreenshotBitmap(context: Context, bitmap: Bitmap): Boolean {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "SolinaPlayer_$timestamp.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/Solina Player"
        )
    }
    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    ) ?: return false
    var saved = false
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }
    }
    if (!saved) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }
    return saved
}

private fun showScreenshotToast(appContext: Context, saved: Boolean) {
    mainHandler.post {
        Toast.makeText(
            appContext,
            appContext.getString(
                if (saved) R.string.video_screenshot_saved else R.string.video_screenshot_failed
            ),
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Lightweight 4 MB quantized frame cache for live seek-bar scrubbing previews.
 */
object VideoSeekPreviewCache {
    private val cache = object : LruCache<String, Bitmap>(4 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    suspend fun loadFrame(context: Context, videoUri: String, positionMs: Long): Bitmap? {
        if (videoUri.isBlank() || positionMs < 0L) return null
        val bucketMs = (positionMs / 2_000L) * 2_000L
        val key = "$videoUri@$bucketMs"
        cache.get(key)?.let { if (!it.isRecycled) return it }
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            val frame = try {
                retriever.setDataSource(context, Uri.parse(videoUri))
                retriever.getScaledFrameAtTime(
                    bucketMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    192,
                    108
                )
            } catch (_: Throwable) {
                null
            } finally {
                runCatching { retriever.release() }
            }
            if (frame != null) {
                cache.put(key, frame)
            }
            frame
        }
    }
}
