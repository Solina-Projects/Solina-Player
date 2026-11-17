package app.solina.player.presentation.components.subcomps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solina.player.R
import app.solina.player.data.preferences.SeekBarStyleNames
import app.solina.player.presentation.components.WavySliderExpressive
import app.solina.player.presentation.viewmodel.AppearanceViewModel
import app.solina.player.utils.formatDuration
import kotlin.math.roundToLong

@Composable
fun PlayerSeekBar(
    backgroundColor: Color,
    onBackgroundColor: Color,
    primaryColor: Color,
    currentPosition: Long,
    totalDuration: Long,
    onSeek: (Long) -> Unit,
    onSeekPreview: ((Long?) -> Unit)? = null,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    // Round 15, wave 2, C4: seek-bar style (wavy / classic).
    appearanceViewModel: AppearanceViewModel = hiltViewModel()
) {
    val appearance by appearanceViewModel.appearance.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    val progressFraction = remember(currentPosition, totalDuration) {
        if (totalDuration > 0) {
            (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    var isUserSeeking by remember { mutableStateOf(false) }
    var lastSeekFinishedTime by remember { mutableStateOf(0L) }
    var targetSeekFraction by remember { mutableFloatStateOf(-1f) }
    var seekFraction by remember { mutableFloatStateOf(progressFraction) }
    val lastHapticStep = remember { intArrayOf(-1) }

    LaunchedEffect(progressFraction, isUserSeeking) {
        if (!isUserSeeking) {
            val now = System.currentTimeMillis()
            val timeSinceSeek = now - lastSeekFinishedTime
            val diffFraction = kotlin.math.abs(progressFraction - targetSeekFraction)
            if (targetSeekFraction < 0f || timeSinceSeek > 5000L || diffFraction < 0.04f) {
                seekFraction = progressFraction
                targetSeekFraction = -1f
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Round 15, wave 2, C4: seek-bar style — wavy or classic.
        // Same seek behaviour (preview while dragging, commit on
        // release, haptic steps); only the slider changes.
        val onSeekFractionChange: (Float) -> Unit = { newFraction ->
            isUserSeeking = true
            seekFraction = newFraction
            onSeekPreview?.invoke((newFraction * totalDuration).roundToLong())
            val quantized = (newFraction.coerceIn(0f, 1f) * 20f).toInt()
            if (quantized != lastHapticStep[0]) {
                lastHapticStep[0] = quantized
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        val onSeekFractionCommit: (Float) -> Unit = { finalFraction ->
            seekFraction = finalFraction
            onSeek((finalFraction * totalDuration).roundToLong())
            onSeekPreview?.invoke(null)
            targetSeekFraction = finalFraction
            lastSeekFinishedTime = System.currentTimeMillis()
            isUserSeeking = false
        }
        if (appearance.seekBarStyle == SeekBarStyleNames.CLASSIC) {
            Slider(
                value = seekFraction,
                onValueChange = onSeekFractionChange,
                onValueChangeFinished = { onSeekFractionCommit(seekFraction) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = primaryColor.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
            )
        } else {
            WavySliderExpressive(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                value = { seekFraction },
                onValueChange = onSeekFractionChange,
                onValueCommit = onSeekFractionCommit,
                strokeWidth = 5.dp,
                thumbRadius = 8.dp,
                activeTrackColor = primaryColor,
                inactiveTrackColor = primaryColor.copy(alpha = 0.2f),
                thumbColor = primaryColor,
                wavelength = 30.dp,
                isPlaying = isPlaying,
                semanticsLabel = stringResource(R.string.cd_playback_position)
            )
        }
    }
}
