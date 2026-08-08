package cn.james.music.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.MoeMediaBadge
import cn.james.music.core.designsystem.component.MoeMediaBadgeTone
import cn.james.music.core.designsystem.component.MoeArtwork
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarTone

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onDismissProblem: () -> Unit,
    onPlay: (HomeSongUi) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val content = state.content) {
                HomeContentUiState.Loading -> HomeLoading(onSearch)
                HomeContentUiState.Empty ->
                    HomeMessage(
                        onSearch = onSearch,
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        onRetry = onRefresh,
                    )
                is HomeContentUiState.Failure ->
                    HomeMessage(
                        onSearch = onSearch,
                        title = stringResource(R.string.home_failure_title),
                        message = homeProblemMessage(content.problem),
                        onRetry = onRefresh,
                    )
                is HomeContentUiState.Content -> HomeContent(content.value, onSearch, onPlay)
            }
        }
        state.refreshProblem?.let { problem ->
            MoeSnackbar(
                message = if (problem == HomeProblemUi.Offline) stringResource(R.string.home_refresh_offline) else homeProblemMessage(problem),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                tone = MoeSnackbarTone.Warning,
                actionLabel = stringResource(R.string.home_acknowledge),
                onAction = onDismissProblem,
            )
        }
    }
}

@Composable
private fun HomeContent(
    content: HomeContentUi,
    onSearch: () -> Unit,
    onPlay: (HomeSongUi) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("home_content"),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item { HomeHeader(onSearch) }
        item { HomeRadioHero() }
        item { HomeQuickEntries() }
        item { Spacer(Modifier.height(2.dp)) }
        if (content.recommendations.isNotEmpty()) {
            item { HomeSectionHeader(stringResource(R.string.home_daily_title)) }
            items(content.recommendations.take(4), key = HomeSongUi::id) { song ->
                HomeRecommendationRow(song = song, onPlay = onPlay)
            }
        }
        if (content.playlists.isNotEmpty()) {
            item { Spacer(Modifier.height(12.dp)) }
            item { HomeSectionHeader(stringResource(R.string.home_playlist_title)) }
            item { HomePlaylistGrid(content.playlists.take(4)) }
        }
    }
}

@Composable
private fun HomeHeader(onSearch: () -> Unit) {
    val reflow = LocalDensity.current.fontScale >= 1.8f
    if (reflow) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HomeBrand()
                Spacer(Modifier.weight(1f))
                HomeAvatar()
            }
            HomeSearch(onSearch = onSearch, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 12.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeBrand()
            Spacer(Modifier.width(16.dp))
            HomeSearch(onSearch = onSearch, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            HomeAvatar()
        }
    }
}

@Composable
private fun HomeBrand() {
    Text(
        text = stringResource(R.string.home_brand),
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 23.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
    )
}

