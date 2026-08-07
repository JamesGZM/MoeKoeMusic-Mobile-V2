package cn.james.music.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.ThemeMode
import cn.james.music.core.model.playback.PlaybackArtwork
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import java.io.File
import kotlin.math.roundToLong

data class PlayerUiState(
    val item: PlaybackItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val controlsEnabled: Boolean = true,
    val mode: PlaybackMode = PlaybackMode.RepeatAll,
)

data class PlayerProgressUiState(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

enum class PlayerPage {
    Cover,
    Lyrics,
}

data class PlayerLyricLineUi(
    val original: String,
    val secondary: String? = null,
    val highlightedCharacterCount: Int = 0,
    val startTimeMs: Long = 0,
)

enum class PlayerLyricsTextSize {
    Standard,
    Large,
    Largest,
}

sealed interface PlayerLyricsUiState {
    data object Loading : PlayerLyricsUiState

    data object Empty : PlayerLyricsUiState

    data object Offline : PlayerLyricsUiState

    data object Error : PlayerLyricsUiState

    data class Content(
        val lines: List<PlayerLyricLineUi>,
        val activeLineIndex: Int,
        val textSize: PlayerLyricsTextSize = PlayerLyricsTextSize.Standard,
    ) : PlayerLyricsUiState
}

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
    artworkContent: (@Composable (PlaybackItem) -> Unit)? = null,
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
                        artworkContent = artworkContent,
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
    item: PlaybackItem,
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
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
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
        PlayerTopBar(onBack = onBack, onMore = onMore)
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
                        artworkContent = artworkContent,
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

@Composable
private fun PlayerCoverPage(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlaybackItem,
    activePage: Int,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(20.dp))
        PlayerArtwork(item = item, artworkContent = artworkContent, modifier = Modifier.fillMaxWidth())
        PlayerPageIndicator(activePage = activePage)
        Spacer(Modifier.height(15.dp))
        PlayerControls(
            state = state,
            progress = progress,
            item = item,
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
            showSecondaryActions = true,
            compactLayout = false,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PlayerLyricsPage(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlaybackItem,
    lyricsState: PlayerLyricsUiState,
    activePage: Int,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onFavorite: () -> Unit,
    onLyricsSettings: () -> Unit,
    onRetryLyrics: () -> Unit,
    onLyricClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LyricsViewport(
                state = lyricsState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 30.dp),
                onRetry = onRetryLyrics,
                onLyricClick = onLyricClick,
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp)
                        .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                        .clickable(onClick = onLyricsSettings),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(35.dp),
                    shape = CircleShape,
                    color = PlayerSecondaryContainer.copy(alpha = 0.9f),
                    contentColor = PlayerAccent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(R.string.player_lyrics_settings),
                            modifier = Modifier.size(19.dp),
                    )
                    }
                }
            }
        }
        PlayerPageIndicator(activePage = activePage)
        PlayerControls(
            state = state,
            progress = progress,
            item = item,
            onTogglePlayback = onTogglePlayback,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
            onChangeMode = onChangeMode,
            onOpenQueue = {},
            onFavorite = onFavorite,
            onDownload = {},
            onAddToPlaylist = {},
            onShare = {},
            showSecondaryActions = false,
            compactLayout = true,
        )
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun LyricsViewport(
    state: PlayerLyricsUiState,
    onRetry: () -> Unit,
    onLyricClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            PlayerLyricsUiState.Loading -> LyricsLoadingState()
            PlayerLyricsUiState.Empty ->
                LyricsMessageState(
                    icon = Icons.Default.MusicOff,
                    title = stringResource(R.string.player_lyrics_empty_title),
                    message = stringResource(R.string.player_lyrics_empty_message),
                    onRetry = onRetry,
                )

            PlayerLyricsUiState.Offline ->
                LyricsMessageState(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.player_lyrics_offline_title),
                    message = stringResource(R.string.player_lyrics_offline_message),
                    onRetry = onRetry,
                )

            PlayerLyricsUiState.Error ->
                LyricsMessageState(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.player_lyrics_error_title),
                    message = stringResource(R.string.player_lyrics_error_message),
                    onRetry = onRetry,
                )

            is PlayerLyricsUiState.Content -> LyricsContent(state, onLyricClick)
        }
    }
}

