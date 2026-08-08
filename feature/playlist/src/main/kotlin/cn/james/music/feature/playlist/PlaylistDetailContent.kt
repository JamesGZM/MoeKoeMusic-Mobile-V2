package cn.james.music.feature.playlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun PlaylistDetailContent(
    playlist: PlaylistDetailUi,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
    onSort: () -> Unit,
    onTrack: (Int) -> Unit,
    onTrackMore: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(PLAYLIST_DETAIL_CONTENT_TAG),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item(key = "summary") { PlaylistSummary(playlist) }
        item(key = "actions") {
            PlaylistActions(
                isFavorite = playlist.isFavorite,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle,
                onFavorite = onFavorite,
                onDownload = onDownload,
                onMore = onMore,
            )
        }
        item(key = "songs-header") { PlaylistSongsHeader(onSort) }
        itemsIndexed(
            items = playlist.songs,
            key = { _, song -> song.id },
            contentType = { _, _ -> "playlist-track" },
        ) { index, song ->
            PlaylistTrackItem(
                index = index,
                song = song,
                isCurrent = song.id == playlist.currentTrackId,
                onClick = { onTrack(index) },
                onMore = { onTrackMore(index) },
                modifier =
                    when {
                        index == 0 -> Modifier.playlistDetailLayoutProbe(PLAYLIST_PROBE_FIRST_TRACK)
                        song.id == playlist.currentTrackId -> Modifier.playlistDetailLayoutProbe(PLAYLIST_PROBE_CURRENT_TRACK)
                        else -> Modifier
                    },
            )
        }
    }
}
