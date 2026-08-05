@file:Suppress("ktlint:standard:max-line-length")

package cn.james.music.features.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportProgress
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicRepository
import cn.james.music.core.model.local.LocalMusicSort
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.playback.PlaybackController
import cn.james.music.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import javax.inject.Inject

enum class MainTab(val label: String) { Home("首页"), Discover("发现"), My("我的") }
enum class AppPage { Tabs, LocalMusic, DeviceScan, FoundationLab }

data class MoeKoeAppState(
    val music: List<LocalMusic> = emptyList(),
    val imports: List<LocalImportProgress> = emptyList(),
    val playback: PlaybackState = PlaybackState(),
    val candidates: List<DeviceAudioCandidate> = emptyList(),
)

@HiltViewModel
class MoeKoeAppViewModel @Inject constructor(
    private val repository: LocalMusicRepository,
    private val playbackController: PlaybackController,
) : ViewModel() {
    private val candidates = kotlinx.coroutines.flow.MutableStateFlow<List<DeviceAudioCandidate>>(emptyList())
    val state: StateFlow<MoeKoeAppState> = combine(
        repository.observeMusic(), repository.observeImports(), playbackController.state, candidates,
    ) { music, imports, playback, scanned -> MoeKoeAppState(music, imports, playback, scanned) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoeKoeAppState())

    fun scanDevice() { viewModelScope.launch { candidates.value = repository.scanDevice() } }
    fun play(music: LocalMusic, visible: List<LocalMusic>) {
        viewModelScope.launch {
            val queue = visible.map(LocalMusic::toPlaybackItem)
            playbackController.replaceQueue(queue, queue.indexOfFirst { it.id == music.id }, true)
        }
    }
    fun togglePlayback() { viewModelScope.launch { if (state.value.playback.isPlaying) playbackController.pause() else playbackController.play() } }
    fun playAt(index: Int) { viewModelScope.launch { playbackController.playAt(index) } }
    fun removeAt(index: Int) { viewModelScope.launch { playbackController.remove(index) } }
    fun clearQueue() { viewModelScope.launch { playbackController.clear() } }
    fun cancelImport(batchId: String) { viewModelScope.launch { repository.cancelImport(batchId) } }
    fun delete(id: String) {
        viewModelScope.launch {
            state.value.playback.queue
                .mapIndexedNotNull { index, item -> index.takeIf { item.id == id } }
                .asReversed()
                .forEach { playbackController.remove(it) }
            repository.delete(id)
        }
    }
}

private fun LocalMusic.toPlaybackItem() = PlaybackItem(id, title, artist, albumTitle, PlaybackSource.ImportedLocal(id))

@Composable
fun MoeKoeAppRoute(
    onChooseFiles: () -> Unit,
    onRequestDeviceScan: (() -> Unit) -> Unit,
    onImportCandidates: (List<Long>) -> Unit,
    openFoundationLab: @Composable (() -> Unit)? = null,
    viewModel: MoeKoeAppViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var page by remember { mutableStateOf(AppPage.Tabs) }
    var tab by remember { mutableStateOf(MainTab.Home) }
    var queueVisible by remember { mutableStateOf(false) }
    if (page == AppPage.FoundationLab && openFoundationLab != null) { openFoundationLab(); return }
    Scaffold(
        bottomBar = {
            Column {
                state.playback.currentItem?.let { item ->
                    MiniPlayer(item.title, item.artist, state.playback.isPlaying, viewModel::togglePlayback) { queueVisible = true }
                }
                if (page == AppPage.Tabs) NavigationBar {
                    MainTab.entries.forEach { item ->
                        NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Text(item.label.take(1)) }, label = { Text(item.label) })
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (page) {
                AppPage.Tabs -> when (tab) {
                    MainTab.Home -> PlaceholderPage("首页", "在线推荐将在酷狗在线闭环完成后出现")
                    MainTab.Discover -> PlaceholderPage("发现", "排行榜、歌单和电台仍在规划阶段")
                    MainTab.My -> MyScreen(onLocalMusic = { page = AppPage.LocalMusic }, onFoundationLab = { page = AppPage.FoundationLab }, showLab = openFoundationLab != null)
                }
                AppPage.LocalMusic -> LocalMusicScreen(state, onBack = { page = AppPage.Tabs }, onChooseFiles, onScan = { page = AppPage.DeviceScan; onRequestDeviceScan(viewModel::scanDevice) }, onPlay = viewModel::play, onDelete = viewModel::delete, onCancelImport = viewModel::cancelImport)
                AppPage.DeviceScan -> DeviceScanScreen(state.candidates, onBack = { page = AppPage.LocalMusic }, onRefresh = { onRequestDeviceScan(viewModel::scanDevice) }, onImport = { onImportCandidates(it); page = AppPage.LocalMusic })
                AppPage.FoundationLab -> Unit
            }
        }
    }
    if (queueVisible) QueueSheet(state.playback, onDismiss = { queueVisible = false }, viewModel::playAt, viewModel::removeAt, viewModel::clearQueue)
}

@Composable private fun PlaceholderPage(title: String, message: String) = Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }

@Composable private fun MyScreen(onLocalMusic: () -> Unit, onFoundationLab: () -> Unit, showLab: Boolean) = LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    item { Text("我的", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text("登录后同步收藏与音乐资产", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("每日签到") }; Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("VIP 领取") } } }
    item { LibraryCard("本地音乐", "导入并离线播放设备上的音乐", onLocalMusic) }
    item { LibraryCard("我喜欢", "等待账号阶段接入", {}) }
    item { LibraryCard("创建与收藏的歌单", "等待账号阶段接入", {}) }
    if (showLab) item { TextButton(onClick = onFoundationLab) { Text("打开播放工程实验台") } }
}

