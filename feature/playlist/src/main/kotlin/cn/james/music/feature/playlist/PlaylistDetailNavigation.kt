package cn.james.music.feature.playlist

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object PlaylistDetailDestination

fun NavGraphBuilder.playlistDetailDestination(onBack: () -> Unit) {
    composable<PlaylistDetailDestination> {
        PlaylistDetailScreen(
            state = PlaylistDetailUiState.Content(playlistDetailDesignPreview),
            onBack = onBack,
            onSearch = {},
            onMore = {},
            onPlayAll = {},
            onShuffle = {},
            onFavorite = {},
            onDownload = {},
            onSort = {},
            onTrack = {},
            onTrackMore = {},
            onRetry = {},
        )
    }
}
