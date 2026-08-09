package cn.james.music.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoeSnackbar
import cn.james.music.core.designsystem.component.MoeSnackbarActionId
import cn.james.music.core.designsystem.component.MoeSnackbarActionUiModel
import cn.james.music.core.designsystem.component.MoeSnackbarIcon
import cn.james.music.core.designsystem.component.MoeSnackbarTone
import cn.james.music.core.designsystem.component.MoeSnackbarUiModel
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
                model =
                    MoeSnackbarUiModel(
                        message = "签到成功，已获得 3 小时 VIP",
                        tone = MoeSnackbarTone.Success,
                        icon = MoeSnackbarIcon.Confirmation,
                    ),
                onEvent = {},
            )
            MoeSnackbar(
                model =
                    MoeSnackbarUiModel(
                        message = "已从歌单移除",
                        tone = MoeSnackbarTone.Info,
                        icon = MoeSnackbarIcon.Removal,
                        action = MoeSnackbarActionUiModel(MoeSnackbarActionId.Undo, "撤销"),
                    ),
                onEvent = {},
            )
            MoeSnackbar(
                model =
                    MoeSnackbarUiModel(
                        message = "网络异常，请稍后重试",
                        tone = MoeSnackbarTone.Error,
                        icon = MoeSnackbarIcon.Warning,
                        action = MoeSnackbarActionUiModel(MoeSnackbarActionId.Retry, "重试"),
                    ),
                onEvent = {},
            )
            MoeSnackbar(
                model =
                    MoeSnackbarUiModel(
                        message = "今天已经领取过了",
                        tone = MoeSnackbarTone.Warning,
                        icon = MoeSnackbarIcon.Information,
                    ),
                onEvent = {},
            )
        }
    }
}
