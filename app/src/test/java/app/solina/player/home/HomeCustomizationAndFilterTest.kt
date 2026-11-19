package app.solina.player.home

import app.solina.player.data.model.Song
import app.solina.player.data.model.VideoItem
import app.solina.player.data.preferences.HomeCustomizationRepository
import app.solina.player.data.preferences.HomeLayoutPreferences
import app.solina.player.data.preferences.HomeSectionId
import app.solina.player.presentation.utils.HomeMediaFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeCustomizationAndFilterTest {

    private fun song(
        id: String,
        title: String,
        durationMs: Long = 200_000L,
        path: String = "/storage/emulated/0/Music/$id.mp3",
        art: String? = null
    ) = Song.emptySong().copy(
        id = id,
        title = title,
        duration = durationMs,
        path = path,
        albumArtUriString = art
    )

    @Test
    fun `smart music filter hides ringtones, call recordings and short clips`() {
        val realSong = song("1", "Midnight City", durationMs = 240_000L)
        val callRec = song("2", "phone_20261118-0912", durationMs = 120_000L)
        val ringtone = song("3", "Ringtone iphone", durationMs = 45_000L, path = "/storage/emulated/0/Ringtones/r.mp3")
        val shortClip = song("4", "Notification", durationMs = 5_000L)

        val filtered = HomeMediaFilter.filterMusicForHome(
            listOf(realSong, callRec, ringtone, shortClip),
            enabled = true
        )
        assertEquals(listOf(realSong), filtered)
        assertEquals(4, HomeMediaFilter.filterMusicForHome(listOf(realSong, callRec, ringtone, shortClip), enabled = false).size)
    }

    @Test
    fun `smart music filter falls back safely when every audio file is filtered out`() {
        val onlyRingtone = song("1", "Ringtone iphone", durationMs = 18_000L)
        assertEquals(listOf(onlyRingtone), HomeMediaFilter.filterMusicForHome(listOf(onlyRingtone), enabled = true))
    }

    @Test
    fun `cleanArtistDisplay sanitizes raw unknown placeholders`() {
        assertEquals("Unknown artist", HomeMediaFilter.cleanArtistDisplay("<unknown>", "Unknown artist"))
        assertEquals("Unknown artist", HomeMediaFilter.cleanArtistDisplay("  ", "Unknown artist"))
        assertEquals("Daft Punk", HomeMediaFilter.cleanArtistDisplay("Daft Punk", "Unknown artist"))
    }

    @Test
    fun `cleanVideoSubtitle shows folder and resolution instead of raw mime type`() {
        val video = VideoItem(
            id = 1L,
            title = "Clip",
            durationMs = 90_000L,
            dateAddedSec = 1L,
            dateModifiedSec = 1L,
            mimeType = "video/mp4",
            contentUriString = "content://media/external/video/media/1",
            path = "/storage/emulated/0/Movies/ScreenRecordings/clip.mp4",
            width = 1920,
            height = 1080,
            sizeBytes = 25L * 1024L * 1024L
        )
        val subtitle = HomeMediaFilter.cleanVideoSubtitle(video, "Unknown folder")
        assertEquals("ScreenRecordings · 1080p", subtitle)
        assertFalse(subtitle.contains("video/mp4"))
    }

    @Test
    fun `prioritizeArtworkSongs puts tracks with album art first`() {
        val noArt1 = song("1", "A", art = null)
        val noArt2 = song("2", "B", art = "")
        val withArt = song("3", "C", art = "content://art/3")

        val picked = HomeMediaFilter.prioritizeArtworkSongs(listOf(noArt1, noArt2, withArt), limit = 2)
        assertEquals(listOf("3", "1"), picked.map { it.id })
    }

    @Test
    fun `default sections include QUICK_SHORTCUTS and layout prefs clamp safely`() {
        assertTrue(HomeCustomizationRepository.DEFAULT_SECTIONS.any { it.id == HomeSectionId.QUICK_SHORTCUTS.name })
        val clamped = HomeLayoutPreferences(recentCardsLimit = 999, sectionSpacingDp = 2)
        assertEquals(20, clamped.effectiveRecentLimit)
        assertEquals(12, clamped.effectiveSectionSpacingDp)
    }
}
