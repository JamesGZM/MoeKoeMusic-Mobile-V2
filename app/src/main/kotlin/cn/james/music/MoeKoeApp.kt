package cn.james.music

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.james.music.core.designsystem.component.MoeMiniPlayerEvent
import cn.james.music.core.designsystem.component.MoeMiniPlayerSemanticsUi
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarActionId
import cn.james.music.core.designsystem.component.MoeSnackbarActionUiModel
import cn.james.music.core.designsystem.component.MoeSnackbarEvent
import cn.james.music.core.designsystem.component.MoeSnackbarTone
import cn.james.music.core.designsystem.component.MoeSnackbarUiModel
import cn.james.music.feature.discover.discoverGraph
import cn.james.music.feature.home.HomeGraph
import cn.james.music.feature.home.homeGraph
import cn.james.music.feature.localmusic.LocalMusicDestination
import cn.james.music.feature.localmusic.localMusicDestinations
import cn.james.music.feature.login.LoginDestination
import cn.james.music.feature.login.TencentCaptchaResult
import cn.james.music.feature.login.loginDestination
import cn.james.music.feature.my.myGraph
import cn.james.music.feature.player.PlayerDestination
import cn.james.music.feature.player.PlayerProgressUiState
import cn.james.music.feature.player.playerDestination
import cn.james.music.feature.playlist.PlaylistDetailDestination
import cn.james.music.feature.playlist.playlistDetailDestination
import cn.james.music.feature.profile.UserProfileDestination
import cn.james.music.feature.profile.userProfileDestination
import cn.james.music.feature.search.SearchDestination
import cn.james.music.feature.search.searchDestination
import cn.james.music.feature.settings.SettingsDestination
import cn.james.music.feature.settings.settingsDestination
import kotlinx.coroutines.delay

