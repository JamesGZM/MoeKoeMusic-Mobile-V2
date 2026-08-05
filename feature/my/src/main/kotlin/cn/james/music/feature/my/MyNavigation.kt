package cn.james.music.feature.my

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable
data object MyGraph

@Serializable
data object MyDestination

fun NavGraphBuilder.myGraph(
    onLocalMusic: () -> Unit,
    onFoundationLab: () -> Unit,
    showFoundationLab: Boolean,
) {
    navigation<MyGraph>(startDestination = MyDestination) {
        composable<MyDestination> {
            MyScreen(
                onLocalMusic = onLocalMusic,
                onFoundationLab = onFoundationLab,
                showFoundationLab = showFoundationLab,
            )
        }
    }
}
