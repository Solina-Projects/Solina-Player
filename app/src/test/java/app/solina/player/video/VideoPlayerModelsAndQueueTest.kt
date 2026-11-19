package app.solina.player.video

import app.solina.player.data.model.VideoItem
import app.solina.player.data.model.VideoProgressEntry
import app.solina.player.data.model.VideoSubtitleStyle
import app.solina.player.data.model.groupVideosByFolder
import app.solina.player.presentation.navigation.VideoQueueStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VideoPlayerModelsAndQueueTest {

    private fun sampleVideo(
        id: Long = 1L,
        title: String = "Clip",
        path: String? = "/storage/emulated/0/Movies/Clip.mp4",
        width: Int = 1920,
        height: Int = 1080,
        durationMs: Long = 120_000L
    ) = VideoItem(
        id = id,
        title = title,
        durationMs = durationMs,
        dateAddedSec = 100L + id,
        dateModifiedSec = 100L + id,
        mimeType = "video/mp4",
        contentUriString = "content://media/external/video/media/$id",
        path = path,
        width = width,
        height = height,
        sizeBytes = 1024L * 1024L
    )

    @Test
    fun `progress entry computes clamped fraction safely`() {
        assertEquals(0f, VideoProgressEntry("u", 5000L, 0L).fraction)
        assertEquals(0.5f, VideoProgressEntry("u", 60_000L, 120_000L).fraction)
        assertEquals(1f, VideoProgressEntry("u", 200_000L, 120_000L).fraction)
        assertEquals(0f, VideoProgressEntry("u", -50L, 120_000L).fraction)
    }

    @Test
    fun `video item displayTitle and orientation flags resolve accurately`() {
        val portrait = sampleVideo(title = "", path = "/Movies/reel.mp4", width = 1080, height = 1920)
        assertEquals("reel.mp4", portrait.displayTitle)
        assertTrue(portrait.isPortrait)

        val landscape = sampleVideo(title = "Documentary", width = 1920, height = 1080)
        assertEquals("Documentary", landscape.displayTitle)
        assertFalse(landscape.isPortrait)
    }

    @Test
    fun `groupVideosByFolder groups items by parent folder`() {
        val v1 = sampleVideo(id = 1, path = "/storage/emulated/0/Movies/a.mp4", durationMs = 10_000L)
        val v2 = sampleVideo(id = 2, path = "/storage/emulated/0/Movies/b.mp4", durationMs = 20_000L)
        val v3 = sampleVideo(id = 3, path = "/storage/emulated/0/Camera/c.mp4", durationMs = 5_000L)

        val folders = groupVideosByFolder(listOf(v1, v2, v3))
        assertEquals(2, folders.size)
        val movies = folders.first { it.name == "Movies" }
        assertEquals(2, movies.videos.size)
        assertEquals(30_000L, movies.totalDurationMs)
    }

    @Test
    fun `VideoQueueStore stages retrieves and evicts oldest queues beyond capacity`() {
        assertNull(VideoQueueStore.stage(emptyList()))
        assertTrue(VideoQueueStore.get(null).isEmpty())

        val keys = (1..20).map { i ->
            val key = VideoQueueStore.stage(listOf("uri/$i"))
            assertNotNull(key)
            key!!
        }
        // Oldest 4 should have been evicted (capacity = 16)
        assertTrue(VideoQueueStore.get(keys.first()).isEmpty())
        assertEquals(listOf("uri/20"), VideoQueueStore.get(keys.last()))
    }

    @Test
    fun `VideoSubtitleStyle defaults provide crisp outline and translucent background`() {
        val style = VideoSubtitleStyle()
        assertEquals(20f, style.textSizeSp)
        assertEquals(1, style.backgroundMode)
        assertEquals(1, style.edgeType)
    }
}
