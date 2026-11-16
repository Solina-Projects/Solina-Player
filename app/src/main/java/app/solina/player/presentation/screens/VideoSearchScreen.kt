package app.solina.player.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.window.core.layout.WindowSizeClass
import app.solina.player.R
import app.solina.player.data.model.VideoItem
import app.solina.player.presentation.components.MiniPlayerHeight
import app.solina.player.presentation.components.VideoThumbnail
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.utils.formatDuration
import kotlin.math.absoluteValue

/**
 * Video Search, rebuilt (Search feedback, 2026-11-16) to mirror the Music
 * Search screen exactly: the same top search bar + settings gear, the same
 * grid/list toggle, the same card style, and the same results list.
 *
 * Search + Player polish (2026-11-16): NO folder grouping here — all
 * videos together in one place (empty query = all videos, newest first;
 * typing filters by title/mime). Folders live on the Folders screen.
 * A row of format chips filters the single list.
 *
 * The floating bottom bar is shown on this screen (video items
 * Home / Search / Folders, Search selected) — see MainActivity's hidden
 * routes. The search bar itself is the same DockedSearchBar as music
 * search; the gear opens the same Settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoSearchScreen(
    navController: NavController,
    videoViewModel: VideoViewModel = hiltViewModel()
) {
    val allVideos by videoViewModel.videos.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var formatFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchInputFocusRequester = remember { FocusRequester() }
    val statusBarTopInset = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val isGridView by videoViewModel.isVideoSearchGridView.collectAsStateWithLifecycle()

    // All videos together in one list: empty query = every video, newest
    // first; typing filters by title/mime; a format chip filters by type.
    val results = remember(query, formatFilter, allVideos) {
        val trimmed = query.trim()
        allVideos.filter { video ->
            val formatMatches = formatFilter == null ||
                video.formatExtension.equals(formatFilter, ignoreCase = true)
            val queryMatches = trimmed.isBlank() ||
                video.displayTitle.contains(trimmed, ignoreCase = true) ||
                (video.mimeType?.contains(trimmed, ignoreCase = true) == true)
            formatMatches && queryMatches
        }
    }

    val formats = remember(allVideos) {
        allVideos.groupBy { it.formatExtension.uppercase() }
            .map { (extension, videos) ->
                VideoFormatGroup(extension = extension, videos = videos)
            }
            .sortedByDescending { it.videos.size }
    }

    val searchbarCornerRadius = 28.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Same top row as the Music Search screen: full-width search bar
            // plus the settings gear that opens the same Settings screen.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = statusBarTopInset + 12.dp, end = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val searchBarInputFieldColors = SearchBarDefaults.inputFieldColors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
                Box(
                    Modifier
                        .weight(1f)
                        .background(color = Color.Transparent)
                ) {
                    DockedSearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchInputFocusRequester),
                                query = query,
                                onQueryChange = {
                                    query = it
                                    if (it.isNotBlank()) formatFilter = null
                                },
                                onSearch = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                expanded = false,
                                onExpandedChange = {},
                                placeholder = {
                                    Text(
                                        stringResource(R.string.video_search_placeholder),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = stringResource(R.string.cd_search_icon),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (query.isNotBlank()) {
                                        IconButton(
                                            onClick = { query = "" },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.cd_clear_search_query),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                colors = searchBarInputFieldColors
                            )
                        },
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(searchbarCornerRadius)),
                        colors = SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            dividerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            inputFieldColors = searchBarInputFieldColors
                        ),
                        content = {}
                    )
                }
                FilledIconButton(
                    modifier = Modifier.padding(bottom = 2.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    onClick = { navController.navigateSafely(Screen.Settings.route) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_settings_24),
                        contentDescription = stringResource(R.string.presentation_batch_d_open_settings_cd)
                    )
                }
            }

            // "All videos" header with the same grid/list toggle as Music
            // Search — the list below is every video, in one place.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, bottom = 4.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.video_all_videos),
                    style = MaterialTheme.typography.titleLarge
                )
                val shape = animateFloatAsState(
                    targetValue = if (!isGridView) 12f else 50f,
                    label = "shapeAnimation"
                )
                androidx.compose.material3.FilledIconButton(
                    onClick = { videoViewModel.toggleVideoSearchViewMode() },
                    colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(shape.value.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = stringResource(R.string.cd_toggle_grid_list)
                    )
                }
            }
            VideoSearchResults(
                results = results,
                formats = formats,
                formatFilter = formatFilter,
                onFormatClick = { formatFilter = it },
                isGridView = isGridView,
                onVideoClick = { video ->
                    navController.navigateSafely(
                        Screen.VideoPlayer.createRoute(video.contentUriString)
                    )
                },
                onPlayAll = {
                    navController.navigateSafely(
                        Screen.VideoPlayer.createRoute(
                            startUri = results.first().contentUriString,
                            queueUris = results.map { it.contentUriString }
                        )
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** A format group for the format-chip filter row. */
private data class VideoFormatGroup(
    val extension: String,
    val videos: List<VideoItem>
) {
    val totalDurationMs: Long get() = videos.sumOf { it.durationMs }
}

