package cn.james.music.feature.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.designsystem.component.MoeSongRowStyle

@Composable
internal fun QueueItem(
    queueItem: PlayerQueueItemUi,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoeSongRow(
        title = queueItem.title,
        subtitle = queueItem.artist,
        metadata = queueItem.durationLabel,
        onClick = onClick,
        style = if (isCurrent) MoeSongRowStyle.QueueCurrent else MoeSongRowStyle.Queue,
        isPlaying = isCurrent,
        showPlayingIndicator = false,
        highlightTitleWhenPlaying = false,
        shape = RoundedCornerShape(8.dp),
        containerColor = if (isCurrent) QueueCurrentContainer else Color.Transparent,
        titleColor = QueueOnSurface,
        subtitleColor = QueueOnSurfaceVariant,
        metadataColor = QueueOnSurfaceVariant,
        playingColor = QueueAccent,
        titleFontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .then(modifier),
        artwork = { QueueArtwork(queueItem) },
        artworkTrailing = {
            if (isCurrent) {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = stringResource(R.string.player_queue_current),
                    modifier = Modifier.padding(start = 9.dp).size(24.dp),
                    tint = QueueAccent,
                )
            }
        },
        trailing = {
            Icon(
                Icons.Default.DragIndicator,
                contentDescription = stringResource(R.string.player_queue_reorder_unavailable),
                modifier = Modifier.padding(start = 9.dp).size(22.dp),
                tint = QueueOnSurfaceVariant,
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, QueueOutline),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.player_queue_remove, queueItem.title),
                            modifier = Modifier.size(18.dp),
                            tint = QueueOnSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun QueueArtwork(
    item: PlayerQueueItemUi,
) {
    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)).background(QueueArtworkPlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = QueueOnSurfaceVariant)
        PlayerArtworkRenderer(model = item.artwork, contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}
