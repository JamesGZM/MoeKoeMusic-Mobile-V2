package cn.james.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import java.io.File

@Composable
internal fun PlayerCoverPage(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlaybackItem,
    activePage: Int,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(23.dp))
        PlayerArtwork(
            item = item,
            artworkContent = artworkContent,
            modifier = Modifier.fillMaxWidth().playerLayoutProbe(PLAYER_PROBE_ARTWORK),
        )
        PlayerPageIndicator(
            activePage = activePage,
            modifier = Modifier.playerLayoutProbe(PLAYER_PROBE_INDICATOR),
        )
        Spacer(Modifier.height(6.dp))
        PlayerControls(
            state = state,
            progress = progress,
            item = item,
            onTogglePlayback = onTogglePlayback,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onChangeMode = onChangeMode,
            onOpenQueue = onOpenQueue,
            onFavorite = onFavorite,
            onDownload = onDownload,
            onAddToPlaylist = onAddToPlaylist,
            onShare = onShare,
            showSecondaryActions = true,
            compactLayout = false,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PlayerArtwork(
    item: PlaybackItem,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val model =
        when (val artwork = item.artwork) {
            is PlaybackArtwork.Remote -> artwork.value
            is PlaybackArtwork.AppFile -> File(context.filesDir, artwork.value)
            null -> null
        }
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 28.5.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val artworkSize = maxWidth.coerceAtMost(333.dp)
        Box(
            modifier =
                Modifier
                    .size(artworkSize)
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(68.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.player_artwork, item.title),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            artworkContent?.invoke(item)
        }
    }
}
