package cn.james.music.feature.player

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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.action.MoeButton

@Composable
internal fun PlayerLyricsPage(
    state: PlayerUiState,
    progress: State<PlayerProgressUiState>,
    item: PlayerItemUiModel,
    lyricsState: PlayerLyricsUiState,
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
                    onLyricClick = onLyricClick,
                    modifier = Modifier.offset(y = lyricsContentOffset(state)),
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
    onLyricClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPlayerPalette.current
    val textSizes = lyricsTextSizes(state.textSize)
    val timing = progress.value
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
        PlayerLyricsTextSize.Standard -> LyricsTextSizes(15.sp, 22.sp, 20.sp, 27.sp, 12.sp, 18.sp, 4.dp, 17.dp, 21.dp)
        PlayerLyricsTextSize.Large -> LyricsTextSizes(19.sp, 27.sp, 25.sp, 32.sp, 15.sp, 22.sp, 5.dp, 23.dp, 26.dp)
        PlayerLyricsTextSize.Largest -> LyricsTextSizes(22.sp, 30.sp, 28.sp, 37.sp, 18.sp, 26.sp, 6.dp, 28.dp, 31.dp)
    }

private fun lyricsContentOffset(state: PlayerLyricsUiState.Content) =
    when (state.textSize) {
        PlayerLyricsTextSize.Standard ->
            when {
                state.lines.all { it.secondary == null } -> (-65).dp
                state.lines.size <= 4 -> (-58).dp
                else -> 18.dp
            }

        PlayerLyricsTextSize.Large -> (-29).dp
        PlayerLyricsTextSize.Largest -> (-44).dp
    }
