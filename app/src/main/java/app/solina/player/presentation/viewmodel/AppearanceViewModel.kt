package app.solina.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.solina.player.data.preferences.AppearancePreferences
import app.solina.player.data.preferences.AppearanceSettings
import app.solina.player.data.preferences.BarAppearance
import app.solina.player.data.preferences.CardAppearance
import app.solina.player.data.preferences.GlassAppearance
import app.solina.player.data.preferences.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Appearance customisation (bug-fix round 2026-11-17, item 6): the
 * Settings screen reads the live [AppearanceSettings] here and writes
 * back through [AppearancePreferences]; every change applies
 * immediately because the bars and cards collect the same flow.
 * The app theme mode lives in [ThemePreferencesRepository] and is
 * exposed here too, so the Theme group sits on the same screen.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val repository: AppearancePreferences,
    private val themePreferencesRepository: ThemePreferencesRepository
) : ViewModel() {

    val appearance: StateFlow<AppearanceSettings> =
        repository.appearanceFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppearancePreferences.defaultSettings()
        )

    val appThemeMode: StateFlow<String> =
        themePreferencesRepository.appThemeModeFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            app.solina.player.data.preferences.AppThemeMode.FOLLOW_SYSTEM
        )

    fun setAppThemeMode(mode: String) {
        viewModelScope.launch { themePreferencesRepository.setAppThemeMode(mode) }
    }

    private fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    fun applyPreset(preset: String) {
        viewModelScope.launch { repository.applyPreset(preset) }
    }

    fun setHomeMusicBar(value: BarAppearance) =
        update { it.copy(homeMusicBar = value) }

    fun setHomeVideoBar(value: BarAppearance) =
        update { it.copy(homeVideoBar = value) }

    fun setSectionVideoBar(value: BarAppearance) =
        update { it.copy(sectionVideoBar = value) }

    fun setFloatingMusicBarPalette(palette: String) =
        update { it.copy(floatingMusicBarPalette = palette) }

    fun setHomeCards(value: CardAppearance) =
        update { it.copy(homeCards = value) }

    fun setHomeSectionPalette(palette: String) =
        update { it.copy(homeSectionPalette = palette) }

    fun setMusicSectionPalette(palette: String) =
        update { it.copy(musicSectionPalette = palette) }

    fun setVideoSectionPalette(palette: String) =
        update { it.copy(videoSectionPalette = palette) }

    fun setFontFamily(family: String) =
        update { it.copy(fontFamily = family) }

    fun setFontScalePercent(percent: Int) =
        update { it.copy(fontScalePercent = percent.coerceIn(85, 125)) }

    fun setGlass(value: GlassAppearance) =
        update { it.copy(glass = value) }

    fun applyGlassPreset(preset: String) = update {
        it.copy(glass = AppearancePreferences.glassPresetSettings(preset))
    }

    fun resetHomeMusicBar() = viewModelScope.launch { repository.resetHomeMusicBar() }
    fun resetHomeVideoBar() = viewModelScope.launch { repository.resetHomeVideoBar() }
    fun resetSectionVideoBar() = viewModelScope.launch { repository.resetSectionVideoBar() }
    fun resetFloatingMusicBar() = viewModelScope.launch { repository.resetFloatingMusicBar() }
    fun resetHomeCards() = viewModelScope.launch { repository.resetHomeCards() }
    fun resetSectionColors() = viewModelScope.launch { repository.resetSectionColors() }
    fun resetTypography() = viewModelScope.launch { repository.resetTypography() }
    fun resetGlass() = viewModelScope.launch { repository.resetGlass() }
    fun resetAll() = viewModelScope.launch { repository.resetAll() }
}
