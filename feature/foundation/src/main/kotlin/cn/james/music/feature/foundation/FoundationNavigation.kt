package cn.james.music.feature.foundation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object FoundationDestination

fun NavGraphBuilder.foundationDestination(content: @Composable () -> Unit) {
    composable<FoundationDestination> { content() }
}
