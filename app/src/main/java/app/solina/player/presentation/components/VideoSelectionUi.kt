package app.solina.player.presentation.components

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solina.player.R
import app.solina.player.data.model.VideoItem
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.utils.ZipShareHelper
import app.solina.player.utils.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Round 22: shared long-press multi-selection UI for video files.
 * Mirrors the music library's selection pattern (selection mode entered
 * by long-press, tap toggles, count in the top bar, actions in a bottom
 * sheet) but works on [VideoItem]s through [VideoViewModel].
 */

/** Top bar shown while video selection mode is active. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSelectionTopBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onOpenActions: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = stringResource(R.string.video_selected_count, selectedCount))
        },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.video_selection_clear)
                )
            }
        },
        actions = {
            TextButton(onClick = onSelectAll) {
                Text(text = stringResource(R.string.video_selection_select_all))
            }
            IconButton(onClick = onOpenActions) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.video_selection_actions)
                )
            }
        }
    )
}

/** Bottom sheet with the batch actions for the selected videos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSelectionBottomSheet(
    selectedVideos: List<VideoItem>,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val single = selectedVideos.size == 1
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.video_selected_count, selectedVideos.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            VideoSelectionActionRow(
                icon = Icons.Rounded.PlayArrow,
                label = stringResource(R.string.video_selection_play),
                onClick = onPlay
            )
            VideoSelectionActionRow(
                icon = Icons.Rounded.PlaylistAdd,
                label = stringResource(R.string.video_selection_add_to_playlist),
                onClick = onAddToPlaylist
            )
            VideoSelectionActionRow(
                icon = Icons.Rounded.Share,
                label = stringResource(R.string.video_selection_share),
                onClick = onShare
            )
            VideoSelectionActionRow(
                icon = Icons.Rounded.Edit,
                label = stringResource(R.string.video_selection_rename),
                enabled = single,
                onClick = onRename
            )
            VideoSelectionActionRow(
                icon = Icons.Rounded.Info,
                label = stringResource(R.string.video_selection_details),
                enabled = single,
                onClick = onDetails
            )
            VideoSelectionActionRow(
                icon = Icons.Rounded.Delete,
                label = stringResource(R.string.video_selection_delete),
                destructive = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun VideoSelectionActionRow(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/** Check badge drawn over a selected video's thumbnail. */
@Composable
fun VideoSelectionCheckOverlay(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    if (!selected) return
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Details dialog for a single video: title, path, size, duration, resolution, format, date. */
@Composable
fun VideoDetailsDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val dateText = remember(video.dateAddedSec) {
        runCatching {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(video.dateAddedSec * 1000L))
        }.getOrDefault("—")
    }
    val resolutionText = if (video.width > 0 && video.height > 0) {
        "${video.width} × ${video.height}"
    } else {
        "—"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.video_info_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                VideoDetailRow(
                    label = stringResource(R.string.video_info_name),
                    value = video.displayTitle
                )
                VideoDetailRow(
                    label = stringResource(R.string.video_info_path),
                    value = video.path ?: video.contentUriString
                )
                VideoDetailRow(
                    label = stringResource(R.string.video_info_size),
                    value = ZipShareHelper.formatFileSize(video.sizeBytes)
                )
                VideoDetailRow(
                    label = stringResource(R.string.video_info_duration),
                    value = formatDuration(video.durationMs)
                )
                VideoDetailRow(
                    label = stringResource(R.string.video_info_resolution),
                    value = resolutionText
                )
                VideoDetailRow(
                    label = stringResource(R.string.video_info_format),
                    value = video.mimeType ?: "—"
                )
                VideoDetailRow(
                    label = stringResource(R.string.video_selection_details_date),
                    value = dateText
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.video_player_done))
            }
        }
    )
}

@Composable
private fun VideoDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Rename dialog for a single video. The name is edited without its file
 * extension; [onConfirm] receives the full display name with the original
 * extension appended.
 */
@Composable
fun VideoRenameDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val (baseName, extension) = remember(video.displayTitle) {
        val title = video.displayTitle
        val dot = title.lastIndexOf('.')
        if (dot > 0) {
            title.substring(0, dot) to title.substring(dot + 1)
        } else {
            title to ""
        }
    }
    var name by remember(baseName) { mutableStateOf(baseName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.video_selection_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = stringResource(R.string.video_selection_rename_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val trimmed = name.trim()
                    val fullName = if (extension.isNotBlank() &&
                        !trimmed.endsWith(".$extension", ignoreCase = true)
                    ) {
                        "$trimmed.$extension"
                    } else {
                        trimmed
                    }
                    onConfirm(fullName)
                }
            ) {
                Text(text = stringResource(R.string.video_selection_rename))
            }
        }
    )
}

