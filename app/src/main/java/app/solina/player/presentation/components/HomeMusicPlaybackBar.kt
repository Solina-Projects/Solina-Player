package app.solina.player.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solina.player.presentation.viewmodel.PlayerViewModel

/**
 * Fixed full-width music playback bar for Home (Home feedback,
 * 2026-11-16).
 *
 * The floating music mini player is disabled on Home now, so Home gets
 * this bar in the flow instead: it sits directly under the
 * "Recently added music" row, spans the full Home width in portrait
 * and landscape, and stays put (it is a list item, not an overlay), so
 * the user can continue music playback from Home.
 *
 * It is the same design as the floating music mini player — the same
 * [MiniPlayerContentInternal] (circular art, title/artist, live EQ
 * bars, white prev / primary play-pause / white next circles) inside
 * the same 64 dp stadium pill. The pill uses the app theme scheme; the
 * floating bar uses the same roles from its album-art scheme, which
 * falls back to this scheme whenever a song has no artwork.
 *
 * Hidden when nothing has ever been played (no current song). Tapping
 * the bar opens the full player; the transport buttons work directly.
 */
@Composable
fun HomeMusicPlaybackBar(
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val currentSong = stablePlayerState.currentSong ?: return

    CompositionLocalProvider(LocalMaterialTheme provides MaterialTheme.colorScheme) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .clickable { playerViewModel.expandPlayerSheet() },
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            MiniPlayerContentInternal(
                song = currentSong,
                isPlaying = stablePlayerState.isPlaying,
                isOutputConnecting = false,
                isPreparingPlayback = stablePlayerState.isBuffering,
                onPlayPause = { playerViewModel.playPause() },
                onPrevious = { playerViewModel.previousSong() },
                onNext = { playerViewModel.nextSong() },
                modifier = Modifier.fillMaxWidth(),
                canScroll = stablePlayerState.isPlaying
            )
        }
    }
}
