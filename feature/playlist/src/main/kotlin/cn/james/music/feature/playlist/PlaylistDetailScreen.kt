package cn.james.music.feature.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
internal fun PlaylistDetailScreen(
    state: PlaylistDetailUiState,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onMore: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onSort: () -> Unit,
    onTrack: (Int) -> Unit,
    onTrackMore: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = (state as? PlaylistDetailUiState.Content)?.value?.title.orEmpty()
    Scaffold(
        modifier = modifier,
        topBar = {
            MoeStandardTopBar(
                title = title,
                navigationContentDescription = stringResource(R.string.playlist_detail_back),
                onNavigateBack = onBack,
                modifier = Modifier.playlistDetailLayoutProbe(PLAYLIST_PROBE_TOOLBAR),
                actions = {
                    MoeStandardTopBarAction(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.playlist_detail_search),
                        onClick = onSearch,
                        horizontalVisualOffset = 8.dp,
                    )
                    MoeStandardTopBarAction(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.playlist_detail_more),
                        onClick = onMore,
                    )
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .background(playlistDetailBackground()),
            contentAlignment = Alignment.TopCenter,
        ) {
            val pageWidth = if (maxWidth >= 600.dp) 480.dp else maxWidth
            Surface(
                modifier = Modifier.width(pageWidth).height(maxHeight),
                color = Color.Transparent,
            ) {
                PlaylistDetailBody(
                    state = state,
                    onPlayAll = onPlayAll,
                    onShuffle = onShuffle,
                    onFavorite = onFavorite,
                    onDownload = onDownload,
                    onMore = onMore,
                    onSort = onSort,
                    onTrack = onTrack,
                    onTrackMore = onTrackMore,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun PlaylistDetailBody(
    state: PlaylistDetailUiState,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onMore: () -> Unit,
    onSort: () -> Unit,
    onTrack: (Int) -> Unit,
    onTrackMore: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        PlaylistDetailUiState.Loading -> PlaylistDetailLoading()
        PlaylistDetailUiState.Empty ->
            PlaylistDetailMessage(
                title = stringResource(R.string.playlist_detail_empty_title),
                message = stringResource(R.string.playlist_detail_empty_message),
            )
        PlaylistDetailUiState.Error ->
            PlaylistDetailMessage(
                title = stringResource(R.string.playlist_detail_error_title),
                message = stringResource(R.string.playlist_detail_error_message),
                onRetry = onRetry,
            )
        PlaylistDetailUiState.Offline ->
            PlaylistDetailMessage(
                title = stringResource(R.string.playlist_detail_offline_title),
                message = stringResource(R.string.playlist_detail_offline_message),
                onRetry = onRetry,
            )
        is PlaylistDetailUiState.Content ->
            PlaylistDetailContent(
                playlist = state.value,
                onPlayAll = onPlayAll,
                onShuffle = onShuffle,
                onFavorite = onFavorite,
                onDownload = onDownload,
                onMore = onMore,
                onSort = onSort,
                onTrack = onTrack,
                onTrackMore = onTrackMore,
            )
    }
}

internal const val PLAYLIST_DETAIL_CONTENT_TAG = "playlist_detail_content"