@Composable
private fun HomeSearch(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(48.dp).clickable(onClick = onSearch),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.home_search),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeAvatar() {
    Image(
        painter = painterResource(R.drawable.home_toolbar_avatar),
        contentDescription = stringResource(R.string.home_account_avatar_description),
        modifier = Modifier.size(40.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun HomeRadioHero() {
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.5f
    val heroHeight = if (largeText) 190.dp + ((fontScale - 1f) * 120f).dp else 190.dp
    val copyWidth = if (largeText) 0.72f else 0.58f
    val darkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.35f
    val textColor = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    val supportingColor = if (darkSurface) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .height(heroHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .testTag("home_radio_hero"),
    ) {
        Image(
            painter = painterResource(R.drawable.home_radio_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors =
                            if (darkSurface) {
                                listOf(Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            } else {
                                listOf(Color.White.copy(alpha = 0.96f), Color.White.copy(alpha = 0.72f), Color.Transparent)
                            },
                        startX = 0f,
                        endX = 820f,
                    ),
                ),
        )
        Column(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(copyWidth).padding(start = 14.dp, top = 5.dp, bottom = 14.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = stringResource(R.string.home_radio_title),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 26.sp),
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = if (largeText) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_radio_subtitle),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp),
                fontWeight = FontWeight.Medium,
                color = supportingColor,
                maxLines = if (largeText) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.home_radio_supporting),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = supportingColor,
                maxLines = if (largeText) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 44.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text(stringResource(R.string.home_radio_play), fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
                if (!largeText) {
                    Row(
                        modifier = Modifier.padding(start = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        listOf(10.dp, 18.dp, 12.dp).forEach { height ->
                            Box(Modifier.width(3.dp).height(height).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickEntries() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp).heightIn(min = 64.dp).testTag("home_quick_entries"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickEntry(
                icon = Icons.Default.BarChart,
                iconColor = Color(0xFF7C2CFF),
                containerColor = Color(0xFFF1E7FF),
                title = stringResource(R.string.home_quick_ranking),
                supporting = stringResource(R.string.home_quick_ranking_supporting),
                modifier = Modifier.weight(1f),
            )
            QuickDivider()
            QuickEntry(
                icon = Icons.Default.CalendarMonth,
                iconColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                title = stringResource(R.string.home_quick_daily),
                supporting = stringResource(R.string.home_quick_daily_supporting),
                modifier = Modifier.weight(1f),
            )
            QuickDivider()
            QuickEntry(
                icon = Icons.Default.LibraryMusic,
                iconColor = Color(0xFF12A665),
                containerColor = Color(0xFFE3F5EB),
                title = stringResource(R.string.home_quick_playlists),
                supporting = stringResource(R.string.home_quick_playlists_supporting),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickEntry(
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
    title: String,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    val largeText = LocalDensity.current.fontScale >= 1.5f
    if (largeText) {
        Column(
            modifier = modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            QuickEntryIcon(icon, iconColor, containerColor)
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Text(
                supporting,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            QuickEntryIcon(icon, iconColor, containerColor)
            Column(Modifier.padding(start = 7.dp)) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    supporting,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun QuickEntryIcon(
    icon: ImageVector,
    iconColor: Color,
    containerColor: Color,
) {
    Surface(shape = RoundedCornerShape(11.dp), color = containerColor) {
        Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp).size(22.dp), tint = iconColor)
    }
}

@Composable
private fun QuickDivider() {
    val height = if (LocalDensity.current.fontScale >= 1.5f) 80.dp else 32.dp
    Box(Modifier.padding(horizontal = 5.dp).width(1.dp).height(height).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun HomeSectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).padding(start = 14.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 20.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 20.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(stringResource(R.string.home_more), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun HomeRecommendationRow(
    song: HomeSongUi,
    onPlay: (HomeSongUi) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onPlay(song) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MoeArtwork(size = 36.dp) {
                HomeArtwork(
                    title = song.title,
                    artworkUrl = song.artworkUrl,
                    previewArtworkRes = song.previewArtworkRes,
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artistName.ifBlank { stringResource(R.string.home_unknown_artist) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            song.previewBadge?.let { badge ->
                MoeMediaBadge(
                    text = badge,
                    tone = if (song.previewBadgeIsError) MoeMediaBadgeTone.Error else MoeMediaBadgeTone.Primary,
                )
            }
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 46.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun HomePlaylistGrid(playlists: List<HomePlaylistUi>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        playlists.forEach { playlist ->
            HomePlaylistCard(playlist = playlist, modifier = Modifier.weight(1f))
        }
        repeat((4 - playlists.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun HomePlaylistCard(
    playlist: HomePlaylistUi,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        HomeArtwork(
            title = playlist.title,
            artworkUrl = playlist.artworkUrl,
            previewArtworkRes = playlist.previewArtworkRes,
            modifier = Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(11.dp)),
        )
        Text(
            playlist.title,
            modifier = Modifier.padding(top = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val playCount = playlist.playCount
        val subtitle = playlist.previewSubtitle ?: if (playCount == null) null else formatPlayCount(playCount)
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeArtwork(
    title: String,
    artworkUrl: String?,
    @DrawableRes previewArtworkRes: Int?,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val description = stringResource(R.string.home_artwork_description, title)
    when {
        previewArtworkRes != null ->
            Image(
                painter = painterResource(previewArtworkRes),
                contentDescription = description,
                modifier = modifier,
                contentScale = ContentScale.Crop,
            )
        artworkUrl != null ->
            AsyncImage(
                model = artworkUrl,
                contentDescription = description,
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentScale = ContentScale.Crop,
            )
        else ->
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = description, tint = MaterialTheme.colorScheme.primary)
            }
    }
}

@Composable
private fun HomeLoading(onSearch: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { HomeHeader(onSearch) }
        item { HomeRadioHero() }
        item { HomeQuickEntries() }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.home_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeMessage(
    onSearch: () -> Unit,
    title: String,
    message: String,
    onRetry: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { HomeHeader(onSearch) }
        item { HomeRadioHero() }
        item { HomeQuickEntries() }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    Text(stringResource(R.string.home_retry))
                }
            }
        }
    }
}

@Composable
private fun homeProblemMessage(problem: HomeProblemUi): String =
    stringResource(
        when (problem) {
            HomeProblemUi.SessionInitialization -> R.string.home_problem_session
            HomeProblemUi.Storage -> R.string.home_problem_storage
            HomeProblemUi.Offline -> R.string.home_problem_offline
            HomeProblemUi.Timeout -> R.string.home_problem_timeout
            HomeProblemUi.Connection -> R.string.home_problem_connection
            HomeProblemUi.ServiceUnavailable -> R.string.home_problem_unavailable
            HomeProblemUi.Rejected -> R.string.home_problem_rejected
            HomeProblemUi.Protocol -> R.string.home_problem_protocol
        },
    )

@Composable
private fun formatPlayCount(count: Long): String =
    when {
        count >= 100_000_000 -> stringResource(R.string.home_play_count_hundred_million, count / 100_000_000)
        count >= 10_000 -> stringResource(R.string.home_play_count_ten_thousand, count / 10_000)
        else -> stringResource(R.string.home_play_count_plain, count)
    }
