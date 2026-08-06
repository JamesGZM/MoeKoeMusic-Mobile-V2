package cn.james.music.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeSectionHeader
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarTone
import cn.james.music.core.designsystem.component.MoeSongRow

@OptIn(ExperimentalMaterial3Api::class)
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
                HomeContentUiState.Empty -> HomeMessage(onSearch, "还没有推荐内容", "稍后再来看看，或立即重试", onRefresh)
                is HomeContentUiState.Failure ->
                    HomeMessage(onSearch, "首页暂时不可用", content.problem.blockingMessage, onRefresh)
                is HomeContentUiState.Content -> HomeContent(content.value, onSearch, onPlay)
            }
        }
        state.refreshProblem?.let { problem ->
            MoeSnackbar(
                message = problem.refreshMessage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                tone = MoeSnackbarTone.Warning,
                actionLabel = "知道了",
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MoeKoeTheme.spacing.large),
    ) {
        item { HomeHeader(onSearch) }
        if (content.banners.isNotEmpty()) {
            item { HomeBannerCard(content.banners.first()) }
        }
        if (content.recommendations.isNotEmpty()) {
            item {
                MoeSectionHeader(
                    title = "每日推荐",
                    supportingText = "为你挑选的今日好歌",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(content.recommendations.take(6), key = HomeSongUi::id) { song ->
                MoeSongRow(
                    title = song.title,
                    subtitle = song.artistName.ifBlank { "未知艺术家" },
                    metadata = formatDuration(song.durationMs),
                    onClick = { onPlay(song) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    artwork = {
                        AsyncImage(
                            model = song.artworkUrl,
                            contentDescription = "${song.title} 封面",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    },
                )
            }
        }
        if (content.playlists.isNotEmpty()) {
            item {
                MoeSectionHeader(
                    title = "推荐歌单",
                    supportingText = "按此刻心情选一张歌单",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(content.playlists, key = HomePlaylistUi::id) { playlist -> HomePlaylistCard(playlist) }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onSearch: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            "MoeKoe Air",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "轻松发现下一首喜欢的音乐",
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).clickable(onClick = onSearch),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "搜索音乐、歌手、歌单…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeBannerCard(banner: HomeBannerUi) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column {
            AsyncImage(
                model = banner.artworkUrl,
                contentDescription = banner.title ?: "首页推荐",
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop,
            )
            banner.title?.let {
                Text(it, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun HomePlaylistCard(playlist: HomePlaylistUi) {
    Column(Modifier.width(148.dp)) {
        Box(
            modifier = Modifier.size(148.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = playlist.artworkUrl,
                contentDescription = "${playlist.title} 封面",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (playlist.artworkUrl == null) {
                Text("♪", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            playlist.title,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        playlist.playCount?.let {
            Text(formatPlayCount(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeLoading(onSearch: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { HomeHeader(onSearch) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                Text("正在准备今日推荐", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) { Text("重试") }
            }
        }
    }
}

private val HomeProblemUi.blockingMessage: String
    get() =
        when (this) {
            HomeProblemUi.SessionInitialization -> "音乐服务初始化失败，请重试"
            HomeProblemUi.Storage -> "本地内容读取失败，请重试"
            HomeProblemUi.Offline -> "当前没有网络连接，请联网后重试"
            HomeProblemUi.Timeout -> "刷新超时，请稍后重试"
            HomeProblemUi.Connection -> "无法连接音乐服务"
            HomeProblemUi.ServiceUnavailable -> "音乐服务暂时不可用"
            HomeProblemUi.Rejected -> "服务暂时拒绝了请求，请稍后重试"
            HomeProblemUi.Protocol -> "服务响应发生变化，请更新应用"
        }

private val HomeProblemUi.refreshMessage: String
    get() =
        if (this == HomeProblemUi.Offline) {
            "当前没有网络连接，已保留上次内容"
        } else {
            blockingMessage
        }

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1_000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

private fun formatPlayCount(count: Long): String =
    when {
        count >= 100_000_000 -> "${count / 100_000_000} 亿次播放"
        count >= 10_000 -> "${count / 10_000} 万次播放"
        else -> "$count 次播放"
    }