@Composable
fun MoeKoeApp(
    hasMediaPermission: () -> Boolean,
    onRequestMediaPermission: ((Boolean) -> Unit) -> Unit,
    onImportCandidates: (List<Long>) -> Unit,
    onLaunchTencentCaptcha: (String, (TencentCaptchaResult) -> Unit) -> Unit,
    foundationContent: (@Composable () -> Unit)?,
    viewModel: AppPlaybackViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress = viewModel.progress.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val appState = rememberMoeKoeAppState(navController)
    val currentDestination = appState.navController.currentBackStackEntryAsState().value?.destination
    var queueVisible by rememberSaveable { mutableStateOf(false) }
    val isPlayer = currentDestination?.hasRoute<PlayerDestination>() == true
    val isSettings = currentDestination?.hasRoute<SettingsDestination>() == true
    val isUserProfile = currentDestination?.hasRoute<UserProfileDestination>() == true
    val showBottomNavigation = !isPlayer && appState.isTopLevel(currentDestination)
    val isImmersiveLogin = currentDestination?.hasRoute<LoginDestination>() == true
    val playerUiState =
        rememberUpdatedState(
            state.toPlayerUiState(),
        )
    val playerProgressUiState =
        remember(progress) {
            derivedStateOf {
                val currentProgress = progress.value
                PlayerProgressUiState(
                    positionMs = currentProgress.positionMs,
                    durationMs = currentProgress.durationMs,
                )
            }
        }

    Scaffold(
        contentWindowInsets =
            if (isImmersiveLogin || isPlayer || isUserProfile) {
                WindowInsets(0, 0, 0, 0)
            } else {
                ScaffoldDefaults.contentWindowInsets
            },
        bottomBar = {
            if (!isImmersiveLogin) {
                Column(
                    modifier =
                        if (showBottomNavigation) {
                            Modifier
                        } else {
                            Modifier.navigationBarsPadding()
                        },
                ) {
                    state.currentItem?.takeUnless { isPlayer || isSettings }?.let { item ->
                        val currentProgress = progress.value
                        val model =
                            state.toMoeMiniPlayerUiModel(
                                positionMs = currentProgress.positionMs,
                                durationMs = currentProgress.durationMs,
                                badgeLabel = stringResource(R.string.mini_player_quality_standard),
                                semantics =
                                    MoeMiniPlayerSemanticsUi(
                                        play = stringResource(R.string.mini_player_play),
                                        pause = stringResource(R.string.mini_player_pause),
                                        previous = stringResource(R.string.mini_player_previous),
                                        next = stringResource(R.string.mini_player_next),
                                        queue = stringResource(R.string.mini_player_queue),
                                    ),
                            ) ?: return@let
                        MoeKoeMiniPlayer(
                            model = model,
                            item = item,
                            onEvent = { event ->
                                when (event) {
                                    MoeMiniPlayerEvent.TogglePlayback -> viewModel.togglePlayback()
                                    MoeMiniPlayerEvent.Previous -> viewModel.skipPrevious()
                                    MoeMiniPlayerEvent.Next -> viewModel.skipNext()
                                    MoeMiniPlayerEvent.OpenQueue -> queueVisible = true
                                    MoeMiniPlayerEvent.OpenPlayer -> {
                                        navController.navigate(PlayerDestination) { launchSingleTop = true }
                                    }
                                }
                            },
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
            }
        },
        snackbarHost = {
            notice?.let { currentNotice ->
                MoeSnackbar(
                    model =
                        MoeSnackbarUiModel(
                            message = currentNotice.message,
                            tone = MoeSnackbarTone.Error,
                            action =
                                if (currentNotice.canRetry) {
                                    MoeSnackbarActionUiModel(
                                        id = MoeSnackbarActionId.Retry,
                                        label = "重试",
                                    )
                                } else {
                                    null
                                },
                        ),
                    onEvent = { event ->
                        when (event) {
                            is MoeSnackbarEvent.Action ->
                                when (event.id) {
                                    MoeSnackbarActionId.Retry -> {
                                        viewModel.dismissNotice(currentNotice.id)
                                        viewModel.retryPlayback()
                                    }
                                    MoeSnackbarActionId.Dismiss,
                                    MoeSnackbarActionId.Undo,
                                    -> Unit
                                }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
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
                homeGraph(
                    onSearch = { navController.navigate(SearchDestination) },
                    onPlay = viewModel::play,
                )
                discoverGraph(onPlaylist = { navController.navigate(PlaylistDetailDestination) })
                myGraph(
                    onLogin = { navController.navigate(LoginDestination) },
                    onLocalMusic = { navController.navigate(LocalMusicDestination) },
                    onSettings = { navController.navigate(SettingsDestination) },
                    onProfile = { navController.navigate(UserProfileDestination) },
                    onFoundationLab = { navController.navigateToFoundation() },
                    showFoundationLab = foundationContent != null,
                )
                searchDestination(onBack = navController::popBackStack, onPlay = viewModel::play)
                settingsDestination(onBack = navController::popBackStack)
                loginDestination(
                    onBack = navController::popBackStack,
                    onLoggedIn = navController::popBackStack,
                    onLaunchTencentCaptcha = onLaunchTencentCaptcha,
                )
                localMusicDestinations(
                    navController = navController,
                    hasMediaPermission = hasMediaPermission,
                    onRequestMediaPermission = onRequestMediaPermission,
                    onImportCandidates = onImportCandidates,
                )
                playerDestination(
                    state = playerUiState,
                    progress = playerProgressUiState,
                    onBack = navController::popBackStack,
                    onTogglePlayback = viewModel::togglePlayback,
                    onSeek = viewModel::seekTo,
                    onPrevious = viewModel::skipPrevious,
                    onNext = viewModel::skipNext,
                    onChangeMode = viewModel::cycleMode,
                    onOpenQueue = { queueVisible = true },
                )
                playlistDetailDestination(onBack = navController::popBackStack)
                userProfileDestination(onBack = navController::popBackStack)
                foundationContent?.let { addFoundationDestination(it) }
            }
        }
    }

    LaunchedEffect(notice?.id) {
        val currentNotice = notice ?: return@LaunchedEffect
        delay(4_000)
        viewModel.dismissNotice(currentNotice.id)
    }

    LaunchedEffect(isPlayer, state.currentItem) {
        if (isPlayer && state.currentItem == null) {
            queueVisible = false
            navController.popBackStack()
        }
    }

    if (queueVisible) {
        MoeKoeQueueSheet(
            model = state.toPlayerQueueUiState(progress.value.durationMs),
            onEvent = { action ->
                when (val resolved = state.resolvePlayerQueueAction(action)) {
                    ResolvedPlayerQueueAction.Dismiss -> queueVisible = false
                    is ResolvedPlayerQueueAction.PlayAt -> viewModel.playAt(resolved.index)
                    is ResolvedPlayerQueueAction.RemoveAt -> viewModel.removeAt(resolved.index)
                    ResolvedPlayerQueueAction.Clear -> viewModel.clearQueue()
                    ResolvedPlayerQueueAction.ChangeMode -> viewModel.cycleMode()
                    null -> Unit
                }
            },
        )
    }
}
