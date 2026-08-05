package cn.james.music.feature.my

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable
data object MyGraph

@Serializable
data object MyDestination

fun NavGraphBuilder.myGraph(
    onLogin: () -> Unit,
    onLocalMusic: () -> Unit,
    onFoundationLab: () -> Unit,
    showFoundationLab: Boolean,
) {
    navigation<MyGraph>(startDestination = MyDestination) {
        composable<MyDestination> {
            MyRoute(
                onLogin = onLogin,
                onLocalMusic = onLocalMusic,
                onFoundationLab = onFoundationLab,
                showFoundationLab = showFoundationLab,
            )
        }
    }
}

@Composable
private fun MyRoute(
    onLogin: () -> Unit,
    onLocalMusic: () -> Unit,
    onFoundationLab: () -> Unit,
    showFoundationLab: Boolean,
    viewModel: MyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    MyScreen(
        state = state,
        onRefresh = viewModel::refresh,
        onLogin = onLogin,
        onLocalMusic = onLocalMusic,
        onAccountSettings = viewModel::requestLogout,
        onDismissLogout = viewModel::dismissLogout,
        onConfirmLogout = viewModel::confirmLogout,
        onFoundationLab = onFoundationLab,
        showFoundationLab = showFoundationLab,
    )
}
