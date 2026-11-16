package app.solina.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.solina.player.data.model.VideoItem
import app.solina.player.data.preferences.UserPreferencesRepository
import app.solina.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Video library state. Separate from [PlayerViewModel] (music): the video
 * list, count and recent videos come only from [VideoRepository].
 */
@HiltViewModel
class VideoViewModel @Inject constructor(
    videoRepository: VideoRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val videos: StateFlow<List<VideoItem>> = videoRepository.observeVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val videoCount: StateFlow<Int> = videos
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val recentVideos: StateFlow<List<VideoItem>> = videos
        .map { it.take(RECENT_VIDEOS_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Search feedback (2026-11-16): the Video Search browse grid has its own
     * persisted grid/list mode, mirroring the music search genre grid
     * ([PlayerViewModel.isGenreGridView]).
     */
    val isVideoSearchGridView: StateFlow<Boolean> =
        userPreferencesRepository.isVideoSearchGridViewFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun toggleVideoSearchViewMode() {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSearchGridView(!isVideoSearchGridView.value)
        }
    }

    companion object {
        const val RECENT_VIDEOS_LIMIT = 12
    }
}
