@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package app.solina.player.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularWavyProgressIndicator
import app.solina.player.presentation.components.player.MorphingPlayPauseIcon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.size.Size
import app.solina.player.R
import app.solina.player.data.model.Song
import app.solina.player.ui.theme.RoundedSans

internal val LocalMaterialTheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

val MiniPlayerHeight = 64.dp
const val ANIMATION_DURATION_MS = 255
val MiniPlayerBottomSpacer = 8.dp

@Composable
fun getNavigationBarHeight(): Dp {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    return sanitizeNavigationBarBottomInset(insets.calculateBottomPadding())
}

@Composable
internal fun MiniPlayerContentInternal(
    song: Song,
    isPlaying: Boolean,
    isOutputConnecting: Boolean,
    isPreparingPlayback: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    canScroll: Boolean = true,
    // Appearance customisation (bug-fix round 2026-11-17, item 6):
    // the Home music bar can be resized independently; the floating
    // mini player always uses the fixed MiniPlayerHeight.
    contentHeight: Dp = MiniPlayerHeight,
    // R15 F7: true while the bar is a transparent glass pill over the
    // blurred list — title/artist then go white with a soft shadow
    // instead of the dark onPrimaryContainer colours.
    glassOn: Boolean = false
) {
    val hapticFeedback = LocalHapticFeedback.current
    val controlsEnabled = !isOutputConnecting && !isPreparingPlayback

    val previousInteraction = remember { MutableInteractionSource() }
    val playPauseInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val miniPlayerIndication = remember { ripple(bounded = false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(contentHeight)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val albumArtModel = song.albumArtUriString?.takeIf { it.isNotBlank() }
        Box(contentAlignment = Alignment.Center) {
            key(song.id) {
                SmartImage(
                    model = albumArtModel,
                    contentDescription = stringResource(R.string.cd_album_art_for_title, song.title),
                    shape = CircleShape,
                    targetSize = Size(150, 150),
                    modifier = Modifier.size(44.dp)
                )
            }
            if (isOutputConnecting) {
                LoadingIndicator(
                    modifier = Modifier.size(24.dp),
                    color = LocalMaterialTheme.current.onPrimaryContainer
                )
            } else if (isPreparingPlayback) {
                CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            val titleStyle = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                fontFamily = RoundedSans,
                color = if (glassOn) Color.White
                else LocalMaterialTheme.current.onPrimaryContainer,
                shadow = if (glassOn) Shadow(
                    color = Color.Black.copy(alpha = 0.45f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                ) else null
            )
            val artistStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                letterSpacing = 0.sp,
                fontFamily = RoundedSans,
                color = if (glassOn) Color.White.copy(alpha = 0.85f)
                else LocalMaterialTheme.current.onPrimaryContainer.copy(alpha = 0.7f),
                shadow = if (glassOn) Shadow(
                    color = Color.Black.copy(alpha = 0.45f),
                    offset = Offset(0f, 1f),
                    blurRadius = 4f
                ) else null
            )

            AutoScrollingText(
                text = when {
                    isOutputConnecting -> stringResource(R.string.mini_player_connecting_device)
                    isPreparingPlayback -> stringResource(R.string.mini_player_preparing_playback)
                    else -> song.title
                },
                style = titleStyle,
                // R15 F3: the marquee fade is drawn with BlendMode.DstIn,
                // which uses only the colour's alpha — so the solid
                // primaryContainer edge already fades correctly over a
                // transparent glass pill (no hue can leak through).
                gradientEdgeColor = LocalMaterialTheme.current.primaryContainer,
                canScroll = canScroll
            )
            AutoScrollingText(
                text = if (isPreparingPlayback) {
                    stringResource(R.string.mini_player_loading_audio)
                } else {
                    song.displayArtist
                },
                style = artistStyle,
                gradientEdgeColor = LocalMaterialTheme.current.primaryContainer,
                canScroll = canScroll
            )
        }
        // R15 F3: 12 dp gaps (was 8 dp) so the EQ bars no longer look
        // glued to the text on long titles.
        Spacer(modifier = Modifier.width(12.dp))

        // Live EQ bars (d3motion music-player / suno): four bars driven
        // by one infiniteTransition, only while actually playing.
        MiniEqBars(
            isPlaying = isPlaying && controlsEnabled,
            color = LocalMaterialTheme.current.primary
        )
        Spacer(modifier = Modifier.width(12.dp))

        // Media transport controls stay left-to-right in RTL locales.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LocalMaterialTheme.current.onPrimary)
                        .clickable(
                            interactionSource = previousInteraction,
                            indication = miniPlayerIndication,
                            enabled = controlsEnabled
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPrevious()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.previous_track),
                        tint = LocalMaterialTheme.current.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                val playPauseCorner by animateDpAsState(
                    targetValue = if (isPlaying) 10.dp else 18.dp,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    label = "MiniPlayerPlayPauseCorner"
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(playPauseCorner))
                        .background(LocalMaterialTheme.current.primary)
                        .clickable(
                            interactionSource = playPauseInteraction,
                            indication = miniPlayerIndication,
                            enabled = controlsEnabled
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MorphingPlayPauseIcon(
                        isPlaying = isPlaying,
                        tint = LocalMaterialTheme.current.onPrimary,
                        size = 20.dp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(LocalMaterialTheme.current.onPrimary)
                        .clickable(
                            interactionSource = nextInteraction,
                            indication = miniPlayerIndication,
                            enabled = controlsEnabled
                        ) { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next_track),
                        tint = LocalMaterialTheme.current.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
