package app.solina.player.presentation.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.solina.player.R
import app.solina.player.data.model.Song
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoProgressEntry
import app.solina.player.presentation.utils.HomeMediaFilter
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.ui.theme.RoundedSans
import app.solina.player.utils.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Home media sections (feature/music-and-video). Music and video stay cleanly
 * separated while sharing cohesive Material 3 Expressive visual craft and deep
 * user customisation.
 */

/** Bounded in-memory LRU cache for video frame thumbnails so scrolling never flashes or re-decodes. */
object VideoThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(16 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

/**
 * Song artwork surface that displays [SmartImage] when album art exists, or a tasteful
 * deterministic tonal gradient with a crisp music glyph when artwork is missing —
 * avoiding monotonous flat-grey note walls across Home cards and mix previews.
 */
@Composable
fun SongArtworkTile(
    songId: String,
    albumArtUrl: String?,
    contentDescription: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp
) {
    if (!albumArtUrl.isNullOrBlank()) {
        SmartImage(
            model = albumArtUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            shape = shape,
            modifier = modifier
        )
    } else {
        val colors = MaterialTheme.colorScheme
        val toneIndex = remember(songId) { abs(songId.hashCode()) % 4 }
        val (startColor, endColor, iconTint) = when (toneIndex) {
            0 -> Triple(colors.primaryContainer, colors.secondaryContainer, colors.onPrimaryContainer)
            1 -> Triple(colors.secondaryContainer, colors.tertiaryContainer, colors.onSecondaryContainer)
            2 -> Triple(colors.tertiaryContainer, colors.primaryContainer, colors.onTertiaryContainer)
            else -> Triple(colors.surfaceContainerHighest, colors.primaryContainer.copy(alpha = 0.7f), colors.primary)
        }
        Box(
            modifier = modifier
                .clip(shape)
                .background(Brush.linearGradient(listOf(startColor, endColor))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = contentDescription,
                tint = iconTint.copy(alpha = 0.8f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/** A video frame thumbnail loaded from MediaStore (cached via [VideoThumbnailCache]), with an optional watched-progress strip. */
@Composable
fun VideoThumbnail(
    video: VideoItem,
    modifier: Modifier = Modifier,
    progressFraction: Float = 0f
) {
    val context = LocalContext.current
    val key = video.contentUriString
    val cached = VideoThumbnailCache.get(key)
    val bitmap by produceState<Bitmap?>(initialValue = cached, key) {
        if (value != null) return@produceState
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        Uri.parse(key),
                        Size(320, 180),
                        null
                    )
                } else {
                    null
                }
            }.getOrNull() ?: runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, Uri.parse(key))
                    retriever.getScaledFrameAtTime(
                        1_000_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        320,
                        180
                    )
                } finally {
                    runCatching { retriever.release() }
                }
            }.getOrNull()
        }
        if (loaded != null) {
            VideoThumbnailCache.put(key, loaded)
            value = loaded
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
            ShimmerBox(modifier = Modifier.matchParentSize())
            Icon(
                imageVector = Icons.Rounded.VideoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
        val clampedProgress = progressFraction.coerceIn(0f, 1f)
        if (clampedProgress > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clampedProgress)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/** Two symmetric Bento entry cards: enter the Music player or the Video player. */
@Composable
fun MediaEntryCards(
    musicCount: Int,
    videoCount: Int,
    albumCount: Int = 0,
    videoFolderCount: Int = 0,
    showQuickActions: Boolean = true,
    onMusicClick: () -> Unit,
    onVideoClick: () -> Unit,
    onQuickPlayMusic: (() -> Unit)? = null,
    onQuickPlayVideo: (() -> Unit)? = null,
    onCustomizeClick: () -> Unit,
    modifier: Modifier = Modifier,
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val cardAppearance = appearance.homeCards
    val cardScale = cardAppearance.scalePercent / 100f
    val fontScale = appearance.fontScalePercent / 100f
    val palette = effectivePalette(cardAppearance.palette, appearance.homeSectionPalette)
    val paletteScheme = palette?.let { MaterialTheme.colorScheme.withPalette(it) }
    val colors = MaterialTheme.colorScheme

    val musicContainerStart = paletteScheme?.primaryContainer ?: colors.primaryContainer
    val musicContainerEnd = paletteScheme?.primaryContainer?.copy(alpha = 0.78f)
        ?: colors.secondaryContainer.copy(alpha = 0.72f)
    val musicContent = paletteScheme?.onPrimaryContainer ?: colors.onPrimaryContainer
    val musicAccent = paletteScheme?.primary ?: colors.primary
    val musicOnAccent = paletteScheme?.onPrimary ?: colors.onPrimary

    val videoContainerStart = paletteScheme?.secondaryContainer ?: colors.tertiaryContainer
    val videoContainerEnd = paletteScheme?.secondaryContainer?.copy(alpha = 0.75f)
        ?: colors.secondaryContainer.copy(alpha = 0.85f)
    val videoContent = paletteScheme?.onSecondaryContainer ?: colors.onTertiaryContainer
    val videoAccent = paletteScheme?.primary ?: colors.tertiary
    val videoOnAccent = paletteScheme?.onPrimary ?: colors.onTertiary

    val musicMeta = if (albumCount > 0) {
        stringResource(R.string.home_music_meta_label, musicCount, albumCount)
    } else {
        stringResource(R.string.home_music_count_label, musicCount)
    }
    val videoMeta = if (videoFolderCount > 0) {
        stringResource(R.string.home_video_meta_label, videoCount, videoFolderCount)
    } else {
        stringResource(R.string.home_video_count_label, videoCount)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.home_choose_player_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RoundedSans
                ),
                color = colors.onSurface
            )
            TextButton(
                onClick = onCustomizeClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_customize_title),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        // IntrinsicSize.Min ensures both cards always render at the exact same height.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MediaEntryCard(
                title = stringResource(R.string.home_music_card_title),
                subtitle = stringResource(R.string.home_music_card_subtitle),
                countText = musicMeta,
                iconVector = Icons.Rounded.MusicNote,
                quickActionIcon = if (showQuickActions && musicCount > 0 && onQuickPlayMusic != null) {
                    Icons.Rounded.Shuffle
                } else null,
                quickActionDesc = stringResource(R.string.home_quick_shuffle_music),
                onQuickAction = onQuickPlayMusic,
                containerStart = musicContainerStart,
                containerEnd = musicContainerEnd,
                contentColor = musicContent,
                accentColor = musicAccent,
                onAccentColor = musicOnAccent,
                onClick = onMusicClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                cardScale = cardScale,
                fontScale = fontScale,
                corners = cardAppearance.effectiveCorners
            )
            MediaEntryCard(
                title = stringResource(R.string.home_video_card_title),
                subtitle = stringResource(R.string.home_video_card_subtitle),
                countText = videoMeta,
                iconVector = Icons.Rounded.VideoLibrary,
                quickActionIcon = if (showQuickActions && videoCount > 0 && onQuickPlayVideo != null) {
                    Icons.Rounded.PlayArrow
                } else null,
                quickActionDesc = stringResource(R.string.home_quick_play_video),
                onQuickAction = onQuickPlayVideo,
                containerStart = videoContainerStart,
                containerEnd = videoContainerEnd,
                contentColor = videoContent,
                accentColor = videoAccent,
                onAccentColor = videoOnAccent,
                onClick = onVideoClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                cardScale = cardScale,
                fontScale = fontScale,
                corners = cardAppearance.effectiveCorners
            )
        }
    }
}

