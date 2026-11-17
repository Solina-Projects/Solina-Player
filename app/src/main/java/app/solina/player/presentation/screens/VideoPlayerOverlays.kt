package app.solina.player.presentation.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import app.solina.player.presentation.components.GlassModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solina.player.presentation.components.glassDialog
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solina.player.R
import app.solina.player.data.model.VideoBookmark
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoSubtitleStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Reference-player redesign (2026-11-16): the player's menus and panels —
 * the three-dot grid menu (like the reference's 4-column sheet), the
 * subtitle side panel / bottom sheet, and the queue / bookmarks /
 * chapters / info / tutorial / display-settings dialogs. All translucent
 * dark with white icons, in the app's Material 3 Expressive language.
 *
 * Nothing here is decorative: every grid item does something real. The
 * reference's "Cut" button is deliberately absent — there is no video
 * transcoder in the app, so a Cut button would be fake. "Online
 * subtitles" is absent too — there is no subtitle provider/API. Chapters
 * show an honest "no chapters" row because Media3 exposes no chapter
 * list for these files.
 */

/** The one accent colour of the player: speed circle, progress, actives. */
val videoAccentColor: Color = Color(0xFF8FB8FF)

/** What the three-dot grid menu can do. */
enum class VideoGridAction {
    AUDIO, SUBTITLE, QUEUE, ASPECT, DISPLAY, BOOKMARK, CHAPTER,
    FAVOURITE, PLAYLIST, INFO, SHARE, TUTORIAL, MORE
}

private data class GridMenuEntry(
    val action: VideoGridAction,
    val icon: ImageVector,
    val labelRes: Int,
    /** Short one-line label for the landscape side panel (round 16). */
    val shortLabelRes: Int? = null
)

/** The three-dot menu as a 4-column grid sheet, like the reference. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoGridMenuSheet(
    isFavourite: Boolean,
    onDismiss: () -> Unit,
    onAction: (VideoGridAction) -> Unit
) {
    val entries = listOf(
        GridMenuEntry(VideoGridAction.AUDIO, Icons.Rounded.Audiotrack, R.string.video_grid_menu_audio),
        GridMenuEntry(VideoGridAction.SUBTITLE, Icons.Rounded.ClosedCaption, R.string.video_grid_menu_subtitle),
        GridMenuEntry(VideoGridAction.QUEUE, Icons.Rounded.QueueMusic, R.string.video_grid_menu_queue, R.string.video_grid_menu_queue_short),
        GridMenuEntry(VideoGridAction.ASPECT, Icons.Rounded.AspectRatio, R.string.video_grid_menu_aspect, R.string.video_grid_menu_aspect_short),
        GridMenuEntry(VideoGridAction.DISPLAY, Icons.Rounded.DisplaySettings, R.string.video_grid_menu_display, R.string.video_grid_menu_display_short),
        GridMenuEntry(VideoGridAction.BOOKMARK, Icons.Rounded.Bookmark, R.string.video_grid_menu_bookmark),
        GridMenuEntry(VideoGridAction.CHAPTER, Icons.Rounded.FormatListBulleted, R.string.video_grid_menu_chapter),
        GridMenuEntry(VideoGridAction.FAVOURITE, Icons.Rounded.Favorite, R.string.video_grid_menu_favourite),
        GridMenuEntry(VideoGridAction.PLAYLIST, Icons.Rounded.PlaylistAdd, R.string.video_grid_menu_playlist, R.string.video_grid_menu_playlist_short),
        GridMenuEntry(VideoGridAction.INFO, Icons.Rounded.Info, R.string.video_grid_menu_info),
        GridMenuEntry(VideoGridAction.SHARE, Icons.Rounded.Share, R.string.video_grid_menu_share),
        GridMenuEntry(VideoGridAction.TUTORIAL, Icons.Rounded.Lightbulb, R.string.video_grid_menu_tutorial),
        GridMenuEntry(VideoGridAction.MORE, Icons.AutoMirrored.Rounded.ArrowForwardIos, R.string.video_grid_menu_more)
    )
    // Round 16, item 8: in landscape the bottom sheet is a narrow
    // centred panel whose bottom rows overlap the seek bar and the
    // transport buttons, so the menu becomes a side panel instead —
    // the same pattern as the landscape subtitle panel: a scrim to
    // dismiss plus a panel on the right, with smaller cells and
    // one-line short labels so nothing wraps. Portrait keeps the
    // bottom sheet exactly as before.
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onDismiss() }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.42f)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .statusBarsPadding()
            ) {
                VideoGridMenuGrid(
                    entries = entries,
                    isFavourite = isFavourite,
                    columns = 4,
                    compact = true,
                    useShortLabels = true,
                    onEntryClick = { action ->
                        onDismiss()
                        onAction(action)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp)
                )
            }
        }
        return
    }
    GlassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Black.copy(alpha = 0.88f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        VideoGridMenuGrid(
            entries = entries,
            isFavourite = isFavourite,
            columns = 4,
            compact = false,
            useShortLabels = false,
            onEntryClick = { action ->
                onDismiss()
                onAction(action)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
        )
    }
}

/** The grid itself, shared by the portrait sheet and the landscape side panel. */
@Composable
private fun VideoGridMenuGrid(
    entries: List<GridMenuEntry>,
    isFavourite: Boolean,
    columns: Int,
    compact: Boolean,
    useShortLabels: Boolean,
    onEntryClick: (VideoGridAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 20.dp)
    ) {
        items(entries, key = { it.action }) { entry ->
            VideoGridMenuCell(
                entry = entry,
                isFavourite = isFavourite,
                compact = compact,
                useShortLabel = useShortLabels,
                onClick = { onEntryClick(entry.action) }
            )
        }
    }
}

