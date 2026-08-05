@file:Suppress("ktlint:standard:max-line-length")

package cn.james.music.feature.localmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.model.local.DeviceAudioCandidate
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalMusic
import cn.james.music.core.model.local.LocalMusicSort
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun LocalMusicScreen(state: LocalMusicUiState, onBack: () -> Unit, onChooseFiles: () -> Unit, onScan: () -> Unit, onPlay: (LocalMusic, List<LocalMusic>) -> Unit, onDelete: (String) -> Unit, onCancelImport: (String) -> Unit) {
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
        if (state.music.isEmpty()) LocalMusicMessage("还没有本地音乐", "选择音频文件，MoeKoe 会复制到 App 专属目录") else if (visible.isEmpty()) LocalMusicMessage("没有找到歌曲", "试试其他歌曲名或艺术家") else LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
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

@Composable
private fun LocalMusicMessage(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(message, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
internal fun DeviceScanScreen(
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
            LocalMusicMessage("等待扫描", "授权媒体权限后点击刷新")
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
