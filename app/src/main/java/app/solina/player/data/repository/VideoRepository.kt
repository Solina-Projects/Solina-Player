package app.solina.player.data.repository

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.content.Context
import android.provider.MediaStore
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Video library, separate from the music library ([SongRepository]).
 * Scans MediaStore.Video only — never audio — and sorts newest first so the
 * Home "recently added videos" row is just the head of this list.
 *
 * Formats: what ExoPlayer can play. On device that is the platform decoders
 * plus the FFmpeg extension bundled in the app (common containers such as
 * MP4, MKV, WebM, MOV, AVI, 3GP and FLV, and their common codecs). It is
 * not literally every proprietary codec in existence.
 */
interface VideoRepository {
    /** Live list of local videos, newest first, re-queried when MediaStore changes. */
    fun observeVideos(): Flow<List<VideoItem>>

    suspend fun getVideosOnce(): List<VideoItem>

    suspend fun getVideoByContentUri(contentUriString: String): VideoItem?

    suspend fun getVideoCount(): Int
}

@Singleton
class MediaStoreVideoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreObserver: MediaStoreObserver
) : VideoRepository {

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
            val uri = runCatching { Uri.parse(contentUriString) }.getOrNull() ?: return@withContext null
            querySingleVideo(uri, contentUriString)
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
            if (singleResult.isNotEmpty()) return singleResult.first()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.tag("MediaStoreVideoRepository").d(e, "Direct single video lookup fell back for %s", contentUriString)
        }
        return queryVideos().firstOrNull { it.contentUriString == contentUriString }
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
        return videos
    }
}
