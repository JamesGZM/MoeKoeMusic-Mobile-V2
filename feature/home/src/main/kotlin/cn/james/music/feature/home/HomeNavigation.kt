package cn.james.music.feature.home

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import cn.james.music.core.model.online.Song
import kotlinx.serialization.Serializable

@Serializable
data object HomeGraph

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeGraph(
    onSearch: () -> Unit,
    onPlay: (Song) -> Unit,
) {
    navigation<HomeGraph>(startDestination = HomeDestination) {
        composable<HomeDestination> {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        HomeAction.Search -> onSearch()

                        is HomeAction.PlaySong -> viewModel.songFor(action.id)?.let(onPlay)

                        HomeAction.Refresh,
                        HomeAction.DismissProblem,
                        -> viewModel.onAction(action)
                    }
                },
            )
        }
    }
}
