package app.solina.player.presentation.screens

import android.app.Activity
import android.content.ContentValues
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.presentation.viewmodel.VideoPlayerViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Video player: a dedicated ExoPlayer with video + subtitle tracks, in the
 * same Expressive visual language as the music player. Separate from the
 * music player — no music queue, no crossfade, no audio-only service.
 * Subtitles: PlayerView's built-in CC button appears when the video carries
 * a subtitle track.
 *
 * Folders + Player feedback (2026-11-16):
 * - The floating bottom bar is HIDDEN while this screen is open (see
 *   MainActivity's hidden routes), so it never covers the player.
 * - A hidden extra control bar sits at the bottom, with an animated arrow
 *   handle in front of it: tapping the arrow slides the bar in/out with
 *   animation. The bar holds aspect / rotation, playback speed, screenshot,
 *   audio settings (volume + audio track), sleep mode, night light and
 *   background playback (audio keeps playing when the app goes to the
 *   background, like music).
 * - Leaving the player (or every 15 s while playing) saves a resume point
 *   so the Folders screen's Resume button can continue where it left off.
 */
@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    navController: NavController,
    videoUri: String,
    videoQueue: List<String> = emptyList(),
    startPositionMs: Long = 0L,
    videoPlayerViewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val player by videoPlayerViewModel.player.collectAsStateWithLifecycle()
    val currentVideo by videoPlayerViewModel.currentVideo.collectAsStateWithLifecycle()
    val playbackSpeed by videoPlayerViewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isFullscreen by videoPlayerViewModel.isFullscreen.collectAsStateWithLifecycle()
    val aspectMode by videoPlayerViewModel.aspectMode.collectAsStateWithLifecycle()
    val backgroundPlayback by videoPlayerViewModel.backgroundPlayback.collectAsStateWithLifecycle()
    val nightLight by videoPlayerViewModel.nightLight.collectAsStateWithLifecycle()
    val brightness by videoPlayerViewModel.brightness.collectAsStateWithLifecycle()
    val volume by videoPlayerViewModel.volume.collectAsStateWithLifecycle()
    val sleepTimerMinutes by videoPlayerViewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
    val audioTrackLabels by videoPlayerViewModel.audioTrackLabels.collectAsStateWithLifecycle()
    val selectedAudioTrack by videoPlayerViewModel.selectedAudioTrack.collectAsStateWithLifecycle()

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showPanelSpeedMenu by remember { mutableStateOf(false) }
    var showAudioTrackMenu by remember { mutableStateOf(false) }
    var controlsExpanded by rememberSaveable { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    // Rotation: null = follow the system, true = landscape, false = portrait.
    var forcedLandscape by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val decodedUri = remember(videoUri) {
        runCatching { URLDecoder.decode(videoUri, "UTF-8") }.getOrElse { videoUri }
    }

    LaunchedEffect(decodedUri, videoQueue) {
        videoPlayerViewModel.loadVideo(decodedUri)
        if (videoQueue.size > 1) {
            videoPlayerViewModel.playQueue(
                contentUriStrings = videoQueue,
                startIndex = videoQueue.indexOf(decodedUri).coerceAtLeast(0),
                startPositionMs = startPositionMs
            )
        } else {
            videoPlayerViewModel.play(decodedUri, startPositionMs)
        }
    }

    // Save the resume point every 15 s while this screen is open.
    LaunchedEffect(Unit) {
        while (currentCoroutineContext().isActive) {
            delay(15_000L)
            videoPlayerViewModel.saveResumePosition()
        }
    }

    // Background playback (like music): when OFF, pause the video as soon
    // as the app goes to the background; when ON, the audio keeps playing.
    DisposableEffect(lifecycleOwner, backgroundPlayback) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !backgroundPlayback) {
                videoPlayerViewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Night light: dim the screen while playing; restore on leave.
    LaunchedEffect(nightLight, brightness) {
        activity?.window?.let { window ->
            val attributes = window.attributes
            attributes.screenBrightness = if (nightLight) {
                brightness.coerceIn(10, 100) / 100f
            } else {
                -1f
            }
            window.attributes = attributes
        }
    }

    // Fullscreen = landscape + hide the top bar; the PlayerView fills the
    // screen. The Rotate button in the extra bar forces an orientation too.
    LaunchedEffect(isFullscreen, forcedLandscape) {
        activity?.requestedOrientation = when {
            isFullscreen || forcedLandscape == true ->
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            forcedLandscape == false -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayerViewModel.saveResumePosition()
            videoPlayerViewModel.pause()
            videoPlayerViewModel.setSleepTimer(0)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val attributes = window.attributes
                attributes.screenBrightness = -1f
                window.attributes = attributes
            }
        }
    }

    fun takeScreenshot() {
        val playerView = playerViewRef
        if (playerView == null) {
            Toast.makeText(
                context,
                context.getString(R.string.video_screenshot_failed),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val surfaceView = playerView.videoSurfaceView
        when (surfaceView) {
            is SurfaceView -> {
                val width = surfaceView.width.takeIf { it > 0 } ?: 1280
                val height = surfaceView.height.takeIf { it > 0 } ?: 720
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(
                    surfaceView,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            saveScreenshotBitmap(context, bitmap)
                        } else {
                            captureFrameWithRetriever(
                                context,
                                decodedUri,
                                videoPlayerViewModel.currentPositionMs()
                            )
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            }
            is TextureView -> {
                val bitmap = surfaceView.bitmap
                if (bitmap != null) {
                    saveScreenshotBitmap(context, bitmap)
                } else {
                    captureFrameWithRetriever(
                        context,
                        decodedUri,
                        videoPlayerViewModel.currentPositionMs()
                    )
                }
            }
            else -> captureFrameWithRetriever(
                context,
                decodedUri,
                videoPlayerViewModel.currentPositionMs()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    playerViewRef = this
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = when (aspectMode) {
                    "FILL" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    "ZOOM" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                    Text(
                        text = currentVideo?.displayTitle
                            ?: stringResource(R.string.video_player_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = stringResource(R.string.video_playback_speed)
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            videoPlayerViewModel.availableSpeeds.forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (speed == 1f) {
                                                "1×"
                                            } else {
                                                "${speed}×"
                                            }
                                        )
                                    },
                                    onClick = {
                                        videoPlayerViewModel.setSpeed(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { videoPlayerViewModel.toggleFullscreen() }) {
                        Icon(
                            imageVector = Icons.Rounded.Fullscreen,
                            contentDescription = stringResource(R.string.video_fullscreen)
                        )
                    }
                }
            }
        } else {
            IconButton(
                onClick = { videoPlayerViewModel.toggleFullscreen() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FullscreenExit,
                    contentDescription = stringResource(R.string.video_exit_fullscreen),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ---- Extra control bar (Folders + Player, 2026-11-16): hidden by
        // default; the animated arrow handle in front of it slides it
        // in/out. ----
        val arrowRotation by animateFloatAsState(
            targetValue = if (controlsExpanded) 180f else 0f,
            label = "videoControlsArrow"
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = controlsExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 28.dp,
                        topEnd = 28.dp
                    ),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Aspect / rotation / speed / screenshot.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = { videoPlayerViewModel.cycleAspectMode() },
                                label = {
                                    Text(
                                        text = stringResource(R.string.video_aspect) + ": " +
                                            when (aspectMode) {
                                                "FILL" -> stringResource(R.string.video_aspect_fill)
                                                "ZOOM" -> stringResource(R.string.video_aspect_zoom)
                                                else -> stringResource(R.string.video_aspect_fit)
                                            }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            FilterChip(
                                selected = forcedLandscape == true,
                                onClick = {
                                    forcedLandscape = if (forcedLandscape == true) false else true
                                },
                                label = { Text(text = stringResource(R.string.video_rotate)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ScreenRotation,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            Spacer(Modifier.weight(1f))
                            Box {
                                FilterChip(
                                    selected = playbackSpeed != 1f,
                                    onClick = { showPanelSpeedMenu = true },
                                    label = {
                                        Text(
                                            text = if (playbackSpeed == 1f) {
                                                "1×"
                                            } else {
                                                "${playbackSpeed}×"
                                            }
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Speed,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                                DropdownMenu(
                                    expanded = showPanelSpeedMenu,
                                    onDismissRequest = { showPanelSpeedMenu = false }
                                ) {
                                    videoPlayerViewModel.availableSpeeds.forEach { speed ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (speed == 1f) {
                                                        "1×"
                                                    } else {
                                                        "${speed}×"
                                                    }
                                                )
                                            },
                                            onClick = {
                                                videoPlayerViewModel.setSpeed(speed)
                                                showPanelSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { takeScreenshot() }) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = stringResource(R.string.video_screenshot),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Audio: volume + audio track.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = stringResource(R.string.video_volume),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Slider(
                                value = volume,
                                onValueChange = { videoPlayerViewModel.setVolume(it) },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box {
                                FilterChip(
                                    selected = audioTrackLabels.size > 1,
                                    onClick = { showAudioTrackMenu = true },
                                    enabled = audioTrackLabels.size > 1,
                                    label = {
                                        Text(
                                            text = if (audioTrackLabels.size > 1) {
                                                audioTrackLabels.getOrNull(selectedAudioTrack)
                                                    ?: stringResource(R.string.video_audio_track)
                                            } else {
                                                stringResource(R.string.video_audio_track_single)
                                            }
                                        )
                                    }
                                )
                                DropdownMenu(
                                    expanded = showAudioTrackMenu,
                                    onDismissRequest = { showAudioTrackMenu = false }
                                ) {
                                    audioTrackLabels.forEachIndexed { index, label ->
                                        DropdownMenuItem(
                                            text = { Text(text = label) },
                                            onClick = {
                                                videoPlayerViewModel.selectAudioTrack(index)
                                                showAudioTrackMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Sleep mode.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (sleepTimerMinutes > 0) {
                                    stringResource(
                                        R.string.video_sleep_active,
                                        sleepTimerMinutes
                                    )
                                } else {
                                    stringResource(R.string.video_sleep_mode)
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.weight(1f))
                            listOf(0, 15, 30, 60).forEach { minutes ->
                                FilterChip(
                                    selected = sleepTimerMinutes == minutes,
                                    onClick = { videoPlayerViewModel.setSleepTimer(minutes) },
                                    label = {
                                        Text(
                                            text = if (minutes == 0) {
                                                stringResource(R.string.video_sleep_off)
                                            } else {
                                                stringResource(
                                                    R.string.video_sleep_minutes,
                                                    minutes
                                                )
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        // Night light.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NightsStay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.video_night_light),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Slider(
                                value = brightness.toFloat(),
                                onValueChange = { videoPlayerViewModel.setBrightness(it.toInt()) },
                                valueRange = 10f..100f,
                                enabled = nightLight,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = nightLight,
                                onCheckedChange = { videoPlayerViewModel.setNightLight(it) }
                            )
                        }
                        // Background playback (like music).
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.video_background_playback),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.video_background_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = backgroundPlayback,
                                onCheckedChange = { videoPlayerViewModel.setBackgroundPlayback(it) }
                            )
                        }
                    }
                }
            }
            // The animated arrow handle in front of the bar.
            Surface(
                onClick = { controlsExpanded = !controlsExpanded },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                tonalElevation = 4.dp,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = stringResource(
                        if (controlsExpanded) {
                            R.string.video_player_hide_controls
                        } else {
                            R.string.video_player_more_controls
                        }
                    ),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .rotate(arrowRotation)
                )
            }
        }
    }
}

/** Save a captured frame under Pictures/Solina Player. */
private fun saveScreenshotBitmap(context: android.content.Context, bitmap: Bitmap) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "SolinaPlayer_$timestamp.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/Solina Player"
        )
    }
    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    )
    var saved = false
    if (uri != null) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            }
        }
        if (!saved) {
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }
    Toast.makeText(
        context,
        if (saved) {
            context.getString(R.string.video_screenshot_saved)
        } else {
            context.getString(R.string.video_screenshot_failed)
        },
        Toast.LENGTH_SHORT
    ).show()
}

/**
 * Fallback frame grab for surfaces PixelCopy cannot read: pull the frame
 * at the current position straight from the file with MediaMetadataRetriever.
 */
private fun captureFrameWithRetriever(
    context: android.content.Context,
    videoUri: String,
    positionMs: Long
) {
    val retriever = MediaMetadataRetriever()
    val bitmap = runCatching {
        retriever.setDataSource(context, Uri.parse(videoUri))
        retriever.getFrameAtTime(
            positionMs * 1000,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
        )
    }.getOrNull()
    runCatching { retriever.release() }
    if (bitmap != null) {
        saveScreenshotBitmap(context, bitmap)
    } else {
        Toast.makeText(
            context,
            context.getString(R.string.video_screenshot_failed),
            Toast.LENGTH_SHORT
        ).show()
    }
}
