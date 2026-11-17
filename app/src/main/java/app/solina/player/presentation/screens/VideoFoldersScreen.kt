package app.solina.player.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.solina.player.presentation.components.D3Reveal
import app.solina.player.presentation.components.VideoThumbnail
import app.solina.player.presentation.components.rememberRevealedKeys
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.utils.formatDuration
import java.net.URLDecoder

private enum class FolderViewMode { ALL_FOLDERS, FILES, FOLDERS }

/**
 * Video folders (bottom-bar feedback, 2026-11-16): the video bottom bar's
 * Folders button replaces the music Library button. Videos are grouped by
 * their parent folder on the device; each folder shows its video count and
 * total duration, a Play-all button, and opens its videos on tap.
 *
 * Folders + Player feedback (2026-11-16): a settings dialog (View Mode /
 * Layout / Sort, mirroring the reference app's dialog) and a search field.
 * View mode, layout and sort are persisted in DataStore. The video
 * mini-player bar above the bottom bar is the resume affordance now
 * (bug-fix 2026-11-17: the old floating Resume button sat in exactly the
 * same spot as the video bar and showed through behind it).
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

    // Persisted settings (Folders + Player, 2026-11-16).
    val isGrid by videoViewModel.isVideoFoldersGridView.collectAsStateWithLifecycle()
    val viewModeName by videoViewModel.videoFoldersViewMode.collectAsStateWithLifecycle()
    val sortName by videoViewModel.videoFoldersSort.collectAsStateWithLifecycle()
    val sortAscending by videoViewModel.videoFoldersSortAscending.collectAsStateWithLifecycle()

    val viewMode = runCatching { FolderViewMode.valueOf(viewModeName) }
        .getOrElse { FolderViewMode.FOLDERS }
    val folderSort = runCatching { FolderSort.valueOf(sortName) }
        .getOrElse { FolderSort.TITLE }

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val folders = remember(videos, folderSort, sortAscending) {
        val grouped = groupVideosByFolder(videos)
        val comparator: Comparator<VideoFolder> = when (folderSort) {
            FolderSort.TITLE -> compareBy { it.name.lowercase() }
            FolderSort.DATE -> compareByDescending<VideoFolder> {
                it.videos.maxOfOrNull { video -> video.dateAddedSec } ?: 0L
            }.thenBy { it.name.lowercase() }
            FolderSort.LENGTH -> compareByDescending<VideoFolder> { it.totalDurationMs }
                .thenBy { it.name.lowercase() }
            FolderSort.SIZE -> compareByDescending<VideoFolder> {
                it.videos.sumOf { video -> video.sizeBytes }
            }.thenBy { it.name.lowercase() }
            FolderSort.RESOLUTION -> compareByDescending<VideoFolder> {
                it.videos.maxOfOrNull { video -> video.width * video.height } ?: 0
            }.thenBy { it.name.lowercase() }
            FolderSort.PATH -> compareBy { it.path.lowercase() }
            FolderSort.TYPE -> compareBy<VideoFolder> {
                it.videos.firstOrNull()?.mimeType.orEmpty()
            }.thenBy { it.name.lowercase() }
            // Kept for the old Name/Count chips: same as TITLE / count.
            FolderSort.NAME -> compareBy { it.name.lowercase() }
            FolderSort.COUNT -> compareByDescending<VideoFolder> { it.videos.size }
                .thenBy { it.name.lowercase() }
        }
        val sorted = grouped.sortedWith(comparator)
        if (sortAscending) sorted else sorted.reversed()
    }

    val flatVideos = remember(videos, folderSort, sortAscending) {
        val comparator: Comparator<VideoItem> = when (folderSort) {
            FolderSort.TITLE, FolderSort.NAME -> compareBy { it.displayTitle.lowercase() }
            FolderSort.DATE -> compareByDescending<VideoItem> { it.dateAddedSec }
                .thenBy { it.displayTitle.lowercase() }
            FolderSort.LENGTH -> compareByDescending<VideoItem> { it.durationMs }
                .thenBy { it.displayTitle.lowercase() }
            FolderSort.SIZE -> compareByDescending<VideoItem> { it.sizeBytes }
                .thenBy { it.displayTitle.lowercase() }
            FolderSort.RESOLUTION -> compareByDescending<VideoItem> { it.width * it.height }
                .thenBy { it.displayTitle.lowercase() }
            FolderSort.PATH -> compareBy { it.path.orEmpty().lowercase() }
            FolderSort.TYPE -> compareBy<VideoItem> { it.mimeType.orEmpty() }
                .thenBy { it.displayTitle.lowercase() }
            FolderSort.COUNT -> compareBy { it.displayTitle.lowercase() }
        }
        val sorted = videos.sortedWith(comparator)
        if (sortAscending) sorted else sorted.reversed()
    }

    val query = searchQuery.trim()
    val shownFolders = remember(folders, query) {
        if (query.isBlank()) folders
        else folders.filter { it.name.contains(query, ignoreCase = true) }
    }
    val shownVideos = remember(flatVideos, query) {
        if (query.isBlank()) flatVideos
        else flatVideos.filter { it.displayTitle.contains(query, ignoreCase = true) }
    }

    // Filter fix (Home-entry + Player, 2026-11-16): "Play all" and the
    // "All videos" card used to play the FULL unfiltered list even while a
    // search was active, so the filter looked broken. They now play exactly
    // what is shown on screen: the filtered videos in Files mode, the
    // videos of the filtered folders in the folder modes.
    val playAllVideos = remember(viewMode, shownVideos, shownFolders) {
        if (viewMode == FolderViewMode.FILES) shownVideos
        else shownFolders.flatMap { it.videos }
    }

    if (showSettings) {
        VideoFoldersSettingsDialog(
            viewMode = viewMode,
            isGrid = isGrid,
            sort = folderSort,
            sortAscending = sortAscending,
            onViewModeChange = { videoViewModel.setVideoFoldersViewMode(it.name) },
            onLayoutChange = { videoViewModel.toggleVideoFoldersViewMode() },
            onSortChange = { videoViewModel.setVideoFoldersSort(it.name) },
            onSortDirectionChange = { videoViewModel.setVideoFoldersSortAscending(it) },
            onDismiss = { showSettings = false }
        )
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
                },
                actions = {
                    IconButton(onClick = {
                        searchActive = !searchActive
                        if (!searchActive) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.video_folders_search_hint)
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = stringResource(R.string.video_folders_settings)
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
            if (searchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(
                                if (viewMode == FolderViewMode.FILES) {
                                    R.string.video_folders_search_videos_hint
                                } else {
                                    R.string.video_folders_search_hint
                                }
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
            // Folder settings: sort order + show full paths.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = folderSort == FolderSort.TITLE,
                    onClick = { videoViewModel.setVideoFoldersSort(FolderSort.TITLE.name) },
                    label = { Text(text = stringResource(R.string.video_folders_sort_name)) }
                )
                FilterChip(
                    selected = folderSort == FolderSort.COUNT,
                    onClick = { videoViewModel.setVideoFoldersSort(FolderSort.COUNT.name) },
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
                if (playAllVideos.isNotEmpty()) {
                    Button(
                        onClick = {
                            navController.navigateSafely(
                                Screen.VideoPlayer.createRoute(
                                    startUri = playAllVideos.first().contentUriString,
                                    queueUris = playAllVideos.map { it.contentUriString }
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
            if (videos.isEmpty()) {
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
            } else if (viewMode == FolderViewMode.FILES) {
                VideoFilesList(
                    videos = shownVideos,
                    isGrid = isGrid,
                    navController = navController
                )
            } else {
                VideoFolderList(
                    folders = shownFolders,
                    isGrid = isGrid,
                    showPaths = showPaths,
                    showAllVideosCard = viewMode == FolderViewMode.ALL_FOLDERS &&
                        playAllVideos.isNotEmpty(),
                    allVideosCount = if (query.isBlank()) videos.size else playAllVideos.size,
                    allVideosDurationMs = if (query.isBlank()) {
                        videos.sumOf { it.durationMs }
                    } else {
                        playAllVideos.sumOf { it.durationMs }
                    },
                    onAllVideosClick = {
                        navController.navigateSafely(
                            Screen.VideoPlayer.createRoute(
                                startUri = playAllVideos.first().contentUriString,
                                queueUris = playAllVideos.map { it.contentUriString }
                            )
                        )
                    },
                    navController = navController
                )
            }
        }
    }
}

/** Sort keys for folders and files. NAME / COUNT are the old quick chips. */
private enum class FolderSort { TITLE, DATE, LENGTH, SIZE, RESOLUTION, PATH, TYPE, NAME, COUNT }

