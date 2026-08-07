package cn.james.music.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.navigation.MoeImmersiveTopBar
import cn.james.music.core.designsystem.component.navigation.MoeNavigateBackIcon
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Top bars", widthDp = 390, heightDp = 384)
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
                MoeImmersiveTopBar(
                    navigationContentDescription = "返回",
                    onNavigateBack = {},
                )
            }
            MoeStandardTopBar(
                title = "设置",
                navigationContentDescription = "返回",
                onNavigateBack = {},
            )
            MoeStandardTopBar(
                title = "选择歌曲",
                navigationContentDescription = "关闭",
                onNavigateBack = {},
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        modifier = Modifier.size(24.dp),
                    )
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                        )
                    }
                },
            )
        }
    }
}

@PreviewTest
@Preview(name = "Navigation back icon RTL", widthDp = 160, heightDp = 80)
@Composable
fun MoeNavigationBackIconRtlScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Row(Modifier.background(MaterialTheme.colorScheme.background)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    MoeNavigateBackIcon(
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    MoeNavigateBackIcon(
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
