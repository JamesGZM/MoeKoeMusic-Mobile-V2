package cn.james.music.feature.search

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import cn.james.music.core.model.online.Song
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

fun NavGraphBuilder.searchDestination(
    onBack: () -> Unit,
    onPlay: (Song) -> Unit,
) {
    composable<SearchDestination> {
        val viewModel: SearchViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        SearchScreen(
            state = state,
            onBack = onBack,
            onQueryChange = viewModel::updateQuery,
            onSearch = viewModel::submit,
            onLoadMore = viewModel::loadMore,
            onPlay = onPlay,
        )
    }
}
