package cn.james.music.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBar
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarEvent
import cn.james.music.core.designsystem.component.navigation.MoeStandardTopBarUiModel
import cn.james.music.core.designsystem.component.navigation.MoeTopBarNavigation

/** User-visible chrome for the isolated Tencent captcha Activity. */
@Composable
fun TencentCaptchaScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    captchaContent: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxSize().background(Color.White)) {
        MoeStandardTopBar(
            model =
                MoeStandardTopBarUiModel(
                    title = stringResource(R.string.login_tencent_page_title),
                    navigationContentDescription = stringResource(R.string.login_back),
                    navigation = MoeTopBarNavigation.Back,
                ),
            onEvent = { event ->
                if (event == MoeStandardTopBarEvent.NavigateBack) onClose()
            },
        )
        TencentCaptchaBody(captchaContent = captchaContent)
    }
}

@Composable
private fun TencentCaptchaBody(captchaContent: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(24.dp)
                        .loginLayoutProbe("tencentLoading"),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.login_tencent_page_loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.fillMaxSize()) { captchaContent() }
    }
}
