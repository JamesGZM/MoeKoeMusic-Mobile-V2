package cn.james.music.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.action.MoeTextButton
import cn.james.music.core.model.online.Song

@Composable
internal fun SearchResults(
    state: SearchUiState,
    onLoadMore: () -> Unit,
    onPlay: (Song) -> Unit,
    onFollowArtist: () -> Unit,
    onViewAllSongs: () -> Unit,
    onViewAllCollections: () -> Unit,
    onCollection: (String) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    val showArtist = state.selectedCategory in listOf(SearchCategory.Overview, SearchCategory.Artists) && state.artist != null
    val showSongs = state.selectedCategory in listOf(SearchCategory.Overview, SearchCategory.Songs) && state.songs.isNotEmpty()
    val showCollections = state.selectedCategory in listOf(SearchCategory.Overview, SearchCategory.Playlists, SearchCategory.Albums) && state.collections.isNotEmpty()
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(SEARCH_RESULTS_TAG),
        contentPadding = PaddingValues(top = 14.dp, bottom = 16.dp),
    ) {
        state.artist?.takeIf { showArtist }?.let { artist ->
            item(key = "artist") { SearchArtistHero(artist, onFollowArtist) }
        }
        if (showSongs) {
            item(key = "songs-title") {
                SearchSectionTitle(
                    title = stringResource(R.string.search_songs),
                    onViewAll = onViewAllSongs,
                    modifier = Modifier.searchLayoutProbe(SEARCH_PROBE_SONGS_HEADER),
                )
            }
            itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                SearchSongItem(
                    song = song,
                    artworkRes = state.songArtwork[song.id],
                    badge = state.songBadges[song.id],
                    isPlaying = song.id == state.playingSongId,
                    onClick = { onPlay(song) },
                    onMore = { onSongMore(song) },
                    modifier =
                        when (index) {
                            0 -> Modifier.searchLayoutProbe(SEARCH_PROBE_FIRST_SONG)
                            3 -> Modifier.searchLayoutProbe(SEARCH_PROBE_FOURTH_SONG)
                            else -> Modifier
                        },
                )
                HorizontalDivider(Modifier.padding(start = 82.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            if (state.hasMore || state.loadingMore || state.error != null) {
                item(key = "load-more") {
                    Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                        when {
                            state.loadingMore -> CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                            state.error != null -> MoeTextButton(onClick = onLoadMore) { Text(stringResource(R.string.search_load_failed)) }
                            else -> MoeTextButton(onClick = onLoadMore) { Text(stringResource(R.string.search_load_more)) }
                        }
                    }
                }
            }
        }
        if (showCollections) {
            item(key = "collections-title") {
                SearchSectionTitle(
                    title = stringResource(R.string.search_collections),
                    onViewAll = onViewAllCollections,
                    modifier = Modifier.searchLayoutProbe(SEARCH_PROBE_COLLECTIONS_HEADER),
                )
            }
            item(key = "collections") {
                LazyRow(
                    modifier = Modifier.searchLayoutProbe(SEARCH_PROBE_COLLECTIONS_ROW),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                ) {
                    items(state.collections, key = SearchCollectionUi::id) { item ->
                        SearchCollectionCard(item, onClick = { onCollection(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(
    title: String,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, top = 2.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        MoeTextButton(onClick = onViewAll) {
            Text(stringResource(R.string.search_view_all), style = MaterialTheme.typography.labelMedium)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}
