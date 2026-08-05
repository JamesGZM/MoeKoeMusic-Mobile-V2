package cn.james.music.features.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import cn.james.music.core.model.online.SearchError
import cn.james.music.core.model.online.SearchRepository
import cn.james.music.core.model.online.SearchResult
import cn.james.music.core.model.online.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val songs: List<Song> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: SearchError? = null,
) {
    val hasSearched: Boolean get() = submittedQuery.isNotEmpty()
}

@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        private val repository: SearchRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(SearchUiState())
        val state: StateFlow<SearchUiState> = mutableState.asStateFlow()
        private var searchJob: Job? = null
        private var requestGeneration = 0L

        fun updateQuery(value: String) {
            mutableState.value = mutableState.value.copy(query = value)
        }

        fun submit() {
            val keyword = mutableState.value.query.trim()
            if (keyword.isEmpty()) return
            searchJob?.cancel()
            val generation = ++requestGeneration
            mutableState.value = SearchUiState(query = mutableState.value.query, submittedQuery = keyword, loading = true)
            searchJob =
                viewModelScope.launch {
                    val result = repository.searchSongs(keyword, page = 1)
                    if (generation != requestGeneration) return@launch
                    mutableState.value = result.toUiState(query = mutableState.value.query, submittedQuery = keyword)
                }
        }

        fun loadMore() {
            val current = mutableState.value
            if (current.loading || current.loadingMore || !current.hasMore || current.submittedQuery.isEmpty()) return
            val generation = requestGeneration
            mutableState.value = current.copy(loadingMore = true, error = null)
            searchJob =
                viewModelScope.launch {
                    when (val result = repository.searchSongs(current.submittedQuery, page = current.page + 1)) {
                        is SearchResult.Failure -> {
                            if (generation == requestGeneration) {
                                mutableState.value = mutableState.value.copy(loadingMore = false, error = result.error)
                            }
                        }

                        is SearchResult.Success -> {
                            if (generation == requestGeneration) {
                                mutableState.value =
                                    mutableState.value.copy(
                                        songs = (current.songs + result.page.items).distinctBy(Song::id),
                                        page = result.page.page,
                                        hasMore = result.page.hasMore,
                                        loadingMore = false,
                                    )
                            }
                        }
                    }
                }
        }

        private fun SearchResult.toUiState(
            query: String,
            submittedQuery: String,
        ): SearchUiState =
            when (this) {
                is SearchResult.Failure -> SearchUiState(query = query, submittedQuery = submittedQuery, error = error)
                is SearchResult.Success ->
                    SearchUiState(
                        query = query,
                        submittedQuery = submittedQuery,
                        songs = page.items,
                        page = page.page,
                        hasMore = page.hasMore,
                    )
            }
    }

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    SearchScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::updateQuery,
        onSearch = viewModel::submit,
        onLoadMore = viewModel::loadMore,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
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
                        SearchSongRow(song)
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
private fun SearchSongRow(song: Song) {
    Row(
        modifier = Modifier.fillMaxWidth().height(76.dp).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = "${song.title} 封面",
            modifier =
                Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(song.artistName.ifBlank { "未知艺术家" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatDuration(song.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
