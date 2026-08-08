package cn.james.music.feature.profile

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object UserProfileDestination

fun NavGraphBuilder.userProfileDestination(onBack: () -> Unit) {
    composable<UserProfileDestination> {
        UserProfileScreen(
            state = UserProfileUiState.Content(userProfileDesignPreview),
            onBack = onBack,
            onAction = {},
        )
    }
}
