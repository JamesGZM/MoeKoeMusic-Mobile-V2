package cn.james.music.feature.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeMediaBadge
import cn.james.music.core.designsystem.component.MoeMediaBadgeTone
import cn.james.music.core.designsystem.component.MoeSongRow

@Composable
internal fun PlaylistTrackItem(
    index: Int,
    song: PlaylistTrackUi,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MoeSongRow(
        title = song.title,
        subtitle = song.artist,
        metadata = song.durationLabel,
        onClick = onClick,
        minimumHeight = if (isCurrent) 58.dp else 57.dp,
        largeTextMinimumHeight = 88.dp,
        artworkSize = 44.dp,
        artworkShape = RoundedCornerShape(8.dp),
        decorateArtwork = false,
        contentStartPadding = 4.dp,
        contentEndPadding = 0.dp,
        verticalContentPadding = 0.dp,
        textHorizontalPadding = 12.dp,
        subtitleTopPadding = 2.dp,
        metadataStartPadding = 8.dp,
        metadataEndPadding = 0.dp,
        metadataInSubtitleOnLargeText = false,
        isPlaying = isCurrent,
        showPlayingIndicator = false,
        highlightTitleWhenPlaying = false,
        shape = RoundedCornerShape(12.dp),
        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f) else Color.Transparent,
        titleStyle = MaterialTheme.typography.titleSmall,
        subtitleStyle = MaterialTheme.typography.bodySmall,
        metadataStyle = MaterialTheme.typography.bodySmall,
        titleFontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
        inlineTitleContent = false,
        modifier = modifier.padding(horizontal = 8.dp).fillMaxWidth(),
        leading = {
            if (isCurrent) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            } else {
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = "${index + 1}",
                modifier = Modifier.width(30.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        },
        artwork = {
            Image(
                painter = painterResource(song.artworkRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )
        },
        contentTrailing = {
            song.badge?.let { badge ->
                MoeMediaBadge(
                    text = if (badge == PlaylistTrackBadgeUi.HighQuality) "HQ" else "MV",
                    tone = if (badge == PlaylistTrackBadgeUi.HighQuality) MoeMediaBadgeTone.Primary else MoeMediaBadgeTone.Error,
                )
            }
        },
        trailing = {
            IconButton(onClick = onMore, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.playlist_detail_track_more, song.title))
            }
        },
    )
}
