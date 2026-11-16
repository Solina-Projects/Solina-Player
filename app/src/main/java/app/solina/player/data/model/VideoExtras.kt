package app.solina.player.data.model

import kotlinx.serialization.Serializable

/**
 * Reference-player redesign (2026-11-16): extras stored for the video
 * player — bookmarks, favourites, a simple video playlist and external
 * subtitle files. All persisted as JSON in the shared settings DataStore
 * (see UserPreferencesRepository); no new database tables.
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

/** Subtitle style (text size / colour / background), persisted per key. */
data class VideoSubtitleStyle(
    val textSizeSp: Float = 20f,
    val textColorArgb: Int = -1, // white
    /** 0 = none, 1 = dark translucent, 2 = solid black. */
    val backgroundMode: Int = 1
)
