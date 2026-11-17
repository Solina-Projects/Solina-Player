package app.solina.player.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-section appearance customisation (bug-fix round 2026-11-17, item 6).
 *
 * Nearly every visible part of the player can be tuned from Settings
 * ("Customise appearance"): the Home bars, the Home cards, the video
 * bar, the floating music bar's colour, the colours of the Home /
 * Music / Video sections each separately, the font family and the font
 * size scale — plus ready-made presets and a reset per section.
 *
 * Stored as one JSON document in the shared "settings" DataStore, the
 * same way [HomeCustomizationRepository] stores the Home order, so a
 * whole appearance is saved atomically. Every value is a plain number
 * or name; the colour tables live in
 * [app.solina.player.presentation.components.AppearancePalettes] so
 * this file stays free of Compose types.
 *
 * Every section is independent: changing the Home music bar never
 * touches the video bar, and a section palette only tints that
 * section's surfaces (a per-bar palette overrides the section one
 * for that bar). "default" palette means "follow the app theme".
 */

/** Colour palette names. "default" follows the app theme scheme. */
object AppearancePaletteNames {
    const val DEFAULT = "default"
    const val BLUE = "blue"
    const val GREEN = "green"
    const val PURPLE = "purple"
    const val ORANGE = "orange"
    const val TEAL = "teal"

    val ALL = listOf(DEFAULT, BLUE, GREEN, PURPLE, ORANGE, TEAL)
}

/** Font family choices. "rounded" is the app's default face. */
object AppearanceFontFamilies {
    const val ROUNDED = "rounded"
    const val VAZIRMATN = "vazirmatn"
    const val SYSTEM = "system"

    val ALL = listOf(ROUNDED, VAZIRMATN, SYSTEM)
}

/** Ready-made looks: a preset is just a whole [AppearanceSettings]. */
object AppearancePresetNames {
    const val DEFAULT = "default"
    const val COMPACT = "compact"
    const val BOLD = "bold"
    const val NIGHT = "night"

    val ALL = listOf(DEFAULT, COMPACT, BOLD, NIGHT)
}

/** One playback bar: show/hide, size (percent), corner radius, palette. */
@Serializable
data class BarAppearance(
    val visible: Boolean = true,
    val scalePercent: Int = 100,
    val cornerDp: Int = 32,
    val palette: String = AppearancePaletteNames.DEFAULT
) {
    /** 0.8x–1.2x, sanitised so a corrupt stored value cannot break layout. */
    val scale: Float get() = (scalePercent.coerceIn(80, 120) / 100f)
    val cornerRadiusDp: Int get() = cornerDp.coerceIn(0, 32)
}

/** One Home entry card: size (percent), corner radius, palette. */
@Serializable
data class CardAppearance(
    val scalePercent: Int = 100,
    val cornerDp: Int = 20,
    val palette: String = AppearancePaletteNames.DEFAULT
) {
    /** 0.85x–1.15x, sanitised. */
    val scale: Float get() = (scalePercent.coerceIn(85, 115) / 100f)
    val cornerRadiusDp: Int get() = cornerDp.coerceIn(0, 32)
}

@Serializable
data class AppearanceSettings(
    /** Fixed music bar under "Recently added music" on Home. */
    val homeMusicBar: BarAppearance = BarAppearance(),
    /** Fixed video bar under "Recently added videos" on Home. */
    val homeVideoBar: BarAppearance = BarAppearance(),
    /** Floating video bar above the bottom bar, in the Video section. */
    val sectionVideoBar: BarAppearance = BarAppearance(),
    /** Floating music mini-player bar (Music section): colour only — its
     * size and corners are fixed by the player sheet's layout math. */
    val floatingMusicBarPalette: String = AppearancePaletteNames.DEFAULT,
    /** The two big Music / Video entry cards on Home. */
    val homeCards: CardAppearance = CardAppearance(),
    /** Section accent palettes, each independent. */
    val homeSectionPalette: String = AppearancePaletteNames.DEFAULT,
    val musicSectionPalette: String = AppearancePaletteNames.DEFAULT,
    val videoSectionPalette: String = AppearancePaletteNames.DEFAULT,
    /** App-wide font family (applied through the theme typography). */
    val fontFamily: String = AppearanceFontFamilies.ROUNDED,
    /** Font size scale for the customisable surfaces (85–125%). */
    val fontScalePercent: Int = 100
) {
    val fontScale: Float get() = (fontScalePercent.coerceIn(85, 125) / 100f)
}