@Composable
private fun VideoFolderList(
    folders: List<VideoFolder>,
    isGrid: Boolean,
    showPaths: Boolean,
    showAllVideosCard: Boolean,
    allVideosCount: Int,
    allVideosDurationMs: Long,
    onAllVideosClick: () -> Unit,
    navController: NavController
) {
    val bottomPadding = PaddingValues(bottom = 160.dp)
    // Staggered tile reveal (d3motion gemini-image): each card fades in
    // once, in order, when the list first loads.
    val revealedKeys = rememberRevealedKeys()
    if (isGrid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = bottomPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showAllVideosCard) {
                item {
                    AllVideosCard(
                        count = allVideosCount,
                        durationMs = allVideosDurationMs,
                        onClick = onAllVideosClick
                    )
                }
            }
            itemsIndexed(folders, key = { _, folder -> folder.path.ifBlank { "unknown" } }) { index, folder ->
                D3Reveal(
                    key = folder.path.ifBlank { "unknown" },
                    index = index,
                    revealed = revealedKeys
                ) {
                VideoFolderGridCard(
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = bottomPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showAllVideosCard) {
                item {
                    AllVideosCard(
                        count = allVideosCount,
                        durationMs = allVideosDurationMs,
                        onClick = onAllVideosClick
                    )
                }
            }
            itemsIndexed(folders, key = { _, folder -> folder.path.ifBlank { "unknown" } }) { index, folder ->
                D3Reveal(
                    key = folder.path.ifBlank { "unknown" },
                    index = index,
                    revealed = revealedKeys
                ) {
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

@Composable
private fun VideoFilesList(
    videos: List<VideoItem>,
    isGrid: Boolean,
    navController: NavController
) {
    val bottomPadding = PaddingValues(bottom = 160.dp)
    // Staggered tile reveal (d3motion gemini-image): each card fades in
    // once, in order, when the list first loads.
    val revealedKeys = rememberRevealedKeys()
    if (isGrid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = bottomPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                D3Reveal(
                    key = video.id,
                    index = index,
                    revealed = revealedKeys
                ) {
                VideoFileGridCard(
                    video = video,
                    onClick = {
                        navController.navigateSafely(
                            Screen.VideoPlayer.createRoute(video.contentUriString)
                        )
                    }
                )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = bottomPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                D3Reveal(
                    key = video.id,
                    index = index,
                    revealed = revealedKeys
                ) {
                VideoFileRow(
                    video = video,
                    onClick = {
                        navController.navigateSafely(
                            Screen.VideoPlayer.createRoute(video.contentUriString)
                        )
                    }
                )
                }
            }
        }
    }
}

@Composable
private fun AllVideosCard(
    count: Int,
    durationMs: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.video_all_videos),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.video_folder_summary,
                        count,
                        formatDuration(durationMs)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.video_play_all),
                tint = MaterialTheme.colorScheme.primary
            )
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
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VideoFolderGridCard(
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onPlayAll) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.video_play_all),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
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
    }
}