/** One cell of the grid menu: round button + label, press-scale feedback.
 * [compact] is the landscape side-panel variant (round 16): smaller
 * button and icon, one-line short label, so nothing wraps. */
@Composable
private fun VideoGridMenuCell(
    entry: GridMenuEntry,
    isFavourite: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
    useShortLabel: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gridCellScale"
    )
    val isFavouriteCell = entry.action == VideoGridAction.FAVOURITE
    val icon = when {
        isFavouriteCell && isFavourite -> Icons.Rounded.Favorite
        isFavouriteCell -> Icons.Rounded.FavoriteBorder
        else -> entry.icon
    }
    // Heart pop (d3motion social-heart-like): on the favourite cell the
    // heart bounces with the d3motion spring and a shockwave ring expands
    // around it; the sheet dismisses just after the pop is visible.
    val scope = rememberCoroutineScope()
    var popping by remember { mutableStateOf(false) }
    val popScale by animateFloatAsState(
        targetValue = if (popping) 1.35f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "favPop"
    )
    val ripple by animateFloatAsState(
        targetValue = if (popping) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "favRipple"
    )
    val cellClick = {
        if (isFavouriteCell) {
            if (!popping) {
                popping = true
                scope.launch {
                    delay(420)
                    onClick()
                }
            }
        } else {
            onClick()
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        // Round 16: the compact cell is 48 dp (button 40 dp, icon
        // 22 dp); the portrait cell stays 72 dp (button 56 dp,
        // icon 26 dp).
        val cellSize = if (compact) 48.dp else 72.dp
        val buttonSize = if (compact) 40.dp else 56.dp
        val iconSize = if (compact) 22.dp else 26.dp
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(cellSize)
        ) {
            if (isFavouriteCell && ripple > 0f) {
                Canvas(modifier = Modifier.size(cellSize)) {
                    drawCircle(
                        color = Color(0xFFFF5A5A).copy(alpha = (1f - ripple) * 0.7f),
                        radius = size.minDimension / 2f * (0.55f + ripple * 0.45f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            Surface(
                onClick = cellClick,
                interactionSource = interactionSource,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.size(buttonSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isFavouriteCell && isFavourite) {
                            Color(0xFFFF5A5A)
                        } else {
                            Color.White
                        },
                        modifier = Modifier
                            .size(iconSize)
                            .graphicsLayer {
                                scaleX = popScale
                                scaleY = popScale
                            }
                    )
                }
            }
        }
        Text(
            text = stringResource(
                if (useShortLabel) entry.shortLabelRes ?: entry.labelRes else entry.labelRes
            ),
            color = Color.White,
            fontSize = if (compact) 10.sp else 11.sp,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The subtitle panel content, shared by the landscape side panel and the
 * portrait bottom sheet: embedded tracks as checkable rows, "Open" for a
 * subtitle file, and subtitle style settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSubtitlePanelSheet(
    subtitleTrackLabels: List<String>,
    selectedSubtitleTrack: Int,
    onSelectTrack: (Int) -> Unit,
    onOpenFile: () -> Unit,
    subtitleStyle: VideoSubtitleStyle,
    onTextSize: (Float) -> Unit,
    onTextColor: (Int) -> Unit,
    onBackground: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    GlassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Black.copy(alpha = 0.88f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        VideoSubtitlePanelContent(
            subtitleTrackLabels = subtitleTrackLabels,
            selectedSubtitleTrack = selectedSubtitleTrack,
            onSelectTrack = onSelectTrack,
            onOpenFile = onOpenFile,
            subtitleStyle = subtitleStyle,
            onTextSize = onTextSize,
            onTextColor = onTextColor,
            onBackground = onBackground
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun VideoSubtitlePanelContent(
    subtitleTrackLabels: List<String>,
    selectedSubtitleTrack: Int,
    onSelectTrack: (Int) -> Unit,
    onOpenFile: () -> Unit,
    subtitleStyle: VideoSubtitleStyle,
    onTextSize: (Float) -> Unit,
    onTextColor: (Int) -> Unit,
    onBackground: (Int) -> Unit,
    onClose: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.video_subtitles),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFile() }
                .padding(vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.video_subtitle_open),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.video_subtitle_open_hint),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        VideoSubtitleTrackRow(
            label = stringResource(R.string.video_subtitle_off),
            selected = selectedSubtitleTrack < 0,
            onClick = { onSelectTrack(-1) }
        )
        if (subtitleTrackLabels.isEmpty()) {
            Text(
                text = stringResource(R.string.video_no_subtitles),
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            subtitleTrackLabels.forEachIndexed { index, label ->
                VideoSubtitleTrackRow(
                    label = label,
                    selected = selectedSubtitleTrack == index,
                    onClick = { onSelectTrack(index) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.video_subtitle_settings),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.video_subtitle_text_size),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                R.string.video_subtitle_small to 16f,
                R.string.video_subtitle_medium to 20f,
                R.string.video_subtitle_large to 26f
            ).forEach { (labelRes, size) ->
                VideoChoiceChip(
                    label = stringResource(labelRes),
                    selected = subtitleStyle.textSizeSp == size,
                    onClick = { onTextSize(size) }
                )
            }
        }
        Text(
            text = stringResource(R.string.video_subtitle_text_color),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "White" to Color.White,
                "Yellow" to Color(0xFFFFFF00),
                "Cyan" to Color(0xFF00E5FF)
            ).forEach { (name, color) ->
                VideoChoiceChip(
                    label = name,
                    selected = subtitleStyle.textColorArgb == color.toArgb(),
                    onClick = { onTextColor(color.toArgb()) }
                )
            }
        }
        Text(
            text = stringResource(R.string.video_subtitle_background),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                R.string.video_subtitle_none to 0,
                R.string.video_subtitle_dark to 1,
                R.string.video_subtitle_solid to 2
            ).forEach { (labelRes, mode) ->
                VideoChoiceChip(
                    label = stringResource(labelRes),
                    selected = subtitleStyle.backgroundMode == mode,
                    onClick = { onBackground(mode) }
                )
            }
        }
    }
}

@Composable
private fun VideoSubtitleTrackRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(
                if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (selected) "●  " else "○  ",
            color = if (selected) videoAccentColor else Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) videoAccentColor else Color.White.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/** A translucent dialog used by all the player's list/info dialogs. */
@Composable
private fun VideoPlayerDialog(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirm: (() -> Unit)? = null,
    appearanceViewModel: AppearanceViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    // Glass & blur (blur-engine test build, 2026-11-17): the dialog
    // becomes a glass card when the master switch and the dialogs
    // toggle are on. Note the video behind it is a SurfaceView, which
    // no blur library can capture — over video the glass shows its
    // tint over a dark backdrop (the video area stays a hole in the
    // capture); over the library it blurs for real.
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val glass = appearance.glass
    val glassOn = glass.enabled && glass.dialogs
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.glassDialog(
            glass = glass,
            tintColor = Color.Black,
            enabled = glassOn
        ),
        containerColor = if (glassOn) Color.Transparent else Color.Black.copy(alpha = 0.88f),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text(text = title) },
        text = { content() },
        confirmButton = {
            TextButton(onClick = { onConfirm?.invoke() ?: onDismissRequest() }) {
                Text(text = stringResource(R.string.video_player_done))
            }
        }
    )
}

