package cn.james.music.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.component.MoeSongRow
import cn.james.music.core.model.online.SearchError
import cn.james.music.core.model.online.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onPlay: (Song) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("搜索") },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("歌曲、歌手或专辑") },
                singleLine = true,
            )
            Button(onClick = onSearch, enabled = state.query.isNotBlank() && !state.loading) { Text("搜索") }
        }
        when {
            state.loading -> SearchMessage(progress = true, title = "正在搜索", message = "正在从酷狗获取歌曲")
            state.error != null && state.songs.isEmpty() ->
                SearchMessage(
                    title = "搜索失败",
                    message = state.error.message,
                    action = "重试",
                    onAction = onSearch,
                )
            !state.hasSearched -> SearchMessage(title = "想听什么？", message = "输入歌曲、歌手或专辑名称开始搜索")
            state.songs.isEmpty() -> SearchMessage(title = "没有找到结果", message = "试试更短的关键词或其他写法")
            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    items(state.songs, key = Song::id) { song ->
                        SearchSongRow(song, onClick = { onPlay(song) })
                        HorizontalDivider()
                    }
                    if (state.hasMore || state.loadingMore || state.error != null) {
                        item(key = "load-more") {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                when {
                                    state.loadingMore -> CircularProgressIndicator(Modifier.size(28.dp))
                                    state.error != null -> TextButton(onClick = onLoadMore) { Text("加载失败，重试") }
                                    else -> TextButton(onClick = onLoadMore) { Text("加载更多") }
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun SearchSongRow(
    song: Song,
    onClick: () -> Unit,
) {
    MoeSongRow(
        title = song.title,
        subtitle = song.artistName.ifBlank { "未知艺术家" },
        metadata = formatDuration(song.durationMs),
        onClick = onClick,
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

@Composable
private fun SearchMessage(
    title: String,
    message: String,
    progress: Boolean = false,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (progress) CircularProgressIndicator(Modifier.padding(bottom = 20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(message, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.let { TextButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(it) } }
    }
}

private val SearchError.message: String
    get() =
        when (this) {
            SearchError.Offline -> "当前没有网络连接"
            SearchError.Timeout -> "请求超时，请稍后重试"
            SearchError.Connection -> "无法连接音乐服务"
            SearchError.VerificationRequired -> "服务需要安全验证，请稍后重试"
            SearchError.AuthenticationRequired -> "当前接口需要登录后使用"
            SearchError.ServiceUnavailable -> "音乐服务暂时不可用"
            SearchError.Protocol -> "服务响应发生变化，请更新应用"
            SearchError.SessionInitialization -> "匿名会话初始化失败"
        }

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1_000
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "${totalSeconds / 60}:$seconds"
}