/** Mime subtype, e.g. "mp4" for video/mp4, "x-matroska" for video/x-matroska. */
private val VideoItem.formatExtension: String
    get() = mimeType?.substringAfter('/')?.ifBlank { "?" } ?: "?"


/**
 * One browse card, same shape as the genre cards in Music Search:
 * 20 dp rounded corners, aspect 1.2 in grid mode / 100 dp high in list
 * mode, pastel container colour, thumbnail bottom-end, title top-start.
 */
@Composable
private fun VideoBrowseCard(
    title: String,
    subtitle: String,
    thumbnailVideo: VideoItem?,
    isGridView: Boolean,
    colorSeed: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val palette = remember(colorScheme) {
        listOf(
            colorScheme.primaryContainer to colorScheme.onPrimaryContainer,
            colorScheme.secondaryContainer to colorScheme.onSecondaryContainer,
            colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer,
            colorScheme.surfaceContainerHigh to colorScheme.onSurface
        )
    }
    val (backgroundColor, onBackgroundColor) = remember(colorSeed, palette) {
        palette[(colorSeed.hashCode().absoluteValue) % palette.size]
    }
    val shape = RoundedCornerShape(20.dp)
    val cardModifier = if (isGridView) {
        Modifier.aspectRatio(1.2f)
    } else {
        Modifier.fillMaxWidth().height(100.dp)
    }

    Card(
        modifier = cardModifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(backgroundColor)
        ) {
            if (thumbnailVideo != null) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 16.dp, y = 16.dp)
                ) {
                    VideoThumbnail(
                        video = thumbnailVideo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        top = 14.dp,
                        end = if (isGridView) 14.dp else 96.dp
                    )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = onBackgroundColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = onBackgroundColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoSearchResults(
    results: List<VideoItem>,
    formats: List<VideoFormatGroup>,
    formatFilter: String?,
    onFormatClick: (String?) -> Unit,
    isGridView: Boolean,
    onVideoClick: (VideoItem) -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localDensity = LocalDensity.current
    val systemNavBarInset = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val imePadding = WindowInsets.ime.getBottom(localDensity).dp
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isMediumWidth = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isExpandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val gridColumns = when {
        isExpandedWidth -> 4
        isMediumWidth -> 3
        else -> 2
    }
    val listBottomPadding = if (imePadding <= 8.dp) {
        MiniPlayerHeight + systemNavBarInset + 94.dp
    } else {
        imePadding
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.video_count_label, results.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (results.isNotEmpty()) {
                Button(onClick = onPlayAll) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = stringResource(R.string.video_play_all))
                }
            }
        }
        // Format filter: "All" + one chip per format, filtering the single
        // list — not a browse section, and never grouped by folder.
        if (formats.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = formatFilter == null,
                        onClick = { onFormatClick(null) },
                        label = { Text(stringResource(R.string.video_all_videos)) }
                    )
                }
                items(formats, key = { it.extension }) { format ->
                    FilterChip(
                        selected = formatFilter.equals(format.extension, ignoreCase = true),
                        onClick = { onFormatClick(format.extension) },
                        label = { Text(format.extension.uppercase()) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.video_search_no_results),
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
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                state = rememberLazyGridState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = listBottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results, key = { it.id }) { video ->
                    VideoBrowseCard(
                        title = video.displayTitle,
                        subtitle = formatDuration(video.durationMs),
                        thumbnailVideo = video,
                        isGridView = true,
                        colorSeed = video.contentUriString,
                        onClick = { onVideoClick(video) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = listBottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(results, key = { it.id }) { video ->
                    VideoSearchRow(
                        video = video,
                        onClick = { onVideoClick(video) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoSearchRow(
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
