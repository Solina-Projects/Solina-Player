package app.solina.player.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A local video file found through MediaStore.Video, kept completely separate
 * from [Song] (music). Video and music libraries never mix: they are scanned,
 * counted, listed and played through separate paths.
 *
 * dateAdded / dateModified are MediaStore seconds since epoch (not millis).
 */
@Parcelize
data class VideoItem(
    val id: Long,
    val title: String,
    val durationMs: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val mimeType: String?,
    /** Content URI string, e.g. content://media/external/video/media/42 */
    val contentUriString: String,
    val path: String?,
    val width: Int,
    val height: Int,
    val sizeBytes: Long
) : Parcelable {

    /** File name fallback when the MediaStore title is blank. */
    val displayTitle: String
        get() = title.ifBlank {
            path?.substringAfterLast('/')?.ifBlank { contentUriString } ?: contentUriString
        }

    /** True for portrait / vertical video. */
    val isPortrait: Boolean
        get() = height > width && width > 0
}
