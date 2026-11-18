package app.solina.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoPlaylistEntry
import app.solina.player.data.model.VideoProgressEntry
import app.solina.player.data.preferences.UserPreferencesRepository
import app.solina.player.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Video library state. Separate from [PlayerViewModel] (music): the video
 * list, count, progress map, favourites, playlist, and recent videos come only from video stores.
 */
@HiltViewModel
class VideoViewModel @Inject constructor(
    videoRepository: VideoRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val videoPlaybackHolder: VideoPlaybackHolder
) : ViewModel() {

    val videos: StateFlow<List<VideoItem>> = videoRepository.observeVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val videoCount: StateFlow<Int> = videos
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val recentVideos: StateFlow<List<VideoItem>> = videos
        .map { it.take(RECENT_VIDEOS_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val videoProgressMap: StateFlow<Map<String, VideoProgressEntry>> =
        userPreferencesRepository.videoProgressMapFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val favouriteVideoUris: StateFlow<Set<String>> =
        userPreferencesRepository.videoFavouriteUrisFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val videoPlaylistEntries: StateFlow<List<VideoPlaylistEntry>> =
        userPreferencesRepository.videoPlaylistFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeFromVideoPlaylist(uri: String) {
        viewModelScope.launch { userPreferencesRepository.removeFromVideoPlaylist(uri) }
    }

    val miniPlayerSwipeToSkip: StateFlow<Boolean> =
        userPreferencesRepository.miniPlayerSwipeToSkipFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isVideoSearchGridView: StateFlow<Boolean> =
        userPreferencesRepository.isVideoSearchGridViewFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun toggleVideoSearchViewMode() {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSearchGridView(!isVideoSearchGridView.value)
        }
    }

    val isVideoFoldersGridView: StateFlow<Boolean> =
        userPreferencesRepository.isVideoFoldersGridViewFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleVideoFoldersViewMode() {
        viewModelScope.launch {
            userPreferencesRepository.setVideoFoldersGridView(!isVideoFoldersGridView.value)
        }
    }

    val videoFoldersViewMode: StateFlow<String> =
        userPreferencesRepository.videoFoldersViewModeFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "FOLDERS")

    fun setVideoFoldersViewMode(mode: String) {
        viewModelScope.launch { userPreferencesRepository.setVideoFoldersViewMode(mode) }
    }

    val videoFoldersSort: StateFlow<String> =
        userPreferencesRepository.videoFoldersSortFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "TITLE")

    fun setVideoFoldersSort(sort: String) {
        viewModelScope.launch { userPreferencesRepository.setVideoFoldersSort(sort) }
    }

    val videoFoldersSortAscending: StateFlow<Boolean> =
        userPreferencesRepository.videoFoldersSortAscendingFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setVideoFoldersSortAscending(ascending: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoFoldersSortAscending(ascending)
        }
    }

    val videoResumeUri: StateFlow<String?> =
        userPreferencesRepository.videoResumeUriFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val videoResumePositionMs: StateFlow<Long> =
        userPreferencesRepository.videoResumePositionMsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val videoResumeTitle: StateFlow<String?> =
        userPreferencesRepository.videoResumeTitleFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearVideoResume() {
        viewModelScope.launch { userPreferencesRepository.clearVideoResume() }
    }

    val showVideoMiniBar: StateFlow<Boolean> = combine(
        videoPlaybackHolder.currentVideo,
        videoResumeUri
    ) { video, resumeUri -> video != null || resumeUri != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    companion object {
        const val RECENT_VIDEOS_LIMIT = 12
    }
}
