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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import java.io.File

@Composable
internal fun QueueItem(
    queueItem: PlayerQueueItemUi,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    MoeSongRow(
        title = queueItem.item.title,
        subtitle = queueItem.item.artist,
        metadata = queueItem.durationLabel,
        onClick = onClick,
        minimumHeight = if (isCurrent) 58.dp else 56.dp,
        largeTextMinimumHeight = if (isCurrent) 58.dp else 56.dp,
        fixedHeight = if (isCurrent) 58.dp else 56.dp,
        artworkSize = 44.dp,
        artworkShape = RoundedCornerShape(7.dp),
        decorateArtwork = false,
        contentStartPadding = 9.dp,
        contentEndPadding = 0.dp,
        verticalContentPadding = 0.dp,
        textStartPadding = if (isCurrent) 9.dp else 15.dp,
        textEndPadding = 8.dp,
        metadataEndPadding = 0.dp,
        metadataInSubtitleOnLargeText = false,
        isPlaying = isCurrent,
        showPlayingIndicator = false,
        highlightTitleWhenPlaying = false,
        shape = RoundedCornerShape(8.dp),
        containerColor = if (isCurrent) QueueCurrentContainer else Color.Transparent,
        titleColor = QueueOnSurface,
        subtitleColor = QueueOnSurfaceVariant,
        metadataColor = QueueOnSurfaceVariant,
        playingColor = QueueAccent,
        titleStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 19.sp),
        subtitleStyle = LocalTextStyle.current.copy(fontSize = 12.sp, lineHeight = 16.sp),
        metadataStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
        titleFontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
        inlineTitleContent = false,
        modifier =
            Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .then(modifier),
        artwork = { QueueArtwork(queueItem.item, artworkContent) },
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
                            contentDescription = stringResource(R.string.player_queue_remove, queueItem.item.title),
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
    item: PlaybackItem,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
) {
    val context = LocalContext.current
    val model =
        when (val artwork = item.artwork) {
            is PlaybackArtwork.Remote -> artwork.value
            is PlaybackArtwork.AppFile -> File(context.filesDir, artwork.value)
            null -> null
        }
    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)).background(QueueArtworkPlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = QueueOnSurfaceVariant)
        AsyncImage(model = model, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        artworkContent?.invoke(item)
    }
}
