package app.solina.player.presentation.components

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solina.player.R
import app.solina.player.data.model.Song
import app.solina.player.data.model.VideoItem
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.utils.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home media sections (feature/music-and-video). Music and video are always
 * separate: two entry cards, two count cards, two recent rows. Same
 * Material 3 Expressive language as the music player — rounded cards,
 * primary/secondary containers, no mixed queue.
 */

/** A video frame thumbnail loaded from MediaStore, with a film icon fallback. */
@Composable
fun VideoThumbnail(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, video.contentUriString) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        Uri.parse(video.contentUriString),
                        Size(320, 180),
                        null
                    )
                } else {
                    null
                }
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            // Skeleton shimmer (d3motion claude-chat thinking state):
            // a travelling gradient while the frame loads, instead of a
            // flat box. It leaves composition the moment the bitmap
            // arrives, so it costs nothing afterwards.
            ShimmerBox(modifier = Modifier.matchParentSize())
            Icon(
                imageVector = Icons.Rounded.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/** Two big entry cards: enter the Music player or the Video player. */
@Composable
fun MediaEntryCards(
    musicCount: Int,
    videoCount: Int,
    onMusicClick: () -> Unit,
    onVideoClick: () -> Unit,
    onCustomizeClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    // Appearance customisation (bug-fix round 2026-11-17, item 6):
    // the Home cards have their own size, corner and colour settings;
    // a chosen palette repaints both cards (music = container,
    // video = containerAlt), otherwise the theme colours stay.
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val cardAppearance = appearance.homeCards
    val cardScale = cardAppearance.scalePercent / 100f
    val fontScale = appearance.fontScalePercent / 100f
    val palette = effectivePalette(cardAppearance.palette, appearance.homeSectionPalette)
    val paletteScheme = palette?.let { MaterialTheme.colorScheme.withPalette(it) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.home_choose_player_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onCustomizeClick) {
                Text(text = stringResource(R.string.home_customize_title))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MediaEntryCard(
                title = stringResource(R.string.home_music_card_title),
                subtitle = stringResource(R.string.home_music_card_subtitle),
                countText = stringResource(R.string.home_music_count_label, musicCount),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp * cardScale)
                    )
                },
                containerColor = paletteScheme?.primaryContainer
                    ?: MaterialTheme.colorScheme.primaryContainer,
                contentColor = paletteScheme?.onPrimaryContainer
                    ?: MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onMusicClick,
                modifier = Modifier.weight(1f),
                cardScale = cardScale,
                fontScale = fontScale,
                cornerDp = cardAppearance.cornerDp
            )
            MediaEntryCard(
                title = stringResource(R.string.home_video_card_title),
                subtitle = stringResource(R.string.home_video_card_subtitle),
                countText = stringResource(R.string.home_video_count_label, videoCount),
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp * cardScale)
                    )
                },
                containerColor = paletteScheme?.secondaryContainer
                    ?: MaterialTheme.colorScheme.secondaryContainer,
                contentColor = paletteScheme?.onSecondaryContainer
                    ?: MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onVideoClick,
                modifier = Modifier.weight(1f),
                cardScale = cardScale,
                fontScale = fontScale,
                cornerDp = cardAppearance.cornerDp
            )
        }
    }
}

@Composable
private fun MediaEntryCard(
    title: String,
    subtitle: String,
    countText: String,
    icon: @Composable () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardScale: Float = 1f,
    fontScale: Float = 1f,
    cornerDp: Int = 20
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(cornerDp.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // Bug-fix (2026-11-17, item 3): the name sits UP next to its icon
        // in one row at the top of the card (it used to sit below the icon),
        // and the card is proportionally smaller — less padding, a smaller
        // icon and title — while both cards stay equal size (weight 1f).
        Column(
            modifier = Modifier.padding(12.dp * cardScale),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon()
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * fontScale
                    ),
                    maxLines = 1
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontScale
                ),
                color = contentColor.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = MaterialTheme.typography.labelLarge.fontSize * fontScale
                ),
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Horizontal row of recently added music (newest audio files first).
 * Home feedback (2026-11-16): 30% smaller on Home — cards 148→104 dp,
 * artwork 120→84 dp — and fonts 20% smaller (title 14→11 sp,
 * artist 12→10 sp, header 22→18 sp). Home only: the Library rows
 * keep their old sizes.
 */
@Composable
fun RecentlyAddedMusicRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onOpenAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.home_recent_music_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onOpenAllClick) {
                Text(
                    text = stringResource(R.string.home_see_all),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                Card(
                    modifier = Modifier
                        .width(104.dp)
                        .clickable { onSongClick(song) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        SmartImage(
                            model = song.albumArtUriString,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.displayArtist,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Horizontal row of recently added videos (newest video files first).
 * Home feedback (2026-11-16): 30% smaller on Home — cards 168→118 dp,
 * thumbnails 96→67 dp, play icon 36→25 dp — and fonts 20% smaller
 * (title 14→11 sp, subtitle 12→10 sp, header 22→18 sp, duration
 * 11→9 sp). Home only: the Library rows keep their old sizes.
 */
@Composable
fun RecentlyAddedVideosRow(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onOpenAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.home_recent_videos_title),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = onOpenAllClick) {
                Text(
                    text = stringResource(R.string.home_see_all),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                Card(
                    modifier = Modifier
                        .width(118.dp)
                        .clickable { onVideoClick(video) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Box {
                            VideoThumbnail(
                                video = video,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(67.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formatDuration(video.durationMs),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(25.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = video.displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.mimeType ?: stringResource(R.string.video_title),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
