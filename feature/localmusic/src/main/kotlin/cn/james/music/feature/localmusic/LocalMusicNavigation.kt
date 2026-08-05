package cn.james.music.feature.localmusic

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
    onChooseFiles: () -> Unit,
    onRequestDeviceScan: (() -> Unit) -> Unit,
    onImportCandidates: (List<Long>) -> Unit,
) {
    composable<LocalMusicDestination> {
        val viewModel: LocalMusicViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LocalMusicScreen(
            state = state,
            onBack = navController::popBackStack,
            onChooseFiles = onChooseFiles,
            onScan = {
                navController.navigate(DeviceScanDestination)
                onRequestDeviceScan(viewModel::scanDevice)
            },
            onPlay = viewModel::play,
            onDelete = viewModel::delete,
            onCancelImport = viewModel::cancelImport,
        )
    }
    composable<DeviceScanDestination> {
        val viewModel: LocalMusicViewModel = hiltViewModel(navController.getBackStackEntry(LocalMusicDestination))
        val state by viewModel.state.collectAsStateWithLifecycle()
        DeviceScanScreen(
            candidates = state.candidates,
            onBack = navController::popBackStack,
            onRefresh = { onRequestDeviceScan(viewModel::scanDevice) },
            onImport = {
                onImportCandidates(it)
                navController.popBackStack()
            },
        )
    }
}
