package cn.james.music.feature.login

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onBack: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onModeChange: (LoginMode) -> Unit,
    onRefreshQrLogin: () -> Unit,
    onSendCode: () -> Unit,
    onSubmitMobileCode: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmitPassword: () -> Unit,
    onStartRiskVerification: () -> Unit,
    onRetryTencentVerification: () -> Unit,
    onRiskCodeChange: (String) -> Unit,
    onVerifyRiskCode: () -> Unit,
    onCancelRisk: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onChooseOtherAccount: () -> Unit,
) {
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
                            )
                        }

                        LoginMode.QrCode -> {
                            QrCodeContent(
                                state = state,
                                onModeChange = onModeChange,
                                onRefresh = onRefreshQrLogin,
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
        LoginRiskDialog(
            state = state,
            onStartVerification = onStartRiskVerification,
            onRetryTencentVerification = onRetryTencentVerification,
            onRiskCodeChange = onRiskCodeChange,
            onVerifyRiskCode = onVerifyRiskCode,
            onCancel = onCancelRisk,
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
                selected = selectedMode == LoginMode.QrCode,
                enabled = enabled,
                onClick = { onModeChange(LoginMode.QrCode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QrCodeContent(
    state: LoginUiState,
    onModeChange: (LoginMode) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LoginModeSelector(state.mode, onModeChange, enabled = true)
        when (val qr = state.qrLogin ?: QrLoginUiState.Generating) {
            QrLoginUiState.Generating -> QrGeneratingContent()
            is QrLoginUiState.Waiting -> {
                QrReadyContent(qr.session.loginUrl, qr.remainingSeconds, scanned = false)
            }

            is QrLoginUiState.Scanned -> {
                QrReadyContent(qr.session.loginUrl, qr.remainingSeconds, scanned = true, nickname = qr.nickname)
            }

            is QrLoginUiState.Expired -> QrExpiredContent(qr.session.loginUrl, onRefresh)
            is QrLoginUiState.Failure -> QrFailureContent(qr.error, onRefresh, onModeChange)
        }
    }
}

@Composable
private fun QrGeneratingContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(186.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(progress = { 0.72f }, modifier = Modifier.size(54.dp), strokeWidth = 4.dp)
        }
        Text(stringResource(R.string.login_qr_generating), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun QrReadyContent(
    loginUrl: String,
    remainingSeconds: Int,
    scanned: Boolean,
    nickname: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            QrCodeImage(loginUrl)
            if (scanned) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(3.dp, Color.White),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(13.dp),
                    )
                }
            }
        }
        Text(
            stringResource(if (scanned) R.string.login_qr_scanned_title else R.string.login_qr_scan_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            if (scanned) {
                nickname?.takeIf(String::isNotBlank)?.let { stringResource(R.string.login_qr_scanned_user, it) }
                    ?: stringResource(R.string.login_qr_scanned_description)
            } else {
                stringResource(R.string.login_qr_expiry, remainingSeconds / 60, remainingSeconds % 60)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!scanned) QrLoginSteps()
    }
}

@Composable
private fun QrCodeImage(loginUrl: String) {
    val bitmap = remember(loginUrl) { createQrBitmap(loginUrl) }
    Box(
        modifier = Modifier.size(190.dp).background(Color.White, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.login_qr_code_description),
            modifier = Modifier.size(178.dp),
        )
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary,
            border = BorderStroke(3.dp, Color.White),
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}

@Composable
private fun QrLoginSteps() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.login_qr_step_open), style = MaterialTheme.typography.labelMedium)
            Text("→", color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.login_qr_step_scan), style = MaterialTheme.typography.labelMedium)
            Text("→", color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.login_qr_step_confirm), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun QrExpiredContent(
    loginUrl: String,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.alpha(0.28f)) { QrCodeImage(loginUrl) }
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                border = BorderStroke(3.dp, Color.White),
            ) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(13.dp),
                )
            }
        }
        Text(stringResource(R.string.login_qr_expired_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.login_qr_expired_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(MoeKoeTheme.dimensions.largeButtonHeight),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.login_qr_refresh))
        }
    }
}

