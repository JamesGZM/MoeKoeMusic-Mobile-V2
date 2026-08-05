package cn.james.music.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarTone
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "MoeSnackbar", widthDp = 390, heightDp = 400)
@Composable
fun MoeSnackbarScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MoeSnackbar(
                message = "签到成功，已获得 3 小时 VIP",
                tone = MoeSnackbarTone.Success,
                icon = Icons.Outlined.CheckCircle,
            )
            MoeSnackbar(
                message = "已从歌单移除",
                tone = MoeSnackbarTone.Info,
                icon = Icons.Outlined.Delete,
                actionLabel = "撤销",
            )
            MoeSnackbar(
                message = "网络异常，请稍后重试",
                tone = MoeSnackbarTone.Error,
                icon = Icons.Outlined.Warning,
                actionLabel = "重试",
            )
            MoeSnackbar(
                message = "今天已经领取过了",
                tone = MoeSnackbarTone.Warning,
                icon = Icons.Outlined.Info,
            )
        }
    }
}
