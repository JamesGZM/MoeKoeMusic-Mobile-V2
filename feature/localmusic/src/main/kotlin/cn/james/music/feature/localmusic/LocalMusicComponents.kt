package cn.james.music.feature.localmusic

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicSort
import coil3.compose.AsyncImage
import java.io.File

internal fun filterAndSortMusic(
    music: List<LocalMusic>,
    query: String,
    sort: LocalMusicSort,
): List<LocalMusic> =
    music
        .filter { item ->
            query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.artist.contains(query, ignoreCase = true)
        }.let { filtered ->
            when (sort) {
                LocalMusicSort.Newest -> filtered.sortedByDescending(LocalMusic::importedAtEpochMs)
                LocalMusicSort.Title -> filtered.sortedBy(LocalMusic::title)
                LocalMusicSort.Artist -> filtered.sortedBy(LocalMusic::artist)
                LocalMusicSort.Duration -> filtered.sortedByDescending(LocalMusic::durationMs)
            }
        }

internal val LocalMusicSort.labelRes: Int
    @StringRes
    get() =
        when (this) {
            LocalMusicSort.Newest -> R.string.local_music_sort_newest
            LocalMusicSort.Title -> R.string.local_music_sort_title
            LocalMusicSort.Artist -> R.string.local_music_sort_artist
            LocalMusicSort.Duration -> R.string.local_music_sort_duration
        }

@Composable
internal fun LocalMusicMessage(
    title: String,
    message: String,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Column(
        modifier = modifier.padding(MoeKoeTheme.spacing.large),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            text = message,
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.compact),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun LocalMusicRow(
    music: LocalMusic,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    MoeSongRow(
        title = music.title,
        subtitle = music.artist,
        metadata = formatDuration(music.durationMs),
        onClick = onClick,
        modifier = modifier,
        artworkSize = 60.dp,
        artwork = {
            AsyncImage(
                model = music.artworkKey?.let { File(context.filesDir, it) },
                contentDescription = stringResource(R.string.local_music_artwork_description, music.title),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        },
        trailing = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.local_music_more, music.title),
                )
            }
        },
    )
}

internal fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1_000
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "${totalSeconds / 60}:$seconds"
}
