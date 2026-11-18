package app.solina.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import app.solina.player.data.model.VideoItem
import app.solina.player.data.observer.MediaStoreObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Video library, separate from the music library ([SongRepository]).
 * Scans MediaStore.Video only — never audio — and sorts newest first.
 */
interface VideoRepository {
    /** Live list of local videos, newest first, re-queried when MediaStore changes. */
    fun observeVideos(): Flow<List<VideoItem>>

    suspend fun getVideosOnce(): List<VideoItem>

    suspend fun getVideoByContentUri(contentUriString: String): VideoItem?

    /** Batch-resolve queue URIs using the warm cache to avoid N+1 ContentResolver queries. */
    suspend fun getVideosByContentUris(contentUriStrings: List<String>): List<VideoItem>

    suspend fun getVideoCount(): Int
}

@Singleton
class MediaStoreVideoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreObserver: MediaStoreObserver
) : VideoRepository {

    private val videoCache = ConcurrentHashMap<String, VideoItem>()

    override fun observeVideos(): Flow<List<VideoItem>> {
        return mediaStoreObserver.mediaStoreChanges
            .onStart { emit(Unit) }
            .map { getVideosOnce() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getVideosOnce(): List<VideoItem> = withContext(Dispatchers.IO) {
        queryVideos()
    }

    override suspend fun getVideoByContentUri(contentUriString: String): VideoItem? =
        withContext(Dispatchers.IO) {
            videoCache[contentUriString]?.let { return@withContext it }
            val uri = runCatching { Uri.parse(contentUriString) }.getOrNull() ?: return@withContext null
            querySingleVideo(uri, contentUriString)?.also {
                videoCache[contentUriString] = it
            }
        }

    override suspend fun getVideosByContentUris(contentUriStrings: List<String>): List<VideoItem> =
        withContext(Dispatchers.IO) {
            if (contentUriStrings.isEmpty()) return@withContext emptyList()
            if (videoCache.isEmpty()) {
                queryVideos()
            }
            contentUriStrings.mapNotNull { uriStr ->
                videoCache[uriStr] ?: run {
                    val uri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return@run null
                    querySingleVideo(uri, uriStr)?.also { videoCache[uriStr] = it }
                }
            }
        }

    override suspend fun getVideoCount(): Int = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Video.Media._ID),
                "${MediaStore.Video.Media.DURATION} > 0",
                null,
                null
            )?.use { it.count } ?: 0
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.tag("MediaStoreVideoRepository").e(e, "Error counting MediaStore.Video")
            0
        }
    }

    private fun videoProjection(): Array<String> = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.MIME_TYPE,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.WIDTH,
        MediaStore.Video.Media.HEIGHT,
        MediaStore.Video.Media.SIZE
    )

    private fun readVideoRows(cursor: Cursor, videos: MutableList<VideoItem>) {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
        val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val mimeTypeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
        val pathCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
        val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
        val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
        val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                ?: cursor.getString(displayNameCol).orEmpty()
            videos.add(
                VideoItem(
                    id = id,
                    title = title,
                    durationMs = cursor.getLong(durationCol),
                    dateAddedSec = cursor.getLong(dateAddedCol),
                    dateModifiedSec = cursor.getLong(dateModifiedCol),
                    mimeType = if (mimeTypeCol != -1) cursor.getString(mimeTypeCol) else null,
                    contentUriString = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    ).toString(),
                    path = if (pathCol != -1) cursor.getString(pathCol) else null,
                    width = if (widthCol != -1) cursor.getInt(widthCol) else 0,
                    height = if (heightCol != -1) cursor.getInt(heightCol) else 0,
                    sizeBytes = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                )
            )
        }
    }

    private fun querySingleVideo(uri: Uri, contentUriString: String): VideoItem? {
        try {
            val singleResult = mutableListOf<VideoItem>()
            context.contentResolver.query(
                uri,
                videoProjection(),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                    val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                    val displayNameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                    val dateModifiedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                    val mimeTypeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                    val pathCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                    val id = if (idCol != -1) cursor.getLong(idCol) else (runCatching { ContentUris.parseId(uri) }.getOrDefault(-1L))
                    val title = (if (titleCol != -1) cursor.getString(titleCol) else null)?.takeIf { it.isNotBlank() }
                        ?: (if (displayNameCol != -1) cursor.getString(displayNameCol) else null).orEmpty()
                    singleResult.add(
                        VideoItem(
                            id = id,
                            title = title,
                            durationMs = if (durationCol != -1) cursor.getLong(durationCol) else 0L,
                            dateAddedSec = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L,
                            dateModifiedSec = if (dateModifiedCol != -1) cursor.getLong(dateModifiedCol) else 0L,
                            mimeType = if (mimeTypeCol != -1) cursor.getString(mimeTypeCol) else null,
                            contentUriString = contentUriString,
                            path = if (pathCol != -1) cursor.getString(pathCol) else null,
                            width = if (widthCol != -1) cursor.getInt(widthCol) else 0,
                            height = if (heightCol != -1) cursor.getInt(heightCol) else 0,
                            sizeBytes = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                        )
                    )
                }
            }
            if (singleResult.isNotEmpty() && singleResult.first().title.isNotBlank()) {
                return singleResult.first()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.tag("MediaStoreVideoRepository").d(e, "Direct single video lookup fell back for %s", contentUriString)
        }
        // Fallback 1: check warm cache / MediaStore list
        queryVideos().firstOrNull { it.contentUriString == contentUriString }?.let { return it }
        // Fallback 2: external content:// or file:// URI opened via ACTION_VIEW / ACTION_SEND
        return resolveExternalVideoMetadata(uri, contentUriString)
    }

    private fun resolveExternalVideoMetadata(uri: Uri, contentUriString: String): VideoItem? {
        var displayName: String? = null
        var sizeBytes = 0L
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) displayName = cursor.getString(nameIdx)
                    if (sizeIdx != -1) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }
        var durationMs = 0L
        var width = 0
        var height = 0
        var mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        val retriever = MediaMetadataRetriever()
        runCatching {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            if (mime.isNullOrBlank()) {
                mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            }
        }
        runCatching { retriever.release() }
        val resolvedTitle = displayName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "Video"
        return VideoItem(
            id = contentUriString.hashCode().toLong(),
            title = resolvedTitle,
            durationMs = durationMs,
            dateAddedSec = System.currentTimeMillis() / 1000L,
            dateModifiedSec = System.currentTimeMillis() / 1000L,
            mimeType = mime,
            contentUriString = contentUriString,
            path = if (uri.scheme == "file") uri.path else null,
            width = width,
            height = height,
            sizeBytes = sizeBytes
        )
    }

    private fun queryVideos(): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection(),
                "${MediaStore.Video.Media.DURATION} > 0",
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                readVideoRows(cursor, videos)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.tag("MediaStoreVideoRepository").e(e, "Error querying MediaStore.Video")
        }
        if (videos.isNotEmpty()) {
            videoCache.clear()
            videos.forEach { videoCache[it.contentUriString] = it }
        }
        return videos
    }
}
