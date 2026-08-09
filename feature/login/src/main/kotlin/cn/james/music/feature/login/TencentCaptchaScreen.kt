package cn.james.music.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.james.music.core.designsystem.MoeKoeTheme
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
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MoeStandardTopBar(
            model =
                MoeStandardTopBarUiModel(
                    title = stringResource(R.string.login_tencent_page_title),
                    navigationContentDescription = stringResource(R.string.login_tencent_page_close),
                    navigation = MoeTopBarNavigation.CaptchaClose,
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
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        0.52f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.surface,
                    ),
                ),
    ) {
        val scale = maxWidth.value / TENCENT_DESIGN_WIDTH

        Column(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (TENCENT_LOADING_TOP * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                progress = { 0.76f },
                modifier =
                    Modifier
                        .size((TENCENT_PROGRESS_SIZE * scale).dp)
                        .loginLayoutProbe("tencentLoading")
                        .rotate(-42f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                strokeWidth = (TENCENT_PROGRESS_STROKE * scale).dp,
            )
            Spacer(Modifier.height((TENCENT_PROGRESS_LABEL_GAP * scale).dp))
            Text(
                text = stringResource(R.string.login_tencent_page_loading),
                color = MaterialTheme.colorScheme.primary,
                style =
                    TextStyle(
                        fontSize = (TENCENT_LOADING_TEXT_SIZE * scale).sp,
                        lineHeight = (TENCENT_LOADING_TEXT_LINE_HEIGHT * scale).sp,
                        fontWeight = FontWeight.Medium,
                    ),
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (TENCENT_FOOTER_TOP * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size((TENCENT_FOOTER_ICON_SIZE * scale).dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height((TENCENT_FOOTER_ICON_GAP * scale).dp))
            Text(
                text = stringResource(R.string.login_tencent_page_isolated),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style =
                    TextStyle(
                        fontSize = (TENCENT_FOOTER_TEXT_SIZE * scale).sp,
                        lineHeight = (TENCENT_FOOTER_TEXT_LINE_HEIGHT * scale).sp,
                    ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height((TENCENT_FOOTER_TEXT_GAP * scale).dp))
            Text(
                text = stringResource(R.string.login_tencent_page_provider),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style =
                    TextStyle(
                        fontSize = (TENCENT_FOOTER_TEXT_SIZE * scale).sp,
                        lineHeight = (TENCENT_FOOTER_TEXT_LINE_HEIGHT * scale).sp,
                    ),
                textAlign = TextAlign.Center,
            )
        }

        Box(Modifier.fillMaxSize()) { captchaContent() }
    }
}

private const val TENCENT_DESIGN_WIDTH = 852f
private const val TENCENT_LOADING_TOP = 565f
private const val TENCENT_PROGRESS_SIZE = 120f
private const val TENCENT_PROGRESS_STROKE = 10f
private const val TENCENT_PROGRESS_LABEL_GAP = 34f
private const val TENCENT_LOADING_TEXT_SIZE = 30f
private const val TENCENT_LOADING_TEXT_LINE_HEIGHT = 42f
private const val TENCENT_FOOTER_TOP = 1318f
private const val TENCENT_FOOTER_ICON_SIZE = 50f
private const val TENCENT_FOOTER_ICON_GAP = 42f
private const val TENCENT_FOOTER_TEXT_GAP = 12f
private const val TENCENT_FOOTER_TEXT_SIZE = 26f
private const val TENCENT_FOOTER_TEXT_LINE_HEIGHT = 38f
