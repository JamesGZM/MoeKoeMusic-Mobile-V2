package cn.james.music.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onDownload: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onShare: () -> Unit = {},
    lyricsState: PlayerLyricsUiState = PlayerLyricsUiState.Loading,
    initialPage: PlayerPage = PlayerPage.Cover,
    onLyricsSettings: () -> Unit = {},
    onRetryLyrics: () -> Unit = {},
    onLyricClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    MoeKoeTheme(themeMode = ThemeMode.Dark) {
        val item = state.item
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Box(
                modifier = modifier.fillMaxSize(),
            ) {
                PlayerBackdrop()
                if (item == null) {
                    PlayerEmptyState(onBack = onBack)
                } else {
                    PlayerContent(
                        state = state,
                        progress = progress,
                        item = item,
                        onBack = onBack,
                        onTogglePlayback = onTogglePlayback,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onChangeMode = onChangeMode,
                        onOpenQueue = onOpenQueue,
                        onMore = onMore,
                        onFavorite = onFavorite,
                        onDownload = onDownload,
                        onAddToPlaylist = onAddToPlaylist,
                        onShare = onShare,
                        lyricsState = lyricsState,
                        initialPage = initialPage,
                        onLyricsSettings = onLyricsSettings,
                        onRetryLyrics = onRetryLyrics,
                        onLyricClick = onLyricClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerContent(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlayerItemUiModel,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onMore: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    lyricsState: PlayerLyricsUiState,
    initialPage: PlayerPage,
    onLyricsSettings: () -> Unit,
    onRetryLyrics: () -> Unit,
    onLyricClick: (Long) -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding().coerceAtLeast(25.dp)
    val pagerState = rememberPagerState(initialPage = initialPage.ordinal, pageCount = { PlayerPage.entries.size })
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = topInset)
                .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        PlayerTopBar(
            onBack = onBack,
            onMore = onMore,
            modifier = Modifier.playerLayoutProbe(PLAYER_PROBE_TOP_BAR),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.Top,
        ) { page ->
            when (PlayerPage.entries[page]) {
                PlayerPage.Cover ->
                    PlayerCoverPage(
                        state = state,
                        progress = progress,
                        item = item,
                        activePage = page,
                        onTogglePlayback = onTogglePlayback,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onChangeMode = onChangeMode,
                        onOpenQueue = onOpenQueue,
                        onFavorite = onFavorite,
                        onDownload = onDownload,
                        onAddToPlaylist = onAddToPlaylist,
                        onShare = onShare,
                    )

                PlayerPage.Lyrics ->
                    PlayerLyricsPage(
                        state = state,
                        progress = progress,
                        item = item,
                        lyricsState = lyricsState,
                        activePage = page,
                        onTogglePlayback = onTogglePlayback,
                        onSeek = onSeek,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onChangeMode = onChangeMode,
                        onFavorite = onFavorite,
                        onLyricsSettings = onLyricsSettings,
                        onRetryLyrics = onRetryLyrics,
                        onLyricClick = onLyricClick,
                    )
            }
        }
    }
}
