package cn.james.music.feature.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cn.james.music.core.designsystem.component.action.MoeButton
import cn.james.music.core.designsystem.component.action.MoeButtonSize
import cn.james.music.core.designsystem.component.action.MoeTextButton

@Composable
internal fun QrGeneratingContent(layout: LoginLayoutSpec) {
    val transition = rememberInfiniteTransition(label = "QrGenerating")
    val rotation by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing)),
            label = "QrGeneratingRotation",
        )
    Box(
        modifier = Modifier.fillMaxWidth().height(layout.qr.standardStateHeight),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(top = layout.dp(52f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val progressColor = MaterialTheme.colorScheme.primary
            val progressTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            Canvas(Modifier.size(layout.qr.progressSize).rotate(rotation).progressSemantics()) {
                drawArc(
                    color = progressTrackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = layout.qr.progressStroke.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 92f,
                    useCenter = false,
                    style = Stroke(width = layout.qr.progressStroke.toPx(), cap = StrokeCap.Round),
                )
            }
            Spacer(Modifier.height(layout.qr.progressToLabel))
            QrBodyText(layout, stringResource(R.string.login_qr_generating))
        }
    }
}

@Composable
internal fun QrWaitingContent(
    layout: LoginLayoutSpec,
    loginUrl: String,
    remainingSeconds: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth().height(layout.qr.standardStateHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        QrCodeImage(layout, loginUrl)
        Spacer(Modifier.height(layout.qr.codeToExpiry))
        QrExpiryText(layout, remainingSeconds)
    }
}

@Composable
internal fun QrScannedContent(
    layout: LoginLayoutSpec,
    remainingSeconds: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth().height(layout.qr.standardStateHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(layout.qr.codeFrameSize),
            shape = RoundedCornerShape(layout.qr.codeFrameRadius),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            border = BorderStroke(layout.dp(2f), MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = layout.dp(30f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    modifier = Modifier.size(layout.qr.scannedIconSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(layout.dp(25f)).size(layout.qr.scannedIconGlyphSize),
                    )
                }
                Spacer(Modifier.height(layout.qr.scannedTextGap))
                QrBodyText(layout, stringResource(R.string.login_qr_scanned_title), color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(R.string.login_qr_scanned_description),
                    style = qrSupportingStyle(layout),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.height(layout.qr.codeToExpiry))
        QrExpiryText(layout, remainingSeconds)
    }
}

@Composable
internal fun QrExpiredContent(
    layout: LoginLayoutSpec,
    loginUrl: String,
    onRefresh: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(layout.qr.expiredStateHeight), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.width(layout.qr.codeFrameSize).height(layout.qr.expiredStateHeight),
            shape = RoundedCornerShape(layout.qr.codeFrameRadius),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(layout.dp(2f), MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.TopCenter) {
                Box(Modifier.alpha(0.12f)) { QrCodeImage(layout, loginUrl, showCenterBadge = false) }
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = layout.dp(130f)).size(layout.qr.terminalIconSize),
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = layout.dp(20f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    QrBodyText(layout, stringResource(R.string.login_qr_expired_title), color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(R.string.login_qr_expired_description),
                        style = qrSupportingStyle(layout),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(layout.dp(24f)))
                    QrTerminalButton(layout, onRefresh, R.string.login_qr_refresh)
                }
            }
        }
    }
}

@Composable
internal fun QrFailureContent(
    layout: LoginLayoutSpec,
    onRefresh: () -> Unit,
    onModeChange: (LoginMode) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().height(layout.qr.failureStateHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(layout.qr.terminalIconContainerSize),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.error,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(layout.qr.terminalIconSize)) {
                    Icon(
                        Icons.Outlined.Wifi,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(layout.dp(72f)),
                    )
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd).size(layout.dp(30f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(layout.qr.terminalTextGap))
        QrBodyText(layout, stringResource(R.string.login_qr_failure_title), color = MaterialTheme.colorScheme.onSurface)
        Text(
            stringResource(R.string.login_qr_failure_description),
            style = qrSupportingStyle(layout),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(layout.dp(30f)))
        QrTerminalButton(layout, onRefresh, R.string.login_qr_retry)
        MoeTextButton(onClick = { onModeChange(LoginMode.MobileCode) }, modifier = Modifier.width(layout.qr.terminalButtonWidth)) {
            Text(stringResource(R.string.login_qr_use_mobile), style = qrSupportingStyle(layout))
        }
    }
}

@Composable
private fun QrTerminalButton(
    layout: LoginLayoutSpec,
    onClick: () -> Unit,
    labelRes: Int,
) {
    MoeButton(
        onClick = onClick,
        modifier = Modifier.width(layout.qr.terminalButtonWidth).height(layout.qr.terminalButtonHeight),
        size = MoeButtonSize.Regular,
        contentPadding = PaddingValues(horizontal = layout.dp(12f)),
    ) {
        Text(stringResource(labelRes), style = qrSupportingStyle(layout))
    }
}

@Composable
private fun QrExpiryText(
    layout: LoginLayoutSpec,
    remainingSeconds: Int,
) {
    val minutes = (remainingSeconds / 60).toString().padStart(2, '0')
    val seconds = (remainingSeconds % 60).toString().padStart(2, '0')
    val formattedTime = "$minutes:$seconds"
    Text(
        buildAnnotatedString {
            append(stringResource(R.string.login_qr_expiry_prefix))
            append(' ')
            pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary))
            append(formattedTime)
            pop()
            append(' ')
            append(stringResource(R.string.login_qr_expiry_suffix))
        },
        style = qrSupportingStyle(layout),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun QrBodyText(
    layout: LoginLayoutSpec,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text,
        style =
            TextStyle(
                fontSize = layout.typography.qrStateTitleSize,
                lineHeight = layout.typography.qrStateTitleLineHeight,
                fontWeight = FontWeight.Medium,
            ),
        color = color,
        textAlign = TextAlign.Center,
    )
}

private fun qrSupportingStyle(layout: LoginLayoutSpec): TextStyle =
    TextStyle(
        fontSize = layout.typography.qrSupportingSize,
        lineHeight = layout.typography.qrSupportingLineHeight,
        fontWeight = FontWeight.Normal,
    )
