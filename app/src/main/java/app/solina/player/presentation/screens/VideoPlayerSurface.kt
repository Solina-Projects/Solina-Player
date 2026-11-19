package app.solina.player.presentation.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import app.solina.player.data.model.VideoSubtitleStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

enum class VideoGestureMode {
    Undecided, Seek, Brightness, Volume, Speed, Pinch, TwoFingerUndecided, TwoFingerSpeed
}

data class GestureHint(val label: String, val percent: Int)

/**
 * Isolated hardware video surface + multi-touch gesture layer.
 * Keeps `AndroidView(PlayerView)` and `CaptionStyleCompat` completely isolated from
 * 250ms position ticks so video frames never suffer redundant Compose updates.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerSurface(
    player: ExoPlayer?,
    playerViewRef: PlayerView?,
    onPlayerViewCreated: (PlayerView) -> Unit,
    aspectMode: String,
    zoomScale: Float,
    isMirrored: Boolean,
    subtitleStyle: VideoSubtitleStyle,
    controlsLocked: Boolean,
    inPip: Boolean,
    swipeGesturesEnabled: Boolean,
    playbackSpeed: Float,
    availableSpeeds: List<Float>,
    lastHoldSpeed: Float?,
    onLastHoldSpeedChanged: (Float) -> Unit,
    gestureBrightness: Float?,
    nightLight: Boolean,
    nightLightBrightnessPercent: Int,
    speedStepPx: Float,
    brightnessLabel: String,
    volumeLabel: String,
    positionMsProvider: () -> Long,
    durationMsProvider: () -> Long,
    onTouchActiveChange: (Boolean) -> Unit,
    onScrubbingChange: (Boolean) -> Unit,
    onSeekPreview: (targetMs: Long, deltaText: String?) -> Unit,
    onCommitSeek: (targetMs: Long) -> Unit,
    onSeekBy: (deltaMs: Long) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onGestureHint: (GestureHint?) -> Unit,
    onFeedback: (String) -> Unit,
    onZoomScaleChange: (Float) -> Unit,
    onSpeedBarState: (speedMode: Boolean, speedBarVisible: Boolean) -> Unit,
    holdSpeedPinned: Boolean,
    onHoldPinnedChange: (Boolean) -> Unit,
    onApplySpeedLive: (Float) -> Unit,
    onCommitSpeed: (Float) -> Unit,
    onSingleTapCandidate: () -> Unit,
    onShowBarsAndPoke: () -> Unit,
    onSpeedBarShown: () -> Unit,
    onCancelPendingSingleTap: () -> Unit,
    onLockedTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val latestControlsLocked by rememberUpdatedState(controlsLocked)
    val latestInPip by rememberUpdatedState(inPip)
    val latestSwipeEnabled by rememberUpdatedState(swipeGesturesEnabled)
    val latestPlaybackSpeed by rememberUpdatedState(playbackSpeed)
    val latestLastHoldSpeed by rememberUpdatedState(lastHoldSpeed)
    val latestGestureBrightness by rememberUpdatedState(gestureBrightness)
    val latestNightLight by rememberUpdatedState(nightLight)
    val latestNightLightPercent by rememberUpdatedState(nightLightBrightnessPercent)
    val latestZoomScale by rememberUpdatedState(zoomScale)
    val latestHoldSpeedPinned by rememberUpdatedState(holdSpeedPinned)

    // Subtitle style updates only when subtitleStyle actually changes.
    LaunchedEffect(playerViewRef, subtitleStyle) {
        val pv = playerViewRef ?: return@LaunchedEffect
        pv.subtitleView?.let { subtitleView ->
            subtitleView.setApplyEmbeddedStyles(false)
            subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleStyle.textSizeSp)
            val edge = when (subtitleStyle.edgeType) {
                0 -> CaptionStyleCompat.EDGE_TYPE_NONE
                2 -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                else -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
            }
            subtitleView.setStyle(
                CaptionStyleCompat(
                    subtitleStyle.textColorArgb,
                    when (subtitleStyle.backgroundMode) {
                        0 -> Color.Transparent.toArgb()
                        2 -> Color.Black.toArgb()
                        else -> Color.Black.copy(alpha = 0.55f).toArgb()
                    },
                    Color.Transparent.toArgb(),
                    edge,
                    Color.Black.toArgb(),
                    null
                )
            )
        }
    }

    // Scale & mirror ONLY the videoSurfaceView so subtitles remain sharp, unflipped, and unclipped!
    LaunchedEffect(playerViewRef, zoomScale, aspectMode, isMirrored) {
        val pv = playerViewRef ?: return@LaunchedEffect
        val surface = pv.videoSurfaceView ?: return@LaunchedEffect
        val aspectExtra = if (aspectMode == "ZOOM") 1.35f else 1f
        val totalScale = (zoomScale * aspectExtra).coerceIn(0.4f, 4.0f)
        surface.scaleX = if (isMirrored) -totalScale else totalScale
        surface.scaleY = totalScale
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    onPlayerViewCreated(this)
                }
            },
            update = { pv ->
                if (pv.player !== player) {
                    pv.player = player
                }
                pv.resizeMode = when (aspectMode) {
                    "FILL", "ZOOM" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    "STRETCH" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            onRelease = { pv ->
                pv.player = null
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val slopPx = viewConfiguration.touchSlop
                    var lastTapTimeMs = 0L

                    kotlinx.coroutines.coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (latestInPip) return@awaitEachGesture

                            if (latestControlsLocked) {
                                // Consume up cleanly and reveal temporary unlock affordance
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                                onLockedTap()
                                return@awaitEachGesture
                            }

                            onTouchActiveChange(true)
                            try {
                                var mode = VideoGestureMode.Undecided
                                val seekStartMs = positionMsProvider()
                                val startBright = latestGestureBrightness
                                    ?: activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f }
                                    ?: if (latestNightLight) (latestNightLightPercent / 100f) else readSystemBrightnessFraction(context)
                                val maxVol = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
                                val startVol = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVol / 2))
                                    .toFloat() / maxVol.toFloat()

                                var seekPreviewMs = -1L
                                var lastHint: GestureHint? = null
                                var draggedWithGesturesOff = false
                                var longPressFired = false
                                var speedSlideChanged = false
                                                                var preHoldSpeed = latestPlaybackSpeed
                                var speedAnchorX = down.position.x
                                val holdBaseline = latestLastHoldSpeed ?: 2f
                                var speedIndex = availableSpeeds.indices.minByOrNull {
                                    abs(availableSpeeds[it] - holdBaseline)
                                } ?: 6

                                var pinchLastSpan = 0f
                                var twoFingerStartSpan = 0f
                                var twoFingerStartMidY = 0f
                                var twoFingerStartSpeed = latestPlaybackSpeed
                                var twoFingerSpeed = twoFingerStartSpeed

                                val longPressJob = launch {
                                    delay(400)
                                    if (mode == VideoGestureMode.Undecided && !draggedWithGesturesOff) {
                                        longPressFired = true
                                        mode = VideoGestureMode.Speed
                                        preHoldSpeed = latestPlaybackSpeed
                                                                                onHoldPinnedChange(false)
                                        val entrySpeed = availableSpeeds[speedIndex]
                                        onApplySpeedLive(entrySpeed)
                                        onSpeedBarState(true, true)
                                        // Round 20: the hold speed bar is solo — hide the
                                        // other bars instead of showing them.
                                        onSpeedBarShown()
                                    }
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressed = event.changes.filter { it.pressed }
                                    if (pressed.isEmpty()) break

                                    if (pressed.size >= 2) {
                                        longPressJob.cancel()
                                        val p1 = pressed[0].position
                                        val p2 = pressed[1].position
                                        val span = hypot(p1.x - p2.x, p1.y - p2.y)
                                        val midY = (p1.y + p2.y) / 2f

                                        when (mode) {
                                            VideoGestureMode.Pinch -> {
                                                if (pinchLastSpan > 0f && span > 0f) {
                                                    val nextZoom = (latestZoomScale * (span / pinchLastSpan))
                                                        .coerceIn(0.4f, 3.0f)
                                                    onZoomScaleChange(nextZoom)
                                                    onFeedback("${(nextZoom * 100).roundToInt()}%")
                                                }
                                                pinchLastSpan = span
                                            }
                                            VideoGestureMode.TwoFingerSpeed -> {
                                                val dy = midY - twoFingerStartMidY
                                                val raw = (twoFingerStartSpeed * 2f.pow(-dy / size.height))
                                                    .coerceIn(0.25f, 4f)
                                                val snapped = ((raw * 20f).roundToInt() / 20f).coerceIn(0.25f, 4f)
                                                if (snapped != twoFingerSpeed) {
                                                    twoFingerSpeed = snapped
                                                    onApplySpeedLive(snapped)
                                                }
                                                onFeedback(formatSpeedText(twoFingerSpeed))
                                            }
                                            VideoGestureMode.TwoFingerUndecided -> {
                                                if (abs(span - twoFingerStartSpan) > slopPx) {
                                                    mode = VideoGestureMode.Pinch
                                                    pinchLastSpan = span
                                                } else if (abs(midY - twoFingerStartMidY) > slopPx) {
                                                    mode = VideoGestureMode.TwoFingerSpeed
                                                    twoFingerSpeed = twoFingerStartSpeed
                                                    onFeedback(formatSpeedText(twoFingerSpeed))
                                                }
                                            }
                                            else -> {
                                                if (mode == VideoGestureMode.Speed) {
                                                    onSpeedBarState(false, false)
                                                }
                                                if (mode == VideoGestureMode.Seek) {
                                                    onScrubbingChange(false)
                                                    onSeekPreview(-1L, null)
                                                }
                                                if (mode == VideoGestureMode.Brightness || mode == VideoGestureMode.Volume) {
                                                    onGestureHint(null)
                                                }
                                                mode = VideoGestureMode.TwoFingerUndecided
                                                twoFingerStartSpan = span
                                                twoFingerStartMidY = midY
                                                twoFingerStartSpeed = latestPlaybackSpeed
                                                twoFingerSpeed = twoFingerStartSpeed
                                                pinchLastSpan = 0f
                                            }
                                        }
                                        pressed.forEach { it.consume() }
                                        continue
                                    }

                                    val change = pressed.firstOrNull { it.id == down.id } ?: pressed.first()
                                    val pos = change.position

                                    when (mode) {
                                        VideoGestureMode.Undecided -> {
                                            val dx = pos.x - down.position.x
                                            val dy = pos.y - down.position.y
                                            if (abs(dx) > slopPx || abs(dy) > slopPx) {
                                                longPressJob.cancel()
                                                if (!latestSwipeEnabled) {
                                                    draggedWithGesturesOff = true
                                                    change.consume()
                                                    continue
                                                }
                                                mode = when {
                                                    abs(dx) > abs(dy) -> VideoGestureMode.Seek
                                                    down.position.x < size.width / 2f -> VideoGestureMode.Brightness
                                                    else -> VideoGestureMode.Volume
                                                }
                                                // Round 20: only a horizontal seek shows the bars.
                                                // Brightness/volume swipes stay clean — just the
                                                // edge slider + % hint, no bars, no timer poke.
                                                if (mode == VideoGestureMode.Seek) onShowBarsAndPoke()
                                                if (mode == VideoGestureMode.Seek) {
                                                    val dur = durationMsProvider()
                                                    if (dur > 0L) {
                                                        onScrubbingChange(true)
                                                        val maxRange = (dur * 0.12f).toLong().coerceIn(30_000L, 180_000L)
                                                        val accum = ((dx / size.width) * maxRange).toLong()
                                                        val target = (seekStartMs + accum).coerceIn(0L, dur)
                                                        seekPreviewMs = target
                                                        onSeekPreview(target, formatSeekDeltaShort(accum))
                                                    }
                                                }
                                            }
                                        }
                                        VideoGestureMode.Seek -> {
                                            val dur = durationMsProvider()
                                            if (dur > 0L) {
                                                val dx = pos.x - down.position.x
                                                val maxRange = (dur * 0.12f).toLong().coerceIn(30_000L, 180_000L)
                                                val accum = ((dx / size.width) * maxRange).toLong()
                                                val target = (seekStartMs + accum).coerceIn(0L, dur)
                                                seekPreviewMs = target
                                                onSeekPreview(target, formatSeekDeltaShort(accum))
                                            }
                                        }
                                        VideoGestureMode.Brightness -> {
                                            val dy = pos.y - down.position.y
                                            val frac = (-dy / size.height).coerceIn(-1f, 1f)
                                            val newVal = (startBright + frac).coerceIn(0.05f, 1f)
                                            onBrightnessChange(newVal)
                                            val hint = GestureHint(brightnessLabel, (newVal * 100).roundToInt())
                                            lastHint = hint
                                            onGestureHint(hint)
                                        }
                                        VideoGestureMode.Volume -> {
                                            val dy = pos.y - down.position.y
                                            val frac = (-dy / size.height).coerceIn(-1f, 1f)
                                            val newVol = (startVol + frac).coerceIn(0f, 1f)
                                            val step = (newVol * maxVol).roundToInt().coerceIn(0, maxVol)
                                            runCatching {
                                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0)
                                            }
                                            val hint = GestureHint(volumeLabel, (newVol * 100).roundToInt())
                                            lastHint = hint
                                            onGestureHint(hint)
                                        }
                                        VideoGestureMode.Speed -> {
                                            val dx = pos.x - speedAnchorX
                                            if (abs(dx) >= speedStepPx) {
                                                val steps = (dx / speedStepPx).toInt()
                                                speedIndex = (speedIndex + steps).coerceIn(0, availableSpeeds.lastIndex)
                                                speedAnchorX = pos.x
                                                speedSlideChanged = true
                                                onApplySpeedLive(availableSpeeds[speedIndex])
                                            }
                                        }
                                        else -> Unit
                                    }
                                    change.consume()
                                }

                                longPressJob.cancel()

                                when (mode) {
                                    VideoGestureMode.Undecided -> {
                                        if (!longPressFired && !draggedWithGesturesOff) {
                                            val now = System.currentTimeMillis()
                                            if (now - lastTapTimeMs < 240L) {
                                                onCancelPendingSingleTap()
                                                lastTapTimeMs = 0L
                                                val tapX = down.position.x
                                                val dur = durationMsProvider()
                                                val cur = positionMsProvider()
                                                when {
                                                    tapX < size.width / 3f -> {
                                                        onSeekBy(-10_000L)
                                                        val target = (cur - 10_000L).coerceIn(0L, dur.coerceAtLeast(0L))
                                                        onFeedback("« 10s • ${formatVideoTimeShort(target)}")
                                                    }
                                                    tapX > size.width * 2f / 3f -> {
                                                        onSeekBy(10_000L)
                                                        val target = (cur + 10_000L).coerceIn(0L, dur.coerceAtLeast(0L))
                                                        onFeedback("10s » • ${formatVideoTimeShort(target)}")
                                                    }
                                                    else -> {
                                                        onZoomScaleChange(1f)
                                                        onFeedback("100%")
                                                    }
                                                }
                                            } else {
                                                lastTapTimeMs = now
                                                onSingleTapCandidate()
                                            }
                                        }
                                    }
                                    VideoGestureMode.Seek -> {
                                        if (seekPreviewMs >= 0L) {
                                            onCommitSeek(seekPreviewMs)
                                        }
                                        onSeekPreview(-1L, null)
                                        onScrubbingChange(false)
                                    }
                                    VideoGestureMode.Brightness, VideoGestureMode.Volume -> {
                                        onGestureHint(null)
                                        lastHint?.let { onFeedback("${it.label} ${it.percent}%") }
                                    }
                                    VideoGestureMode.Speed -> {
                                        onSpeedBarState(false, false)
                                        if (speedSlideChanged) {
                                            onLastHoldSpeedChanged(latestPlaybackSpeed)
                                        }
                                        if (!latestHoldSpeedPinned) {
                                            onCommitSpeed(preHoldSpeed)
                                        }
                                    }
                                    VideoGestureMode.TwoFingerSpeed -> {
                                        onCommitSpeed(twoFingerSpeed)
                                    }
                                    else -> Unit
                                }
                            } finally {
                                onTouchActiveChange(false)
                            }
                        }
                    }
                }
        )
    }
}

private fun readSystemBrightnessFraction(context: Context): Float {
    val raw = runCatching {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }.getOrDefault(128)
    return (raw / 255f).coerceIn(0.05f, 1f)
}

private fun formatSeekDeltaShort(deltaMs: Long): String {
    val seconds = abs(deltaMs) / 1000L
    return if (deltaMs >= 0L) "+${seconds}s" else "-${seconds}s"
}

private fun formatSpeedText(speed: Float): String {
    val text = if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()
    return "${text}×"
}
