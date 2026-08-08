package cn.james.music.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarTone

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onDismissProblem: () -> Unit,
    onPlay: (HomeSongUi) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val content = state.content) {
                HomeContentUiState.Loading -> HomeLoading(onSearch)
                HomeContentUiState.Empty ->
                    HomeMessage(
                        onSearch = onSearch,
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        onRetry = onRefresh,
                    )
                is HomeContentUiState.Failure ->
                    HomeMessage(
                        onSearch = onSearch,
                        title = stringResource(R.string.home_failure_title),
                        message = homeProblemMessage(content.problem),
                        onRetry = onRefresh,
                    )
                is HomeContentUiState.Content -> HomeContent(content.value, onSearch, onPlay)
            }
        }
        state.refreshProblem?.let { problem ->
            MoeSnackbar(
                message =
                    if (problem == HomeProblemUi.Offline) {
                        stringResource(R.string.home_refresh_offline)
                    } else {
                        homeProblemMessage(problem)
                    },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                tone = MoeSnackbarTone.Warning,
                actionLabel = stringResource(R.string.home_acknowledge),
                onAction = onDismissProblem,
            )
        }
    }
}

@Composable
private fun HomeContent(
    content: HomeContentUi,
    onSearch: () -> Unit,
    onPlay: (HomeSongUi) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("home_content"),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item { HomeHeader(onSearch) }
        item { HomeRadioHero(modifier = Modifier.homeLayoutProbe(HOME_PROBE_HERO)) }
        item { HomeQuickEntries(modifier = Modifier.homeLayoutProbe(HOME_PROBE_QUICK_ENTRIES)) }
        item { Spacer(Modifier.height(2.dp)) }
        if (content.recommendations.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title = stringResource(R.string.home_daily_title),
                    modifier = Modifier.homeLayoutProbe(HOME_PROBE_DAILY_HEADER),
                )
            }
            itemsIndexed(content.recommendations.take(4), key = { _, song -> song.id }) { index, song ->
                HomeSongRow(
                    song = song,
                    onPlay = onPlay,
                    modifier = if (index == 0) Modifier.homeLayoutProbe(HOME_PROBE_FIRST_SONG) else Modifier,
                )
            }
        }
        if (content.playlists.isNotEmpty()) {
            item { Spacer(Modifier.height(12.dp)) }
            item {
                HomeSectionHeader(
                    title = stringResource(R.string.home_playlist_title),
                    modifier = Modifier.homeLayoutProbe(HOME_PROBE_PLAYLIST_HEADER),
                )
            }
            item {
                HomePlaylistGrid(
                    playlists = content.playlists.take(4),
                    modifier = Modifier.homeLayoutProbe(HOME_PROBE_PLAYLIST_GRID),
                )
            }
        }
    }
}
