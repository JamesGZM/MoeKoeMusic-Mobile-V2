package cn.james.music.feature.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.designsystem.component.MoeKoeImmersiveTopBar
import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthError

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onBack: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onModeChange: (LoginMode) -> Unit,
    onSendCode: () -> Unit,
    onSubmitMobileCode: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmitPassword: () -> Unit,
    onStartRiskVerification: () -> Unit,
    onRiskCodeChange: (String) -> Unit,
    onVerifyRiskCode: () -> Unit,
    onCancelRisk: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onChooseOtherAccount: () -> Unit,
) {
    if (state.risk is PasswordRiskUiState.Sms) {
        RiskSmsScreen(
            state = state,
            onBack = onCancelRisk,
            onRiskCodeChange = onRiskCodeChange,
            onVerify = onVerifyRiskCode,
            onCancel = onCancelRisk,
        )
        return
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
        ) {
            LoginHero()
            Surface(
                modifier = Modifier.fillMaxWidth().offset(y = (-28).dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
            ) {
                if (state.hasMultipleAccounts) {
                    AccountSelectionContent(
                        state = state,
                        onSelectAccount = onSelectAccount,
                        onSubmit = onSubmitMobileCode,
                        onChooseOtherAccount = onChooseOtherAccount,
                    )
                } else {
                    when (state.mode) {
                        LoginMode.MobileCode -> {
                            MobileCodeContent(
                                state = state,
                                onModeChange = onModeChange,
                                onPhoneChange = onPhoneChange,
                                onCodeChange = onCodeChange,
                                onSendCode = onSendCode,
                                onSubmit = onSubmitMobileCode,
                            )
                        }

                        LoginMode.Password -> {
                            PasswordContent(
                                state = state,
                                onModeChange = onModeChange,
                                onUsernameChange = onUsernameChange,
                                onPasswordChange = onPasswordChange,
                                onTogglePasswordVisibility = onTogglePasswordVisibility,
                                onSubmit = onSubmitPassword,
                                onStartRiskVerification = onStartRiskVerification,
                                onCancelRisk = onCancelRisk,
                            )
                        }
                    }
                }
            }
        }
        MoeKoeImmersiveTopBar(
            navigationContentDescription = stringResource(R.string.login_back),
            onNavigateBack = onBack,
            foregroundColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun LoginHero() {
    Box(Modifier.fillMaxWidth().height(300.dp)) {
        Image(
            painter = painterResource(R.drawable.login_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                            0.58f to MaterialTheme.colorScheme.surface.copy(alpha = 0.02f),
                            1f to MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        ),
                    ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 88.dp),
        ) {
            Text(
                text = stringResource(R.string.login_brand),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.login_welcome),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MobileCodeContent(
    state: LoginUiState,
    onModeChange: (LoginMode) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LoginModeSelector(state.mode, onModeChange, enabled = !state.isBusy)
        Text(stringResource(R.string.login_phone_title), style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.sendingCode && !state.loggingIn,
            leadingIcon = {
                FieldLeadingIcon { Icon(Icons.Filled.Phone, contentDescription = null) }
            },
            prefix = { Text("+86  ") },
            placeholder = { Text(stringResource(R.string.login_phone_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = RoundedCornerShape(18.dp),
        )
        OutlinedTextField(
            value = state.code,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loggingIn,
            leadingIcon = {
                FieldLeadingIcon { Icon(Icons.Filled.VerifiedUser, contentDescription = null) }
            },
            placeholder = { Text(stringResource(R.string.login_code_hint)) },
            trailingIcon = {
                TextButton(onClick = onSendCode, enabled = state.canSendCode) {
                    Text(
                        if (state.countdownSeconds > 0) {
                            stringResource(R.string.login_resend_seconds, state.countdownSeconds)
                        } else {
                            stringResource(R.string.login_send_code)
                        },
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(18.dp),
        )
        LoginNoticeText(state.notice)
        Button(
            onClick = onSubmit,
            enabled = state.canSubmitMobileCode,
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.largeButtonHeight),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (state.loggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(if (state.loggingIn) stringResource(R.string.login_submitting) else stringResource(R.string.login_submit))
        }
        LoginFooter()
    }
}

@Composable
private fun LoginModeSelector(
    selectedMode: LoginMode,
    onModeChange: (LoginMode) -> Unit,
    enabled: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LoginModeItem(
                label = stringResource(R.string.login_mode_code),
                icon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null) },
                selected = selectedMode == LoginMode.MobileCode,
                enabled = enabled,
                onClick = { onModeChange(LoginMode.MobileCode) },
                modifier = Modifier.weight(1f),
            )
            LoginModeItem(
                label = stringResource(R.string.login_mode_password),
                icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                selected = selectedMode == LoginMode.Password,
                enabled = enabled,
                onClick = { onModeChange(LoginMode.Password) },
                modifier = Modifier.weight(1f),
            )
            LoginModeItem(
                label = stringResource(R.string.login_mode_qr),
                icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                selected = false,
                enabled = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LoginModeItem(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled && !selected, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null,
    ) {
        Row(
            modifier = Modifier.height(52.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val contentColor =
                when {
                    selected -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides contentColor) {
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { icon() }
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PasswordContent(
    state: LoginUiState,
    onModeChange: (LoginMode) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onStartRiskVerification: () -> Unit,
    onCancelRisk: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LoginModeSelector(state.mode, onModeChange, enabled = !state.isBusy && !state.hasActiveRisk)
        when (val risk = state.risk) {
            null -> {
                PasswordForm(
                    state = state,
                    onUsernameChange = onUsernameChange,
                    onPasswordChange = onPasswordChange,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    onSubmit = onSubmit,
                    onUseMobileCode = { onModeChange(LoginMode.MobileCode) },
                )
            }

            is PasswordRiskUiState.Required -> {
                RiskRequiredContent(
                    state = state,
                    onStart = onStartRiskVerification,
                    onCancel = onCancelRisk,
                )
            }

            is PasswordRiskUiState.Tencent -> {
                TencentRiskFallbackContent(
                    onUseMobileCode = { onModeChange(LoginMode.MobileCode) },
                    onCancel = onCancelRisk,
                )
            }

            is PasswordRiskUiState.Sms -> Unit
        }
        LoginFooter()
    }
}

@Composable
private fun PasswordForm(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onUseMobileCode: () -> Unit,
) {
    OutlinedTextField(
        value = state.username,
        onValueChange = onUsernameChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.passwordLoggingIn,
        leadingIcon = {
            FieldLeadingIcon { Icon(Icons.Filled.AccountCircle, contentDescription = null) }
        },
        placeholder = { Text(stringResource(R.string.login_username_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(18.dp),
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.passwordLoggingIn,
        leadingIcon = {
            FieldLeadingIcon { Icon(Icons.Filled.Lock, contentDescription = null) }
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    imageVector = if (state.passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription =
                        stringResource(
                            if (state.passwordVisible) {
                                R.string.login_hide_password
                            } else {
                                R.string.login_show_password
                            },
                        ),
                )
            }
        },
        placeholder = { Text(stringResource(R.string.login_password_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        shape = RoundedCornerShape(18.dp),
    )
    LoginNoticeText(state.notice)
    Button(
        onClick = onSubmit,
        enabled = state.canSubmitPassword,
        modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.largeButtonHeight),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (state.passwordLoggingIn) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            if (state.passwordLoggingIn) {
                stringResource(R.string.login_password_submitting)
            } else if (state.notice is LoginNotice.PasswordRejected) {
                stringResource(R.string.login_password_retry)
            } else {
                stringResource(R.string.login_password_submit)
            },
        )
    }
    if (
        state.notice is LoginNotice.PasswordRejected ||
            state.notice is LoginNotice.PasswordMultipleAccounts ||
            state.notice is LoginNotice.UnsupportedRisk
    ) {
        OutlinedButton(
            onClick = onUseMobileCode,
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.buttonHeight),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.login_use_mobile_code))
        }
    }
}

@Composable
private fun RiskRequiredContent(
    state: LoginUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        color = MoeKoeTheme.extraColors.warningContainer,
        contentColor = MoeKoeTheme.extraColors.onWarningContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = MoeKoeTheme.extraColors.warning)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.login_risk_required_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.login_risk_required_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoeKoeTheme.extraColors.onWarningContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
    LoginNoticeText(state.notice)
    Button(
        onClick = onStart,
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.largeButtonHeight),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (state.resolvingRisk) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            if (state.resolvingRisk) {
                stringResource(R.string.login_risk_loading)
            } else {
                stringResource(R.string.login_risk_start)
            },
        )
    }
    OutlinedButton(
        onClick = onCancel,
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.buttonHeight),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(stringResource(R.string.login_risk_cancel))
    }
}

@Composable
private fun TencentRiskFallbackContent(
    onUseMobileCode: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.login_tencent_required_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.login_tencent_required_description), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Button(
        onClick = onUseMobileCode,
        modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.largeButtonHeight),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(stringResource(R.string.login_use_mobile_code))
    }
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.buttonHeight),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(stringResource(R.string.login_risk_cancel))
    }
}

@Composable
private fun RiskSmsScreen(
    state: LoginUiState,
    onBack: () -> Unit,
    onRiskCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 24.dp, top = 96.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(76.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
            Text(
                stringResource(R.string.login_risk_sms_title),
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.login_risk_sms_description),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                    OtpCodeField(state.riskCode, onRiskCodeChange, enabled = !state.verifyingRisk)
                    Text(
                        stringResource(R.string.login_risk_sms_expiry),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            LoginNoticeText(state.notice)
            Button(
                onClick = onVerify,
                enabled = state.canSubmitRiskCode,
                modifier = Modifier.fillMaxWidth().padding(top = 26.dp).height(MoeKoeTheme.dimensions.largeButtonHeight),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.verifyingRisk || state.passwordLoggingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    if (state.verifyingRisk || state.passwordLoggingIn) {
                        stringResource(R.string.login_risk_verifying)
                    } else {
                        stringResource(R.string.login_risk_verify_continue)
                    },
                )
            }
            OutlinedButton(
                onClick = onCancel,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(MoeKoeTheme.dimensions.buttonHeight),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.login_risk_cancel_login))
            }
            Spacer(Modifier.height(34.dp))
            LoginFooter()
        }
        MoeKoeImmersiveTopBar(
            navigationContentDescription = stringResource(R.string.login_back),
            onNavigateBack = onBack,
            foregroundColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
        )
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
        modifier = Modifier.fillMaxWidth().height(58.dp),
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
                        Box(
                            modifier = Modifier.weight(1f).height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = value.getOrNull(index)?.toString().orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            HorizontalDivider(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                thickness = if (index == value.length.coerceAtMost(5)) 2.dp else 1.dp,
                                color =
                                    if (index == value.length.coerceAtMost(5)) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun AccountSelectionContent(
    state: LoginUiState,
    onSelectAccount: (String) -> Unit,
    onSubmit: () -> Unit,
    onChooseOtherAccount: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.login_accounts_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.login_accounts_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                state.accounts.forEachIndexed { index, account ->
                    AccountRow(
                        account = account,
                        selected = state.selectedUserId == account.userId,
                        onClick = { onSelectAccount(account.userId) },
                    )
                    if (index != state.accounts.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        LoginNoticeText(state.notice)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                stringResource(R.string.login_security),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onSubmit,
            enabled = state.canSubmitMobileCode,
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.largeButtonHeight),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (state.loggingIn) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(stringResource(R.string.login_account_submit))
        }
        OutlinedButton(
            onClick = onChooseOtherAccount,
            enabled = !state.loggingIn,
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.buttonHeight),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.login_account_other))
        }
    }
}

@Composable
private fun AccountRow(
    account: AuthAccountOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = account.avatarUrl,
            contentDescription = stringResource(R.string.login_account_avatar, account.nickname.ifBlank { stringResource(R.string.login_account_unnamed) }),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                account.nickname.ifBlank { stringResource(R.string.login_account_unnamed) },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.login_account_id, maskUserId(account.userId)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun LoginNoticeText(notice: LoginNotice?) {
    if (notice == null) return
    val isError = notice !is LoginNotice.CodeSent
    Text(
        text = notice.message(),
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MoeKoeTheme.extraColors.success,
    )
}

@Composable
private fun LoginNotice.message(): String =
    when (this) {
        LoginNotice.CodeSent -> stringResource(R.string.login_code_sent)
        is LoginNotice.Validation ->
            when (error) {
                LoginValidationError.InvalidPhone -> stringResource(R.string.login_invalid_phone)
                LoginValidationError.InvalidCode -> stringResource(R.string.login_invalid_code)
                LoginValidationError.AccountRequired -> stringResource(R.string.login_account_required)
                LoginValidationError.UsernameRequired -> stringResource(R.string.login_username_required)
                LoginValidationError.PasswordRequired -> stringResource(R.string.login_password_required)
                LoginValidationError.RiskCodeRequired -> stringResource(R.string.login_risk_code_required)
            }

        is LoginNotice.Failure -> error.message()
        LoginNotice.PasswordRejected -> stringResource(R.string.login_password_rejected)
        LoginNotice.PasswordMultipleAccounts -> stringResource(R.string.login_password_multiple_accounts)
        LoginNotice.RiskRejected -> stringResource(R.string.login_risk_rejected)
        LoginNotice.RiskRepeated -> stringResource(R.string.login_risk_repeated)
        is LoginNotice.UnsupportedRisk -> stringResource(R.string.login_risk_unsupported, type)
    }

@Composable
private fun AuthError.message(): String =
    stringResource(
        when (this) {
            AuthError.SessionInitialization -> R.string.login_error_session
            AuthError.Offline -> R.string.login_error_offline
            AuthError.Timeout -> R.string.login_error_timeout
            AuthError.Connection -> R.string.login_error_connection
            AuthError.ServiceUnavailable -> R.string.login_error_service
            AuthError.Rejected -> R.string.login_error_rejected
            AuthError.Protocol -> R.string.login_error_protocol
            AuthError.Storage -> R.string.login_error_storage
        },
    )

@Composable
private fun LoginFooter() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.login_terms_prefix))
                pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary))
                append(stringResource(R.string.login_terms_user_agreement))
                pop()
                append(stringResource(R.string.login_terms_and))
                pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary))
                append(stringResource(R.string.login_terms_privacy_policy))
                pop()
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Icon(
                Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp).size(20.dp),
            )
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Text(
            stringResource(R.string.login_security),
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FieldLeadingIcon(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.primary,
            ) {
                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { content() }
            }
        }
    }
}

private fun maskUserId(value: String): String =
    when {
        value.length <= 4 -> "••••"
        else -> "${value.take(2)}****${value.takeLast(2)}"
    }
