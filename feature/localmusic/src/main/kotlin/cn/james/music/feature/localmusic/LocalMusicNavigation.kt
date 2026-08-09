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
            onAction = { action ->
                when (action) {
                    LocalMusicAction.Back -> navController.popBackStack()

                    LocalMusicAction.OpenImporter -> navController.navigate(DeviceScanDestination)

                    is LocalMusicAction.QueryChanged,
                    is LocalMusicAction.SelectSort,
                    is LocalMusicAction.PlaySong,
                    is LocalMusicAction.RequestDeleteSong,
                    is LocalMusicAction.ConfirmDeleteSong,
                    LocalMusicAction.DismissDeleteSong,
                    is LocalMusicAction.CancelImport,
                    -> viewModel.onAction(action)
                }
            },
        )
    }
    composable<DeviceScanDestination> {
        val viewModel: LocalMusicViewModel = hiltViewModel(navController.getBackStackEntry(LocalMusicDestination))
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(viewModel) {
            viewModel.onDeviceImportAction(DeviceImportAction.Open(hasMediaPermission()))
        }
        DisposableEffect(viewModel) {
            onDispose { viewModel.onDeviceImportAction(DeviceImportAction.Cancel) }
        }
        LocalMusicImportScreen(
            state = state.deviceImport,
            onAction = { action ->
                when (action) {
                    DeviceImportAction.Back -> {
                        viewModel.onDeviceImportAction(action)
                        navController.popBackStack()
                    }

                    DeviceImportAction.RequestPermission -> {
                        onRequestMediaPermission { granted ->
                            viewModel.onDeviceImportAction(DeviceImportAction.PermissionResult(granted))
                        }
                    }

                    is DeviceImportAction.Import -> {
                        onImportCandidates(action.ids)
                        navController.popBackStack()
                    }

                    is DeviceImportAction.Open,
                    is DeviceImportAction.PermissionResult,
                    DeviceImportAction.Retry,
                    is DeviceImportAction.ToggleCandidate,
                    DeviceImportAction.ToggleAll,
                    DeviceImportAction.Cancel,
                    -> {
                        viewModel.onDeviceImportAction(action)
                    }
                }
            },
        )
    }
}
