package cn.james.music.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.action.MoeTextButton
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
            modifier = Modifier.searchLayoutProbe(SEARCH_PROBE_TOOLBAR),
            trailingAction = {
                IconButton(onClick = onVoice) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = stringResource(R.string.search_voice),
                        modifier = Modifier.size(18.dp),
                    )
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
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(40.dp)
                .searchLayoutProbe(SEARCH_PROBE_TABS)
                .padding(start = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        SearchCategory.entries.forEach { category ->
            Column(
                modifier = Modifier.width(68.dp).height(40.dp).clickable { onSelected(category) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    category.label(),
                    color = if (category == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (category == selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(5.dp))
                Box(Modifier.width(32.dp).height(3.dp).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))) {
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
                title = if (state.songs.isEmpty()) stringResource(R.string.search_empty_title) else stringResource(R.string.search_category_empty_title),
                message = if (state.songs.isEmpty()) stringResource(R.string.search_empty_message) else stringResource(R.string.search_category_empty_message),
            )
        else ->
            SearchResults(
                state = state,
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
        action?.let { MoeTextButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(it) } }
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
    @Composable
    get() =
        when (this) {
            SearchError.Offline -> stringResource(R.string.search_error_offline)
            SearchError.Timeout -> stringResource(R.string.search_error_timeout)
            SearchError.Connection -> stringResource(R.string.search_error_connection)
            SearchError.VerificationRequired -> stringResource(R.string.search_error_verification_required)
            SearchError.AuthenticationRequired -> stringResource(R.string.search_error_authentication_required)
            SearchError.ServiceUnavailable -> stringResource(R.string.search_error_service_unavailable)
            SearchError.Protocol -> stringResource(R.string.search_error_protocol)
            SearchError.SessionInitialization -> stringResource(R.string.search_error_session_initialization)
        }

internal const val SEARCH_RESULTS_TAG = "search_results"
