package cn.james.music.feature.playlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.component.action.MoeTextButton

@Composable
internal fun PlaylistActions(
    isFavorite: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
) {
    val reflow = LocalDensity.current.fontScale >= 1.3f
    if (reflow) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .playlistDetailLayoutProbe(PLAYLIST_PROBE_ACTIONS),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlaylistPlayButton(onPlayAll, Modifier.weight(1f))
                PlaylistShuffleButton(onShuffle, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                PlaylistCircleAction(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.playlist_detail_favorite),
                    onClick = onFavorite,
                )
                PlaylistCircleAction(Icons.Default.Download, stringResource(R.string.playlist_detail_download), onDownload)
                PlaylistCircleAction(Icons.Default.MoreHoriz, stringResource(R.string.playlist_detail_actions_more), onMore)
            }
        }
        return
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(start = 14.dp, top = 6.dp, end = 14.dp)
                .playlistDetailLayoutProbe(PLAYLIST_PROBE_ACTIONS),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistPlayButton(onPlayAll, Modifier.weight(1f))
        PlaylistShuffleButton(onShuffle, Modifier.weight(1f))
        PlaylistCircleAction(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = stringResource(R.string.playlist_detail_favorite),
            onClick = onFavorite,
        )
        PlaylistCircleAction(Icons.Default.Download, stringResource(R.string.playlist_detail_download), onDownload)
        PlaylistCircleAction(Icons.Default.MoreHoriz, stringResource(R.string.playlist_detail_actions_more), onMore)
    }
}

@Composable
private fun PlaylistPlayButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.height(48.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.playlist_detail_play_all), modifier = Modifier.padding(start = 4.dp), fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PlaylistShuffleButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.height(48.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.playlist_detail_shuffle), modifier = Modifier.padding(start = 4.dp), fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PlaylistCircleAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ) {}
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(21.dp))
    }
}

@Composable
internal fun PlaylistSongsHeader(onSort: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 18.dp, end = 12.dp)
                .playlistDetailLayoutProbe(PLAYLIST_PROBE_SONGS_HEADER),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.playlist_detail_songs),
            modifier = Modifier.weight(1f),
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        MoeTextButton(onClick = onSort, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.playlist_detail_sort),
                modifier = Modifier.padding(start = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
