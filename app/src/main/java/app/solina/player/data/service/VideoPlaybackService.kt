package app.solina.player.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.solina.player.MainActivity
import app.solina.player.R
import app.solina.player.presentation.viewmodel.VideoPlaybackHolder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground MediaSessionService for video background playback.
 * Keeps background video audio alive with notification & lockscreen media controls,
 * and launches MainActivity straight back into the active video player on tap.
 */
@UnstableApi
@AndroidEntryPoint
class VideoPlaybackService : MediaSessionService() {

    @Inject
    lateinit var videoPlaybackHolder: VideoPlaybackHolder

    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "solina_video_playback_channel"
        const val NOTIFICATION_ID = 202
        const val EXTRA_OPEN_VIDEO_PLAYER = "app.solina.player.extra.OPEN_VIDEO_PLAYER"
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        val player = videoPlaybackHolder.playerOrNull
        if (player != null) {
            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_VIDEO_PLAYER, true)
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            mediaSession = MediaSession.Builder(this, player)
                .setId("SolinaVideoSession")
                .setSessionActivity(pendingIntent)
                .build()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (mediaSession == null) {
            // If started with startForegroundService while player is already null,
            // post a brief foreground notification before stopping to avoid system crash.
            runCatching { startForeground(NOTIFICATION_ID, buildFallbackNotification()) }
            stopSelf()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!videoPlaybackHolder.backgroundPlaybackEnabled.value ||
            videoPlaybackHolder.playerOrNull?.isPlaying != true
        ) {
            videoPlaybackHolder.pause()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.video_background_play),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildFallbackNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.alt_video)
            .setContentTitle(getString(R.string.video_player_title))
            .setOngoing(false)
            .build()
    }
}
