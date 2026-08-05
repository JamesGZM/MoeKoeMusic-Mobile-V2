package cn.james.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.playback.PlaybackState

@Composable
internal fun MoeKoeMiniPlayer(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onQueue: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Text(artist, maxLines = 1, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(
            onClick = onToggle,
            modifier = Modifier.semantics { contentDescription = if (isPlaying) "暂停" else "播放" },
        ) {
            Text(if (isPlaying) "暂停" else "播放")
        }
        TextButton(onClick = onQueue) { Text("队列") }
    }
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("播放队列", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onClear) { Text("清空") }
        }
        LazyColumn {
            items(state.queue.size, key = { state.queue[it].id }) { index ->
                val item = state.queue[index]
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onPlayAt(index) }.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (index == state.currentIndex) "♪" else "", Modifier.size(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title)
                        Text(item.artist, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { onRemove(index) }) { Text("移除") }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
