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
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onFoundationLab: () -> Unit,
    showFoundationLab: Boolean,
) {
    navigation<MyGraph>(startDestination = MyDestination) {
        composable<MyDestination> {
            MyRoute(
                onLogin = onLogin,
                onLocalMusic = onLocalMusic,
                onSettings = onSettings,
                onProfile = onProfile,
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
    onSettings: () -> Unit,
    onProfile: () -> Unit,
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
        onAction = { action ->
            when (action) {
                MyAction.RefreshProfile -> viewModel.refresh()
                MyAction.OpenLogin -> onLogin()
                MyAction.OpenLocalMusic -> onLocalMusic()
                MyAction.OpenSettings -> onSettings()
                MyAction.OpenProfile -> onProfile()
                MyAction.RequestLogout -> viewModel.requestLogout()
                MyAction.DismissLogout -> viewModel.dismissLogout()
                MyAction.ConfirmLogout -> viewModel.confirmLogout()
                MyAction.OpenFoundationLab -> onFoundationLab()
            }
        },
        showFoundationLab = showFoundationLab,
    )
}
