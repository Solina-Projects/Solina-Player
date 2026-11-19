package app.solina.player.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Immutable


@Immutable
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Library : Screen("library")
    object Settings : Screen("settings")
    object Accounts : Screen("settings_accounts?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "settings_accounts?highlightKey=$highlightKey" else "settings_accounts"
    }
    object SettingsCategory : Screen("settings_category/{categoryId}?highlightKey={highlightKey}") {
        fun createRoute(categoryId: String, highlightKey: String? = null) =
            if (highlightKey != null) "settings_category/$categoryId?highlightKey=$highlightKey" else "settings_category/$categoryId"
    }
    object PaletteStyle : Screen("palette_style_settings?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "palette_style_settings?highlightKey=$highlightKey" else "palette_style_settings"
    }
    object Experimental : Screen("experimental_settings?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "experimental_settings?highlightKey=$highlightKey" else "experimental_settings"
    }
    object LyricsSettings : Screen("lyrics_settings?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "lyrics_settings?highlightKey=$highlightKey" else "lyrics_settings"
    }
    object NavBarCrRad : Screen("nav_bar_corner_radius?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "nav_bar_corner_radius?highlightKey=$highlightKey" else "nav_bar_corner_radius"
    }
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }

    object  DailyMixScreen : Screen("daily_mix")
    object RecentlyPlayed : Screen("recently_played")
    object Stats : Screen("stats")
    object Recap : Screen("recap/{year}") {
        fun createRoute(year: Int) = "recap/$year"
    }
    object Duplicates : Screen("duplicates?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "duplicates?highlightKey=$highlightKey" else "duplicates"
    }
    object GenreDetail : Screen("genre_detail/{genreId}") {
        fun createRoute(genreId: String) = "genre_detail/$genreId"
    }
    object DJSpace : Screen("dj_space")
    object AlbumDetail : Screen("album_detail/{albumId}") {
        fun createRoute(albumId: Long) = "album_detail/$albumId"
    }

    object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: Long) = "artist_detail/$artistId"
    }

    object EditTransition : Screen("edit_transition?playlistId={playlistId}") {
        fun createRoute(playlistId: String?) =
            if (playlistId != null) "edit_transition?playlistId=$playlistId" else "edit_transition"
    }

    object About : Screen("about")
    object Licenses : Screen("licenses")
    object EasterEgg : Screen("easter_egg")

    object ArtistSettings : Screen("artist_settings?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "artist_settings?highlightKey=$highlightKey" else "artist_settings"
    }
    object DelimiterConfig : Screen("delimiter_config")
    object WordDelimiterConfig : Screen("word_delimiter_config")
    object ExcludedDirectories : Screen("excluded_directories")
    object Equalizer : Screen("equalizer?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "equalizer?highlightKey=$highlightKey" else "equalizer"
    }
    object DeviceCapabilities : Screen("device_capabilities?highlightKey={highlightKey}") {
        fun createRoute(highlightKey: String? = null) =
            if (highlightKey != null) "device_capabilities?highlightKey=$highlightKey" else "device_capabilities"
    }
    object NavidromeDashboard : Screen("navidrome_dashboard")
    object JellyfinDashboard : Screen("jellyfin_dashboard")
    object CloudDownloads : Screen("cloud_downloads")
    object Updates : Screen("updates")

    object AudioBookmarks : Screen("audio_bookmarks")
    object AudioBookmarkFolder : Screen("audio_bookmarks/{songId}") {
        fun createRoute(songId: String) = "audio_bookmarks/${Uri.encode(songId)}"
    }

    // Music + Video: video library, video player and Home customisation.
    // Video stays separate from music — its own library, player and routes.
    object VideoLibrary : Screen("video_library")
    object VideoPlayer : Screen("video_player/{videoUri}") {
        fun createRoute(videoUri: String) =
            "video_player/${Uri.encode(videoUri)}"

        /** Play a queue of videos: the player starts at [startUri] and the
         *  PlayerView next/previous controls step through [queueUris]. The
         *  queue is passed as one encoded route argument (URIs joined by
         *  newline); empty queue = single-video playback as before. */
        fun createRoute(startUri: String, queueUris: List<String>) =
            createRoute(startUri, queueUris, startPositionMs = 0L)

        /** Same as [createRoute], but the first video starts at
         *  [startPositionMs] — used by the Folders screen's Resume button. */
        fun createRoute(
            startUri: String,
            queueUris: List<String>,
            startPositionMs: Long
        ): String {
            val queueKey = VideoQueueStore.stage(queueUris)
            val base = if (queueKey == null) {
                createRoute(startUri)
            } else {
                "video_player/${Uri.encode(startUri)}?queueKey=${Uri.encode(queueKey)}"
            }
            return if (startPositionMs > 0) {
                "$base${if (base.contains("?")) "&" else "?"}startMs=$startPositionMs"
            } else {
                base
            }
        }
    }
    object HomeCustomization : Screen("home_customization")
    object MusicCustomization : Screen("music_customization")
    object VideoCustomization : Screen("video_customization")

    // Appearance customisation (bug-fix round 2026-11-17, item 6):
    // per-section look of bars, cards, colours and fonts.
    object AppearanceCustomization : Screen("appearance_customization")

    // Bottom-bar feedback (2026-11-16): the video section gets its own
    // bottom bar — Home, video Search, Folders — instead of the music
    // Home / Search / Library bar.
    object VideoSearch : Screen("video_search")
    object VideoFolders : Screen("video_folders")
    object VideoFolderDetail : Screen("video_folder/{folderPath}") {
        fun createRoute(folderPath: String) =
            "video_folder/${Uri.encode(folderPath)}"
    }

}
