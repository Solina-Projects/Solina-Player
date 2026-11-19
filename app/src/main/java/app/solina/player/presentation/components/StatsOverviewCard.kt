package app.solina.player.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solina.player.R
import app.solina.player.data.stats.PlaybackStatsRepository
import app.solina.player.data.stats.StatsTimeRange
import app.solina.player.presentation.stats.displayNameRes
import app.solina.player.presentation.viewmodel.HomeStatsOverviewState
import app.solina.player.utils.formatListeningDurationCompact
import app.solina.player.utils.formatListeningDurationLong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private enum class OverviewContentKey { Loading, Empty, Ready }

/**
 * Home entry point to the Stats screen. Highlights peak listening day,
 * softens zero-activity days, and optionally shows a compact Video Library summary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsOverviewCard(
    state: HomeStatsOverviewState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    videoCount: Int = 0,
    inProgressVideosCount: Int = 0,
    showVideoSummary: Boolean = false
) {
    val summary = (state as? HomeStatsOverviewState.Ready)?.summary
    val contentKey = when (state) {
        HomeStatsOverviewState.Loading -> OverviewContentKey.Loading
        HomeStatsOverviewState.Empty -> OverviewContentKey.Empty
        is HomeStatsOverviewState.Ready -> OverviewContentKey.Ready
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = AbsoluteSmoothCornerShape(
        cornerRadiusTL = 28.dp,
        smoothnessAsPercentTR = 60,
        cornerRadiusBR = 28.dp,
        smoothnessAsPercentTL = 60,
        cornerRadiusBL = 28.dp,
        smoothnessAsPercentBR = 60,
        cornerRadiusTR = 28.dp,
        smoothnessAsPercentBL = 60,
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.surfaceContainer),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        Modifier.padding(start = 24.dp, top = 22.dp, bottom = 22.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.presentation_batch_g_stats_overview_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource((summary?.range ?: StatsTimeRange.WEEK).displayNameRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Crossfade(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    targetState = contentKey,
                    label = "stats_overview_content"
                ) { key ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (key) {
                            OverviewContentKey.Loading -> PlaceholderOverviewContent()
                            OverviewContentKey.Empty -> EmptyOverviewContent()
                            OverviewContentKey.Ready -> {
                                if (summary != null) {
                                    OverviewContent(summary)
                                } else {
                                    PlaceholderOverviewContent()
                                }
                            }
                        }
                        if (showVideoSummary && videoCount > 0) {
                            VideoStatsFooterChip(
                                videoCount = videoCount,
                                inProgressVideosCount = inProgressVideosCount
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoStatsFooterChip(
    videoCount: Int,
    inProgressVideosCount: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = if (inProgressVideosCount > 0) {
                    stringResource(
                        R.string.home_stats_video_summary_with_progress,
                        videoCount,
                        inProgressVideosCount
                    )
                } else {
                    stringResource(R.string.home_stats_video_summary_plain, videoCount)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverviewContent(summary: PlaybackStatsRepository.PlaybackStatsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = formatListeningDurationLong(summary.totalDurationMs),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.presentation_batch_g_stats_overview_total_plays),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = summary.totalPlayCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.presentation_batch_g_stats_overview_avg_per_day),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatListeningDurationCompact(summary.averageDailyDurationMs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        val topTrack = summary.topSongs.firstOrNull()
        if (topTrack != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.presentation_batch_g_stats_overview_top_track),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = topTrack.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.presentation_batch_g_stats_overview_top_track_line,
                        topTrack.artist,
                        topTrack.playCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        MiniListeningTimeline(summary)
    }
}

@Composable
private fun EmptyOverviewContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.presentation_batch_g_stats_empty_no_timeline_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.presentation_batch_g_stats_empty_no_timeline_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(modifier = Modifier.alpha(0.5f)) {
            MiniListeningTimeline(null)
        }
    }
}

@Composable
private fun PlaceholderOverviewContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlaceholderLine(width = 140.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            PlaceholderLine(width = 60.dp)
            PlaceholderLine(width = 60.dp)
        }
        PlaceholderLine(width = 120.dp)
        MiniListeningTimeline(null)
    }
}

@Composable
private fun MiniListeningTimeline(summary: PlaybackStatsRepository.PlaybackStatsSummary?) {
    val timeline = remember(summary) { (summary?.timeline ?: emptyList()).toImmutableList() }
    if (summary?.range == StatsTimeRange.MONTH && timeline.isNotEmpty()) {
        MonthlyHorizontalListeningTimeline(timeline)
        return
    }
    val maxDuration = timeline.maxOfOrNull { it.totalDurationMs }?.takeIf { it > 0 } ?: 1L
    val hasAnyActivity = timeline.any { it.totalDurationMs > 0L }

    // 128dp fits peak label (16dp) + 70dp bar + 8dp gap + up to two lines of localized day label.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val entries = if (timeline.isEmpty()) {
            List(5) { null }
        } else {
            timeline.takeLast(minOf(7, timeline.size))
        }
        entries.forEach { entry ->
            val durationMs = entry?.totalDurationMs ?: 0L
            val isPeak = hasAnyActivity && durationMs == maxDuration
            val isEmptyDay = durationMs <= 0L
            val heightFraction = entry?.let { it.totalDurationMs.toFloat() / maxDuration.toFloat() }
                ?.coerceIn(0f, 1f) ?: 0.1f
            val barColor = when {
                isPeak -> MaterialTheme.colorScheme.primary
                !isEmptyDay -> MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isPeak) {
                    Text(
                        text = formatListeningDurationCompact(durationMs),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (isEmptyDay) 8.dp
                            else (70.dp * heightFraction).coerceAtLeast(14.dp)
                        )
                        .clip(CircleShape)
                        .background(color = barColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry?.label ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isPeak) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MonthlyHorizontalListeningTimeline(
    timeline: ImmutableList<PlaybackStatsRepository.TimelineEntry>
) {
    val maxDuration = timeline.maxOfOrNull { it.totalDurationMs }?.takeIf { it > 0 } ?: 1L
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        timeline.forEach { entry ->
            val widthFraction = (entry.totalDurationMs.toFloat() / maxDuration.toFloat())
                .coerceIn(0f, 1f)
                .takeIf { it > 0f }
                ?: 0.06f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.widthIn(min = 56.dp, max = 88.dp),
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderLine(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(18.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    )
}