@Composable
private fun LyricsLoadingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = PlayerAccent,
            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            strokeWidth = 3.dp,
        )
        Text(
            text = stringResource(R.string.player_lyrics_loading_title),
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.player_lyrics_loading_message),
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LyricsMessageState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = PlayerSecondaryContainer.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, PlayerSecondaryContent.copy(alpha = 0.18f)),
            contentColor = PlayerSecondaryContent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            }
        }
        Text(
            text = title,
            modifier = Modifier.padding(top = 16.dp),
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 18.dp).height(48.dp).widthIn(min = 112.dp),
            shape = RoundedCornerShape(16.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = PlayerAccent,
                    contentColor = Color(0xFF141329),
                ),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.player_lyrics_retry), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LyricsContent(
    state: PlayerLyricsUiState.Content,
    onLyricClick: (Long) -> Unit,
) {
    val textSizes = lyricsTextSizes(state.textSize)
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        state.lines.forEachIndexed { index, line ->
            val isActive = index == state.activeLineIndex
            val original =
                if (isActive && line.highlightedCharacterCount > 0) {
                    val split = (line.original.length - line.highlightedCharacterCount).coerceIn(0, line.original.length)
                    buildAnnotatedString {
                        append(line.original.substring(0, split))
                        withStyle(SpanStyle(color = PlayerAccent)) {
                            append(line.original.substring(split))
                        }
                    }
                } else {
                    buildAnnotatedString { append(line.original) }
                }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isActive) textSizes.activeBottomPadding else textSizes.bottomPadding)
                        .clickable { onLyricClick(line.startTimeMs) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = original,
                    color =
                        if (isActive) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
                        },
                    fontSize = if (isActive) textSizes.activeFontSize else textSizes.fontSize,
                    lineHeight = if (isActive) textSizes.activeLineHeight else textSizes.lineHeight,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                line.secondary?.let { secondary ->
                    Text(
                        text = secondary,
                        modifier = Modifier.padding(top = textSizes.secondaryTopPadding),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isActive) 0.78f else 0.42f),
                        fontSize = textSizes.secondaryFontSize,
                        lineHeight = textSizes.secondaryLineHeight,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private data class LyricsTextSizes(
    val fontSize: androidx.compose.ui.unit.TextUnit,
    val lineHeight: androidx.compose.ui.unit.TextUnit,
    val activeFontSize: androidx.compose.ui.unit.TextUnit,
    val activeLineHeight: androidx.compose.ui.unit.TextUnit,
    val secondaryFontSize: androidx.compose.ui.unit.TextUnit,
    val secondaryLineHeight: androidx.compose.ui.unit.TextUnit,
    val secondaryTopPadding: androidx.compose.ui.unit.Dp,
    val bottomPadding: androidx.compose.ui.unit.Dp,
    val activeBottomPadding: androidx.compose.ui.unit.Dp,
)

private fun lyricsTextSizes(size: PlayerLyricsTextSize): LyricsTextSizes =
    when (size) {
        PlayerLyricsTextSize.Standard -> LyricsTextSizes(15.sp, 22.sp, 20.sp, 27.sp, 12.sp, 18.sp, 4.dp, 20.dp, 24.dp)
        PlayerLyricsTextSize.Large -> LyricsTextSizes(19.sp, 27.sp, 25.sp, 32.sp, 15.sp, 22.sp, 5.dp, 26.dp, 29.dp)
        PlayerLyricsTextSize.Largest -> LyricsTextSizes(22.sp, 30.sp, 28.sp, 37.sp, 18.sp, 26.sp, 6.dp, 31.dp, 34.dp)
    }

@Composable
private fun PlayerTopBar(
    onBack: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(MoeKoeTheme.dimensions.toolbarHeight)
                .padding(horizontal = MoeKoeTheme.spacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.player_collapse))
        }
        Text(
            text = stringResource(R.string.player_title),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onMore, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.player_more))
        }
    }
}