/** In-app delete confirmation (used when no system delete dialog is available). */
@Composable
fun VideoDeleteConfirmDialog(
    videos: List<VideoItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.video_selection_delete_title)) },
        text = {
            Text(text = stringResource(R.string.video_selection_delete_message, videos.size))
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.video_selection_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

/**
 * Registers the two system-permission launchers (delete / write) for video
 * selection and collects the ViewModel's one-shot permission requests.
 * Must be composed once per video selection screen.
 */
@Composable
fun VideoSelectionPermissionLaunchers(videoViewModel: VideoViewModel) {
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        videoViewModel.onVideoDeletePermissionResult(
            result.resultCode == Activity.RESULT_OK
        )
    }
    val writeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        videoViewModel.onVideoWritePermissionResult(
            result.resultCode == Activity.RESULT_OK
        )
    }
    LaunchedEffect(videoViewModel) {
        launch {
            videoViewModel.videoDeletePermissionRequest.collect { sender ->
                runCatching {
                    deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
            }
        }
        launch {
            videoViewModel.videoWritePermissionRequest.collect { sender ->
                runCatching {
                    writeLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
            }
        }
    }
}

/** Shows the ViewModel's toast messages. Compose once per video selection screen. */
@Composable
fun VideoSelectionToastCollector(videoViewModel: VideoViewModel) {
    val context = LocalContext.current
    LaunchedEffect(videoViewModel) {
        videoViewModel.videoToastEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Host for the whole video-selection flow: permission launchers, toasts,
 * the actions bottom sheet and the single-video dialogs. Compose once per
 * video selection screen, passing the full library list so the sheet
 * resolves the selection even when the screen shows a filtered subset.
 * [onPlayVideos] receives the selected videos (play them as one queue).
 */
@Composable
fun VideoSelectionActionsHost(
    videoViewModel: VideoViewModel,
    allVideos: List<VideoItem>,
    onPlayVideos: (List<VideoItem>) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val selectedUris by videoViewModel.selectedVideoUris.collectAsStateWithLifecycle()
    val selected = remember(selectedUris, allVideos) {
        allVideos.filter { it.contentUriString in selectedUris }
    }
    val showSheet by videoViewModel.showVideoSelectionSheet.collectAsStateWithLifecycle()
    val detailsVideo by videoViewModel.videoDetailsTarget.collectAsStateWithLifecycle()
    val renameVideo by videoViewModel.videoRenameTarget.collectAsStateWithLifecycle()
    val pendingConfirm by videoViewModel.pendingVideoDeleteConfirm.collectAsStateWithLifecycle()

    VideoSelectionPermissionLaunchers(videoViewModel)
    VideoSelectionToastCollector(videoViewModel)

    if (showSheet && selected.isNotEmpty()) {
        VideoSelectionBottomSheet(
            selectedVideos = selected,
            onDismiss = { videoViewModel.closeVideoSelectionSheet() },
            onPlay = {
                videoViewModel.closeVideoSelectionSheet()
                onPlayVideos(selected)
            },
            onAddToPlaylist = {
                videoViewModel.closeVideoSelectionSheet()
                videoViewModel.addVideosToPlaylist(selected)
            },
            onShare = {
                videoViewModel.closeVideoSelectionSheet()
                activity?.let { videoViewModel.shareVideos(it, selected) }
            },
            onRename = {
                videoViewModel.closeVideoSelectionSheet()
                selected.firstOrNull()?.let { videoViewModel.showVideoRename(it) }
            },
            onDetails = {
                videoViewModel.closeVideoSelectionSheet()
                selected.firstOrNull()?.let { videoViewModel.showVideoDetails(it) }
            },
            onDelete = {
                videoViewModel.closeVideoSelectionSheet()
                videoViewModel.requestDeleteVideos(selected)
            }
        )
    }
    detailsVideo?.let { video ->
        VideoDetailsDialog(
            video = video,
            onDismiss = { videoViewModel.showVideoDetails(null) }
        )
    }
    renameVideo?.let { video ->
        VideoRenameDialog(
            video = video,
            onDismiss = { videoViewModel.showVideoRename(null) },
            onConfirm = { newName ->
                videoViewModel.showVideoRename(null)
                videoViewModel.requestRenameVideo(video, newName)
            }
        )
    }
    pendingConfirm?.let { videos ->
        VideoDeleteConfirmDialog(
            videos = videos,
            onDismiss = { videoViewModel.dismissVideoDeleteConfirm() },
            onConfirm = { videoViewModel.confirmVideoDelete() }
        )
    }
}
