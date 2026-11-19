package app.solina.player.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.solina.player.R
import app.solina.player.data.model.Song
import app.solina.player.data.model.VideoItem
import app.solina.player.presentation.components.VideoThumbnail
import app.solina.player.presentation.utils.HomeMediaFilter
import app.solina.player.utils.formatDuration

/**
 * Full-list modal sheet for "Your Mix" so users can browse, play, or open options
 * for all tracks in their mix — not only the 3 preview items shown on Home.
 */
@Composable
internal fun YourMixFullListSheetContent(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onPlayShuffled: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    val totalDurationMs = remember(songs) { songs.sumOf { it.duration } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_your_mix_queue_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stringResource(R.string.home_music_count_label, songs.size)} · ${HomeMediaFilter.formatCompactMixDuration(totalDurationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onPlayShuffled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(R.string.home_mix_shuffle_short))
            }
        }
        HorizontalDivider(thickness = 0.5.dp)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongListItemFavs(
                    cardCorners = 16.dp,
                    songId = song.id,
                    title = song.title,
                    artist = song.displayArtist,
                    albumArtUrl = song.albumArtUriString,
                    isPlaying = isPlaying,
                    isCurrentSong = song.id == currentSongId,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongLongClick(song) }
                )
            }
        }
    }
}

/**
 * Quick long-press options sheet for a video card on Home: resume from saved point,
 * start from beginning, or jump to Video Folders.
 */
@Composable
internal fun HomeVideoOptionsSheetContent(
    video: VideoItem,
    savedPositionMs: Long,
    onResumeOrPlay: () -> Unit,
    onPlayFromStart: () -> Unit,
    onOpenFolders: () -> Unit
) {
    val unknownFolder = stringResource(R.string.video_folder_unknown)
    val subtitle = remember(video, unknownFolder) {
        HomeMediaFilter.cleanVideoSubtitle(video, unknownFolder)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VideoThumbnail(
                video = video,
                modifier = Modifier
                    .size(width = 88.dp, height = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$subtitle · ${formatDuration(video.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HomeVideoActionRow(
                    icon = Icons.Rounded.PlayArrow,
                    label = if (savedPositionMs > 5_000L) {
                        stringResource(R.string.video_resume_prompt_title, formatDuration(savedPositionMs))
                    } else {
                        stringResource(R.string.home_video_action_play)
                    },
                    onClick = onResumeOrPlay
                )
                if (savedPositionMs > 5_000L) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    HomeVideoActionRow(
                        icon = Icons.Rounded.Replay,
                        label = stringResource(R.string.video_resume_start_over),
                        onClick = onPlayFromStart
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                HomeVideoActionRow(
                    icon = Icons.Rounded.Folder,
                    label = stringResource(R.string.video_folders_title),
                    onClick = onOpenFolders
                )
            }
        }
    }
}

@Composable
private fun HomeVideoActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
