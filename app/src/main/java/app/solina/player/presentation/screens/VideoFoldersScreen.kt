package app.solina.player.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.model.VideoFolder
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.groupVideosByFolder
import app.solina.player.data.model.parentFolderPath
import app.solina.player.presentation.components.VideoThumbnail
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.utils.formatDuration
import java.net.URLDecoder

private enum class FolderSort { NAME, COUNT }

/**
 * Video folders (bottom-bar feedback, 2026-11-16): the video bottom bar's
 * Folders button replaces the music Library button. Videos are grouped by
 * their parent folder on the device; each folder shows its video count and
 * total duration, a Play-all button, and opens its videos on tap. The
 * settings row at the top sorts folders by name or by count and toggles
 * showing the full folder path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFoldersScreen(
    navController: NavController,
    videoViewModel: VideoViewModel = hiltViewModel()
) {
    val videos by videoViewModel.videos.collectAsStateWithLifecycle()
    var sort by rememberSaveable { mutableStateOf(FolderSort.NAME) }
    var showPaths by rememberSaveable { mutableStateOf(false) }

    val folders = remember(videos, sort) {
        val grouped = groupVideosByFolder(videos)
        when (sort) {
            FolderSort.NAME -> grouped.sortedBy { it.name.lowercase() }
            FolderSort.COUNT -> grouped.sortedWith(
                compareByDescending<VideoFolder> { it.videos.size }
                    .thenBy { it.name.lowercase() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.video_folders_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Folder settings: sort order + show full paths.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sort == FolderSort.NAME,
                    onClick = { sort = FolderSort.NAME },
                    label = { Text(text = stringResource(R.string.video_folders_sort_name)) }
                )
                FilterChip(
                    selected = sort == FolderSort.COUNT,
                    onClick = { sort = FolderSort.COUNT },
                    label = { Text(text = stringResource(R.string.video_folders_sort_count)) }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.video_folders_show_paths),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showPaths,
                    onCheckedChange = { showPaths = it }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.video_count_label, videos.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (videos.isNotEmpty()) {
                    Button(
                        onClick = {
                            navController.navigateSafely(
                                Screen.VideoPlayer.createRoute(
                                    startUri = videos.first().contentUriString,
                                    queueUris = videos.map { it.contentUriString }
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = stringResource(R.string.video_play_all))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (folders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.video_no_videos_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.video_no_videos_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(folders, key = { it.path.ifBlank { "unknown" } }) { folder ->
                        VideoFolderRow(
                            folder = folder,
                            showPath = showPaths,
                            onClick = {
                                navController.navigateSafely(
                                    Screen.VideoFolderDetail.createRoute(folder.path)
                                )
                            },
                            onPlayAll = {
                                navController.navigateSafely(
                                    Screen.VideoPlayer.createRoute(
                                        startUri = folder.videos.first().contentUriString,
                                        queueUris = folder.videos.map { it.contentUriString }
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoFolderRow(
    folder: VideoFolder,
    showPath: Boolean,
    onClick: () -> Unit,
    onPlayAll: () -> Unit
) {
    val folderName = folder.name.ifBlank {
        stringResource(R.string.video_folder_unknown)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.video_folder_summary,
                        folder.videos.size,
                        formatDuration(folder.totalDurationMs)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showPath && folder.path.isNotBlank()) {
                    Text(
                        text = folder.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onPlayAll) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.video_play_all),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * One folder's videos, newest first. Reached from [VideoFoldersScreen];
 * tapping a video opens the video player, Play all plays the folder as
 * one queue.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFolderDetailScreen(
    navController: NavController,
    folderPathArg: String,
    videoViewModel: VideoViewModel = hiltViewModel()
) {
    val folderPath = remember(folderPathArg) {
        runCatching { URLDecoder.decode(folderPathArg, "UTF-8") }
            .getOrElse { folderPathArg }
    }
    val allVideos by videoViewModel.videos.collectAsStateWithLifecycle()
    val folderVideos = remember(allVideos, folderPath) {
        allVideos.filter { it.parentFolderPath() == folderPath }
    }
    val folderName = folderPath.substringAfterLast('/').ifBlank {
        stringResource(R.string.video_folder_unknown)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = folderName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.video_folder_summary,
                        folderVideos.size,
                        formatDuration(folderVideos.sumOf { it.durationMs })
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (folderVideos.isNotEmpty()) {
                    Button(
                        onClick = {
                            navController.navigateSafely(
                                Screen.VideoPlayer.createRoute(
                                    startUri = folderVideos.first().contentUriString,
                                    queueUris = folderVideos.map { it.contentUriString }
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = stringResource(R.string.video_play_all))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folderVideos, key = { it.id }) { video ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigateSafely(
                                    Screen.VideoPlayer.createRoute(video.contentUriString)
                                )
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VideoThumbnail(
                                video = video,
                                modifier = Modifier.size(width = 112.dp, height = 64.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.displayTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatDuration(video.durationMs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (video.mimeType != null) {
                                    Text(
                                        text = video.mimeType,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
