package cn.james.music.feature.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun LoginRiskDialog(
    state: LoginUiState,
    onStartVerification: () -> Unit,
    onRetryTencentVerification: () -> Unit,
    onRiskCodeChange: (String) -> Unit,
    onVerifyRiskCode: () -> Unit,
    onCancel: () -> Unit,
) {
    val risk = state.risk ?: return
    if (risk is PasswordRiskUiState.Tencent && state.notice == null) return
    val dialogWidth = if (risk is PasswordRiskUiState.Sms) 320.dp else 304.dp
    Dialog(
        onDismissRequest = { if (!state.isBusy) onCancel() },
        properties =
            DialogProperties(
                dismissOnBackPress = !state.isBusy,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.width(dialogWidth),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                when (risk) {
                    is PasswordRiskUiState.Required ->
                        RiskRequiredDialogContent(
                            state = state,
                            onStart = onStartVerification,
                            onCancel = onCancel,
                        )

                    is PasswordRiskUiState.Sms ->
                        RiskSmsDialogContent(
                            state = state,
                            onRiskCodeChange = onRiskCodeChange,
                            onVerify = onVerifyRiskCode,
                            onCancel = onCancel,
                        )

                    is PasswordRiskUiState.Tencent ->
                        TencentFailureDialogContent(
                            state = state,
                            onRetry = onRetryTencentVerification,
                            onCancel = onCancel,
                        )
                }
            }
        }
    }
}

@Composable
private fun RiskRequiredDialogContent(
    state: LoginUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    RiskDialogLayout(
        title = stringResource(R.string.login_risk_required_dialog_title),
        description = stringResource(R.string.login_risk_required_description),
        notice = state.notice,
    ) {
        RiskDialogActions(
            primaryLabel =
                stringResource(
                    if (state.resolvingRisk) R.string.login_risk_loading else R.string.login_risk_start,
                ),
            busy = state.resolvingRisk,
            primaryEnabled = !state.isBusy,
            onPrimary = onStart,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun RiskSmsDialogContent(
    state: LoginUiState,
    onRiskCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onCancel: () -> Unit,
) {
    RiskDialogLayout(
        title = stringResource(R.string.login_risk_sms_title),
        description = stringResource(R.string.login_risk_sms_description),
        notice = state.notice,
        content = {
            OtpCodeField(
                value = state.riskCode,
                onValueChange = onRiskCodeChange,
                enabled = !state.verifyingRisk,
            )
            Text(
                stringResource(R.string.login_risk_sms_expiry),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        },
        actions = {
            RiskDialogActions(
                primaryLabel =
                    stringResource(
                        if (state.verifyingRisk || state.passwordLoggingIn) {
                            R.string.login_risk_verifying
                        } else {
                            R.string.login_risk_verify_continue
                        },
                    ),
                busy = state.verifyingRisk || state.passwordLoggingIn,
                primaryEnabled = state.canSubmitRiskCode,
                onPrimary = onVerify,
                onCancel = onCancel,
            )
        },
    )
}

@Composable
private fun TencentFailureDialogContent(
    state: LoginUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    RiskDialogLayout(
        title = stringResource(R.string.login_tencent_failure_title),
        description = stringResource(R.string.login_tencent_failure_description),
        notice = null,
    ) {
        RiskDialogActions(
            primaryLabel = stringResource(R.string.login_tencent_retry),
            busy = false,
            primaryEnabled = !state.isBusy,
            onPrimary = onRetry,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun RiskDialogLayout(
    title: String,
    description: String,
    notice: LoginNotice?,
    content: @Composable () -> Unit = {},
    actions: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
        LoginNoticeText(notice)
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) { actions() }
    }
}

@Composable
private fun RiskDialogActions(
    primaryLabel: String,
    busy: Boolean,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FilledTonalButton(
            onClick = onCancel,
            enabled = !busy,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.login_risk_cancel_short))
        }
        Button(
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(primaryLabel, maxLines = 1)
        }
    }
}

@Composable
private fun OtpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = { innerTextField ->
            Box {
                innerTextField()
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(6) { index ->
                        val focused = index == value.length.coerceAtMost(5)
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .border(
                                        BorderStroke(
                                            if (focused) 2.dp else 1.dp,
                                            if (focused) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        ),
                                        RoundedCornerShape(12.dp),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = value.getOrNull(index)?.toString().orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
    )
}
