package app.solina.player.presentation.viewmodel

package app.solina.player.presentation.viewmodel

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.solina.player.R
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoPlaylistEntry
import app.solina.player.data.model.VideoProgressEntry
import app.solina.player.data.preferences.UserPreferencesRepository
import app.solina.player.data.repository.VideoRepository
import app.solina.player.utils.MediaStorePermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Video library state. Separate from [PlayerViewModel] (music): the video
 * list, count, progress map, favourites, playlist, and recent videos come only from video stores.
 */
@HiltViewModel
class VideoViewModel @Inject constructor(
    videoRepository: VideoRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val videoPlaybackHolder: VideoPlaybackHolder,
    @ApplicationContext private val appContext: Context
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

    // ---- Round 22: long-press multi-selection for video files ----
    // Mirrors the music library's multi-selection pattern (selection mode
    // entered by long-press, tap toggles, actions in a bottom sheet).
    // State lives per screen: each video screen gets its own
    // VideoViewModel instance from hiltViewModel(), so selections never
    // leak from one list into another.

    private val _selectedVideoUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedVideoUris: StateFlow<Set<String>> = _selectedVideoUris.asStateFlow()

    val isVideoSelectionMode: StateFlow<Boolean> = _selectedVideoUris
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleVideoSelection(uri: String) {
        _selectedVideoUris.update { current ->
            if (uri in current) current - uri else current + uri
        }
    }

    fun enterVideoSelection(uri: String) {
        _selectedVideoUris.update { it + uri }
    }

    fun clearVideoSelection() {
        _selectedVideoUris.value = emptySet()
    }

    fun selectAllVideos(uris: List<String>) {
        _selectedVideoUris.value = uris.toSet()
    }

    fun selectedVideos(all: List<VideoItem>): List<VideoItem> {
        val selected = _selectedVideoUris.value
        return all.filter { it.contentUriString in selected }
    }

    // One-shot UI events for the selection screens: system permission
    // dialogs (delete / write) and toast messages.
    private val _videoDeletePermissionRequest =
        MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val videoDeletePermissionRequest: SharedFlow<IntentSender> =
        _videoDeletePermissionRequest.asSharedFlow()

    private val _videoWritePermissionRequest =
        MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val videoWritePermissionRequest: SharedFlow<IntentSender> =
        _videoWritePermissionRequest.asSharedFlow()

    private val _videoToastEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val videoToastEvents: SharedFlow<String> = _videoToastEvents.asSharedFlow()

    // In-app delete confirmation (pre-Android 11, or when no system delete
    // request can be built). Observed by the selection screens.
    private val _pendingVideoDeleteConfirm = MutableStateFlow<List<VideoItem>?>(null)
    val pendingVideoDeleteConfirm: StateFlow<List<VideoItem>?> =
        _pendingVideoDeleteConfirm.asStateFlow()

    // Bottom sheet / single-video dialogs, driven from the selection top
    // bar and the sheet itself so every video screen shares one host.
    private val _showVideoSelectionSheet = MutableStateFlow(false)
    val showVideoSelectionSheet: StateFlow<Boolean> = _showVideoSelectionSheet.asStateFlow()

    fun openVideoSelectionSheet() {
        _showVideoSelectionSheet.value = true
    }

    fun closeVideoSelectionSheet() {
        _showVideoSelectionSheet.value = false
    }

    private val _videoDetailsTarget = MutableStateFlow<VideoItem?>(null)
    val videoDetailsTarget: StateFlow<VideoItem?> = _videoDetailsTarget.asStateFlow()

    fun showVideoDetails(video: VideoItem?) {
        _videoDetailsTarget.value = video
    }

    private val _videoRenameTarget = MutableStateFlow<VideoItem?>(null)
    val videoRenameTarget: StateFlow<VideoItem?> = _videoRenameTarget.asStateFlow()

    fun showVideoRename(video: VideoItem?) {
        _videoRenameTarget.value = video
    }

    private var pendingVideoDelete: List<VideoItem>? = null
    private var pendingVideoDeleteSkippedCount = 0
    private var pendingVideoRename: Pair<VideoItem, String>? = null

    /**
     * Delete the given videos. The video that is currently playing is
     * never deleted. On Android 11+ the system delete dialog is shown
     * (MediaStore.createDeleteRequest) and the actual delete happens in
     * [onVideoDeletePermissionResult]. Otherwise the in-app confirmation
     * dialog is used ([pendingVideoDeleteConfirm] + [confirmVideoDelete]).
     */
    fun requestDeleteVideos(videos: List<VideoItem>) {
        viewModelScope.launch {
            val playingUri = videoPlaybackHolder.currentVideo.value?.contentUriString
            val deletable = videos.filter { it.contentUriString != playingUri }
            val skipped = videos.size - deletable.size
            if (deletable.isEmpty()) {
                if (videos.isNotEmpty()) {
                    _videoToastEvents.emit(
                        appContext.getString(R.string.video_selection_cannot_delete_playing)
                    )
                }
                return@launch
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uris = deletable.map { Uri.parse(it.contentUriString) }
                val request = withContext(Dispatchers.IO) {
                    MediaStorePermissionHelper.createDeleteRequest(appContext, uris)
                }
                if (request != null) {
                    val accepted = request.acceptedUris.map { it.toString() }.toSet()
                    val acceptedVideos = deletable.filter { it.contentUriString in accepted }
                    if (acceptedVideos.isEmpty()) {
                        _videoToastEvents.emit(
                            appContext.getString(R.string.video_selection_delete_failed)
                        )
                        return@launch
                    }
                    pendingVideoDelete = acceptedVideos
                    pendingVideoDeleteSkippedCount =
                        skipped + (deletable.size - acceptedVideos.size)
                    _videoDeletePermissionRequest.emit(request.intentSender)
                    return@launch
                }
            }
            // No system request available: fall back to the in-app confirmation.
            pendingVideoDelete = deletable
            pendingVideoDeleteSkippedCount = skipped
            _pendingVideoDeleteConfirm.value = deletable
        }
    }

    fun dismissVideoDeleteConfirm() {
        _pendingVideoDeleteConfirm.value = null
        pendingVideoDelete = null
        pendingVideoDeleteSkippedCount = 0
    }

    fun confirmVideoDelete() {
        val videos = pendingVideoDelete ?: return
        val skipped = pendingVideoDeleteSkippedCount
        _pendingVideoDeleteConfirm.value = null
        pendingVideoDelete = null
        pendingVideoDeleteSkippedCount = 0
        deleteVideosFromStore(videos, skipped)
    }

    fun onVideoDeletePermissionResult(granted: Boolean) {
        val videos = pendingVideoDelete ?: return
        val skipped = pendingVideoDeleteSkippedCount
        pendingVideoDelete = null
        pendingVideoDeleteSkippedCount = 0
        if (!granted) {
            viewModelScope.launch {
                _videoToastEvents.emit(
                    appContext.getString(R.string.video_selection_delete_cancelled)
                )
            }
            return
        }
        deleteVideosFromStore(videos, skipped)
    }

    private fun deleteVideosFromStore(videos: List<VideoItem>, skippedCount: Int) {
        viewModelScope.launch {
            var deleted = 0
            withContext(Dispatchers.IO) {
                videos.forEach { video ->
                    val rows = runCatching {
                        appContext.contentResolver.delete(
                            Uri.parse(video.contentUriString), null, null
                        )
                    }.getOrDefault(0)
                    if (rows > 0) {
                        deleted++
                        runCatching {
                            userPreferencesRepository.removeFromVideoPlaylist(video.contentUriString)
                        }
                        runCatching {
                            userPreferencesRepository.setVideoFavourite(video.contentUriString, false)
                        }
                    }
                }
            }
            if (deleted > 0) {
                val deletedUris = videos.map { it.contentUriString }.toSet()
                _selectedVideoUris.update { it - deletedUris }
                _videoToastEvents.emit(
                    appContext.getString(R.string.video_selection_deleted_count, deleted)
                )
            } else {
                _videoToastEvents.emit(
                    appContext.getString(R.string.video_selection_delete_failed)
                )
            }
        }
    }

    /**
     * Rename one video (MediaStore DISPLAY_NAME). Renaming a file the app
     * does not own throws [RecoverableSecurityException] on Android 10+ —
     * the system write dialog is shown and the rename is retried in
     * [onVideoWritePermissionResult] once the user grants access.
     */
    fun requestRenameVideo(video: VideoItem, newDisplayName: String) {
        viewModelScope.launch {
            val uri = Uri.parse(video.contentUriString)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
            }
            val rows = try {
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.update(uri, values, null, null)
                }
            } catch (e: RecoverableSecurityException) {
                pendingVideoRename = video to newDisplayName
                val sender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    withContext(Dispatchers.IO) {
                        MediaStorePermissionHelper.createWriteRequestIntentSender(
                            appContext, listOf(uri)
                        )
                    }
                } else {
                    null
                }
                if (sender != null) {
                    _videoWritePermissionRequest.emit(sender)
                } else {
                    pendingVideoRename = null
                    _videoToastEvents.emit(
                        appContext.getString(R.string.video_selection_rename_failed)
                    )
                }
                return@launch
            } catch (_: SecurityException) {
                _videoToastEvents.emit(
                    appContext.getString(R.string.video_selection_rename_failed)
                )
                return@launch
            }
            _videoToastEvents.emit(
                appContext.getString(
                    if (rows > 0) R.string.video_selection_rename_done
                    else R.string.video_selection_rename_failed
                )
            )
        }
    }

    fun onVideoWritePermissionResult(granted: Boolean) {
        val (video, newDisplayName) = pendingVideoRename ?: return
        pendingVideoRename = null
        if (!granted) {
            viewModelScope.launch {
                _videoToastEvents.emit(
                    appContext.getString(R.string.video_selection_rename_failed)
                )
            }
            return
        }
        // Consent granted: retry the rename now.
        viewModelScope.launch {
            val uri = Uri.parse(video.contentUriString)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
            }
            val rows = runCatching {
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.update(uri, values, null, null)
                }
            }.getOrDefault(0)
            _videoToastEvents.emit(
                appContext.getString(
                    if (rows > 0) R.string.video_selection_rename_done
                    else R.string.video_selection_rename_failed
                )
            )
        }
    }

    /** Share the given videos with the system share sheet (ACTION_SEND_MULTIPLE). */
    fun shareVideos(activity: Activity, videos: List<VideoItem>) {
        if (videos.isEmpty()) return
        val uris = videos.map { Uri.parse(it.contentUriString) }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            clipData = ClipData.newUri(
                appContext.contentResolver, videos.first().displayTitle, uris.first()
            ).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { activity.startActivity(Intent.createChooser(intent, null)) }
    }

    /** Add the given videos to the Videos playlist (duplicates are skipped). */
    fun addVideosToPlaylist(videos: List<VideoItem>) {
        viewModelScope.launch {
            var added = 0
            videos.forEach { video ->
                if (userPreferencesRepository.addToVideoPlaylist(
                        VideoPlaylistEntry(
                            uri = video.contentUriString,
                            title = video.displayTitle
                        )
                    )
                ) {
                    added++
                }
            }
            _videoToastEvents.emit(
                appContext.getString(
                    if (added > 0) R.string.video_added_to_playlist
                    else R.string.video_already_in_playlist
                )
            )
        }
    }
}
