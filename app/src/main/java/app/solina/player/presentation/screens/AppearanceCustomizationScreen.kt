package app.solina.player.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.solina.player.R
import app.solina.player.data.preferences.AppThemeMode
import app.solina.player.data.preferences.AppearanceFontFamilies
import app.solina.player.data.preferences.AppearancePresetNames
import app.solina.player.data.preferences.GlassPresetNames
import app.solina.player.data.preferences.GlassStyleNames
import app.solina.player.presentation.components.MiniPlayerHeight
import app.solina.player.presentation.navigation.Screen
import app.solina.player.presentation.navigation.navigateSafely
import app.solina.player.presentation.viewmodel.AppearanceViewModel

/**
 * Global Appearance & Theme hub:
 * - Quick switcher to the 3 dedicated section customisers (Home, Music, Video)
 * - App-wide presets & theme mode (System / Light / Dark)
 * - Glass & blur engine
 * - Global typography (font family & base scale)
 * - Saved looks (save, apply, delete)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceCustomizationScreen(
    navController: NavController,
    viewModel: AppearanceViewModel = hiltViewModel()
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val savedPresets by viewModel.savedPresets.collectAsStateWithLifecycle()
    val presetNoticeRes by viewModel.presetNoticeRes.collectAsStateWithLifecycle()
    var showSaveLookDialog by remember { mutableStateOf(false) }

    if (showSaveLookDialog) {
        SaveLookDialog(
            onDismiss = { showSaveLookDialog = false },
            onSave = { name ->
                viewModel.saveCurrentLook(name)
                showSaveLookDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.appearance_customize_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.resetAll() }) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(text = stringResource(R.string.appearance_reset_all))
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
            item(key = "appearance_intro") {
                Text(
                    text = stringResource(R.string.appearance_customize_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---- Dedicated section customisers hub ----
            item(key = "appearance_section_hub") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomizationGroupHeading(
                        text = stringResource(R.string.customize_by_section_heading)
                    )
                    CustomizationLinkCard(
                        title = stringResource(R.string.home_customize_title),
                        subtitle = stringResource(R.string.home_customize_hub_subtitle),
                        icon = Icons.Rounded.Home,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = { navController.navigateSafely(Screen.HomeCustomization.route) }
                    )
                    CustomizationLinkCard(
                        title = stringResource(R.string.music_customize_title),
                        subtitle = stringResource(R.string.music_customize_subtitle),
                        icon = Icons.Rounded.MusicNote,
                        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { navController.navigateSafely(Screen.MusicCustomization.route) }
                    )
                    CustomizationLinkCard(
                        title = stringResource(R.string.video_customize_title),
                        subtitle = stringResource(R.string.video_customize_subtitle),
                        icon = Icons.Rounded.VideoLibrary,
                        iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { navController.navigateSafely(Screen.VideoCustomization.route) }
                    )
                }
            }

            // ---- Theme + ready-made looks ----
            item(key = "appearance_theme") {
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_theme_title),
                    onReset = { viewModel.resetAll() }
                ) {
                    ChoiceChips(
                        options = listOf(
                            AppearancePresetNames.DEFAULT to stringResource(R.string.appearance_preset_default),
                            AppearancePresetNames.COMPACT to stringResource(R.string.appearance_preset_compact),
                            AppearancePresetNames.BOLD to stringResource(R.string.appearance_preset_bold),
                            AppearancePresetNames.NIGHT to stringResource(R.string.appearance_preset_night)
                        ),
                        selected = null,
                        onSelect = { viewModel.applyPreset(it) },
                        label = stringResource(R.string.appearance_preset_title)
                    )
                    Spacer(Modifier.height(8.dp))
                    ChoiceChips(
                        options = listOf(
                            AppThemeMode.FOLLOW_SYSTEM to stringResource(R.string.appearance_theme_system),
                            AppThemeMode.LIGHT to stringResource(R.string.appearance_theme_light),
                            AppThemeMode.DARK to stringResource(R.string.appearance_theme_dark)
                        ),
                        selected = appThemeMode,
                        onSelect = { viewModel.setAppThemeMode(it) },
                        label = null
                    )
                }
            }

            // ---- Glass & blur ----
            item(key = "appearance_glass") {
                val glass = appearance.glass
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_glass_title),
                    onReset = { viewModel.resetGlass() }
                ) {
                    Text(
                        text = stringResource(R.string.appearance_glass_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.appearance_glass_enable),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = glass.enabled,
                            onCheckedChange = {
                                viewModel.setGlass(glass.copy(enabled = it))
                            }
                        )
                    }
                    ChoiceChips(
                        options = listOf(
                            GlassPresetNames.OFF to stringResource(R.string.appearance_glass_preset_off),
                            GlassPresetNames.SUBTLE to stringResource(R.string.appearance_glass_preset_subtle),
                            GlassPresetNames.FROSTED to stringResource(R.string.appearance_glass_preset_frosted),
                            GlassPresetNames.LIQUID to stringResource(R.string.appearance_glass_preset_liquid)
                        ),
                        selected = null,
                        onSelect = { viewModel.applyGlassPreset(it) },
                        label = stringResource(R.string.appearance_glass_preset_title)
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassToggleRow(
                        label = stringResource(R.string.appearance_glass_mini_bars),
                        checked = glass.miniBars,
                        enabled = glass.enabled,
                        onChange = { viewModel.setGlass(glass.copy(miniBars = it)) }
                    )
                    GlassToggleRow(
                        label = stringResource(R.string.appearance_glass_sheets),
                        checked = glass.sheets,
                        enabled = glass.enabled,
                        onChange = { viewModel.setGlass(glass.copy(sheets = it)) }
                    )
                    GlassToggleRow(
                        label = stringResource(R.string.appearance_glass_dialogs),
                        checked = glass.dialogs,
                        enabled = glass.enabled,
                        onChange = { viewModel.setGlass(glass.copy(dialogs = it)) }
                    )
                    GlassToggleRow(
                        label = stringResource(R.string.appearance_glass_now_playing),
                        checked = glass.nowPlayingBackground,
                        enabled = glass.enabled,
                        onChange = {
                            viewModel.setGlass(glass.copy(nowPlayingBackground = it))
                        }
                    )
                    ChoiceChips(
                        options = listOf(
                            GlassStyleNames.FROSTED to stringResource(R.string.appearance_glass_style_frosted),
                            GlassStyleNames.LIQUID to stringResource(R.string.appearance_glass_style_liquid),
                            GlassStyleNames.CLEAR to stringResource(R.string.appearance_glass_style_clear)
                        ),
                        selected = glass.style,
                        onSelect = { viewModel.setGlass(glass.copy(style = it)) },
                        label = stringResource(R.string.appearance_glass_style)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.appearance_glass_mini_bars),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DpSlider(
                        label = stringResource(R.string.appearance_glass_radius),
                        value = glass.miniBarRadius,
                        range = 0..40,
                        onChange = {
                            viewModel.setGlass(glass.copy(miniBarRadiusDp = it))
                        },
                        enabled = glass.enabled
                    )
                    PercentSlider(
                        label = stringResource(R.string.appearance_glass_tint),
                        value = (glass.miniBarTintAlpha * 100).toInt(),
                        range = 0..60,
                        onChange = {
                            viewModel.setGlass(glass.copy(miniBarTintPercent = it))
                        },
                        enabled = glass.enabled
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.appearance_glass_sheets_group),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    DpSlider(
                        label = stringResource(R.string.appearance_glass_radius),
                        value = glass.sheetRadius,
                        range = 0..40,
                        onChange = {
                            viewModel.setGlass(glass.copy(sheetRadiusDp = it))
                        },
                        enabled = glass.enabled
                    )
                    PercentSlider(
                        label = stringResource(R.string.appearance_glass_tint),
                        value = (glass.sheetTintAlpha * 100).toInt(),
                        range = 0..60,
                        onChange = {
                            viewModel.setGlass(glass.copy(sheetTintPercent = it))
                        },
                        enabled = glass.enabled
                    )
                    Spacer(Modifier.height(8.dp))
                    PercentSlider(
                        label = stringResource(R.string.appearance_glass_saturation),
                        value = glass.saturationPercent,
                        range = 100..160,
                        onChange = {
                            viewModel.setGlass(glass.copy(saturationPercent = it))
                        },
                        enabled = glass.enabled
                    )
                    DpSlider(
                        label = stringResource(R.string.appearance_glass_refraction),
                        value = glass.refractionDp,
                        range = 0..26,
                        onChange = { viewModel.setGlass(glass.copy(refractionDp = it)) },
                        enabled = glass.enabled && glass.style == GlassStyleNames.LIQUID
                    )
                    Text(
                        text = stringResource(R.string.appearance_glass_refraction_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.appearance_glass_video_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Global Typography ----
            item(key = "appearance_typography") {
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_typography_title),
                    onReset = { viewModel.resetTypography() }
                ) {
                    ChoiceChips(
                        options = listOf(
                            AppearanceFontFamilies.ROUNDED to stringResource(R.string.appearance_font_rounded),
                            AppearanceFontFamilies.VAZIRMATN to stringResource(R.string.appearance_font_vazirmatn),
                            AppearanceFontFamilies.SYSTEM to stringResource(R.string.appearance_font_system)
                        ),
                        selected = appearance.fontFamily,
                        onSelect = { viewModel.setFontFamily(it) },
                        label = stringResource(R.string.appearance_font_family_title)
                    )
                    Spacer(Modifier.height(8.dp))
                    PercentSlider(
                        label = stringResource(R.string.appearance_font_size_title),
                        value = appearance.fontScalePercent,
                        range = 85..125,
                        onChange = { viewModel.setFontScalePercent(it) }
                    )
                }
            }

            // ---- Saved looks ----
            item(key = "appearance_saved_presets") {
                AppearanceSectionCard(
                    title = stringResource(R.string.appearance_saved_presets_title),
                    onReset = null
                ) {
                    Text(
                        text = stringResource(R.string.appearance_saved_preset_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.clearPresetNotice()
                            showSaveLookDialog = true
                        }
                    ) {
                        Text(text = stringResource(R.string.appearance_save_current_look))
                    }
                    if (presetNoticeRes != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(presetNoticeRes!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (savedPresets.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.appearance_saved_preset_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        for (preset in savedPresets) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.applySavedPreset(preset.name)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteSavedPreset(preset.name) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = stringResource(
                                            R.string.appearance_saved_preset_delete
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