/** Audio tracks (dubbing) as a checkable list. */
@Composable
fun VideoAudioTrackDialog(
    audioTrackLabels: List<String>,
    selectedAudioTrack: Int,
    onSelectTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_audio_track)
    ) {
        Column {
            if (audioTrackLabels.isEmpty()) {
                Text(
                    text = stringResource(R.string.video_no_audio_tracks),
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                audioTrackLabels.forEachIndexed { index, label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTrack(index)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (index == selectedAudioTrack) "●  $label" else "○  $label",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** The playing queue: tap an item to play it. */
@Composable
fun VideoQueueDialog(
    queueItems: List<VideoItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_grid_menu_queue)
    ) {
        Column {
            if (queueItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.video_queue_empty),
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                queueItems.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(index)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = item.displayTitle,
                            color = if (index == currentIndex) videoAccentColor else Color.White,
                            fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatVideoTimeShort(item.durationMs),
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/** Bookmarks: add one at the current position, jump to one, delete one. */
@Composable
fun VideoBookmarksDialog(
    bookmarks: List<VideoBookmark>,
    onAdd: () -> Unit,
    onJump: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_bookmarks_title)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAdd() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkBorder,
                    contentDescription = null,
                    tint = videoAccentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.video_bookmark_add_here),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (bookmarks.isEmpty()) {
                Text(
                    text = stringResource(R.string.video_no_bookmarks),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                bookmarks.forEach { bookmark ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onJump(bookmark.positionMs)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = formatVideoTimeShort(bookmark.positionMs),
                            color = videoAccentColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(bookmark.positionMs) }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Chapters: Media3 exposes no chapter list for these files, said plainly. */
@Composable
fun VideoChaptersDialog(onDismiss: () -> Unit) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_chapters_title)
    ) {
        Column {
            Text(
                text = stringResource(R.string.video_no_chapters),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(R.string.video_no_chapters_hint),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Real file information: name, path, size, duration, resolution, format, decoder. */
@Composable
fun VideoInfoDialog(
    video: VideoItem?,
    decoderBadge: String,
    onDismiss: () -> Unit
) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_info_title)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (video == null) {
                Text(
                    text = stringResource(R.string.video_unknown_title),
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                VideoInfoRow(
                    stringResource(R.string.video_info_name),
                    video.displayTitle
                )
                VideoInfoRow(
                    stringResource(R.string.video_info_path),
                    video.path ?: video.contentUriString
                )
                VideoInfoRow(
                    stringResource(R.string.video_info_size),
                    formatVideoSize(video.sizeBytes)
                )
                VideoInfoRow(
                    stringResource(R.string.video_info_duration),
                    formatVideoTimeShort(video.durationMs)
                )
                VideoInfoRow(
                    stringResource(R.string.video_info_resolution),
                    if (video.width > 0 && video.height > 0) {
                        "${video.width} × ${video.height}"
                    } else {
                        "—"
                    }
                )
                VideoInfoRow(
                    stringResource(R.string.video_info_format),
                    video.mimeType ?: "—"
                )
                VideoInfoRow(
                    stringResource(R.string.video_info_decoder),
                    decoderBadge
                )
            }
        }
    }
}

@Composable
private fun VideoInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The gestures, in plain words. */
@Composable
fun VideoTutorialDialog(onDismiss: () -> Unit) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_tutorial_title)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                R.string.video_tutorial_swipe,
                R.string.video_tutorial_long_press,
                R.string.video_tutorial_vertical,
                R.string.video_tutorial_pinch,
                R.string.video_tutorial_tap,
                R.string.video_tutorial_top
            ).forEach { lineRes ->
                Text(
                    text = "•  " + stringResource(lineRes),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** Display settings: night light + brightness (same controls as before). */
@Composable
fun VideoDisplaySettingsDialog(
    nightLight: Boolean,
    brightness: Int,
    onNightLight: (Boolean) -> Unit,
    onBrightness: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_grid_menu_display)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.video_night_light),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = nightLight, onCheckedChange = onNightLight)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.video_brightness),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { onBrightness(it.toInt()) },
                    valueRange = 10f..100f,
                    enabled = nightLight,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "$brightness%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * The volume / brightness gesture hint as a vertical edge slider, like
 * the reference (portrait frame): % on top, a vertical track, the
 * matching icon at the bottom. Shown while the finger drags.
 */
@Composable
fun VideoEdgeSlider(
    label: String,
    percent: Int,
    isBrightness: Boolean,
    modifier: Modifier = Modifier
) {
    // Fluid fill (d3motion volume-brightness-sliders): the fill height
    // chases the target with the d3motion spring instead of jumping, so
    // it settles with a small bounce when the finger stops.
    val fillHeight by animateDpAsState(
        targetValue = (120 * percent / 100).dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 140f),
        label = "edgeFill"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Text(
            text = "$percent%",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 120.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(
                        width = 4.dp,
                        height = fillHeight
                    )
                    .background(videoAccentColor, RoundedCornerShape(2.dp))
            )
        }
        Icon(
            imageVector = if (isBrightness) {
                Icons.Rounded.BrightnessHigh
            } else {
                Icons.Rounded.VolumeUp
            },
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** "mm:ss" / "h:mm:ss", shared by the overlays. */
fun formatVideoTimeShort(positionMs: Long): String {
    val totalSeconds = (positionMs / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/** "12.4 MB" / "1.2 GB", for the Information dialog. */
fun formatVideoSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "—"
    val mb = sizeBytes / (1024.0 * 1024.0)
    return if (mb >= 1024) {
        "%.1f GB".format(mb / 1024.0)
    } else if (mb >= 1) {
        "%.1f MB".format(mb)
    } else {
        "%d KB".format(sizeBytes / 1024)
    }
}