@Composable
private fun QrFailureContent(
    error: AuthError,
    onRefresh: () -> Unit,
    onModeChange: (LoginMode) -> Unit,
) {
    QrTerminalContent(
        icon = { Icon(Icons.Filled.CloudOff, contentDescription = null) },
        title = stringResource(R.string.login_qr_failure_title),
        description = error.message(),
    ) {
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.largeButtonHeight),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.login_qr_retry))
        }
        OutlinedButton(
            onClick = { onModeChange(LoginMode.MobileCode) },
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.buttonHeight),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.login_qr_use_mobile))
        }
    }
}

@Composable
private fun QrTerminalContent(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
        ) {
            Box(Modifier.padding(19.dp), contentAlignment = Alignment.Center) { icon() }
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        actions()
    }
}

private fun createQrBitmap(content: String): Bitmap {
    val matrix =
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            QR_BITMAP_SIZE,
            QR_BITMAP_SIZE,
            mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1,
            ),
        )
    val pixels =
        IntArray(QR_BITMAP_SIZE * QR_BITMAP_SIZE) { index ->
            if (matrix[index % QR_BITMAP_SIZE, index / QR_BITMAP_SIZE]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    return Bitmap.createBitmap(QR_BITMAP_SIZE, QR_BITMAP_SIZE, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, QR_BITMAP_SIZE, 0, 0, QR_BITMAP_SIZE, QR_BITMAP_SIZE)
    }
}

private const val QR_BITMAP_SIZE = 512

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
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LoginModeSelector(state.mode, onModeChange, enabled = !state.isBusy && !state.hasActiveRisk)
        Text(stringResource(R.string.login_password_title), style = MaterialTheme.typography.headlineLarge)
        PasswordForm(
            state = state,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onSubmit = onSubmit,
        )
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
) {
    OutlinedTextField(
        value = state.username,
        onValueChange = onUsernameChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.passwordLoggingIn && !state.hasActiveRisk,
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
        enabled = !state.passwordLoggingIn && !state.hasActiveRisk,
        leadingIcon = {
            FieldLeadingIcon { Icon(Icons.Filled.Lock, contentDescription = null) }
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility, enabled = !state.hasActiveRisk) {
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
    LoginNoticeText(state.notice.takeUnless { state.hasActiveRisk })
    Button(
        onClick = onSubmit,
        enabled = state.canSubmitPassword && !state.hasActiveRisk,
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
            if (state.passwordLoggingIn || state.hasActiveRisk) {
                stringResource(R.string.login_password_submitting)
            } else if (state.notice is LoginNotice.PasswordRejected) {
                stringResource(R.string.login_password_retry)
            } else {
                stringResource(R.string.login_password_submit)
            },
        )
    }
}

@Composable
private fun AccountSelectionContent(
    state: LoginUiState,
    onSelectAccount: (String) -> Unit,
    onSubmit: () -> Unit,
    onChooseOtherAccount: () -> Unit,
) {
    val accountFailure = (state.notice as? LoginNotice.Failure)?.error
    val requiresPhoneReverification = accountFailure == AuthError.Rejected
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
        if (accountFailure != null) {
            AccountFailureNotice(accountFailure)
        } else {
            LoginNoticeText(state.notice)
        }
        Button(
            onClick = if (requiresPhoneReverification) onChooseOtherAccount else onSubmit,
            enabled = if (requiresPhoneReverification) !state.loggingIn else state.canSubmitMobileCode,
            modifier = Modifier.fillMaxWidth().height(MoeKoeTheme.dimensions.largeButtonHeight),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (state.loggingIn) {
                CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                stringResource(
                    if (requiresPhoneReverification) {
                        R.string.login_account_reverify_phone
                    } else {
                        R.string.login_account_submit
                    },
                ),
            )
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
private fun AccountFailureNotice(error: AuthError) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null)
            Text(
                if (error == AuthError.Rejected) {
                    stringResource(R.string.login_account_verification_expired)
                } else {
                    error.message()
                },
                style = MaterialTheme.typography.bodyMedium,
            )
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
internal fun LoginNoticeText(notice: LoginNotice?) {
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
