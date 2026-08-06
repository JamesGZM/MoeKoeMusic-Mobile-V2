package cn.james.music

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.MoeMiniPlayer
import cn.james.music.core.designsystem.component.MoeQueueRow
import cn.james.music.core.designsystem.component.MoeSectionHeader
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.playback.PlaybackState
import java.io.File

@Composable
internal fun MoeKoeMiniPlayer(
    item: PlaybackItem,
    isPlaying: Boolean,
    progress: Float,
    onToggle: () -> Unit,
    onQueue: () -> Unit,
    onOpenPlayer: () -> Unit,
) {
    MoeMiniPlayer(
        title = item.title,
        artist = item.artist,
        isPlaying = isPlaying,
        progress = progress,
        playContentDescription = "播放",
        pauseContentDescription = "暂停",
        queueContentDescription = "播放队列",
        onTogglePlayback = onToggle,
        onOpenQueue = onQueue,
        onOpenPlayer = onOpenPlayer,
        artwork = { PlaybackArtworkImage(item) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoeKoeQueueSheet(
    state: PlaybackState,
    onDismiss: () -> Unit,
    onPlayAt: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        MoeSectionHeader(
            title = "播放队列",
            modifier = Modifier.padding(horizontal = 20.dp),
            action = { TextButton(onClick = onClear) { Text("清空") } },
        )
        LazyColumn {
            items(state.queue.size, key = { state.queue[it].id }) { index ->
                val item = state.queue[index]
                MoeQueueRow(
                    positionLabel = if (index == state.currentIndex) "♪" else "${index + 1}",
                    title = item.title,
                    subtitle = item.artist,
                    onClick = { onPlayAt(index) },
                    removeContentDescription = "从队列移除 ${item.title}",
                    onRemove = { onRemove(index) },
                    artwork = { PlaybackArtworkImage(item) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    isPlaying = index == state.currentIndex,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PlaybackArtworkImage(item: PlaybackItem) {
    val context = LocalContext.current
    val model =
        when (val artwork = item.artwork) {
            is PlaybackArtwork.Remote -> artwork.value
            is PlaybackArtwork.AppFile -> File(context.filesDir, artwork.value)
            null -> null
        }
    AsyncImage(
        model = model,
        contentDescription = "${item.title} 封面",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
