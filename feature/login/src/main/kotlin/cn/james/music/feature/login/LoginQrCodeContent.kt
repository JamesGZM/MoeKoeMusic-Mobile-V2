package cn.james.music.feature.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
internal fun QrCodeContent(
    layout: LoginLayoutSpec,
    state: LoginUiState,
    onModeChange: (LoginMode) -> Unit,
    onRefresh: () -> Unit,
) {
    LoginFormColumn(layout = layout) {
        Spacer(Modifier.height(layout.form.selectorHeight + layout.form.selectorToTitle))
        Text(
            text = stringResource(R.string.login_qr_title),
            style =
                TextStyle(
                    fontSize = layout.typography.titleSize,
                    lineHeight = layout.typography.titleLineHeight,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
        Spacer(Modifier.height(layout.qr.titleToInstruction))
        QrLoginInstruction(layout)
        Spacer(Modifier.height(layout.qr.instructionToState))

        val terminalState =
            when (val qr = state.qrLogin ?: QrLoginUiState.Generating) {
                QrLoginUiState.Generating -> {
                    QrGeneratingContent(layout)
                    false
                }
                is QrLoginUiState.Waiting -> {
                    QrWaitingContent(layout, qr.session.loginUrl, qr.remainingSeconds)
                    false
                }
                is QrLoginUiState.Scanned -> {
                    QrScannedContent(layout, qr.remainingSeconds)
                    false
                }
                is QrLoginUiState.Expired -> {
                    QrExpiredContent(layout, qr.session.loginUrl, onRefresh)
                    true
                }
                is QrLoginUiState.Failure -> {
                    QrFailureContent(layout, onRefresh, onModeChange)
                    true
                }
            }
        Spacer(Modifier.height(if (terminalState) layout.qr.terminalStateToSteps else layout.qr.standardStateToSteps))
        QrLoginSteps(layout)
        Spacer(Modifier.height(layout.qr.stepsToFooter))
        LoginSecurityFooter(layout)
    }
}

@Composable
private fun QrLoginInstruction(layout: LoginLayoutSpec) {
    Text(
        buildAnnotatedString {
            append(stringResource(R.string.login_qr_instruction_prefix))
            pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary))
            append(stringResource(R.string.login_qr_instruction_app))
            pop()
            append(stringResource(R.string.login_qr_instruction_suffix))
        },
        modifier = Modifier.fillMaxWidth().testTag("qr_login_instruction"),
        style =
            TextStyle(
                    fontSize = layout.typography.qrInstructionSize,
                    lineHeight = layout.typography.qrInstructionLineHeight,
                fontWeight = FontWeight.Normal,
            ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun QrLoginSteps(layout: LoginLayoutSpec) {
    Row(
        modifier = Modifier.fillMaxWidth().height(layout.qr.stepRowHeight),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center,
    ) {
        QrLoginStep(
            layout = layout,
            icon = {
                Surface(
                    modifier = Modifier.size(layout.qr.stepIconSize),
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    border = BorderStroke(layout.dp(3f), MaterialTheme.colorScheme.primary),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "K",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            },
            label = stringResource(R.string.login_qr_step_open),
        )
        QrStepConnector(layout)
        QrLoginStep(
            layout = layout,
            icon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
            label = stringResource(R.string.login_qr_step_scan),
        )
        QrStepConnector(layout)
        QrLoginStep(
            layout = layout,
            icon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null) },
            label = stringResource(R.string.login_qr_step_confirm),
        )
    }
}

@Composable
private fun QrLoginStep(
    layout: LoginLayoutSpec,
    icon: @Composable () -> Unit,
    label: String,
) {
    Column(
        modifier = Modifier.width(layout.qr.stepItemWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(layout.qr.stepIconContainerSize),
            shape = RoundedCornerShape(layout.qr.stepIconContainerRadius),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(layout.qr.stepIconSize), contentAlignment = Alignment.Center) { icon() }
            }
        }
        Spacer(Modifier.height(layout.qr.stepIconToLabel))
        Text(
            label,
            style =
                TextStyle(
                    fontSize = layout.typography.qrStepSize,
                    lineHeight = layout.typography.qrStepLineHeight,
                    fontWeight = FontWeight.Normal,
                ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun QrStepConnector(layout: LoginLayoutSpec) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.width(layout.qr.stepConnectorWidth).height(layout.qr.stepIconContainerSize)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = layout.dp(2f).toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(layout.dp(8f).toPx(), layout.dp(7f).toPx())),
        )
    }
}
