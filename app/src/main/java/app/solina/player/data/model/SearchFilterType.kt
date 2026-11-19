package app.solina.player.data.model

import androidx.compose.runtime.Immutable

@Immutable
enum class SearchFilterType {
    ALL,
    SONGS,
    VIDEOS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS
}
