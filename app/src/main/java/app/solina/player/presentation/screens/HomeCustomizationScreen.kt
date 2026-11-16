package app.solina.player.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.preferences.HomeSectionId
import app.solina.player.presentation.viewmodel.HomeCustomizationViewModel

/**
 * Home customisation (Settings): for every Home section — new music/video
 * sections and the existing music sections — choose whether it shows and
 * move it up or down. Changes apply to Home immediately (DataStore Flow).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCustomizationScreen(
    navController: NavController,
    viewModel: HomeCustomizationViewModel = hiltViewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "home_customize_intro") {
                Text(
                    text = stringResource(R.string.home_customize_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(sections, key = { it.id }) { section ->
                val sectionId = runCatching { HomeSectionId.valueOf(section.id) }.getOrNull()
                if (sectionId != null) {
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
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

private val HomeSectionId.titleRes: Int
    get() = when (this) {
        HomeSectionId.ENTRY_CARDS -> R.string.home_section_entry_cards
        HomeSectionId.COUNTS -> R.string.home_section_counts
        HomeSectionId.RECENT_MUSIC -> R.string.home_section_recent_music
        HomeSectionId.RECENT_VIDEOS -> R.string.home_section_recent_videos
        HomeSectionId.YOUR_MIX -> R.string.home_section_your_mix
        HomeSectionId.DAILY_MIX -> R.string.home_section_daily_mix
        HomeSectionId.RECENTLY_PLAYED -> R.string.home_section_recently_played
        HomeSectionId.STATS -> R.string.home_section_stats
        HomeSectionId.RECAP -> R.string.home_section_recap
    }
