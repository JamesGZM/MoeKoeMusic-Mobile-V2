package cn.james.music.feature.profile

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarAction

@Composable
internal fun UserProfileScreen(
    state: UserProfileUiState,
    onBack: () -> Unit,
    onAction: (UserProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MoeStandardTopBar(
                title = stringResource(R.string.profile_title),
                navigationContentDescription = stringResource(R.string.profile_back),
                onNavigateBack = onBack,
                actions = {
                    MoeStandardTopBarAction(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.profile_share),
                        onClick = { onAction(UserProfileAction.Share) },
                        horizontalVisualOffset = 8.dp,
                    )
                    MoeStandardTopBarAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.profile_more),
                        onClick = { onAction(UserProfileAction.More) },
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val pageWidth = if (maxWidth >= 600.dp) 480.dp else maxWidth
            Surface(modifier = Modifier.width(pageWidth).height(maxHeight), color = Color.Transparent) {
                when (state) {
                    UserProfileUiState.Loading -> UserProfileLoading()
                    UserProfileUiState.Error ->
                        UserProfileMessage(
                            title = stringResource(R.string.profile_error_title),
                            message = stringResource(R.string.profile_error_message),
                            onRetry = { onAction(UserProfileAction.Retry) },
                        )
                    UserProfileUiState.Offline ->
                        UserProfileMessage(
                            title = stringResource(R.string.profile_offline_title),
                            message = stringResource(R.string.profile_offline_message),
                            onRetry = { onAction(UserProfileAction.Retry) },
                        )
                    is UserProfileUiState.Content -> UserProfileContent(profile = state.value, onAction = onAction)
                }
            }
        }
    }
}