@Composable
private fun MediaEntryCard(
    title: String,
    subtitle: String,
    countText: String,
    iconVector: ImageVector,
    quickActionIcon: ImageVector?,
    quickActionDesc: String,
    onQuickAction: (() -> Unit)?,
    containerStart: Color,
    containerEnd: Color,
    contentColor: Color,
    accentColor: Color,
    onAccentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardScale: Float = 1f,
    fontScale: Float = 1f,
    corners: app.solina.player.data.preferences.CornerRadii =
        app.solina.player.data.preferences.CornerRadii.all(22)
) {
    val shape = corners.toShape()
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(containerStart, containerEnd)))
        ) {
            // Subtle graphic watermark in the trailing bottom corner for unmistakable visual identity.
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(72.dp * cardScale)
                    .offset(x = 14.dp, y = 14.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp * cardScale),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp * cardScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(contentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp * cardScale)
                                )
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp * fontScale,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RoundedSans
                                ),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (quickActionIcon != null && onQuickAction != null) {
                            Surface(
                                onClick = onQuickAction,
                                shape = CircleShape,
                                color = accentColor,
                                contentColor = onAccentColor,
                                modifier = Modifier.size(30.dp * cardScale)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = quickActionIcon,
                                        contentDescription = quickActionDesc,
                                        modifier = Modifier.size(16.dp * cardScale)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp * fontScale,
                            lineHeight = 16.sp * fontScale
                        ),
                        color = contentColor.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp * fontScale,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = contentColor.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * One-tap customizable shortcut strip for quick access to high-frequency destinations.
 * Users can show/hide or reorder it from Customise Home.
 */
@Composable
fun HomeQuickShortcutsRow(
    onRecentlyPlayedClick: () -> Unit,
    onVideoFoldersClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onDailyMixClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class ShortcutItem(
        val key: String,
        val labelRes: Int,
        val icon: ImageVector,
        val onClick: () -> Unit
    )
    val shortcuts = remember(
        onRecentlyPlayedClick,
        onVideoFoldersClick,
        onEqualizerClick,
        onDailyMixClick,
        onAppearanceClick
    ) {
        listOf(
            ShortcutItem("recent", R.string.home_shortcut_recent, Icons.Rounded.History, onRecentlyPlayedClick),
            ShortcutItem("folders", R.string.home_shortcut_video_folders, Icons.Rounded.Folder, onVideoFoldersClick),
            ShortcutItem("daily_mix", R.string.home_shortcut_daily_mix, Icons.Rounded.AutoAwesome, onDailyMixClick),
            ShortcutItem("eq", R.string.home_shortcut_equalizer, Icons.Rounded.Equalizer, onEqualizerClick),
            ShortcutItem("style", R.string.home_shortcut_appearance, Icons.Rounded.Palette, onAppearanceClick)
        )
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shortcuts, key = { it.key }) { item ->
            Surface(
                onClick = item.onClick,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Horizontal row of recently added music with readable typography, vivid fallback artwork,
 * Play All action, and long-press song options.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentlyAddedMusicRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onOpenAllClick: () -> Unit,
    onSongLongClick: ((Song) -> Unit)? = null,
    onPlayAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    if (songs.isEmpty()) return
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val recent = appearance.effectiveRecentMusicCards
    val cardScale = recent.scale
    val fontScale = recent.fontScale
    val cardCorners = recent.effectiveCorners
    val thumbCorners = cardCorners.insetBy(4)
    val unknownArtist = stringResource(R.string.home_unknown_artist)

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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RoundedSans
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onPlayAllClick != null) {
                    Surface(
                        onClick = onPlayAllClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.home_play_all),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onOpenAllClick,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_see_all),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(recent.gap.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                val cleanArtist = remember(song.displayArtist, unknownArtist) {
                    HomeMediaFilter.cleanArtistDisplay(song.displayArtist, unknownArtist)
                }
                Card(
                    modifier = Modifier
                        .width(116.dp * cardScale)
                        .clip(cardCorners.toShape())
                        .combinedClickable(
                            onClick = { onSongClick(song) },
                            onLongClick = { onSongLongClick?.invoke(song) }
                        ),
                    shape = cardCorners.toShape(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(7.dp)) {
                        SongArtworkTile(
                            songId = song.id,
                            albumArtUrl = song.albumArtUriString,
                            contentDescription = null,
                            shape = thumbCorners.toShape(),
                            iconSize = 30.dp * cardScale,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp * cardScale)
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 12.5.sp * fontScale,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = cleanArtist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp * fontScale
                            ),
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
 * Horizontal row of recently added videos with 16:9 thumbnails, optional watched-progress
 * strip, clean folder/resolution metadata (no raw MIME strings), and long-press actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentlyAddedVideosRow(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onOpenAllClick: () -> Unit,
    onVideoLongClick: ((VideoItem) -> Unit)? = null,
    progressMap: Map<String, VideoProgressEntry> = emptyMap(),
    showProgressStrip: Boolean = true,
    // Round 22, item 5: inline playback inside one card at a time.
    inlinePlayingUri: String? = null,
    inlinePlayer: ExoPlayer? = null,
    inlineIsPlaying: Boolean = false,
    onInlinePlayPause: (VideoItem) -> Unit = {},
    modifier: Modifier = Modifier,
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    if (videos.isEmpty()) return
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val recent = appearance.effectiveRecentVideoCards
    val cardScale = recent.scale
    val fontScale = recent.fontScale
    val cardCorners = recent.effectiveCorners
    val thumbCorners = cardCorners.insetBy(4)
    val unknownFolder = stringResource(R.string.video_folder_unknown)

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
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RoundedSans
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            TextButton(
                onClick = onOpenAllClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_see_all),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(recent.gap.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                val progressFraction = if (showProgressStrip) {
                    progressMap[video.contentUriString]?.fraction ?: 0f
                } else 0f
                val subtitle = remember(video.path, video.width, video.height, video.sizeBytes, unknownFolder) {
                    HomeMediaFilter.cleanVideoSubtitle(video, unknownFolder)
                }
                Card(
                    modifier = Modifier
                        .width(152.dp * cardScale)
                        .clip(cardCorners.toShape())
                        .combinedClickable(
                            onClick = { onVideoClick(video) },
                            onLongClick = { onVideoLongClick?.invoke(video) }
                        ),
                    shape = cardCorners.toShape(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(7.dp)) {
                        Box {
                            // Round 22, item 5: the tapped video plays
                            // INSIDE its own card (small PlayerView over
                            // the shared holder's ExoPlayer, no controls);
                            // every other card keeps its thumbnail.
                            if (inlinePlayingUri == video.contentUriString && inlinePlayer != null) {
                                AndroidView(
                                    factory = { context ->
                                        PlayerView(context).apply {
                                            player = inlinePlayer
                                            useController = false
                                        }
                                    },
                                    update = { view -> view.player = inlinePlayer },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(86.dp * cardScale)
                                        .clip(thumbCorners.toShape())
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(32.dp * cardScale)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .clickable { onInlinePlayPause(video) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (inlineIsPlaying) {
                                            Icons.Rounded.Pause
                                        } else {
                                            Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp * cardScale)
                                    )
                                }
                            } else {
                                VideoThumbnail(
                                    video = video,
                                    progressFraction = progressFraction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(86.dp * cardScale)
                                        .clip(thumbCorners.toShape())
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(5.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.72f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = formatDuration(video.durationMs),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp * fontScale,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(32.dp * cardScale)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp * cardScale)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = video.displayTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 12.5.sp * fontScale,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp * fontScale
                            ),
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