@Composable
private fun PlayerPageIndicator(activePage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PlayerPage.entries.size) { page ->
            if (page > 0) Spacer(Modifier.size(8.dp))
            Box(
                Modifier
                    .size(if (page == activePage) 8.dp else 7.dp)
                    .background(
                        if (page == activePage) PlayerAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun PlayerArtwork(
    item: PlaybackItem,
    artworkContent: (@Composable (PlaybackItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val model =
        when (val artwork = item.artwork) {
            is PlaybackArtwork.Remote -> artwork.value
            is PlaybackArtwork.AppFile -> File(context.filesDir, artwork.value)
            null -> null
        }
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = MoeKoeTheme.spacing.extraLarge),
        contentAlignment = Alignment.TopCenter,
    ) {
        val artworkSize = maxWidth.coerceAtMost(333.dp)
        Box(
            modifier =
                Modifier
                    .size(artworkSize)
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(68.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.player_artwork, item.title),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            artworkContent?.invoke(item)
        }
    }
}

@Composable
private fun PlayerControls(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlaybackItem,
    onTogglePlayback: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onChangeMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    showSecondaryActions: Boolean,
    compactLayout: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compactLayout) 26.dp else MoeKoeTheme.spacing.large),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = if (compactLayout) 18.sp else 20.sp,
                    lineHeight = if (compactLayout) 22.sp else 25.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artist,
                    modifier = Modifier.padding(top = 3.dp),
                    style = if (compactLayout) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PlayerQualityBadge(compact = compactLayout)
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget)) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.player_favorite),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        PlayerProgress(
            progress = progress,
            itemId = item.id,
            enabled = state.controlsEnabled,
            onSeek = onSeek,
            compact = compactLayout,
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(if (compactLayout) 76.dp else 88.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(Icons.Default.Shuffle, R.string.player_mode_shuffle, state.controlsEnabled, onChangeMode)
            PlayerIconButton(Icons.Default.SkipPrevious, R.string.player_previous, state.controlsEnabled, onPrevious)
            Surface(
                onClick = onTogglePlayback,
                enabled = state.controlsEnabled,
                modifier = Modifier.size(if (compactLayout) 68.dp else 72.dp),
                shape = CircleShape,
                color =
                    if (state.controlsEnabled) {
                        PlayerAccent
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                contentColor =
                    if (state.controlsEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            PlayerIconButton(Icons.Default.SkipNext, R.string.player_next, state.controlsEnabled, onNext)
            PlayerModeButton(state.mode, state.controlsEnabled, onChangeMode)
        }
        if (showSecondaryActions) {
            PlayerSecondaryActions(
                onDownload = onDownload,
                onAddToPlaylist = onAddToPlaylist,
                onShare = onShare,
                onOpenQueue = onOpenQueue,
            )
        }
    }
}

@Composable
private fun PlayerQualityBadge(compact: Boolean) {
    Surface(
        modifier = Modifier.padding(top = if (compact) 2.dp else 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = PlayerSecondaryContainer,
        contentColor = PlayerSecondaryContent,
    ) {
        Text(
            text = stringResource(R.string.player_quality_standard),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerSecondaryActions(
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(76.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerSecondaryAction(Icons.Default.Download, R.string.player_download, onDownload)
        PlayerSecondaryAction(Icons.AutoMirrored.Filled.PlaylistAdd, R.string.player_add_to_playlist, onAddToPlaylist)
        PlayerSecondaryAction(Icons.Default.Share, R.string.player_share, onShare)
        PlayerSecondaryAction(Icons.AutoMirrored.Filled.QueueMusic, R.string.player_queue, onOpenQueue)
    }
}

@Composable
private fun PlayerSecondaryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = PlayerSecondaryContainer,
        contentColor = PlayerSecondaryContent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, stringResource(label), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerProgress(
    progress: State<PlayerProgressUiState>,
    itemId: String,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    compact: Boolean,
) {
    var draggedPosition by remember(itemId) { mutableStateOf<Long?>(null) }
    val current = progress.value
    val duration = current.durationMs.coerceAtLeast(0)
    val shownPosition = (draggedPosition ?: current.positionMs).coerceIn(0, duration)
    val activeColor = PlayerAccent
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    Slider(
        value = shownPosition.toFloat(),
        onValueChange = { draggedPosition = it.roundToLong() },
        onValueChangeFinished = {
            draggedPosition?.let(onSeek)
            draggedPosition = null
        },
        valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
        enabled = enabled && duration > 0,
        modifier = Modifier.fillMaxWidth().then(if (compact) Modifier.height(34.dp) else Modifier),
        thumb = {
            Box(
                Modifier
                    .size(16.dp)
                    .background(
                        color = if (enabled && duration > 0) activeColor else activeColor.copy(alpha = 0.38f),
                        shape = CircleShape,
                    ),
            )
        },
        track = { sliderState ->
            PlayerProgressTrack(
                progress = sliderState.value / duration.coerceAtLeast(1).toFloat(),
                activeColor = if (enabled && duration > 0) activeColor else activeColor.copy(alpha = 0.38f),
                inactiveColor = if (enabled && duration > 0) inactiveColor else inactiveColor.copy(alpha = 0.38f),
            )
        },
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        PlayerTime(shownPosition, valid = duration > 0)
        PlayerTime((duration - shownPosition).coerceAtLeast(0), valid = duration > 0, remaining = true)
    }
}

@Composable
private fun PlayerProgressTrack(
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
        val centerY = size.height / 2f
        val strokeWidth = 4.dp.toPx()
        drawLine(
            color = inactiveColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = activeColor,
            start = Offset(0f, centerY),
            end = Offset(size.width * progress.coerceIn(0f, 1f), centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PlayerModeButton(
    mode: PlaybackMode,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val icon =
        when (mode) {
            PlaybackMode.Sequential, PlaybackMode.RepeatAll, PlaybackMode.Shuffle -> Icons.Default.Repeat
            PlaybackMode.RepeatOne -> Icons.Default.RepeatOne
        }
    val label =
        when (mode) {
            PlaybackMode.Sequential -> R.string.player_mode_sequence
            PlaybackMode.RepeatAll -> R.string.player_mode_repeat_all
            PlaybackMode.RepeatOne -> R.string.player_mode_repeat_one
            PlaybackMode.Shuffle -> R.string.player_mode_shuffle
        }
    PlayerIconButton(icon, label, enabled, onClick)
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(MoeKoeTheme.dimensions.minimumTouchTarget),
    ) {
        Icon(icon, stringResource(label), modifier = Modifier.size(MoeKoeTheme.dimensions.iconStandard))
    }
}

@Composable
private fun PlayerTime(
    millis: Long,
    valid: Boolean,
    remaining: Boolean = false,
) {
    Text(
        text =
            if (valid) {
                (if (remaining) "−" else "") + formatPlayerTime(millis)
            } else {
                stringResource(R.string.player_unknown_time)
            },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PlayerEmptyState(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.player_no_current),
            modifier = Modifier.padding(top = MoeKoeTheme.spacing.medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = MoeKoeTheme.spacing.large)) {
            Text(stringResource(R.string.player_back))
        }
    }
}

@Composable
private fun PlayerBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFF07101F))
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0x8C6A2C5C), Color.Transparent),
                    center = Offset(size.width * 0.48f, size.height * 0.06f),
                    radius = size.height * 0.31f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0xC06A2058), Color.Transparent),
                    center = Offset(-size.width * 0.08f, size.height * 0.34f),
                    radius = size.height * 0.42f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0x7A173B69), Color.Transparent),
                    center = Offset(size.width * 1.04f, size.height * 0.22f),
                    radius = size.height * 0.42f,
                ),
        )
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0x66552A58), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.52f),
                    radius = size.height * 0.34f,
                ),
        )
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0x26030A16), Color(0xB307101F)),
                    startY = size.height * 0.36f,
                    endY = size.height,
                ),
        )
    }
}

internal fun formatPlayerTime(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private val PlayerAccent = Color(0xFF9488FF)
private val PlayerSecondaryContainer = Color(0xFF252A43)
private val PlayerSecondaryContent = Color(0xFFC3BCFF)
