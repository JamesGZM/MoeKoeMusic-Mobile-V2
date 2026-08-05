package cn.james.music.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoeKoeImmersiveTopBar
import cn.james.music.core.designsystem.component.MoeKoeStandardTopBar
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Top bars", widthDp = 390, heightDp = 320)
@Composable
fun MoeKoeTopBarsScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(192.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                MoeKoeImmersiveTopBar(
                    navigationContentDescription = "返回",
                    onNavigateBack = {},
                )
            }
            MoeKoeStandardTopBar(
                title = "设置",
                navigationContentDescription = "返回",
                onNavigateBack = {},
            )
        }
    }
}
