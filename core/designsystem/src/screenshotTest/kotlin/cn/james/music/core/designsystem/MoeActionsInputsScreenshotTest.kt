package cn.james.music.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.action.MoeButton
import cn.james.music.core.designsystem.component.action.MoeButtonSize
import cn.james.music.core.designsystem.component.action.MoeDestructiveButton
import cn.james.music.core.designsystem.component.action.MoeOutlinedButton
import cn.james.music.core.designsystem.component.action.MoeTextButton
import cn.james.music.core.designsystem.component.action.MoeTonalButton
import cn.james.music.core.designsystem.component.input.MoeTextField
import cn.james.music.core.designsystem.component.input.MoeTextFieldSize
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Actions and inputs", widthDp = 390, heightDp = 760)
@Composable
fun MoeActionsInputsScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Light) {
        ActionsInputsContent()
    }
}

@PreviewTest
@Preview(name = "Actions and inputs dark", widthDp = 390, heightDp = 760)
@Composable
fun MoeActionsInputsDarkScreenshot() {
    MoeKoeTheme(themeMode = ThemeMode.Dark) {
        ActionsInputsContent()
    }
}

@Composable
private fun ActionsInputsContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MoeTextField(
            value = "",
            onValueChange = {},
            placeholder = "请输入内容",
            leadingContent = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        )
        MoeTextField(
            value = "错误内容",
            onValueChange = {},
            placeholder = "请输入内容",
            isError = true,
            errorMessage = "输入内容不正确",
            trailingContent = { Icon(Icons.Outlined.Lock, contentDescription = "密码输入") },
        )
        MoeTextField(
            value = "紧凑输入",
            onValueChange = {},
            placeholder = "请输入内容",
            size = MoeTextFieldSize.Compact,
        )
        MoeButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            size = MoeButtonSize.Large,
        ) {
            Text("主要操作")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoeTonalButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("次要") }
            MoeOutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("描边") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MoeDestructiveButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("删除") }
            MoeTextButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("文字") }
        }
        MoeButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            loading = true,
        ) {
            Text("正在提交")
        }
        MoeButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
        ) {
            Text("不可用")
        }
    }
}
