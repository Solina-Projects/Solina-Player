package app.solina.player.presentation.utils

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.compose.runtime.staticCompositionLocalOf
import app.solina.player.R

/**
 * Provides current Picture-in-Picture mode state to composables so
 * [app.solina.player.presentation.screens.VideoPlayerScreen] can hide
 * every overlay, slider, and dialog when shrunk into PiP.
 */
val LocalIsInPictureInPicture = staticCompositionLocalOf { false }

object VideoPipContract {
    const val ACTION_PIP_CONTROL = "app.solina.player.ACTION_VIDEO_PIP_CONTROL"
    const val EXTRA_CONTROL_TYPE = "control_type"
    const val CONTROL_PREV = 1
    const val CONTROL_PLAY_PAUSE = 2
    const val CONTROL_NEXT = 3

    private val minRational = Rational(100, 239)
    private val maxRational = Rational(239, 100)

    fun clampAspectRatio(width: Int, height: Int): Rational {
        if (width <= 0 || height <= 0) return Rational(16, 9)
        val raw = Rational(width, height)
        val value = raw.toFloat()
        return when {
            value < minRational.toFloat() -> minRational
            value > maxRational.toFloat() -> maxRational
            else -> raw
        }
    }

    fun buildParams(
        context: Context,
        isPlaying: Boolean,
        videoWidth: Int,
        videoHeight: Int,
        autoEnter: Boolean
    ): PictureInPictureParams {
        val ratio = clampAspectRatio(videoWidth, videoHeight)
        val actions = listOf(
            createAction(
                context = context,
                requestCode = 2101,
                controlType = CONTROL_PREV,
                iconRes = R.drawable.rounded_skip_previous_24,
                title = context.getString(R.string.video_previous_video)
            ),
            createAction(
                context = context,
                requestCode = 2102,
                controlType = CONTROL_PLAY_PAUSE,
                iconRes = if (isPlaying) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24,
                title = context.getString(R.string.video_pip_play_pause)
            ),
            createAction(
                context = context,
                requestCode = 2103,
                controlType = CONTROL_NEXT,
                iconRes = R.drawable.rounded_skip_next_24,
                title = context.getString(R.string.video_next_video)
            )
        )
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(ratio)
            .setActions(actions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun createAction(
        context: Context,
        requestCode: Int,
        controlType: Int,
        iconRes: Int,
        title: String
    ): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_CONTROL_TYPE, controlType)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(context, iconRes)
        return RemoteAction(icon, title, title, pendingIntent)
    }
}
