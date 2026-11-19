package app.solina.player.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.preferences.HomeSectionId
import app.solina.player.presentation.components.MiniPlayerHeight
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.HomeCustomizationViewModel

/**
 * Full Home customisation hub: configure header/search behaviour, smart filtering,
 * card density & item counts, quick appearance jump, and section visibility/ordering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCustomizationScreen(
    navController: NavController,
    viewModel: HomeCustomizationViewModel = hiltViewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val layoutPrefs by viewModel.layoutPrefs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home_customize_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = stringResource(R.string.home_customize_reset))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = MiniPlayerHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                    24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "home_customize_appearance_link") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable {
                            navController.navigateSafely(Screen.AppearanceCustomization.route)
                        },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_customize_appearance_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.home_customize_appearance_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item(key = "home_customize_header_group") {
                SectionGroupHeading(text = stringResource(R.string.home_customize_group_header))
                Spacer(Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HomeSwitchRow(
                            title = stringResource(R.string.home_pref_greeting_title),
                            subtitle = stringResource(R.string.home_pref_greeting_subtitle),
                            checked = layoutPrefs.showGreetingHeader,
                            onCheckedChange = viewModel::setShowGreetingHeader
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        HomeSwitchRow(
                            title = stringResource(R.string.home_pref_search_bar_title),
                            subtitle = stringResource(R.string.home_pref_search_bar_subtitle),
                            checked = layoutPrefs.showSearchBar,
                            onCheckedChange = viewModel::setShowSearchBar
                        )
                        if (layoutPrefs.showSearchBar) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            HomeSwitchRow(
                                title = stringResource(R.string.home_pref_pin_search_title),
                                subtitle = stringResource(R.string.home_pref_pin_search_subtitle),
                                checked = layoutPrefs.pinSearchBar,
                                onCheckedChange = viewModel::setPinSearchBar
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        HomeSwitchRow(
                            title = stringResource(R.string.home_pref_changelog_btn_title),
                            subtitle = stringResource(R.string.home_pref_changelog_btn_subtitle),
                            checked = layoutPrefs.showChangelogInTopBar,
                            onCheckedChange = viewModel::setShowChangelogInTopBar
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        HomeSwitchRow(
                            title = stringResource(R.string.home_pref_build_badge_title),
                            subtitle = stringResource(R.string.home_pref_build_badge_subtitle),
                            checked = layoutPrefs.showBuildBadgeInTopBar,
                            onCheckedChange = viewModel::setShowBuildBadgeInTopBar
                        )
                    }
                }
            }

            item(key = "home_customize_content_group") {
                SectionGroupHeading(text = stringResource(R.string.home_customize_group_content))
                Spacer(Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HomeSwitchRow(
                            title = stringResource(R.string.home_pref_smart_filter_title),
                            subtitle = stringResource(R.string.home_pref_smart_filter_subtitle),
                            checked = layoutPrefs.filterShortAudioInMix,
                            onCheckedChange = viewModel::setFilterShortAudioInMix
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        HomeSwitchRow(
                            title = stringResource(R.string.home_pref_entry_quick_actions_title),
                            subtitle = stringResource(R.string.home_pref_entry_quick_actions_subtitle),
                            checked = layoutPrefs.showQuickActionsOnEntryCards,
                            onCheckedChange = viewModel::setShowQuickActionsOnEntryCards
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        HomeSwitchRow(
                            title = stringResource(R.string.home_pref_video_progress_title),
                            subtitle = stringResource(R.string.home_pref_video_progress_subtitle),
                            checked = layoutPrefs.showVideoProgressOnHome,
                            onCheckedChange = viewModel::setShowVideoProgressOnHome
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        HomeSwitchRow(
                            title = stringResource(R.string.home_pref_video_stats_title),
                            subtitle = stringResource(R.string.home_pref_video_stats_subtitle),
                            checked = layoutPrefs.showVideoSummaryInStats,
                            onCheckedChange = viewModel::setShowVideoSummaryInStats
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_pref_recent_limit_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(6, 12, 20).forEach { count ->
                                    FilterChip(
                                        selected = layoutPrefs.effectiveRecentLimit == count,
                                        onClick = { viewModel.setRecentCardsLimit(count) },
                                        label = { Text(count.toString()) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_pref_density_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    14 to R.string.home_pref_density_compact,
                                    20 to R.string.home_pref_density_comfortable,
                                    28 to R.string.home_pref_density_spacious
                                ).forEach { (dpValue, labelRes) ->
                                    FilterChip(
                                        selected = layoutPrefs.effectiveSectionSpacingDp == dpValue,
                                        onClick = { viewModel.setSectionSpacingDp(dpValue) },
                                        label = { Text(stringResource(labelRes)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "home_customize_sections_heading") {
                SectionGroupHeading(text = stringResource(R.string.home_customize_group_sections))
                Text(
                    text = stringResource(R.string.home_customize_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            items(sections, key = { it.id }) { section ->
                val sectionId = runCatching { HomeSectionId.valueOf(section.id) }.getOrNull()
                if (sectionId != null && sectionId != HomeSectionId.COUNTS) {
                    val index = sections.indexOf(section)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (section.visible) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = sectionId.iconVector,
                                    contentDescription = null,
                                    tint = if (section.visible) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(sectionId.titleRes),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (section.visible) {
                                        stringResource(R.string.home_customize_show)
                                    } else {
                                        stringResource(R.string.home_customize_hidden)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveUp(sectionId) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.home_customize_move_up)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveDown(sectionId) },
                                enabled = index < sections.lastIndex
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.home_customize_move_down)
                                )
                            }
                            Switch(
                                checked = section.visible,
                                onCheckedChange = { checked ->
                                    viewModel.setVisible(sectionId, checked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionGroupHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun HomeSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private val HomeSectionId.titleRes: Int
    get() = when (this) {
        HomeSectionId.ENTRY_CARDS -> R.string.home_section_entry_cards
        HomeSectionId.QUICK_SHORTCUTS -> R.string.home_section_quick_shortcuts
        HomeSectionId.COUNTS -> R.string.home_section_counts
        HomeSectionId.RECENT_MUSIC -> R.string.home_section_recent_music
        HomeSectionId.RECENT_VIDEOS -> R.string.home_section_recent_videos
        HomeSectionId.YOUR_MIX -> R.string.home_section_your_mix
        HomeSectionId.DAILY_MIX -> R.string.home_section_daily_mix
        HomeSectionId.RECENTLY_PLAYED -> R.string.home_section_recently_played
        HomeSectionId.STATS -> R.string.home_section_stats
        HomeSectionId.RECAP -> R.string.home_section_recap
    }

private val HomeSectionId.iconVector: ImageVector
    get() = when (this) {
        HomeSectionId.ENTRY_CARDS -> Icons.Rounded.GridView
        HomeSectionId.QUICK_SHORTCUTS -> Icons.Rounded.Bolt
        HomeSectionId.COUNTS -> Icons.Rounded.GridView
        HomeSectionId.RECENT_MUSIC -> Icons.Rounded.LibraryMusic
        HomeSectionId.RECENT_VIDEOS -> Icons.Rounded.VideoLibrary
        HomeSectionId.YOUR_MIX -> Icons.Rounded.QueueMusic
        HomeSectionId.DAILY_MIX -> Icons.Rounded.AutoAwesome
        HomeSectionId.RECENTLY_PLAYED -> Icons.Rounded.History
        HomeSectionId.STATS -> Icons.Rounded.BarChart
        HomeSectionId.RECAP -> Icons.Rounded.CalendarMonth
    }
