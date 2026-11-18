package app.solina.player.presentation.screens

import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solina.player.R
import app.solina.player.data.model.VideoBookmark
import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoSubtitleStyle
import app.solina.player.presentation.components.GlassModalBottomSheet
import app.solina.player.presentation.components.glassDialog
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.presentation.viewmodel.VideoPlaybackError
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The one accent colour of the player: speed circle, progress, actives. */
val videoAccentColor: Color = Color(0xFF8FB8FF)

/** Actions exposed in the 4-column video player grid menu. */
enum class VideoGridAction {
    AUDIO, SUBTITLE, QUEUE, ASPECT, DISPLAY, BOOKMARK, JUMP_TO, MIRROR,
    FAVOURITE, PLAYLIST, INFO, SHARE, TUTORIAL, MORE
}

private data class GridMenuEntry(
    val action: VideoGridAction,
    val icon: ImageVector,
    val labelRes: Int,
    val shortLabelRes: Int? = null
)

/** 4-column grid menu sheet (portrait modal sheet, landscape translucent side panel). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoGridMenuSheet(
    isFavourite: Boolean,
    isMirrored: Boolean = false,
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
        GridMenuEntry(VideoGridAction.JUMP_TO, Icons.Rounded.Timer, R.string.video_grid_menu_jump_to),
        GridMenuEntry(VideoGridAction.MIRROR, Icons.Rounded.Flip, R.string.video_grid_menu_mirror),
        GridMenuEntry(VideoGridAction.FAVOURITE, Icons.Rounded.Favorite, R.string.video_grid_menu_favourite),
        GridMenuEntry(VideoGridAction.PLAYLIST, Icons.Rounded.PlaylistAdd, R.string.video_grid_menu_playlist, R.string.video_grid_menu_playlist_short),
        GridMenuEntry(VideoGridAction.INFO, Icons.Rounded.Info, R.string.video_grid_menu_info),
        GridMenuEntry(VideoGridAction.SHARE, Icons.Rounded.Share, R.string.video_grid_menu_share),
        GridMenuEntry(VideoGridAction.TUTORIAL, Icons.Rounded.Lightbulb, R.string.video_grid_menu_tutorial),
        GridMenuEntry(VideoGridAction.MORE, Icons.AutoMirrored.Rounded.ArrowForwardIos, R.string.video_grid_menu_more)
    )
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
                    .fillMaxWidth(0.46f)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .statusBarsPadding()
            ) {
                VideoGridMenuGrid(
                    entries = entries,
                    isFavourite = isFavourite,
                    isMirrored = isMirrored,
                    columns = 4,
                    compact = true,
                    useShortLabels = true,
                    onEntryClick = { action ->
                        onDismiss()
                        onAction(action)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 20.dp)
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
            isMirrored = isMirrored,
            columns = 4,
            compact = false,
            useShortLabels = false,
            onEntryClick = { action ->
                onDismiss()
                onAction(action)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp)
        )
    }
}

@Composable
private fun VideoGridMenuGrid(
    entries: List<GridMenuEntry>,
    isFavourite: Boolean,
    isMirrored: Boolean,
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
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)
    ) {
        items(entries, key = { it.action }) { entry ->
            VideoGridMenuCell(
                entry = entry,
                isFavourite = isFavourite,
                isMirrored = isMirrored,
                compact = compact,
                useShortLabel = useShortLabels,
                onClick = { onEntryClick(entry.action) }
            )
        }
    }
}

@Composable
private fun VideoGridMenuCell(
    entry: GridMenuEntry,
    isFavourite: Boolean,
    isMirrored: Boolean,
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
    val isMirrorActive = entry.action == VideoGridAction.MIRROR && isMirrored
    val icon = when {
        isFavouriteCell && isFavourite -> Icons.Rounded.Favorite
        isFavouriteCell -> Icons.Rounded.FavoriteBorder
        else -> entry.icon
    }
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
                    delay(320)
                    onClick()
                }
            }
        } else {
            onClick()
        }
    }
    val buttonSize = if (compact) 46.dp else 56.dp
    val iconSize = if (compact) 21.dp else 25.dp
    val activeTint = when {
        isFavouriteCell && isFavourite -> Color(0xFFFF6B81)
        isMirrorActive -> videoAccentColor
        else -> Color.White
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { cellClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(buttonSize)
        ) {
            if (isFavouriteCell && popping) {
                val ringAlpha = (1f - ripple).coerceIn(0f, 1f)
                Canvas(modifier = Modifier.size(buttonSize)) {
                    drawCircle(
                        color = Color(0xFFFF6B81).copy(alpha = ringAlpha * 0.65f),
                        radius = (size.minDimension / 2f) * (0.65f + ripple * 0.5f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(buttonSize)
                    .background(
                        if (isMirrorActive) videoAccentColor.copy(alpha = 0.22f)
                        else Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(entry.labelRes),
                    tint = activeTint,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            if (isFavouriteCell) {
                                scaleX = popScale
                                scaleY = popScale
                            }
                        }
                )
            }
        }
        val labelRes = if (useShortLabel) entry.shortLabelRes ?: entry.labelRes else entry.labelRes
        Text(
            text = stringResource(labelRes),
            color = if (isMirrorActive) videoAccentColor else Color.White,
            fontSize = if (compact) 11.sp else 12.sp,
            maxLines = if (useShortLabel) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/** Portrait modal sheet for subtitle selection and styling. */
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
    onEdgeType: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    GlassModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Black.copy(alpha = 0.90f),
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
            onBackground = onBackground,
            onEdgeType = onEdgeType,
            onClose = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        )
    }
}

