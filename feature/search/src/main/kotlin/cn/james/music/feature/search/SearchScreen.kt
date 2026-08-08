package cn.james.music.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.designsystem.component.navigation.MoeSearchTopBar
import cn.james.music.core.model.online.SearchError
import cn.james.music.core.model.online.Song

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (Song) -> Unit,
    onCategorySelected: (SearchCategory) -> Unit = {},
    onVoice: () -> Unit = {},
    onFollowArtist: () -> Unit = {},
    onViewAllSongs: () -> Unit = {},
    onViewAllCollections: () -> Unit = {},
    onCollection: (String) -> Unit = {},
    onSongMore: (Song) -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        MoeSearchTopBar(
            query = state.query,
            placeholder = stringResource(R.string.search_placeholder),
            navigationContentDescription = stringResource(R.string.search_back),
            clearContentDescription = stringResource(R.string.search_clear),
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            onNavigateBack = onBack,
            trailingAction = {
                IconButton(onClick = onVoice) {
                    Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.search_voice))
                }
            },
        )
        SearchTabs(state.selectedCategory, onCategorySelected)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.fillMaxSize().widthIn(max = 520.dp)) {
                SearchBody(
                    state = state,
                    onSearch = onSearch,
                    onLoadMore = onLoadMore,
                    onPlay = onPlay,
                    onFollowArtist = onFollowArtist,
                    onViewAllSongs = onViewAllSongs,
                    onViewAllCollections = onViewAllCollections,
                    onCollection = onCollection,
                    onSongMore = onSongMore,
                )
            }
        }
    }
}

