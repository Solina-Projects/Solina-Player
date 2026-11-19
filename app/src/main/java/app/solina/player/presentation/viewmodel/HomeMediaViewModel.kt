package app.solina.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.solina.player.data.model.Song
import app.solina.player.data.model.VideoItem
import app.solina.player.data.repository.MusicRepository
import app.solina.player.data.repository.VideoRepository
import app.solina.player.presentation.utils.HomeMediaFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Home screen media data: music and video kept strictly separate.
 * Exposes enough recent items (up to 40 music / 20 video) so Home layout
 * preferences (smart filter + customizable item count) apply instantly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeMediaViewModel @Inject constructor(
    musicRepository: MusicRepository,
    videoRepository: VideoRepository
) : ViewModel() {

    private val videoRefreshTick = MutableStateFlow(0)

    private val allSongsFlow = musicRepository.getAudioFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allVideosFlow = videoRefreshTick
        .flatMapLatest { videoRepository.observeVideos() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentMusic: StateFlow<List<Song>> = allSongsFlow
        .map { songs -> songs.sortedByDescending { it.dateAdded }.take(RECENT_MUSIC_POOL) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val audioCount: StateFlow<Int> = allSongsFlow
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val albumCount: StateFlow<Int> = allSongsFlow
        .map { songs -> songs.mapTo(hashSetOf()) { it.albumId }.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val recentVideos: StateFlow<List<VideoItem>> = allVideosFlow
        .map { it.take(RECENT_VIDEO_POOL) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val videoCount: StateFlow<Int> = allVideosFlow
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val videoFolderCount: StateFlow<Int> = allVideosFlow
        .map { videos ->
            videos.mapNotNull { HomeMediaFilter.extractFolderName(it.path) }.toSet().size
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun refreshVideos() {
        videoRefreshTick.value += 1
    }

    companion object {
        const val RECENT_LIMIT = 12
        const val RECENT_MUSIC_POOL = 40
        const val RECENT_VIDEO_POOL = 20
    }
}
