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
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarTone
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
import cn.james.music.feature.player.PlayerUiState
import cn.james.music.feature.player.playerDestination
import cn.james.music.feature.playlist.PlaylistDetailDestination
import cn.james.music.feature.playlist.playlistDetailDestination
import cn.james.music.feature.profile.UserProfileDestination
import cn.james.music.feature.profile.userProfileDestination
import cn.james.music.feature.search.SearchDestination
import cn.james.music.feature.search.searchDestination
import cn.james.music.feature.settings.SettingsDestination
import cn.james.music.feature.settings.settingsDestination
import cn.james.music.playback.PlaybackConnectionState
import cn.james.music.playback.PlaybackStatus
import kotlinx.coroutines.delay

@Composable
fun MoeKoeApp(
    onChooseFiles: () -> Unit,
    onRequestDeviceScan: (() -> Unit) -> Unit,
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
    val showBottomNavigation = !isPlayer && appState.isTopLevel(currentDestination)
    val isImmersiveLogin = currentDestination?.hasRoute<LoginDestination>() == true
    val playerUiState =
        rememberUpdatedState(
            PlayerUiState(
                item = state.currentItem,
                isPlaying = state.isPlaying,
                isBuffering = state.status == PlaybackStatus.Buffering,
                controlsEnabled = state.connection == PlaybackConnectionState.Connected,
                mode = state.mode,
            ),
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
        contentWindowInsets = if (isImmersiveLogin || isPlayer) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
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
                    MoeKoeMiniPlayer(
                        item = item,
                        isPlaying = state.isPlaying,
                        progress =
                            if (currentProgress.durationMs > 0) {
                                currentProgress.positionMs.toFloat() / currentProgress.durationMs
                            } else {
                                0f
                            },
                        positionMs = currentProgress.positionMs,
                        durationMs = currentProgress.durationMs,
                        onToggle = viewModel::togglePlayback,
                        onPrevious = viewModel::skipPrevious,
                        onNext = viewModel::skipNext,
                        onQueue = { queueVisible = true },
                        onOpenPlayer = {
                            navController.navigate(PlayerDestination) { launchSingleTop = true }
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
                    message = currentNotice.message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    tone = MoeSnackbarTone.Error,
                    actionLabel = if (currentNotice.canRetry) "重试" else null,
                    onAction = {
                        viewModel.dismissNotice(currentNotice.id)
                        viewModel.retryPlayback()
                    },
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
                    onChooseFiles = onChooseFiles,
                    onRequestDeviceScan = onRequestDeviceScan,
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
            state = state,
            currentDurationMs = progress.value.durationMs,
            onDismiss = { queueVisible = false },
            onPlayAt = viewModel::playAt,
            onRemove = viewModel::removeAt,
            onClear = viewModel::clearQueue,
            onChangeMode = viewModel::cycleMode,
        )
    }
}
