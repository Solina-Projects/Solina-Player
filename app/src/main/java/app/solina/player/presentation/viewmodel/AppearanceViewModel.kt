package app.solina.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.solina.player.data.preferences.AppearancePreferences
import app.solina.player.data.preferences.AppearanceSettings
import app.solina.player.data.preferences.BarAppearance
import app.solina.player.data.preferences.CardAppearance
import app.solina.player.data.preferences.CornerRadii
import app.solina.player.data.preferences.GlassAppearance
import app.solina.player.data.preferences.ListRowsAppearance
import app.solina.player.data.preferences.RecentCardsAppearance
import app.solina.player.data.preferences.SavePresetResult
import app.solina.player.data.preferences.SavedAppearancePreset
import app.solina.player.data.preferences.SeekBarStyleNames
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

    /** User-saved looks (round 15, wave 2, C7). */
    val savedPresets: StateFlow<List<SavedAppearancePreset>> =
        repository.savedPresetsFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    /**
     * One-shot notice for the saved-looks section (a string resource
     * id, or null). Set by save/apply/delete, cleared when the user
     * opens the save dialog or taps a look again.
     */
    private val _presetNoticeRes = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    val presetNoticeRes: StateFlow<Int?> = _presetNoticeRes

    fun clearPresetNotice() {
        _presetNoticeRes.value = null
    }

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

    fun setListFontScalePercent(percent: Int) =
        update { it.copy(listFontScalePercent = percent.coerceIn(85, 125)) }

    fun setRecentCards(value: RecentCardsAppearance) =
        update { it.copy(recentCards = value) }

    fun setListRows(value: ListRowsAppearance) =
        update { it.copy(listRows = value) }

    fun setYourMixCard(value: CardAppearance) =
        update { it.copy(yourMixCard = value) }

    fun setSeekBarStyle(style: String) =
        update {
            it.copy(
                seekBarStyle = if (style == SeekBarStyleNames.CLASSIC) {
                    SeekBarStyleNames.CLASSIC
                } else {
                    SeekBarStyleNames.WAVY
                }
            )
        }

    /** Save the current look under [name]; reports LIMIT_REACHED via the notice. */
    fun saveCurrentLook(name: String) {
        viewModelScope.launch {
            val result = repository.savePreset(name, appearance.value)
            _presetNoticeRes.value = when (result) {
                SavePresetResult.SAVED -> app.solina.player.R.string.appearance_saved_preset_saved
                SavePresetResult.LIMIT_REACHED -> app.solina.player.R.string.appearance_saved_preset_limit
            }
        }
    }

    fun applySavedPreset(name: String) {
        viewModelScope.launch {
            repository.applySavedPreset(name)
            _presetNoticeRes.value =
                app.solina.player.R.string.appearance_saved_preset_applied
        }
    }

    fun deleteSavedPreset(name: String) {
        viewModelScope.launch {
            repository.deleteSavedPreset(name)
            _presetNoticeRes.value =
                app.solina.player.R.string.appearance_saved_preset_deleted
        }
    }

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
    fun resetRecentCards() = viewModelScope.launch { repository.resetRecentCards() }
    fun resetListRows() = viewModelScope.launch { repository.resetListRows() }
    fun resetYourMixCard() = viewModelScope.launch { repository.resetYourMixCard() }
    fun resetSeekBarStyle() = viewModelScope.launch { repository.resetSeekBarStyle() }
    fun resetSectionColors() = viewModelScope.launch { repository.resetSectionColors() }
    fun resetTypography() = viewModelScope.launch { repository.resetTypography() }
    fun resetGlass() = viewModelScope.launch { repository.resetGlass() }
    fun resetAll() = viewModelScope.launch { repository.resetAll() }
}
