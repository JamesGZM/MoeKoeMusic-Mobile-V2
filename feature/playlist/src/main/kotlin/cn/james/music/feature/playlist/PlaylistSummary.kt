package cn.james.music.feature.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.MoeMediaBadge
import cn.james.music.core.designsystem.component.MoeMediaBadgeTone

@Composable
internal fun PlaylistSummary(playlist: PlaylistDetailUi) {
    val reflow = LocalDensity.current.fontScale >= 1.3f
    if (reflow) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .playlistDetailLayoutProbe(PLAYLIST_PROBE_SUMMARY),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlaylistCover(playlist, Modifier.size(184.dp))
            PlaylistSummaryText(playlist, Modifier.fillMaxWidth().padding(top = 16.dp))
        }
    } else {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 8.dp)
                    .playlistDetailLayoutProbe(PLAYLIST_PROBE_SUMMARY),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistCover(playlist, Modifier.width(150.dp).height(164.dp))
            PlaylistSummaryText(playlist, Modifier.weight(1f).padding(start = 16.dp))
        }
    }
}

@Composable
private fun PlaylistCover(
    playlist: PlaylistDetailUi,
    modifier: Modifier,
) {
    Image(
        painter = painterResource(playlist.artworkRes),
        contentDescription = playlist.title,
        modifier = modifier.clip(RoundedCornerShape(20.dp)).playlistDetailLayoutProbe(PLAYLIST_PROBE_COVER),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun PlaylistSummaryText(
    playlist: PlaylistDetailUi,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text(
            text = playlist.title,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(playlist.creatorAvatarRes),
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text(
                text = playlist.creatorName,
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (playlist.creatorVip) {
                MoeMediaBadge(
                    text = stringResource(R.string.playlist_detail_vip),
                    modifier = Modifier.padding(start = 8.dp),
                    tone = MoeMediaBadgeTone.Primary,
                )
            }
        }
        Text(
            text = stringResource(R.string.playlist_detail_statistics, playlist.songCount, playlist.durationLabel),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = playlist.description,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (LocalDensity.current.fontScale >= 1.3f) 4 else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