@Composable
private fun VideoFileRow(
    video: VideoItem,
    onClick: () -> Unit
) {
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

@Composable
private fun VideoFileGridCard(
    video: VideoItem,
    onClick: () -> Unit
) {
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
        Column {
            VideoThumbnail(
                video = video,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            Column(modifier = Modifier.padding(12.dp)) {
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
            }
        }
    }
}

/**
 * The Folders settings dialog, mirroring the reference app's
 * View Mode / Layout / Sort dialog: View Mode (All folders / Files /
 * Folders), Layout (List / Grid), Sort (Title / Date / Length / Size /
 * Resolution / Path / Type) with A to Z / Z to A direction.
 *
 * The reference also has "Played time", "Status" and "Frame rate" sorts
 * plus "Fields" and "Advanced" sections — MediaStore gives this app no
 * per-video play history, watch status or frame rate, so those are left
 * out rather than shown as dead controls.
 *
 * Home-entry + Player feedback (2026-11-16): the old dialog put the three
 * View Mode chips in a single non-wrapping [Row] inside the narrow
 * AlertDialog text area. The three chips did not fit, so the last one
 * ("Folders") was squeezed to ~one character wide and its label wrapped
 * vertically, one letter per line — the "folder filter is broken" the user
 * reported. Every chip group here is now a [FlowRow] that wraps onto a
 * second row instead of squeezing, and every chip label is a single
 * non-wrapping line, so no label can ever render vertically.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VideoFoldersSettingsDialog(
    viewMode: FolderViewMode,
    isGrid: Boolean,
    sort: FolderSort,
    sortAscending: Boolean,
    onViewModeChange: (FolderViewMode) -> Unit,
    onLayoutChange: () -> Unit,
    onSortChange: (FolderSort) -> Unit,
    onSortDirectionChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.video_folders_settings)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.video_folders_view_mode),
                    style = MaterialTheme.typography.titleMedium
                )
                // Two rows on a phone: "All folders" + "Files" on the first,
                // "Folders" on the second — FlowRow wraps instead of squeezing.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewMode == FolderViewMode.ALL_FOLDERS,
                        onClick = { onViewModeChange(FolderViewMode.ALL_FOLDERS) },
                        label = {
                            Text(
                                text = stringResource(R.string.video_folders_view_all),
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.FolderCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = viewMode == FolderViewMode.FILES,
                        onClick = { onViewModeChange(FolderViewMode.FILES) },
                        label = {
                            Text(
                                text = stringResource(R.string.video_folders_view_files),
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = viewMode == FolderViewMode.FOLDERS,
                        onClick = { onViewModeChange(FolderViewMode.FOLDERS) },
                        label = {
                            Text(
                                text = stringResource(R.string.video_folders_view_folders),
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
                Text(
                    text = stringResource(R.string.video_folders_layout),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isGrid,
                        onClick = { if (isGrid) onLayoutChange() },
                        label = {
                            Text(
                                text = stringResource(R.string.video_folders_layout_list),
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ViewList,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    FilterChip(
                        selected = isGrid,
                        onClick = { if (!isGrid) onLayoutChange() },
                        label = {
                            Text(
                                text = stringResource(R.string.video_folders_layout_grid),
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
                Text(
                    text = stringResource(R.string.video_folders_sort),
                    style = MaterialTheme.typography.titleMedium
                )
                val sortOptions = listOf(
                    FolderSort.TITLE to R.string.video_folders_sort_title,
                    FolderSort.DATE to R.string.video_folders_sort_date,
                    FolderSort.LENGTH to R.string.video_folders_sort_length,
                    FolderSort.SIZE to R.string.video_folders_sort_size,
                    FolderSort.RESOLUTION to R.string.video_folders_sort_resolution,
                    FolderSort.PATH to R.string.video_folders_sort_path,
                    FolderSort.TYPE to R.string.video_folders_sort_type
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortOptions.forEach { (option, labelRes) ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { onSortChange(option) },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sortAscending) {
                        FilledTonalButton(onClick = { }) {
                            Text(text = "↑ " + stringResource(R.string.video_folders_sort_a_to_z))
                        }
                        OutlinedButton(onClick = { onSortDirectionChange(false) }) {
                            Text(text = "↓ " + stringResource(R.string.video_folders_sort_z_to_a))
                        }
                    } else {
                        OutlinedButton(onClick = { onSortDirectionChange(true) }) {
                            Text(text = "↑ " + stringResource(R.string.video_folders_sort_a_to_z))
                        }
                        FilledTonalButton(onClick = { }) {
                            Text(text = "↓ " + stringResource(R.string.video_folders_sort_z_to_a))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.ok))
            }
        }
    )
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
                contentPadding = PaddingValues(bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(folderVideos, key = { it.id }) { video ->
                    VideoFileRow(
                        video = video,
                        onClick = {
                            navController.navigateSafely(
                                Screen.VideoPlayer.createRoute(video.contentUriString)
                            )
                        }
                    )
                }
            }
        }
    }
}
