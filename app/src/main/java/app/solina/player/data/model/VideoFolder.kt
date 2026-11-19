package app.solina.player.data.model

/**
 * A device folder containing local videos, grouped by parent folder path.
 * Shared by the Video Folders screen and the Video Search browse grid so
 * both group videos the same way.
 */
data class VideoFolder(
    val path: String,
    val name: String,
    val videos: List<VideoItem>
) {
    val totalDurationMs: Long get() = videos.sumOf { it.durationMs }
}

fun VideoItem.parentFolderPath(): String =
    path?.substringBeforeLast('/') ?: ""

fun groupVideosByFolder(videos: List<VideoItem>): List<VideoFolder> =
    videos.groupBy { it.parentFolderPath() }
        .map { (folderPath, folderVideos) ->
            VideoFolder(
                path = folderPath,
                name = folderPath.substringAfterLast('/').ifBlank { "" },
                videos = folderVideos
            )
        }
