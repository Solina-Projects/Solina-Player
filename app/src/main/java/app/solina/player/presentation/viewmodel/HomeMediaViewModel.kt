package app.solina.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.solina.player.data.model.Song
import app.solina.player.data.model.VideoItem
import app.solina.player.data.repository.MusicRepository
import app.solina.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Home screen media data: music and video kept strictly separate.
 * Recent music = newest audio files by MediaStore date added; recent videos
 * = newest video files. Counts are the two libraries' sizes.
 */
@HiltViewModel
class HomeMediaViewModel @Inject constructor(
    musicRepository: MusicRepository,
    videoRepository: VideoRepository
) : ViewModel() {

    val recentMusic: StateFlow<List<Song>> = musicRepository.getAudioFiles()
        .map { songs -> songs.sortedByDescending { it.dateAdded }.take(RECENT_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val audioCount: StateFlow<Int> = musicRepository.getAudioFiles()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val recentVideos: StateFlow<List<VideoItem>> = videoRepository.observeVideos()
        .map { it.take(RECENT_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val videoCount: StateFlow<Int> = videoRepository.observeVideos()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        const val RECENT_LIMIT = 12
    }
}