/** Scrollable subtitle panel content used by portrait sheet and landscape side drawer. */
@OptIn(ExperimentalLayoutApi::class)
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
    onEdgeType: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.video_subtitles),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
        VideoSubtitleTrackRow(
            label = stringResource(R.string.video_subtitle_off),
            selected = selectedSubtitleTrack < 0,
            onClick = { onSelectTrack(-1) }
        )
        subtitleTrackLabels.forEachIndexed { index, label ->
            VideoSubtitleTrackRow(
                label = label,
                selected = index == selectedSubtitleTrack,
                onClick = { onSelectTrack(index) }
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenFile() }
                .padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = videoAccentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.video_subtitle_open),
                color = videoAccentColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
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
            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                R.string.video_color_white to Color.White,
                R.string.video_color_yellow to Color(0xFFFFEB3B),
                R.string.video_color_cyan to Color(0xFF00E5FF),
                R.string.video_color_green to Color(0xFF69F0AE)
            ).forEach { (labelRes, color) ->
                VideoColorChoiceChip(
                    label = stringResource(labelRes),
                    swatchColor = color,
                    selected = subtitleStyle.textColorArgb == color.toArgb(),
                    onClick = { onTextColor(color.toArgb()) }
                )
            }
        }
        Text(
            text = stringResource(R.string.video_subtitle_edge),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                R.string.video_subtitle_edge_none to 0,
                R.string.video_subtitle_edge_outline to 1,
                R.string.video_subtitle_edge_shadow to 2
            ).forEach { (labelRes, edge) ->
                VideoChoiceChip(
                    label = stringResource(labelRes),
                    selected = subtitleStyle.edgeType == edge,
                    onClick = { onEdgeType(edge) }
                )
            }
        }
        Text(
            text = stringResource(R.string.video_subtitle_background),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = videoAccentColor,
                unselectedColor = Color.White.copy(alpha = 0.6f)
            )
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
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun VideoColorChoiceChip(
    label: String,
    swatchColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) videoAccentColor else Color.White.copy(alpha = 0.12f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(swatchColor, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
            )
            Text(
                text = label,
                color = if (selected) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VideoPlayerDialog(
    onDismissRequest: () -> Unit,
    title: String,
    onConfirm: (() -> Unit)? = null,
    appearanceViewModel: AppearanceViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
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
        containerColor = if (glassOn) Color.Transparent else Color.Black.copy(alpha = 0.90f),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text(text = title) },
        text = { content() },
        confirmButton = {
            TextButton(onClick = { onConfirm?.invoke() ?: onDismissRequest() }) {
                Text(text = stringResource(R.string.video_player_done), color = videoAccentColor)
            }
        }
    )
}

