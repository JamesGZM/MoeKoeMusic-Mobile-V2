package cn.james.music

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import cn.james.music.feature.discover.DiscoverDestination
import cn.james.music.feature.discover.DiscoverGraph
import cn.james.music.feature.home.HomeDestination
import cn.james.music.feature.home.HomeGraph
import cn.james.music.feature.my.MyDestination
import cn.james.music.feature.my.MyGraph

@Composable
internal fun rememberMoeKoeAppState(navController: NavHostController): MoeKoeAppState =
    remember(navController) { MoeKoeAppState(navController) }

@Stable
internal class MoeKoeAppState(
    val navController: NavHostController,
) {
    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.entries

    fun isTopLevel(destination: NavDestination?): Boolean =
        destination != null &&
            (
                destination.hasRoute<HomeDestination>() ||
                    destination.hasRoute<DiscoverDestination>() ||
                    destination.hasRoute<MyDestination>()
            )

    fun isInGraph(
        currentDestination: NavDestination?,
        destination: TopLevelDestination,
    ): Boolean =
        currentDestination?.hierarchy?.any { graph ->
            when (destination) {
                TopLevelDestination.Home -> graph.hasRoute<HomeGraph>()
                TopLevelDestination.Discover -> graph.hasRoute<DiscoverGraph>()
                TopLevelDestination.My -> graph.hasRoute<MyGraph>()
            }
        } == true

    fun navigateToTopLevel(destination: TopLevelDestination) {
        val route =
            when (destination) {
                TopLevelDestination.Home -> HomeGraph
                TopLevelDestination.Discover -> DiscoverGraph
                TopLevelDestination.My -> MyGraph
            }
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

internal enum class TopLevelDestination(
    @param:StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.navigation_home, Icons.Default.Home),
    Discover(R.string.navigation_discover, Icons.Default.Search),
    My(R.string.navigation_my, Icons.Default.Person),
}
