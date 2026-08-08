package cn.james.music.feature.localmusic

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object LocalMusicDestination

@Serializable
data object DeviceScanDestination

fun NavGraphBuilder.localMusicDestinations(
    navController: NavHostController,
    hasMediaPermission: () -> Boolean,
    onRequestMediaPermission: ((Boolean) -> Unit) -> Unit,
    onImportCandidates: (List<Long>) -> Unit,
) {
    composable<LocalMusicDestination> {
        val viewModel: LocalMusicViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LocalMusicScreen(
            state = state,
            onBack = navController::popBackStack,
            onImport = { navController.navigate(DeviceScanDestination) },
            onPlay = viewModel::play,
            onDelete = viewModel::delete,
            onCancelImport = viewModel::cancelImport,
        )
    }
    composable<DeviceScanDestination> {
        val viewModel: LocalMusicViewModel = hiltViewModel(navController.getBackStackEntry(LocalMusicDestination))
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(viewModel) { viewModel.openImporter(hasMediaPermission()) }
        DisposableEffect(viewModel) {
            onDispose(viewModel::cancelDeviceScan)
        }
        LocalMusicImportScreen(
            state = state.deviceImport,
            onBack = {
                viewModel.cancelDeviceScan()
                navController.popBackStack()
            },
            onGrantPermission = { onRequestMediaPermission(viewModel::onPermissionResult) },
            onRetry = viewModel::retryDeviceScan,
            onToggleCandidate = viewModel::toggleCandidate,
            onToggleAll = viewModel::toggleAllCandidates,
            onImport = {
                onImportCandidates(it)
                navController.popBackStack()
            },
        )
    }
}