@Composable
private fun SearchTabs(
    selected: SearchCategory,
    onSelected: (SearchCategory) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).height(40.dp).padding(start = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        SearchCategory.entries.forEach { category ->
            val label = category.label()
            Column(
                modifier =
                    Modifier
                        .width(68.dp)
                        .height(40.dp)
                        .clickable { onSelected(category) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    label,
                    color = if (category == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (category == selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier
                        .width(32.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                ) {
                    if (category == selected) Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {}
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
}

@Composable
private fun SearchBody(
    state: SearchUiState,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (Song) -> Unit,
    onFollowArtist: () -> Unit,
    onViewAllSongs: () -> Unit,
    onViewAllCollections: () -> Unit,
    onCollection: (String) -> Unit,
    onSongMore: (Song) -> Unit,
) {
    when {
        state.loading -> SearchMessage(true, stringResource(R.string.search_loading_title), stringResource(R.string.search_loading_message))
        state.error != null && state.songs.isEmpty() ->
            SearchMessage(
                title = stringResource(R.string.search_error_title),
                message = state.error.message,
                action = stringResource(R.string.search_retry),
                onAction = onSearch,
            )
        !state.hasSearched -> SearchMessage(false, stringResource(R.string.search_idle_title), stringResource(R.string.search_idle_message))
        !state.selectedCategory.hasContent(state) ->
            SearchMessage(
                progress = false,
                title = if (state.songs.isEmpty()) stringResource(R.string.search_empty_title) else stringResource(R.string.search_category_empty_title),
                message = if (state.songs.isEmpty()) stringResource(R.string.search_empty_message) else stringResource(R.string.search_category_empty_message),
            )
        else ->
            SearchResults(
                state,
                onLoadMore,
                onPlay,
                onFollowArtist,
                onViewAllSongs,
                onViewAllCollections,
                onCollection,
                onSongMore,
            )
    }
}

@Composable
private fun SearchResults(
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
            item(key = "artist") { ArtistHero(artist, onFollowArtist) }
        }
        if (showSongs) {
            item(key = "songs-title") { SearchSectionTitle(stringResource(R.string.search_songs), true, onViewAllSongs) }
            items(state.songs, key = Song::id) { song ->
                SearchSongRow(
                    song = song,
                    artworkRes = state.songArtwork[song.id],
                    badge = state.songBadges[song.id],
                    isPlaying = song.id == state.playingSongId,
                    onClick = { onPlay(song) },
                    onMore = { onSongMore(song) },
                )
                HorizontalDivider(Modifier.padding(start = 82.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            if (state.hasMore || state.loadingMore || state.error != null) {
                item(key = "load-more") {
                    Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                        when {
                            state.loadingMore -> CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                            state.error != null -> TextButton(onClick = onLoadMore) { Text(stringResource(R.string.search_load_failed)) }
                            else -> TextButton(onClick = onLoadMore) { Text(stringResource(R.string.search_load_more)) }
                        }
                    }
                }
            }
        }
        if (showCollections) {
            item(key = "collections-title") {
                SearchSectionTitle(stringResource(R.string.search_collections), true, onViewAllCollections)
            }
            item(key = "collections") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
private fun ArtistHero(
    artist: SearchArtistUi,
    onFollow: () -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
    ) {
        if (largeText) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ArtistAvatar(artist)
                ArtistDetails(artist, Modifier.fillMaxWidth().padding(vertical = 10.dp))
                FollowButton(onFollow)
            }
        } else {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                ArtistAvatar(artist)
                ArtistDetails(artist, Modifier.weight(1f).padding(horizontal = 14.dp))
                FollowButton(onFollow)
            }
        }
    }
}

@Composable
private fun FollowButton(onFollow: () -> Unit) {
    Button(
        onClick = onFollow,
        modifier = Modifier.width(64.dp).height(36.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        Text(stringResource(R.string.search_follow))
    }
}

@Composable
private fun ArtistAvatar(artist: SearchArtistUi) {
    Image(
        painterResource(artist.artworkRes),
        contentDescription = artist.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(64.dp).clip(CircleShape),
    )
}

@Composable
private fun ArtistDetails(
    artist: SearchArtistUi,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(artist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            artist.badge?.let { SearchBadge(it, MaterialTheme.colorScheme.secondary) }
        }
        Text(artist.stats, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Text(artist.representativeWorks, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SearchSectionTitle(
    title: String,
    showViewAll: Boolean,
    onViewAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 2.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (showViewAll) {
            TextButton(onClick = onViewAll) {
                Text(stringResource(R.string.search_view_all))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SearchSongRow(
    song: Song,
    artworkRes: Int?,
    badge: SearchSongBadge?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val unknownArtist = stringResource(R.string.search_unknown_artist)
    MoeSongRow(
        title = song.title,
        subtitle = buildString {
            append(song.artistName.ifBlank { unknownArtist })
            song.albumTitle?.takeIf(String::isNotBlank)?.let { append("  ·  ").append(it) }
        },
        onClick = onClick,
        modifier = Modifier.padding(start = 14.dp),
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
private fun SearchBadge(
    label: String,
    color: Color,
) {
    Surface(shape = RoundedCornerShape(6.dp), color = Color.Transparent, border = BorderStroke(1.dp, color)) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}

@Composable
private fun SearchCollectionCard(
    item: SearchCollectionUi,
    onClick: () -> Unit,
) {
    Column(Modifier.width(96.dp).clickable(onClick = onClick)) {
        Box {
            Image(
                painterResource(item.artworkRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(34.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.search_play_collection, item.title), modifier = Modifier.padding(6.dp))
            }
        }
        Text(item.title, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.metadata, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun SearchMessage(
    progress: Boolean = false,
    title: String,
    message: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (progress) CircularProgressIndicator(Modifier.padding(bottom = 20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Text(message, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.let { TextButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(it) } }
    }
}

@Composable
private fun SearchCategory.label(): String =
    stringResource(
        when (this) {
            SearchCategory.Overview -> R.string.search_tab_overview
            SearchCategory.Songs -> R.string.search_tab_songs
            SearchCategory.Playlists -> R.string.search_tab_playlists
            SearchCategory.Albums -> R.string.search_tab_albums
            SearchCategory.Artists -> R.string.search_tab_artists
            SearchCategory.Mv -> R.string.search_tab_mv
        },
    )

private fun SearchCategory.hasContent(state: SearchUiState): Boolean =
    when (this) {
        SearchCategory.Overview -> state.songs.isNotEmpty() || state.artist != null || state.collections.isNotEmpty()
        SearchCategory.Songs -> state.songs.isNotEmpty()
        SearchCategory.Playlists, SearchCategory.Albums -> state.collections.isNotEmpty()
        SearchCategory.Artists -> state.artist != null
        SearchCategory.Mv -> false
    }

private val SearchError.message: String
    get() =
        when (this) {
            SearchError.Offline -> "当前没有网络连接"
            SearchError.Timeout -> "请求超时，请稍后重试"
            SearchError.Connection -> "无法连接音乐服务"
            SearchError.VerificationRequired -> "服务需要安全验证，请稍后重试"
            SearchError.AuthenticationRequired -> "当前接口需要登录后使用"
            SearchError.ServiceUnavailable -> "音乐服务暂时不可用"
            SearchError.Protocol -> "服务响应发生变化，请更新应用"
            SearchError.SessionInitialization -> "匿名会话初始化失败"
        }

internal const val SEARCH_RESULTS_TAG = "search_results"
