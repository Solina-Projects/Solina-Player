package app.solina.player.data.model

import kotlinx.serialization.Serializable

/**
 * Extras stored for the video player — bookmarks, favourites, video playlist,
 * external subtitle files, per-video watch progress, and subtitle style.
 * Persisted as JSON in the shared settings DataStore.
 */
@Serializable
data class VideoBookmark(
    val uri: String,
    val positionMs: Long,
    val title: String
)

@Serializable
data class VideoPlaylistEntry(
    val uri: String,
    val title: String
)

@Serializable
data class ExternalVideoSubtitle(
    val videoUri: String,
    val subUri: String,
    val label: String,
    val mimeType: String
)

@Serializable
data class VideoProgressEntry(
    val uri: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long = 0L
) {
    val fraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/** Subtitle style (text size / colour / background / edge), persisted per key. */
data class VideoSubtitleStyle(
    val textSizeSp: Float = 20f,
    val textColorArgb: Int = -1, // white
    /** 0 = none, 1 = dark translucent, 2 = solid black. */
    val backgroundMode: Int = 1,
    /** 0 = none, 1 = crisp outline, 2 = drop shadow. */
    val edgeType: Int = 1
)