/** Unified Audio & Display dialog: real system volume, player gain, audio tracks, and night light / brightness. */
@Composable
fun VideoAudioDisplayDialog(
    audioTrackLabels: List<String>,
    selectedAudioTrack: Int,
    onSelectTrack: (Int) -> Unit,
    playerGain: Float,
    onPlayerGain: (Float) -> Unit,
    nightLight: Boolean,
    brightness: Int,
    onNightLight: (Boolean) -> Unit,
    onBrightness: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVol = remember(audioManager) {
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
    }
    var systemVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: maxVol / 2)
    }

    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_audio_display_title)
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.video_system_volume),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = systemVolume.toFloat(),
                    onValueChange = { raw ->
                        val next = raw.toInt().coerceIn(0, maxVol)
                        systemVolume = next
                        runCatching {
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
                        }
                    },
                    valueRange = 0f..maxVol.toFloat(),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(systemVolume * 100 / maxVol)}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                text = stringResource(R.string.video_player_gain),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = playerGain,
                    onValueChange = onPlayerGain,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(playerGain * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

            Text(
                text = stringResource(R.string.video_audio_track),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            if (audioTrackLabels.isEmpty()) {
                Text(
                    text = stringResource(R.string.video_no_audio_tracks),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                audioTrackLabels.forEachIndexed { index, label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectTrack(index) }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = index == selectedAudioTrack,
                            onClick = { onSelectTrack(index) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = videoAccentColor,
                                unselectedColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = label,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.NightsStay,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.video_night_light),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = nightLight, onCheckedChange = onNightLight)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.BrightnessHigh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { onBrightness(it.toInt()) },
                    valueRange = 10f..100f,
                    enabled = nightLight,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$brightness%",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

/** Playing queue dialog with scroll, active highlight, reorder, and remove controls. */
@Composable
fun VideoQueueDialog(
    queueItems: List<VideoItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_grid_menu_queue)
    ) {
        if (queueItems.isEmpty()) {
            Text(
                text = stringResource(R.string.video_queue_empty),
                color = Color.White.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(queueItems, key = { idx, item -> "${idx}_${item.contentUriString}" }) { index, item ->
                    val isCurrent = index == currentIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCurrent) videoAccentColor.copy(alpha = 0.16f) else Color.Transparent
                            )
                            .clickable {
                                onSelect(index)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.displayTitle,
                                color = if (isCurrent) videoAccentColor else Color.White,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatVideoTimeShort(item.durationMs),
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (index > 0) {
                            IconButton(
                                onClick = { onMove(index, index - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = stringResource(R.string.video_move_up),
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < queueItems.lastIndex) {
                            IconButton(
                                onClick = { onMove(index, index + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = stringResource(R.string.video_move_down),
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (queueItems.size > 1) {
                            IconButton(
                                onClick = { onRemove(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.video_remove_from_queue),
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Bookmarks dialog with scrollable bookmark list. */
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onAdd() }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    itemsIndexed(bookmarks, key = { _, b -> b.positionMs }) { _, bookmark ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onJump(bookmark.positionMs)
                                    onDismiss()
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
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
}

/** Interactive Jump-to-timestamp dialog with Minutes/Seconds input & quick percentage chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoJumpToDialog(
    currentPositionMs: Long,
    durationMs: Long,
    onJump: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialSec = (currentPositionMs / 1000L).coerceAtLeast(0L)
    var minutesText by remember { mutableStateOf((initialSec / 60L).toString()) }
    var secondsText by remember { mutableStateOf("%02d".format(initialSec % 60L)) }

    val parsedTargetMs = remember(minutesText, secondsText, durationMs) {
        val m = minutesText.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val s = secondsText.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val raw = (m * 60L + s) * 1000L
        if (durationMs > 0L) raw.coerceIn(0L, durationMs) else raw.coerceAtLeast(0L)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = videoAccentColor,
        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
        focusedLabelColor = videoAccentColor,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
    )

    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_jump_to_title),
        onConfirm = {
            onJump(parsedTargetMs)
            onDismiss()
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.take(4) },
                    label = { Text(stringResource(R.string.video_jump_to_minutes)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = secondsText,
                    onValueChange = { secondsText = it.take(2) },
                    label = { Text(stringResource(R.string.video_jump_to_seconds)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
            }
            if (durationMs > 0L) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25, 50, 75).forEach { pct ->
                        VideoChoiceChip(
                            label = "$pct%",
                            selected = false,
                            onClick = {
                                val target = (durationMs * pct / 100L)
                                onJump(target)
                                onDismiss()
                            }
                        )
                    }
                }
                Text(
                    text = "${formatVideoTimeShort(parsedTargetMs)} / ${formatVideoTimeShort(durationMs)}",
                    color = videoAccentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Real file metadata dialog (scrollable in landscape). */
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
        Column(
            modifier = Modifier
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (video == null) {
                Text(
                    text = stringResource(R.string.video_unknown_title),
                    color = Color.White.copy(alpha = 0.6f)
                )
            } else {
                VideoInfoRow(stringResource(R.string.video_info_name), video.displayTitle)
                VideoInfoRow(stringResource(R.string.video_info_path), video.path ?: video.contentUriString)
                VideoInfoRow(stringResource(R.string.video_info_size), formatVideoSize(video.sizeBytes))
                VideoInfoRow(stringResource(R.string.video_info_duration), formatVideoTimeShort(video.durationMs))
                VideoInfoRow(
                    stringResource(R.string.video_info_resolution),
                    if (video.width > 0 && video.height > 0) "${video.width} × ${video.height}" else "—"
                )
                VideoInfoRow(stringResource(R.string.video_info_format), video.mimeType ?: "—")
                VideoInfoRow(stringResource(R.string.video_info_decoder), decoderBadge)
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

/** Gesture tutorial dialog (scrollable in landscape). */
@Composable
fun VideoTutorialDialog(onDismiss: () -> Unit) {
    VideoPlayerDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.video_tutorial_title)
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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

/** Non-destructive floating banner for "Ask before resuming" preference mode. */
@Composable
fun VideoResumePromptBanner(
    resumePositionMs: Long,
    onResume: () -> Unit,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.82f),
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.video_resume_prompt_title, formatVideoTimeShort(resumePositionMs)),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            FilledTonalButton(
                onClick = onResume,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = videoAccentColor,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = stringResource(R.string.video_resume_continue), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onStartOver) {
                Text(text = stringResource(R.string.video_resume_start_over), color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

/** Friendly playback error overlay with Retry, Software Decoder fallback, and Next Video. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoPlaybackErrorOverlay(
    error: VideoPlaybackError,
    hasNext: Boolean,
    onRetry: () -> Unit,
    onRetrySoftware: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.88f),
        modifier = modifier
            .padding(horizontal = 24.dp)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF8A80),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = stringResource(R.string.video_error_title),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (error.message.isNotBlank()) {
                Text(
                    text = error.message,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = videoAccentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.video_error_retry))
                }
                if (error.canRetrySoftware) {
                    OutlinedButton(onClick = onRetrySoftware) {
                        Text(stringResource(R.string.video_error_try_software), color = Color.White)
                    }
                }
                if (hasNext) {
                    OutlinedButton(onClick = onNext) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.video_error_next), color = Color.White)
                    }
                }
            }
        }
    }
}

/** Vertical edge slider for volume / brightness gestures. */
@Composable
fun VideoEdgeSlider(
    label: String,
    percent: Int,
    isBrightness: Boolean,
    modifier: Modifier = Modifier
) {
    val fillHeight by animateDpAsState(
        targetValue = (120 * percent / 100).dp,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 140f),
        label = "edgeFill"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(20.dp))
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
                    .size(width = 4.dp, height = fillHeight)
                    .background(videoAccentColor, RoundedCornerShape(2.dp))
            )
        }
        Icon(
            imageVector = if (isBrightness) Icons.Rounded.BrightnessHigh else Icons.Rounded.VolumeUp,
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
