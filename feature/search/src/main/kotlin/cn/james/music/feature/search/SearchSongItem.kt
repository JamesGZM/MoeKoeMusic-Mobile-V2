package cn.james.music.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.model.online.Song

@Composable
internal fun SearchSongItem(
    song: Song,
    artworkRes: Int?,
    badge: SearchSongBadge?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unknownArtist = stringResource(R.string.search_unknown_artist)
    MoeSongRow(
        title = song.title,
        subtitle = buildString {
            append(song.artistName.ifBlank { unknownArtist })
            song.albumTitle?.takeIf(String::isNotBlank)?.let { append("  ·  ").append(it) }
        },
        onClick = onClick,
        modifier = modifier.padding(start = 14.dp),
        minimumHeight = 64.dp,
        verticalContentPadding = 6.dp,
        titleLeading = {
            if (isPlaying) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp).size(18.dp),
                )
            }
        },
        artwork = {
            if (artworkRes != null) {
                Image(painterResource(artworkRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                AsyncImage(song.artworkUrl, "${song.title} 封面", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        },
        badges = {
            badge?.let {
                SearchBadge(
                    if (it == SearchSongBadge.Mv) stringResource(R.string.search_mv) else stringResource(R.string.search_quality),
                    if (it == SearchSongBadge.Mv) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailing = {
            IconButton(onClick = onMore) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.search_more_song, song.title), modifier = Modifier.size(20.dp))
            }
        },
    )
}

@Composable
internal fun SearchBadge(
    label: String,
    color: Color,
) {
    Surface(shape = RoundedCornerShape(6.dp), color = Color.Transparent, border = BorderStroke(1.dp, color)) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}
