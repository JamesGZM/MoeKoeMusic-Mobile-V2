package cn.james.music.feature.player

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.action.MoeButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlin.math.abs

@Composable
internal fun PlayerLyricsPage(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlayerItemUiModel,
    lyricsState: PlayerLyricsUiState,
    showSupplementalText: Boolean,
    lyricsTextSize: PlayerLyricsTextSize,
    lyricsProgress: State<PlayerLyricsProgressUiState>,
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
    val palette = LocalPlayerPalette.current
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LyricsViewport(
                state = lyricsState,
                progress = lyricsProgress,
                showSupplementalText = showSupplementalText,
                lyricsTextSize = lyricsTextSize,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, top = 30.dp, end = 28.dp, bottom = 12.dp)
                        .playerLayoutProbe(PLAYER_PROBE_VIEWPORT),
                onRetry = onRetryLyrics,
                onLyricClick = onLyricClick,
            )
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 12.dp)
                        .size(MoeKoeTheme.dimensions.minimumTouchTarget)
                        .playerLayoutProbe(PLAYER_PROBE_SETTINGS)
                        .clickable(onClick = onLyricsSettings),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(35.dp),
                    shape = CircleShape,
                    color = palette.secondaryContainer.copy(alpha = 0.9f),
                    contentColor = palette.accent,
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
        PlayerPageIndicator(
            activePage = activePage,
            modifier = Modifier.playerLayoutProbe(PLAYER_PROBE_INDICATOR),
        )
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
    progress: State<PlayerLyricsProgressUiState>,
    showSupplementalText: Boolean,
    lyricsTextSize: PlayerLyricsTextSize,
    onRetry: () -> Unit,
    onLyricClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (state) {
            PlayerLyricsUiState.Unrequested,
            PlayerLyricsUiState.Loading,
            -> LyricsLoadingState(modifier = Modifier.offset(y = (-45).dp))
            PlayerLyricsUiState.Empty ->
                LyricsMessageState(
                    icon = Icons.Default.MusicOff,
                    title = stringResource(R.string.player_lyrics_empty_title),
                    message = stringResource(R.string.player_lyrics_empty_message),
                    onRetry = onRetry,
                    modifier = Modifier,
                )

            PlayerLyricsUiState.Offline ->
                LyricsMessageState(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.player_lyrics_offline_title),
                    message = stringResource(R.string.player_lyrics_offline_message),
                    onRetry = onRetry,
                    modifier = Modifier,
                )

            PlayerLyricsUiState.Error ->
                LyricsMessageState(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.player_lyrics_error_title),
                    message = stringResource(R.string.player_lyrics_error_message),
                    onRetry = onRetry,
                    modifier = Modifier,
                )

            is PlayerLyricsUiState.Content ->
                LyricsContent(
                    state = state,
                    progress = progress,
                    showSupplementalText = showSupplementalText,
                    lyricsTextSize = lyricsTextSize,
                    onLyricClick = onLyricClick,
                    modifier =
                        Modifier.offset(
                            y =
                                lyricsTextSize.contentOffsetFor(
                                    lines = state.lines,
                                    showSupplementalText = showSupplementalText,
                                ),
                        ),
                )
        }
    }
}

