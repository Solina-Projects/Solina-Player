package app.solina.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.solina.player.data.preferences.HomeCustomizationRepository
import app.solina.player.data.preferences.HomeLayoutPreferences
import app.solina.player.data.preferences.HomeSectionConfig
import app.solina.player.data.preferences.HomeSectionId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Home customisation state: show/hide, reorder sections, and tune Home layout & filters. */
@HiltViewModel
class HomeCustomizationViewModel @Inject constructor(
    private val repository: HomeCustomizationRepository
) : ViewModel() {

    val sections: StateFlow<List<HomeSectionConfig>> = repository.sectionsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeCustomizationRepository.DEFAULT_SECTIONS
        )

    val layoutPrefs: StateFlow<HomeLayoutPreferences> = repository.layoutPrefsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeCustomizationRepository.defaultLayoutPreferences()
        )

    fun setVisible(id: HomeSectionId, visible: Boolean) {
        viewModelScope.launch { repository.setVisible(id, visible) }
    }

    fun moveUp(id: HomeSectionId) {
        viewModelScope.launch { repository.move(id, -1) }
    }

    fun moveDown(id: HomeSectionId) {
        viewModelScope.launch { repository.move(id, +1) }
    }

    fun updateLayout(transform: (HomeLayoutPreferences) -> HomeLayoutPreferences) {
        viewModelScope.launch { repository.updateLayoutPreferences(transform) }
    }

    fun setShowGreetingHeader(show: Boolean) = updateLayout { it.copy(showGreetingHeader = show) }
    fun setShowSearchBar(show: Boolean) = updateLayout { it.copy(showSearchBar = show) }
    fun setPinSearchBar(pin: Boolean) = updateLayout { it.copy(pinSearchBar = pin) }
    fun setShowQuickActionsOnEntryCards(show: Boolean) =
        updateLayout { it.copy(showQuickActionsOnEntryCards = show) }
    fun setFilterShortAudioInMix(filter: Boolean) =
        updateLayout { it.copy(filterShortAudioInMix = filter) }
    fun setShowVideoProgressOnHome(show: Boolean) =
        updateLayout { it.copy(showVideoProgressOnHome = show) }
    fun setShowVideoSummaryInStats(show: Boolean) =
        updateLayout { it.copy(showVideoSummaryInStats = show) }
    fun setShowChangelogInTopBar(show: Boolean) =
        updateLayout { it.copy(showChangelogInTopBar = show) }
    fun setShowBuildBadgeInTopBar(show: Boolean) =
        updateLayout { it.copy(showBuildBadgeInTopBar = show) }
    fun setRecentCardsLimit(limit: Int) =
        updateLayout { it.copy(recentCardsLimit = limit.coerceIn(6, 20)) }
    fun setSectionSpacingDp(spacingDp: Int) =
        updateLayout { it.copy(sectionSpacingDp = spacingDp.coerceIn(12, 32)) }

    fun resetToDefaults() {
        viewModelScope.launch { repository.resetToDefaults() }
    }
}
