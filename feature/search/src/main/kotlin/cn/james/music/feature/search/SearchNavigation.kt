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
            onAction = { action ->
                when (action) {
                    SearchAction.Back -> onBack()

                    is SearchAction.PlaySong -> viewModel.songFor(action.id)?.let(onPlay)

                    is SearchAction.QueryChanged,
                    SearchAction.ClearQuery,
                    SearchAction.Submit,
                    is SearchAction.SelectCategory,
                    SearchAction.LoadMore,
                    is SearchAction.MoreSong,
                    SearchAction.Voice,
                    SearchAction.FollowArtist,
                    SearchAction.ViewAllSongs,
                    SearchAction.ViewAllCollections,
                    is SearchAction.OpenCollection,
                    -> viewModel.onAction(action)
                }
            },
        )
    }
}