@Singleton
class AppearancePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    private object Keys {
        val APPEARANCE_SETTINGS = stringPreferencesKey("appearance_settings_v1")
    }

    companion object {
        fun defaultSettings(): AppearanceSettings = AppearanceSettings()

        fun presetSettings(preset: String): AppearanceSettings = when (preset) {
            AppearancePresetNames.COMPACT -> AppearanceSettings(
                homeMusicBar = BarAppearance(scalePercent = 90, cornerDp = 24),
                homeVideoBar = BarAppearance(scalePercent = 90, cornerDp = 24),
                sectionVideoBar = BarAppearance(scalePercent = 90, cornerDp = 24),
                homeCards = CardAppearance(scalePercent = 90, cornerDp = 16),
                fontScalePercent = 95
            )

            AppearancePresetNames.BOLD -> AppearanceSettings(
                homeMusicBar = BarAppearance(scalePercent = 110, cornerDp = 32),
                homeVideoBar = BarAppearance(scalePercent = 110, cornerDp = 32),
                sectionVideoBar = BarAppearance(scalePercent = 110, cornerDp = 32),
                homeCards = CardAppearance(scalePercent = 108, cornerDp = 28),
                fontScalePercent = 110
            )

            AppearancePresetNames.NIGHT -> AppearanceSettings(
                homeMusicBar = BarAppearance(palette = AppearancePaletteNames.PURPLE),
                homeVideoBar = BarAppearance(palette = AppearancePaletteNames.BLUE),
                sectionVideoBar = BarAppearance(palette = AppearancePaletteNames.BLUE),
                floatingMusicBarPalette = AppearancePaletteNames.PURPLE,
                homeSectionPalette = AppearancePaletteNames.TEAL,
                musicSectionPalette = AppearancePaletteNames.PURPLE,
                videoSectionPalette = AppearancePaletteNames.BLUE,
                homeCards = CardAppearance(palette = AppearancePaletteNames.TEAL)
            )

            else -> AppearanceSettings()
        }
    }

    /** Whole appearance, live. Falls back to defaults when unset or corrupt. */
    val appearanceFlow: Flow<AppearanceSettings> = dataStore.data.map { preferences ->
        val raw = preferences[Keys.APPEARANCE_SETTINGS] ?: return@map defaultSettings()
        runCatching { json.decodeFromString<AppearanceSettings>(raw) }
            .getOrElse { defaultSettings() }
    }

    suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        val current = appearanceFlow.first()
        val updated = transform(current)
        dataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_SETTINGS] = json.encodeToString(updated)
        }
    }

    suspend fun applyPreset(preset: String) {
        dataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_SETTINGS] =
                json.encodeToString(presetSettings(preset))
        }
    }

    suspend fun resetHomeMusicBar() =
        update { it.copy(homeMusicBar = BarAppearance()) }

    suspend fun resetHomeVideoBar() =
        update { it.copy(homeVideoBar = BarAppearance()) }

    suspend fun resetSectionVideoBar() =
        update { it.copy(sectionVideoBar = BarAppearance()) }

    suspend fun resetFloatingMusicBar() =
        update { it.copy(floatingMusicBarPalette = AppearancePaletteNames.DEFAULT) }

    suspend fun resetHomeCards() =
        update { it.copy(homeCards = CardAppearance()) }

    suspend fun resetSectionColors() = update {
        it.copy(
            homeSectionPalette = AppearancePaletteNames.DEFAULT,
            musicSectionPalette = AppearancePaletteNames.DEFAULT,
            videoSectionPalette = AppearancePaletteNames.DEFAULT
        )
    }

    suspend fun resetTypography() = update {
        it.copy(
            fontFamily = AppearanceFontFamilies.ROUNDED,
            fontScalePercent = 100
        )
    }

    suspend fun resetAll() {
        dataStore.edit { preferences ->
            preferences[Keys.APPEARANCE_SETTINGS] =
                json.encodeToString(defaultSettings())
        }
    }
}