@Composable
private fun LyricsLoadingState(modifier: Modifier = Modifier) {
    val palette = LocalPlayerPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = palette.accent,
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
    modifier: Modifier = Modifier,
) {
    val palette = LocalPlayerPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = palette.secondaryContainer.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.secondaryContent.copy(alpha = 0.18f)),
            contentColor = palette.secondaryContent,
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
        MoeButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 18.dp).widthIn(min = 112.dp),
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
    progress: State<PlayerLyricsProgressUiState>,
    showSupplementalText: Boolean,
    lyricsTextSize: PlayerLyricsTextSize,
    onLyricClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPlayerPalette.current
    val textSizes = lyricsTextSizes(lyricsTextSize)
    val timing = progress.value
    val scrollState = rememberScrollState()
    val coordinator = remember { PlayerLyricsAutoFollowCoordinator() }
    val userDragObserver = remember { PlayerLyricsUserDragObserver(coordinator) }
    val geometryKey = remember(state.lines, showSupplementalText, lyricsTextSize) { Any() }
    val lineLayouts = remember(geometryKey) { mutableStateMapOf<Int, LyricsLineLayout>() }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var layoutVersion by remember { mutableIntStateOf(0) }
    var interactionVersion by remember { mutableIntStateOf(0) }
    var reserveFollowSpace by remember(geometryKey) { mutableStateOf(false) }
    val followPadding = with(LocalDensity.current) { (viewportHeightPx * LYRICS_AUTO_FOLLOW_VIEWPORT_FRACTION).toInt().toDp() }
    val dragObserver =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (
                        source == NestedScrollSource.UserInput &&
                            abs(available.y) > abs(available.x) &&
                            available.y != 0f &&
                            userDragObserver.onUserVerticalScroll(SystemClock.elapsedRealtime())
                    ) {
                        interactionVersion += 1
                    }
                    return Offset.Zero
                }
            }
        }

    LaunchedEffect(scrollState, userDragObserver) {
        var wasScrolling = scrollState.isScrollInProgress
        snapshotFlow { scrollState.isScrollInProgress }.collect { isScrolling ->
            if (wasScrolling && !isScrolling) {
                userDragObserver.onScrollIdle()
            }
            wasScrolling = isScrolling
        }
    }

    DisposableEffect(state.lines) {
        coordinator.onDocumentChanged()
        onDispose {
            coordinator.onDisposed()
        }
    }

    DisposableEffect(geometryKey) {
        coordinator.onLayoutChanged()
        layoutVersion += 1
        onDispose {}
    }

    LaunchedEffect(state.lines) {
        scrollState.scrollTo(0)
    }

    LaunchedEffect(geometryKey, interactionVersion) {
        val remainingMs = coordinator.resumeDelayRemainingMs(SystemClock.elapsedRealtime())
        if (remainingMs > 0L) {
            delay(remainingMs)
            interactionVersion += 1
        }
    }

    LaunchedEffect(
        geometryKey,
        timing.activeLineIndex,
        interactionVersion,
        layoutVersion,
        viewportHeightPx,
        scrollState.maxValue,
    ) {
        val request =
            coordinator.request(
                activeIndex = timing.activeLineIndex,
                lineCount = state.lines.size,
                nowMs = SystemClock.elapsedRealtime(),
            ) ?: return@LaunchedEffect
        val lineLayout = lineLayouts[request.activeIndex] ?: return@LaunchedEffect
        if (viewportHeightPx == 0 || scrollState.maxValue == 0 || !coordinator.isCurrent(request)) return@LaunchedEffect

        val target =
            (lineLayout.topPx + lineLayout.heightPx / 2f - viewportHeightPx * LYRICS_AUTO_FOLLOW_VIEWPORT_FRACTION)
                .toInt()
                .coerceIn(0, scrollState.maxValue)
        scrollState.animateScrollTo(target)
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .onSizeChanged { viewportHeightPx = it.height }
                .nestedScroll(dragObserver)
                .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (reserveFollowSpace) {
            Spacer(Modifier.height(followPadding))
        }
        state.lines.forEachIndexed { index, line ->
            val isActive = index == timing.activeLineIndex
            val original =
                if (isActive && timing.highlightedPrefixCharacterCount > 0) {
                    val split = timing.highlightedPrefixCharacterCount.coerceIn(0, line.original.length)
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = palette.accent)) {
                            append(line.original.substring(0, split))
                        }
                        append(line.original.substring(split))
                    }
                } else {
                    buildAnnotatedString { append(line.original) }
                }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isActive) textSizes.activeBottomPadding else textSizes.bottomPadding)
                        .onPlaced { coordinates ->
                            // The row's direct parent is this scrolling Column, so this is stable
                            // content space; verticalScroll translates the Column relative to its viewport.
                            val layout = LyricsLineLayout(coordinates.positionInParent().y.toInt(), coordinates.size.height)
                            if (lineLayouts[index] != layout) {
                                lineLayouts[index] = layout
                                layoutVersion += 1
                            }
                            if (!reserveFollowSpace && shouldReserveLyricsAutoFollowSpace(layout.bottomPx, viewportHeightPx)) {
                                reserveFollowSpace = true
                            }
                        }
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
                line.secondaryForDisplay(showSupplementalText)?.let { secondary ->
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
        if (reserveFollowSpace) {
            Spacer(Modifier.height(followPadding))
        }
    }
}

private data class LyricsLineLayout(
    val topPx: Int,
    val heightPx: Int,
) {
    val bottomPx: Int get() = topPx + heightPx
}

private const val LYRICS_AUTO_FOLLOW_VIEWPORT_FRACTION = 0.42f

internal fun shouldReserveLyricsAutoFollowSpace(
    lineBottomPx: Int,
    viewportHeightPx: Int,
): Boolean = viewportHeightPx > 0 && lineBottomPx > viewportHeightPx

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
        PlayerLyricsTextSize.Standard -> LyricsTextSizes(15.sp, 22.sp, 20.sp, 27.sp, 12.sp, 18.sp, 4.dp, 17.dp, 21.dp)
        PlayerLyricsTextSize.Large -> LyricsTextSizes(19.sp, 27.sp, 25.sp, 32.sp, 15.sp, 22.sp, 5.dp, 23.dp, 26.dp)
        PlayerLyricsTextSize.Largest -> LyricsTextSizes(22.sp, 30.sp, 28.sp, 37.sp, 18.sp, 26.sp, 6.dp, 28.dp, 31.dp)
    }

internal fun PlayerLyricsTextSize.contentOffsetFor(
    lines: List<PlayerLyricLineUi>,
    showSupplementalText: Boolean,
) =
    when (this) {
        PlayerLyricsTextSize.Standard ->
            when {
                !showSupplementalText || lines.all { it.secondary == null } -> (-65).dp
                lines.size <= 4 -> (-58).dp
                else -> 18.dp
            }

        PlayerLyricsTextSize.Large -> (-29).dp
        PlayerLyricsTextSize.Largest -> (-44).dp
    }