@Composable private fun LibraryCard(title: String, subtitle: String, onClick: () -> Unit) = Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).clickable(onClick = onClick).padding(20.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun LocalMusicScreen(state: MoeKoeAppState, onBack: () -> Unit, onChooseFiles: () -> Unit, onScan: () -> Unit, onPlay: (LocalMusic, List<LocalMusic>) -> Unit, onDelete: (String) -> Unit, onCancelImport: (String) -> Unit) {
    var deleting by remember { mutableStateOf<LocalMusic?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(LocalMusicSort.Newest) }
    val visible =
        state.music
            .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
            .let { music ->
                when (sort) {
                    LocalMusicSort.Newest -> music.sortedByDescending(LocalMusic::importedAtEpochMs)
                    LocalMusicSort.Title -> music.sortedBy(LocalMusic::title)
                    LocalMusicSort.Artist -> music.sortedBy(LocalMusic::artist)
                    LocalMusicSort.Duration -> music.sortedByDescending(LocalMusic::durationMs)
                }
            }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("本地音乐") }, navigationIcon = { TextButton(onClick = onBack) { Text("返回") } })
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Button(onClick = onChooseFiles) { Text("导入文件") }; Button(onClick = onScan) { Text("扫描设备") } }
        state.imports.firstOrNull { it.state == LocalImportBatchState.Running || it.state == LocalImportBatchState.Queued }?.let { progress ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("正在导入 ${progress.completedCount}/${progress.totalCount}", Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { onCancelImport(progress.batchId) }) { Text("取消") }
            }
        }
        if (state.music.isNotEmpty()) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("搜索歌曲或艺术家") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
            LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LocalMusicSort.entries) { option ->
                    FilterChip(selected = sort == option, onClick = { sort = option }, label = { Text(option.label) })
                }
            }
        }
        if (state.music.isEmpty()) PlaceholderPage("还没有本地音乐", "选择音频文件，MoeKoe 会复制到 App 专属目录") else if (visible.isEmpty()) PlaceholderPage("没有找到歌曲", "试试其他歌曲名或艺术家") else LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
            items(visible, key = LocalMusic::id) { music -> MusicRow(music, onClick = { onPlay(music, visible) }, onDelete = { deleting = music }); HorizontalDivider() }
        }
    }
    deleting?.let { music -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("删除本地音乐？") }, text = { Text("将删除 MoeKoe 保存的副本，不影响原始文件。") }, confirmButton = { TextButton(onClick = { onDelete(music.id); deleting = null }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } }) }
}

private val LocalMusicSort.label: String
    get() = when (this) {
        LocalMusicSort.Newest -> "最近导入"
        LocalMusicSort.Title -> "标题"
        LocalMusicSort.Artist -> "艺术家"
        LocalMusicSort.Duration -> "时长"
    }

@Composable private fun MusicRow(music: LocalMusic, onClick: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().height(76.dp).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = music.artworkKey?.let { File(context.filesDir, it) }, contentDescription = "${music.title} 封面", modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(music.title, maxLines = 1, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium); Text(music.artist, maxLines = 1, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        TextButton(onClick = onDelete) { Text("删除") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceScanScreen(
    candidates: List<DeviceAudioCandidate>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onImport: (List<Long>) -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<Long>()) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设备音乐") },
            navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            actions = { TextButton(onClick = onRefresh) { Text("刷新") } },
        )
        if (candidates.isEmpty()) {
            PlaceholderPage("等待扫描", "授权媒体权限后点击刷新")
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(candidates, key = DeviceAudioCandidate::mediaStoreId) { candidate ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected =
                                    if (candidate.mediaStoreId in selected) {
                                        selected - candidate.mediaStoreId
                                    } else {
                                        selected + candidate.mediaStoreId
                                    }
                            }.padding(18.dp),
                    ) {
                        Text(if (candidate.mediaStoreId in selected) "✓ " else "○ ")
                        Column {
                            Text(candidate.displayName)
                            Text(candidate.artist ?: "未知艺术家", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Button(
            onClick = { onImport(selected.toList()) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text("导入 ${selected.size} 首")
        }
    }
}

@Composable private fun MiniPlayer(title: String, artist: String, isPlaying: Boolean, onToggle: () -> Unit, onQueue: () -> Unit) = Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, maxLines = 1, fontWeight = FontWeight.SemiBold); Text(artist, maxLines = 1, style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = onToggle, modifier = Modifier.semantics { contentDescription = if (isPlaying) "暂停" else "播放" }) { Text(if (isPlaying) "暂停" else "播放") }; TextButton(onClick = onQueue) { Text("队列") } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun QueueSheet(state: PlaybackState, onDismiss: () -> Unit, onPlayAt: (Int) -> Unit, onRemove: (Int) -> Unit, onClear: () -> Unit) = ModalBottomSheet(onDismissRequest = onDismiss) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("播放队列", style = MaterialTheme.typography.titleLarge); TextButton(onClick = onClear) { Text("清空") } }; LazyColumn { items(state.queue.size, key = { state.queue[it].id }) { index -> val item = state.queue[index]; Row(Modifier.fillMaxWidth().clickable { onPlayAt(index) }.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (index == state.currentIndex) "♪" else "", Modifier.size(28.dp)); Column(Modifier.weight(1f)) { Text(item.title); Text(item.artist, style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = { onRemove(index) }) { Text("移除") } } } }; Spacer(Modifier.height(24.dp)) }
