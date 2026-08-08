package cn.james.music.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.model.playback.PlaybackMode

@Composable
internal fun QueueEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp), tint = QueueAccent)
        Text(
            stringResource(R.string.player_queue_empty),
            modifier = Modifier.padding(top = 12.dp),
            color = QueueOnSurfaceVariant,
        )
    }
}

@Composable
internal fun QueueDismissHint(onDismiss: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(29.dp)
                .playerLayoutProbe(PLAYER_PROBE_QUEUE_DISMISS)
                .clickable(role = Role.Button, onClick = onDismiss),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = QueueOnSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.player_queue_swipe_to_close),
            modifier = Modifier.padding(start = 8.dp),
            color = QueueOnSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

internal fun queueModeIcon(mode: PlaybackMode): ImageVector =
    when (mode) {
        PlaybackMode.Shuffle -> Icons.Default.Shuffle
        PlaybackMode.RepeatOne -> Icons.Default.RepeatOne
        PlaybackMode.Sequential, PlaybackMode.RepeatAll -> Icons.Default.Repeat
    }

internal fun queueModeLabel(mode: PlaybackMode): Int =
    when (mode) {
        PlaybackMode.Sequential -> R.string.player_mode_sequence
        PlaybackMode.RepeatAll -> R.string.player_mode_repeat_all
        PlaybackMode.RepeatOne -> R.string.player_mode_repeat_one
        PlaybackMode.Shuffle -> R.string.player_mode_shuffle
    }

internal val QueueContainer = Color(0xFF222538)
internal val QueueCurrentContainer = Color(0xFF2E304F)
internal val QueueAccent = Color(0xFFA49BFF)
internal val QueueHandle = Color(0xFFAAA9CE)
internal val QueueOnSurface = Color(0xFFF5F2FF)
internal val QueueOnSurfaceVariant = Color(0xFFB8B8CF)
internal val QueueOutline = Color(0xFF505872)
internal val QueueArtworkPlaceholder = Color(0xFF252D48)
internal val QueueDesignWidth = 390.dp
internal val QueueContentHeight = 469.dp
