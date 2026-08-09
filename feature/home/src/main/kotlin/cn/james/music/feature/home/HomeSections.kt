package cn.james.music.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.MoeMediaBadge
import cn.james.music.core.designsystem.component.MoeMediaBadgeTone
import cn.james.music.core.designsystem.component.MoeSongMoreAction
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.designsystem.component.MoeSongRowStyle

@Composable
internal fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 33.dp).padding(start = 14.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 20.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(stringResource(R.string.home_more), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun HomeSongRow(
    song: HomeSongUi,
    onPlay: (HomeSongUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        MoeSongRow(
            title = song.title,
            subtitle = song.artistName.ifBlank { stringResource(R.string.home_unknown_artist) },
            onClick = { onPlay(song) },
            style = MoeSongRowStyle.Compact,
            artwork = {
                HomeArtwork(
                    title = song.title,
                    artworkUrl = song.artworkUrl,
                    previewArtworkRes = song.previewArtworkRes,
                )
            },
            trailing = {
                song.previewBadge?.let { badge ->
                    MoeMediaBadge(
                        text = badge,
                        tone = if (song.previewBadgeIsError) MoeMediaBadgeTone.Error else MoeMediaBadgeTone.Primary,
                    )
                }
                MoeSongMoreAction(contentDescription = null, onClick = null)
            },
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 58.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        )
    }
}

@Composable
internal fun HomePlaylistGrid(
    playlists: List<HomePlaylistUi>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        playlists.forEach { playlist ->
            HomePlaylistCard(playlist = playlist, modifier = Modifier.weight(1f))
        }
        repeat((4 - playlists.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun HomePlaylistCard(
    playlist: HomePlaylistUi,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        HomeArtwork(
            title = playlist.title,
            artworkUrl = playlist.artworkUrl,
            previewArtworkRes = playlist.previewArtworkRes,
            modifier = Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(11.dp)),
        )
        Text(
            playlist.title,
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val playCount = playlist.playCount
        val subtitle = playlist.previewSubtitle ?: if (playCount == null) null else formatPlayCount(playCount)
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeArtwork(
    title: String,
    artworkUrl: String?,
    @DrawableRes previewArtworkRes: Int?,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val description = stringResource(R.string.home_artwork_description, title)
    when {
        previewArtworkRes != null ->
            Image(
                painter = painterResource(previewArtworkRes),
                contentDescription = description,
                modifier = modifier,
                contentScale = ContentScale.Crop,
            )
        artworkUrl != null ->
            AsyncImage(
                model = artworkUrl,
                contentDescription = description,
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale = ContentScale.Crop,
            )
        else ->
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = description, tint = MaterialTheme.colorScheme.primary)
            }
    }
}

@Composable
private fun formatPlayCount(count: Long): String =
    when {
        count >= 100_000_000 -> stringResource(R.string.home_play_count_hundred_million, count / 100_000_000)
        count >= 10_000 -> stringResource(R.string.home_play_count_ten_thousand, count / 10_000)
        else -> stringResource(R.string.home_play_count_plain, count)
    }
