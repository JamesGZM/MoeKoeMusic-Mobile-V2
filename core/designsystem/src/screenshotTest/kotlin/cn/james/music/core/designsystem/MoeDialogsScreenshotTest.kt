package cn.james.music.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialogContent
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialogIcon
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialogTone
import cn.james.music.core.designsystem.component.overlay.MoeAlertDialogUiModel
import cn.james.music.core.designsystem.component.overlay.MoeDialogActionsUiModel
import cn.james.music.core.designsystem.component.overlay.MoeDialogSize
import cn.james.music.core.designsystem.component.overlay.MoeDialogSurface
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Dialogs", widthDp = 390, heightDp = 660)
@Composable
fun MoeDialogsScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                    .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MoeDialogSurface(size = MoeDialogSize.Confirm) {
                MoeAlertDialogContent(
                    model =
                        MoeAlertDialogUiModel(
                            title = "退出登录？",
                            message = "退出后仍可浏览本地内容，账号数据不会删除。",
                            actions = MoeDialogActionsUiModel(confirmLabel = "退出", dismissLabel = "取消"),
                        ),
                    onEvent = {},
                )
            }
            MoeDialogSurface(size = MoeDialogSize.Confirm) {
                MoeAlertDialogContent(
                    model =
                        MoeAlertDialogUiModel(
                            title = "删除本地音乐？",
                            message = "将删除 MoeKoe 保存的副本，不影响原始文件。",
                            icon = MoeAlertDialogIcon.Warning,
                            actions =
                                MoeDialogActionsUiModel(
                                    confirmLabel = "删除",
                                    dismissLabel = "取消",
                                    tone = MoeAlertDialogTone.Destructive,
                                ),
                        ),
                    onEvent = {},
                )
            }
        }
    }
}
