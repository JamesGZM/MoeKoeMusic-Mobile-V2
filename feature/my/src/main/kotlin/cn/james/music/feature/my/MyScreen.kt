package cn.james.music.feature.my

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun MyScreen(
    onLogin: () -> Unit,
    onLocalMusic: () -> Unit,
    onFoundationLab: () -> Unit,
    showFoundationLab: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("我的", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("登录后同步收藏与音乐资产", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            LibraryCard("登录 MoeKoe Air", "使用手机号验证码安全登录", onLogin)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("每日签到") }
                Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("VIP 领取") }
            }
        }
        item { LibraryCard("本地音乐", "导入并离线播放设备上的音乐", onLocalMusic) }
        item { LibraryCard("我喜欢", "等待账号阶段接入", {}) }
        item { LibraryCard("创建与收藏的歌单", "等待账号阶段接入", {}) }
        if (showFoundationLab) {
            item { TextButton(onClick = onFoundationLab) { Text("打开播放工程实验台") } }
        }
    }
}

@Composable
private fun LibraryCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
