package app.solina.player.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Native Compose re-implementations of the motion ideas from d3motion.pro
 * (reviewed 2026-11-16, original HTML files in ~/workspace/d3motion/).
 *
 * The d3motion components are standalone web files (HTML/CSS/canvas-JS);
 * Compose cannot run HTML and no WebView is used anywhere in the app, so
 * these are native ports of the *motion* only — no d3motion code, no
 * third-party branding. Every animation here is deliberately cheap:
 *
 * - EQ bars / gradient / rings are drawn in a single Canvas (GPU) with no
 *   layout recomposition per frame; the only animated value is one float
 *   from a single infiniteTransition, and it only runs while playing.
 * - Rolling digits recompose only the one character that changed, once
 *   per second at most.
 * - Reveal / pop / morph animations are one-shot (spring or tween) and
 *   settle to a static state; nothing animates in the background.
 * - Animations stop when their screen is gone (composition leaves) or
 *   playback pauses, so they cost nothing at rest.
 */

/**
 * The d3motion spring, verified from their source:
 * `1 - exp(-6.5p)·(cos(9.86p) + (6.5/9.86)·sin(9.86p))`, normalised.
 * That is an underdamped spring with damping ratio ≈ 0.55 and stiffness
 * ≈ 140 — the same feel as their gliding pill, checkmark pop and
 * slider bounce.
 */
fun d3Spring() = spring<Float>(dampingRatio = 0.55f, stiffness = 140f)

/**
 * Animated equalizer bars (d3motion music-player / suno / pill-chatgpt).
 * Their formula, layered sines over time:
 * `h = .35 + .65·|sin(t·(3.1+i·.7) + i·1.7)·cos(t·(1.3+i·.4))|`
 * with t in seconds. Four bars, one infiniteTransition driving a single
 * time float; bars are drawn in Canvas so no layout work happens per
 * frame. When [isPlaying] is false the transition is not created at all
 * and the bars sit still at a low flat height.
 */
@Composable
fun EqBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 4
) {
    val time = if (isPlaying) {
        val transition = rememberInfiniteTransition(label = "eqBars")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 100_000, easing = LinearEasing)
            ),
            label = "eqTime"
        ).value
    } else {
        0f
    }
    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2f - 1f)
        val gap = barWidth
        for (i in 0 until barCount) {
            val frac = if (isPlaying) {
                val t = time
                0.35f + 0.65f * abs(
                    sin(t * (3.1f + i * 0.7f) + i * 1.7f) *
                        cos(t * (1.3f + i * 0.4f))
                )
            } else {
                0.22f
            }
            val barHeight = size.height * (0.18f + 0.82f * frac)
            val x = i * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Rolling digits (d3motion otp-verification / countdown-timer / gemini %):
 * every character rolls vertically when it changes, with a small
 * stagger between neighbours. Only the changed characters recompose.
 *
 * R15 F1: NO LONGER used for player times. Fed by a 250 ms position
 * poll, the per-character AnimatedContent drew the old and new digit
 * simultaneously in the same slot and garbled the time ("65:00:20 /
 * 55:91"); its per-character Row also has no LTR pinning, so RTL
 * could reorder the digits. Player times are plain Text now. Kept
 * for non-polled rolling counters only.
 */
@Composable
fun RollingTimeText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, ch ->
            AnimatedContent(
                targetState = ch,
                transitionSpec = {
                    (slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up
                    ) + fadeIn()) togetherWith
                        (slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Up
                        ) + fadeOut())
                },
                label = "rollDigit$index"
            ) { c ->
                Text(
                    text = c.toString(),
                    color = color,
                    fontSize = fontSize,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Gradient progress bar (d3motion file-upload): a rounded pill track with
 * a horizontal gradient fill. Drawn in Canvas; the fill width follows
 * [progress] directly, so there is no extra animation cost.
 */
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    startColor: Color,
    endColor: Color,
    trackColor: Color
) {
    Canvas(modifier = modifier) {
        val radius = size.height / 2f
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(radius, radius)
        )
        val fillWidth = size.width * progress.coerceIn(0f, 1f)
        if (fillWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(startColor, endColor)
                ),
                topLeft = Offset.Zero,
                size = Size(fillWidth, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

/**
 * Staggered tile reveal (d3motion gemini-image 6×6 tile reveal): a list
 * item fades in and scales up from 0.94 with the d3motion spring, delayed
 * by [index] × 35 ms (capped at 10 steps). [revealed] is a screen-level
 * remembered set of item keys, so an item animates exactly once — never
 * again when it scrolls back into view.
 */
@Composable
fun D3Reveal(
    key: Any,
    index: Int,
    revealed: MutableSet<Any>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shouldAnimate = !revealed.contains(key)
    var visible by remember(key) { mutableStateOf(!shouldAnimate) }
    LaunchedEffect(key) {
        if (shouldAnimate) {
            visible = true
            revealed.add(key)
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = (index.coerceAtMost(10) * 35)
        ),
        label = "revealAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "revealScale"
    )
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
        }
    ) {
        content()
    }
}

/** Remembered set of revealed item keys for one list screen. */
@Composable
fun rememberRevealedKeys(): MutableSet<Any> = remember { mutableStateSetOf() }

/** Small EQ bars sized for the mini-player bars (music + video).
 * R15 F3: 16×18 dp (was 18×20 dp) — the bigger bars crowded the
 * title/artist text on long titles. */
@Composable
fun MiniEqBars(
    isPlaying: Boolean,
    color: Color
) {
    EqBars(
        isPlaying = isPlaying,
        color = color,
        modifier = Modifier.size(width = 16.dp, height = 18.dp)
    )
}
