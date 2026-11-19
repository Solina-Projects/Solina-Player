package app.solina.player.presentation.utils

import app.solina.player.data.model.Song
import app.solina.player.data.model.VideoItem
import java.util.Locale

/**
 * Pure presentation helpers for sanitising metadata, filtering non-music noise
 * (ringtones / call recordings / short notification clips), and building clean
 * human-readable subtitles on the Home screen.
 */
object HomeMediaFilter {

    private const val MIN_MUSIC_DURATION_MS = 30_000L

    private val NON_MUSIC_PATH_MARKERS = listOf(
        "/ringtones/",
        "/notifications/",
        "/alarms/",
        "/call/",
        "/call recordings/",
        "/callrecordings/",
        "/recordings/",
        "/voice recorder/",
        "/whatsapp voice notes/",
        "/telegram audio/"
    )

    private val NON_MUSIC_TITLE_PREFIXES = listOf(
        "phone_",
        "call_",
        "call ",
        "ringtone",
        "audiocutter_",
        "voice_",
        "recording_",
        "ptt-"
    )

    fun isLikelyNonMusicClip(song: Song): Boolean {
        if (song.duration in 1L until MIN_MUSIC_DURATION_MS) return true
        val lowerPath = song.path.lowercase(Locale.ROOT)
        if (NON_MUSIC_PATH_MARKERS.any { lowerPath.contains(it) }) return true
        val lowerTitle = song.title.trim().lowercase(Locale.ROOT)
        if (NON_MUSIC_TITLE_PREFIXES.any { lowerTitle.startsWith(it) }) return true
        return false
    }

    /**
     * Filters out ringtones, call recordings and very short clips when [enabled] is true.
     * Falls back to [songs] if filtering would leave the list completely empty.
     */
    fun filterMusicForHome(songs: List<Song>, enabled: Boolean): List<Song> {
        if (!enabled || songs.isEmpty()) return songs
        val filtered = songs.filterNot(::isLikelyNonMusicClip)
        return filtered.ifEmpty { songs }
    }

    /**
     * Picks songs that carry actual album artwork first so overlapping artwork strips
     * don't show 4 identical grey music notes when real artwork exists in the mix.
     */
    fun prioritizeArtworkSongs(songs: List<Song>, limit: Int): List<Song> {
        if (songs.size <= limit) return songs
        val withArt = songs.filter { !it.albumArtUriString.isNullOrBlank() }
        if (withArt.size >= limit) return withArt.take(limit)
        val withArtIds = withArt.mapTo(hashSetOf()) { it.id }
        val remainder = songs.filterNot { it.id in withArtIds }
        return (withArt + remainder).take(limit)
    }

    /** Replaces raw MediaStore `<unknown>` / blank artist metadata with [unknownLabel]. */
    fun cleanArtistDisplay(rawArtist: String?, unknownLabel: String): String {
        val trimmed = rawArtist?.trim().orEmpty()
        if (trimmed.isEmpty()) return unknownLabel
        val lower = trimmed.lowercase(Locale.ROOT)
        return if (lower == "<unknown>" || lower == "unknown" || lower == "unknown artist") {
            unknownLabel
        } else {
            trimmed
        }
    }

    /**
     * Builds a clean subtitle for a Home video card (e.g. `Movies · 1080p` or `Camera · 48 MB`)
     * instead of exposing raw MIME strings like `video/mp4`.
     */
    fun cleanVideoSubtitle(video: VideoItem, unknownFolderLabel: String): String {
        val folderName = extractFolderName(video.path) ?: unknownFolderLabel
        val resolution = formatResolutionTag(video.width, video.height)
        val sizeTag = formatCompactFileSize(video.sizeBytes)
        val badge = resolution ?: sizeTag
        return if (badge != null) "$folderName · $badge" else folderName
    }

    fun extractFolderName(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val trimmed = path.trimEnd('/')
        val parent = trimmed.substringBeforeLast('/', "")
        val folder = parent.substringAfterLast('/', "").trim()
        return folder.ifEmpty { null }
    }

    fun formatResolutionTag(width: Int, height: Int): String? {
        val shortEdge = minOf(width, height)
        val longEdge = maxOf(width, height)
        if (shortEdge <= 0 || longEdge <= 0) return null
        return when {
            longEdge >= 3800 || shortEdge >= 2100 -> "4K"
            shortEdge >= 1400 -> "1440p"
            shortEdge >= 1000 -> "1080p"
            shortEdge >= 700 -> "720p"
            shortEdge >= 460 -> "480p"
            else -> "${shortEdge}p"
        }
    }

    fun formatCompactFileSize(sizeBytes: Long): String? {
        if (sizeBytes <= 0L) return null
        val mb = sizeBytes / (1024.0 * 1024.0)
        return when {
            mb >= 1024.0 -> String.format(Locale.US, "%.1f GB", mb / 1024.0)
            mb >= 1.0 -> String.format(Locale.US, "%.0f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", (sizeBytes / 1024.0).coerceAtLeast(1.0))
        }
    }

    /** Compact mix duration like `5h 47m` or `42m` for hero chips. */
    fun formatCompactMixDuration(durationMs: Long): String {
        val totalMinutes = (durationMs / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) {
            if (minutes > 0L) "${hours}h ${minutes}m" else "${hours}h"
        } else {
            "${minutes}m"
        }
    }
}
