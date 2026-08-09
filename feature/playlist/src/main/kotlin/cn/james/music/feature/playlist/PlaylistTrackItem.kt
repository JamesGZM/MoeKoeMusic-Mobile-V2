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
import androidx.compose.material3.Icon
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
import cn.james.music.core.designsystem.component.MoeSongMoreAction
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.designsystem.component.MoeSongRowStyle

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
        style = if (isCurrent) MoeSongRowStyle.PlaylistCurrent else MoeSongRowStyle.Playlist,
        isPlaying = isCurrent,
        showPlayingIndicator = false,
        highlightTitleWhenPlaying = false,
        shape = RoundedCornerShape(12.dp),
        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f) else Color.Transparent,
        titleFontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
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
            MoeSongMoreAction(
                contentDescription = stringResource(R.string.playlist_detail_track_more, song.title),
                onClick = onMore,
            )
        },
    )
}
