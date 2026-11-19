package app.solina.player.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which sections the Home screen shows, and in which order — for the new
 * music/video sections AND the existing music sections. The user picks what
 * is shown, what is hidden, and where each section sits, from the Home
 * customisation screen in Settings.
 *
 * Stored as one JSON document in the shared "settings" DataStore so a whole
 * order is saved atomically (a toggle or a move never leaves a half order).
 */
enum class HomeSectionId {
    ENTRY_CARDS,
    QUICK_SHORTCUTS,
    // COUNTS was removed 2026-11-17 (bug-fix item 3: the count cards duplicated
    // the counts already shown on the entry cards). The value stays so a stored
    // section id can never fail to parse; it is no longer in the defaults.
    COUNTS,
    RECENT_MUSIC,
    RECENT_VIDEOS,
    YOUR_MIX,
    DAILY_MIX,
    RECENTLY_PLAYED,
    STATS,
    RECAP
}

@Serializable
data class HomeSectionConfig(
    val id: String,
    val visible: Boolean,
    val order: Int
)

@Serializable
data class HomeSectionsState(
    val sections: List<HomeSectionConfig>
)

/**
 * Granular Home layout & content preferences so users can tune the header,
 * search bar behaviour, smart filtering, card metadata, recent item counts,
 * and section spacing right from Customise Home.
 */
@Serializable
data class HomeLayoutPreferences(
    val showGreetingHeader: Boolean = true,
    val showSearchBar: Boolean = true,
    /** When false, the search bar scrolls naturally and collapses into a TopBar icon. */
    val pinSearchBar: Boolean = false,
    val showQuickActionsOnEntryCards: Boolean = true,
    /** Hide short clips (<30s), ringtones & call recordings from Your Mix & Recent Music. */
    val filterShortAudioInMix: Boolean = true,
    val showVideoProgressOnHome: Boolean = true,
    val showVideoSummaryInStats: Boolean = true,
    val showChangelogInTopBar: Boolean = false,
    val showBuildBadgeInTopBar: Boolean = false,
    val recentCardsLimit: Int = 12,
    val sectionSpacingDp: Int = 20
) {
    val effectiveRecentLimit: Int
        get() = recentCardsLimit.coerceIn(6, 20)

    val effectiveSectionSpacingDp: Int
        get() = sectionSpacingDp.coerceIn(12, 32)
}

@Singleton
class HomeCustomizationRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    private object Keys {
        val HOME_SECTIONS = stringPreferencesKey("home_sections_v1")
        val HOME_LAYOUT_PREFS = stringPreferencesKey("home_layout_prefs_v1")
    }

    companion object {
        /** Default order, top to bottom, everything visible. */
        val DEFAULT_SECTIONS: List<HomeSectionConfig> = listOf(
            HomeSectionConfig(HomeSectionId.ENTRY_CARDS.name, true, 0),
            HomeSectionConfig(HomeSectionId.QUICK_SHORTCUTS.name, true, 1),
            HomeSectionConfig(HomeSectionId.RECENT_MUSIC.name, true, 2),
            HomeSectionConfig(HomeSectionId.RECENT_VIDEOS.name, true, 3),
            HomeSectionConfig(HomeSectionId.YOUR_MIX.name, true, 4),
            HomeSectionConfig(HomeSectionId.DAILY_MIX.name, true, 5),
            HomeSectionConfig(HomeSectionId.RECENTLY_PLAYED.name, true, 6),
            HomeSectionConfig(HomeSectionId.STATS.name, true, 7),
            HomeSectionConfig(HomeSectionId.RECAP.name, true, 8)
        )

        fun defaultState(): HomeSectionsState = HomeSectionsState(DEFAULT_SECTIONS)
        fun defaultLayoutPreferences(): HomeLayoutPreferences = HomeLayoutPreferences()
    }

    /** Ordered, visible-or-not section list. Falls back to defaults when unset or corrupt. */
    private fun decodeSections(preferences: Preferences): List<HomeSectionConfig> {
        val raw = preferences[Keys.HOME_SECTIONS] ?: return DEFAULT_SECTIONS
        return runCatching {
            val stored = json.decodeFromString<HomeSectionsState>(raw).sections
            // Merge with defaults so a section added by an app update appears.
            val byId = stored.associateBy { it.id }
            val merged = DEFAULT_SECTIONS.map { default ->
                byId[default.id] ?: default
            }
            merged.sortedBy { it.order }
        }.getOrElse { DEFAULT_SECTIONS }
    }

    private fun decodeLayoutPrefs(preferences: Preferences): HomeLayoutPreferences {
        val raw = preferences[Keys.HOME_LAYOUT_PREFS] ?: return defaultLayoutPreferences()
        return runCatching {
            json.decodeFromString<HomeLayoutPreferences>(raw)
        }.getOrElse { defaultLayoutPreferences() }
    }

    val sectionsFlow: Flow<List<HomeSectionConfig>> = dataStore.data
        .map { preferences -> decodeSections(preferences) }
        .distinctUntilChanged()

    val layoutPrefsFlow: Flow<HomeLayoutPreferences> = dataStore.data
        .map { preferences -> decodeLayoutPrefs(preferences) }
        .distinctUntilChanged()

    fun isVisible(sections: List<HomeSectionConfig>, id: HomeSectionId): Boolean =
        sections.firstOrNull { it.id == id.name }?.visible ?: true

    suspend fun setVisible(id: HomeSectionId, visible: Boolean) {
        updateSections { sections ->
            sections.map { if (it.id == id.name) it.copy(visible = visible) else it }
        }
    }

    /** Move a section one step up (direction = -1) or down (direction = +1). */
    suspend fun move(id: HomeSectionId, direction: Int) {
        updateSections { sections ->
            val sorted = sections.sortedBy { it.order }.toMutableList()
            val index = sorted.indexOfFirst { it.id == id.name }
            val target = index + direction
            if (index < 0 || target !in sorted.indices) return@updateSections sections
            val moved = sorted.removeAt(index)
            sorted.add(target, moved)
            sorted.mapIndexed { newOrder, config -> config.copy(order = newOrder) }
        }
    }

    suspend fun updateLayoutPreferences(
        transform: (HomeLayoutPreferences) -> HomeLayoutPreferences
    ) {
        dataStore.edit { preferences ->
            val current = decodeLayoutPrefs(preferences)
            val updated = transform(current)
            preferences[Keys.HOME_LAYOUT_PREFS] = json.encodeToString(updated)
        }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences[Keys.HOME_SECTIONS] = json.encodeToString(defaultState())
            preferences[Keys.HOME_LAYOUT_PREFS] = json.encodeToString(defaultLayoutPreferences())
        }
    }

    private suspend fun updateSections(
        transform: (List<HomeSectionConfig>) -> List<HomeSectionConfig>
    ) {
        dataStore.edit { preferences ->
            val current = decodeSections(preferences)
            val updated = transform(current).sortedBy { it.order }
                .mapIndexed { index, config -> config.copy(order = index) }
            preferences[Keys.HOME_SECTIONS] =
                json.encodeToString(HomeSectionsState(updated))
        }
    }
}
