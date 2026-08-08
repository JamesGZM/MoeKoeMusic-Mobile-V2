package cn.james.music.feature.discover

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable
data object DiscoverGraph

@Serializable
data object DiscoverDestination

fun NavGraphBuilder.discoverGraph(onPlaylist: () -> Unit) {
    navigation<DiscoverGraph>(startDestination = DiscoverDestination) {
        composable<DiscoverDestination> { DiscoverRoute(onPlaylist = onPlaylist) }
    }
}
