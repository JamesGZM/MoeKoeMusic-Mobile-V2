package cn.james.music.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable
data object HomeGraph

@Serializable
data object HomeDestination

fun NavGraphBuilder.homeGraph(onSearch: () -> Unit) {
    navigation<HomeGraph>(startDestination = HomeDestination) {
        composable<HomeDestination> { HomeScreen(onSearch = onSearch) }
    }
}
