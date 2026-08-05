package cn.james.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.james.music.feature.discover.discoverGraph
import cn.james.music.feature.home.HomeGraph
import cn.james.music.feature.home.homeGraph
import cn.james.music.feature.localmusic.LocalMusicDestination
import cn.james.music.feature.localmusic.localMusicDestinations
import cn.james.music.feature.login.LoginDestination
import cn.james.music.feature.login.loginDestination
import cn.james.music.feature.my.myGraph
import cn.james.music.feature.search.SearchDestination
import cn.james.music.feature.search.searchDestination

@Composable
fun MoeKoeApp(
    onChooseFiles: () -> Unit,
    onRequestDeviceScan: (() -> Unit) -> Unit,
    onImportCandidates: (List<Long>) -> Unit,
    foundationContent: (@Composable () -> Unit)?,
    viewModel: AppPlaybackViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appState = rememberMoeKoeAppState(navController)
    val currentDestination = appState.navController.currentBackStackEntryAsState().value?.destination
    var queueVisible by rememberSaveable { mutableStateOf(false) }
    val showBottomNavigation = appState.isTopLevel(currentDestination)
    val isImmersiveLogin = currentDestination?.hasRoute<LoginDestination>() == true

    Scaffold(
        contentWindowInsets = if (isImmersiveLogin) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            Column {
                state.currentItem?.let { item ->
                    MoeKoeMiniPlayer(
                        title = item.title,
                        artist = item.artist,
                        isPlaying = state.isPlaying,
                        onToggle = viewModel::togglePlayback,
                        onQueue = { queueVisible = true },
                    )
                }
                if (showBottomNavigation) {
                    NavigationBar {
                        appState.topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = appState.isInGraph(currentDestination, destination),
                                onClick = { appState.navigateToTopLevel(destination) },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
        ) {
            NavHost(
                navController = appState.navController,
                startDestination = HomeGraph,
            ) {
                homeGraph(onSearch = { navController.navigate(SearchDestination) })
                discoverGraph()
                myGraph(
                    onLogin = { navController.navigate(LoginDestination) },
                    onLocalMusic = { navController.navigate(LocalMusicDestination) },
                    onFoundationLab = { navController.navigateToFoundation() },
                    showFoundationLab = foundationContent != null,
                )
                searchDestination(onBack = navController::popBackStack, onPlay = viewModel::play)
                loginDestination(
                    onBack = navController::popBackStack,
                    onLoggedIn = navController::popBackStack,
                )
                localMusicDestinations(
                    navController = navController,
                    onChooseFiles = onChooseFiles,
                    onRequestDeviceScan = onRequestDeviceScan,
                    onImportCandidates = onImportCandidates,
                )
                foundationContent?.let { addFoundationDestination(it) }
            }
        }
    }

    if (queueVisible) {
        MoeKoeQueueSheet(
            state = state,
            onDismiss = { queueVisible = false },
            onPlayAt = viewModel::playAt,
            onRemove = viewModel::removeAt,
            onClear = viewModel::clearQueue,
        )
    }
}
