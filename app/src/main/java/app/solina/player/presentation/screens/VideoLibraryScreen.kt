package app.solina.player.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.presentation.components.D3Reveal
import app.solina.player.presentation.components.VideoSelectionActionsHost
import app.solina.player.presentation.components.VideoSelectionCheckOverlay
import app.solina.player.presentation.components.VideoThumbnail
import app.solina.player.presentation.components.rememberRevealedKeys
import app.solina.player.presentation.components.subcomps.SelectionActionRow
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.VideoViewModel
import app.solina.player.utils.formatDuration

/**
 * Video library: local videos only, newest first. Music never appears here.
 * Tapping a video opens the separate video player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLibraryScreen(
    navController: NavController,
    videoViewModel: VideoViewModel = hiltViewModel()
) {
    val videos by videoViewModel.videos.collectAsStateWithLifecycle()
    val videoCount by videoViewModel.videoCount.collectAsStateWithLifecycle()
    // Mini-player bar (2026-11-16): it overlays the bottom of this list, so
    // leave room for it (64 dp bar + 8 dp gap) when it is showing.
    val showVideoMiniBar by videoViewModel.showVideoMiniBar.collectAsStateWithLifecycle()
    val progressMap by videoViewModel.videoProgressMap.collectAsStateWithLifecycle()
    // Round 22: long-press multi-selection (mirrors the music library).
    val selectedUris by videoViewModel.selectedVideoUris.collectAsStateWithLifecycle()
    val isSelectionMode by videoViewModel.isVideoSelectionMode.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    BackHandler(enabled = isSelectionMode) {
        videoViewModel.clearVideoSelection()
    }
    // Staggered tile reveal (d3motion gemini-image): each row fades/scales
    // in once, in order, when the list first loads.
    val revealedKeys = rememberRevealedKeys()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            R.string.video_count_label,
                            videoCount
                        )
                    )
                },
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
                        navController.navigateSafely(Screen.VideoCustomization.route)
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.DashboardCustomize,
                            contentDescription = stringResource(R.string.video_customize_title)
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
        ) {
            // Round 22: selection action row (same component as music).
            if (isSelectionMode) {
                SelectionActionRow(
                    selectedCount = selectedUris.size,
                    onSelectAll = {
                        videoViewModel.selectAllVideos(videos.map { it.contentUriString })
                    },
                    onDeselect = { videoViewModel.clearVideoSelection() },
                    onOptionsClick = { videoViewModel.openVideoSelectionSheet() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = if (showVideoMiniBar) 88.dp else 16.dp
                ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                    val selected = video.contentUriString in selectedUris
                    D3Reveal(
                        key = video.id,
                        index = index,
                        revealed = revealedKeys
                    ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        videoViewModel.toggleVideoSelection(video.contentUriString)
                                    } else {
                                        navController.navigateSafely(
                                            // Round 16, item 1a: tapping a video
                                            // plays the whole visible list as one
                                            // queue, starting at the tapped video —
                                            // so playback auto-advances instead of
                                            // stopping at the end of one video.
                                            // A one-item list still plays alone.
                                            Screen.VideoPlayer.createRoute(
                                                startUri = video.contentUriString,
                                                queueUris = videos.map { it.contentUriString }
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    videoViewModel.enterVideoSelection(video.contentUriString)
                                }
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                VideoThumbnail(
                                    video = video,
                                    progressFraction = progressMap[video.contentUriString]?.fraction ?: 0f,
                                    modifier = Modifier.size(width = 112.dp, height = 64.dp)
                                )
                                VideoSelectionCheckOverlay(
                                    selected = selected,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                )
                            }
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
        // Round 22: selection bottom sheet + dialogs + permission launchers.
        VideoSelectionActionsHost(
            videoViewModel = videoViewModel,
            allVideos = videos,
            onPlayVideos = { selected ->
                if (selected.isNotEmpty()) {
                    videoViewModel.clearVideoSelection()
                    navController.navigateSafely(
                        Screen.VideoPlayer.createRoute(
                            startUri = selected.first().contentUriString,
                            queueUris = selected.map { it.contentUriString }
                        )
                    )
                }
            }
        )
    }
}
