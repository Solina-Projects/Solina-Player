package app.solina.player.presentation.viewmodel

import androidx.lifecycle.ViewModel
import app.solina.player.data.preferences.HomeCustomizationRepository
import app.solina.player.data.preferences.HomeSectionConfig
import app.solina.player.data.preferences.HomeSectionId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/** Home customisation state: show/hide and reorder Home sections. */
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

    fun setVisible(id: HomeSectionId, visible: Boolean) {
        viewModelScope.launch { repository.setVisible(id, visible) }
    }

    fun moveUp(id: HomeSectionId) {
        viewModelScope.launch { repository.move(id, -1) }
    }

    fun moveDown(id: HomeSectionId) {
        viewModelScope.launch { repository.move(id, +1) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { repository.resetToDefaults() }
    }
}
