package app.solina.player.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.solina.player.data.model.VideoBookmark
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoPlaylistEntry
import app.solina.player.data.model.VideoSubtitleStyle
import app.solina.player.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Navigation-scoped ViewModel driving the UI of [VideoPlayerScreen] while
 * delegating ExoPlayer ownership, queue, sleep timer, A-B repeat, and error
 * recovery to app-scoped [VideoPlaybackHolder].
 */
@UnstableApi
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val videoPlaybackHolder: VideoPlaybackHolder
) : ViewModel() {

    private val exoPlayer: ExoPlayer?
        get() = videoPlaybackHolder.playerOrNull

    val player: StateFlow<ExoPlayer?> = videoPlaybackHolder.player
    val currentVideo: StateFlow<VideoItem?> = videoPlaybackHolder.currentVideo
    val playbackSpeed: StateFlow<Float> = videoPlaybackHolder.playbackSpeed
    val isPlaying: StateFlow<Boolean> = videoPlaybackHolder.isPlaying
    val isLoading: StateFlow<Boolean> = videoPlaybackHolder.isLoading
    val positionMs: StateFlow<Long> = videoPlaybackHolder.positionMs
    val holderDurationMs: StateFlow<Long> = videoPlaybackHolder.durationMs
    val playbackError: StateFlow<VideoPlaybackError?> = videoPlaybackHolder.playbackError
    val pendingResumePromptMs: StateFlow<Long?> = videoPlaybackHolder.pendingResumePromptMs

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    val availableSpeeds: List<Float> = listOf(0.1f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 4f)

    val aspectMode: StateFlow<String> =
        userPreferencesRepository.videoAspectModeFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "FIT")

    fun cycleAspectMode() {
        val next = nextAspectMode()
        viewModelScope.launch { userPreferencesRepository.setVideoAspectMode(next) }
    }

    fun nextAspectMode(): String = when (aspectMode.value) {
        "FIT" -> "FILL"
        "FILL" -> "ZOOM"
        "ZOOM" -> "STRETCH"
        else -> "FIT"
    }

    val decoderBadge: StateFlow<String> = videoPlaybackHolder.decoderBadge

    val backgroundPlayback: StateFlow<Boolean> =
        userPreferencesRepository.videoBackgroundPlaybackFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val swipeGesturesEnabled: StateFlow<Boolean> =
        userPreferencesRepository.videoSwipeGesturesEnabledFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoBackgroundPlayback(enabled)
        }
    }

    val nightLight: StateFlow<Boolean> =
        userPreferencesRepository.videoNightLightFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setNightLight(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setVideoNightLight(enabled) }
    }

    val brightness: StateFlow<Int> =
        userPreferencesRepository.videoBrightnessFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 40)

    fun setBrightness(percent: Int) {
        viewModelScope.launch { userPreferencesRepository.setVideoBrightness(percent) }
    }

    val volume: StateFlow<Float> = videoPlaybackHolder.volume
    fun setVolume(volume: Float) = videoPlaybackHolder.setVolume(volume)
    fun setVolumeLive(volume: Float) = videoPlaybackHolder.setVolumeLive(volume)

    val visibleControlButtons: StateFlow<Set<String>> =
        userPreferencesRepository.videoPlayerVisibleButtonsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setVisibleControlButtons(buttonIds: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoPlayerVisibleButtons(buttonIds)
        }
    }

    // ---- Sleep timer (holder-owned so background play keeps counting down) ----
    val sleepTimerMinutes: StateFlow<Int> = videoPlaybackHolder.sleepTimerMinutes
    val sleepTimerRemainingSeconds: StateFlow<Int> = videoPlaybackHolder.sleepTimerRemainingSeconds
    fun setSleepTimer(minutes: Int) = videoPlaybackHolder.setSleepTimer(minutes)

    // ---- Audio tracks ----
    private val _audioTrackLabels = MutableStateFlow<List<String>>(emptyList())
    val audioTrackLabels: StateFlow<List<String>> = _audioTrackLabels.asStateFlow()

    private val _selectedAudioTrack = MutableStateFlow(0)
    val selectedAudioTrack: StateFlow<Int> = _selectedAudioTrack.asStateFlow()

    private fun refreshAudioTracks() {
        val player = exoPlayer ?: return
        val audioGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
        _audioTrackLabels.value = audioGroups.mapIndexed { index, group ->
            val format = group.mediaTrackGroup.getFormat(0)
            format.label?.takeIf { it.isNotBlank() }
                ?: format.language?.takeIf { it.isNotBlank() }
                ?: "Track ${index + 1}"
        }
        _selectedAudioTrack.value = audioGroups.indexOfFirst { group ->
            group.isSelected
        }.coerceAtLeast(0)
    }

    fun selectAudioTrack(index: Int) {
        val player = exoPlayer ?: return
        val audioGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO }
        val group = audioGroups.getOrNull(index) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, 0)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
        _selectedAudioTrack.value = index
    }

    // ---- Subtitle tracks ----
    private val _subtitleTrackLabels = MutableStateFlow<List<String>>(emptyList())
    val subtitleTrackLabels: StateFlow<List<String>> = _subtitleTrackLabels.asStateFlow()

    private val _selectedSubtitleTrack = MutableStateFlow(-1)
    val selectedSubtitleTrack: StateFlow<Int> = _selectedSubtitleTrack.asStateFlow()

    private var pendingExternalSubtitleLabel: String? = null

    private fun refreshSubtitleTracks() {
        val player = exoPlayer ?: return
        val textGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
        _subtitleTrackLabels.value = textGroups.mapIndexed { index, group ->
            val format = group.mediaTrackGroup.getFormat(0)
            format.label?.takeIf { it.isNotBlank() }
                ?: format.language?.takeIf { it.isNotBlank() }
                ?: "Subtitle ${index + 1}"
        }
        _selectedSubtitleTrack.value = textGroups.indexOfFirst { group ->
            group.isSelected
        }.let { if (it < 0) -1 else it }
        val pending = pendingExternalSubtitleLabel
        if (pending != null) {
            val index = _subtitleTrackLabels.value.indexOfFirst { it == pending }
            if (index >= 0) {
                pendingExternalSubtitleLabel = null
                selectSubtitleTrack(index)
            }
        }
    }

    fun selectSubtitleTrack(index: Int) {
        val player = exoPlayer ?: return
        if (index < 0) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _selectedSubtitleTrack.value = -1
            return
        }
        val textGroups = player.currentTracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT }
        val group = textGroups.getOrNull(index) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, 0)
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(override)
            .build()
        _selectedSubtitleTrack.value = index
    }

    // ---- Action messages, bookmarks, favourites, playlist, queue, subtitle style ----
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun postActionMessage(message: String) {
        _actionMessage.value = message
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    val bookmarksForCurrentVideo: StateFlow<List<VideoBookmark>> =
        combine(
            userPreferencesRepository.videoBookmarksFlow,
            currentVideo
        ) { bookmarks, video ->
            if (video == null) emptyList()
            else bookmarks.filter { it.uri == video.contentUriString }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addBookmark() {
        val video = currentVideo.value ?: return
        val position = currentPositionMs()
        viewModelScope.launch {
            userPreferencesRepository.addVideoBookmark(
                VideoBookmark(
                    uri = video.contentUriString,
                    positionMs = position,
                    title = video.displayTitle
                )
            )
            postActionMessage("bookmark_saved")
        }
    }

    fun deleteBookmark(positionMs: Long) {
        val video = currentVideo.value ?: return
        viewModelScope.launch {
            userPreferencesRepository.deleteVideoBookmark(
                uri = video.contentUriString,
                positionMs = positionMs
            )
            postActionMessage("bookmark_deleted")
        }
    }

    val isFavourite: StateFlow<Boolean> =
        combine(
            userPreferencesRepository.videoFavouriteUrisFlow,
            currentVideo
        ) { favourites, video ->
            video != null && favourites.contains(video.contentUriString)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleFavourite() {
        val video = currentVideo.value ?: return
        val favourite = !isFavourite.value
        viewModelScope.launch {
            userPreferencesRepository.setVideoFavourite(
                uri = video.contentUriString,
                favourite = favourite
            )
            postActionMessage(if (favourite) "favourite_added" else "favourite_removed")
        }
    }

    fun addCurrentVideoToPlaylist() {
        val video = currentVideo.value ?: return
        viewModelScope.launch {
            val added = userPreferencesRepository.addToVideoPlaylist(
                VideoPlaylistEntry(
                    uri = video.contentUriString,
                    title = video.displayTitle
                )
            )
            postActionMessage(
                if (added) "added_to_playlist" else "already_in_playlist"
            )
        }
    }

    /** Batch-resolved playing queue using VideoRepository's warm cache (no N+1 queries). */
    val queueItems: StateFlow<List<VideoItem>> =
        videoPlaybackHolder.queueUris.map { uris ->
            runCatching { videoPlaybackHolder.getVideosByUris(uris) }.getOrElse { emptyList() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playQueueIndex(index: Int) = videoPlaybackHolder.playQueueIndex(index)
    fun removeQueueItem(index: Int) = videoPlaybackHolder.removeQueueItem(index)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) = videoPlaybackHolder.moveQueueItem(fromIndex, toIndex)

    val subtitleStyle: StateFlow<VideoSubtitleStyle> =
        combine(
            userPreferencesRepository.videoSubtitleTextSizeFlow,
            userPreferencesRepository.videoSubtitleTextColorFlow,
            userPreferencesRepository.videoSubtitleBackgroundFlow,
            userPreferencesRepository.videoSubtitleEdgeTypeFlow
        ) { sizeSp, colorArgb, backgroundMode, edgeType ->
            VideoSubtitleStyle(
                textSizeSp = sizeSp,
                textColorArgb = colorArgb,
                backgroundMode = backgroundMode,
                edgeType = edgeType
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            VideoSubtitleStyle()
        )

    fun setSubtitleTextSize(sizeSp: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSubtitleTextSize(sizeSp)
        }
    }

    fun setSubtitleTextColor(argb: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSubtitleTextColor(argb)
        }
    }

    fun setSubtitleBackground(mode: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSubtitleBackground(mode)
        }
    }

    fun setSubtitleEdgeType(edgeType: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setVideoSubtitleEdgeType(edgeType)
        }
    }

    fun addExternalSubtitle(subUri: Uri, label: String, mimeType: String) {
        val selectedLabel = videoPlaybackHolder.attachExternalSubtitle(
            subUri = subUri,
            label = label,
            mimeType = mimeType
        )
        pendingExternalSubtitleLabel = selectedLabel
        postActionMessage("subtitle_loaded")
    }

    fun subtitleMimeTypeFor(fileName: String): String? = when (
        fileName.substringAfterLast('.', "").lowercase()
    ) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "vtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> null
    }

    // ---- Holder-backed modes ----
    val shuffleEnabled: StateFlow<Boolean> = videoPlaybackHolder.shuffleEnabled
    fun toggleShuffle() = videoPlaybackHolder.toggleShuffle()

    val repeatMode: StateFlow<Int> = videoPlaybackHolder.repeatMode
    fun cycleRepeatMode() = videoPlaybackHolder.cycleRepeatMode()

    val isMuted: StateFlow<Boolean> = videoPlaybackHolder.isMuted
    fun toggleMute() = videoPlaybackHolder.toggleMute()

    val aPointMs: StateFlow<Long> = videoPlaybackHolder.aPointMs
    val bPointMs: StateFlow<Long> = videoPlaybackHolder.bPointMs
    fun tapABRepeat() = videoPlaybackHolder.tapABRepeat()
    fun clearABRepeat() = videoPlaybackHolder.clearABRepeat()

    init {
        viewModelScope.launch {
            videoPlaybackHolder.tracks.collect {
                refreshAudioTracks()
                refreshSubtitleTracks()
            }
        }
    }

    fun play(contentUriString: String, startPositionMs: Long = 0L) =
        videoPlaybackHolder.play(contentUriString, startPositionMs)

    suspend fun loadVideo(contentUriString: String) {
        videoPlaybackHolder.loadVideo(contentUriString)
    }

    fun playQueue(
        contentUriStrings: List<String>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L
    ) = videoPlaybackHolder.playQueue(contentUriStrings, startIndex, startPositionMs)

    fun currentPositionMs(): Long = videoPlaybackHolder.currentPositionMs()
    fun saveResumePosition() = videoPlaybackHolder.saveResumePosition()
    fun clearResumePosition() = videoPlaybackHolder.clearResumePosition()
    fun acceptResumePrompt() = videoPlaybackHolder.acceptResumePrompt()
    fun dismissResumePrompt() = videoPlaybackHolder.dismissResumePrompt()
    fun restartFromBeginning() = videoPlaybackHolder.restartFromBeginning()
    fun clearError() = videoPlaybackHolder.clearError()
    fun retryCurrent() = videoPlaybackHolder.retryCurrent()
    fun retryWithSoftwareDecoder() = videoPlaybackHolder.retryWithSoftwareDecoder()

    fun setSpeed(speed: Float) = videoPlaybackHolder.setSpeed(speed)
    fun setSpeedLive(speed: Float) = videoPlaybackHolder.setSpeedLive(speed)

    fun onLeavePlayer() {
        saveResumePosition()
        if (!backgroundPlayback.value) {
            setSleepTimer(0)
            videoPlaybackHolder.pause()
        }
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun seekTo(positionMs: Long) = videoPlaybackHolder.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = videoPlaybackHolder.seekBy(deltaMs)
    fun togglePlayPause() = videoPlaybackHolder.togglePlayPause()
    fun seekToNext() = videoPlaybackHolder.seekToNext()
    fun seekToPrevious() = videoPlaybackHolder.seekToPrevious()
    fun pause() = videoPlaybackHolder.pause()
    fun resume() = videoPlaybackHolder.resume()
}
