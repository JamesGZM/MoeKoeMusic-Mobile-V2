package cn.james.music.feature.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
internal fun PasswordContent(
    layout: LoginLayoutSpec,
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
) {
    LoginFormColumn(layout = layout) {
        Spacer(Modifier.height(layout.form.selectorHeight + layout.form.selectorToTitle))
        Text(
            text = stringResource(R.string.login_password_title),
            modifier = Modifier.padding(start = layout.form.titleStart),
            style =
                TextStyle(
                    fontSize = layout.typography.titleSize,
                    lineHeight = layout.typography.titleLineHeight,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
        Spacer(Modifier.height(layout.form.titleToField))
        PasswordForm(
            layout = layout,
            state = state,
            onUsernameChange = onUsernameChange,
            onPasswordChange = onPasswordChange,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onSubmit = onSubmit,
        )
        Spacer(Modifier.height(layout.form.buttonToFooter))
        LoginFooter(layout)
    }
}

@Composable
private fun PasswordForm(
    layout: LoginLayoutSpec,
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
) {
    LoginOutlinedField(
        layout = layout,
        value = state.username,
        onValueChange = onUsernameChange,
        enabled = !state.passwordLoggingIn && !state.hasActiveRisk,
        leadingIcon = {
            FieldLeadingIcon(layout) { Icon(Icons.Filled.AccountCircle, contentDescription = null) }
        },
        placeholder = stringResource(R.string.login_username_hint),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
    )
    Spacer(Modifier.height(layout.form.fieldSpacing))
    LoginOutlinedField(
        layout = layout,
        value = state.password,
        onValueChange = onPasswordChange,
        enabled = !state.passwordLoggingIn && !state.hasActiveRisk,
        leadingIcon = {
            FieldLeadingIcon(layout) { Icon(Icons.Filled.Lock, contentDescription = null) }
        },
        trailingIcon = {
            IconButton(
                onClick = onTogglePasswordVisibility,
                enabled = !state.passwordLoggingIn && !state.hasActiveRisk,
            ) {
                Icon(
                    imageVector = if (state.passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    modifier = Modifier.size(layout.form.countryDropDownIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PASSWORD_VISIBILITY_ICON_ALPHA),
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
        placeholder = stringResource(R.string.login_password_hint),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    )
    val notice = state.notice.takeUnless { state.hasActiveRisk }
    Spacer(Modifier.height(layout.form.passwordNoticeTop))
    LoginInlineFeedback(
        layout = layout,
        notice = notice,
        reservedHeight = layout.form.passwordNoticeHeight,
    )
    Spacer(Modifier.height(layout.form.passwordNoticeToButton))
    LoginPrimaryButton(
        layout = layout,
        onClick = onSubmit,
        enabled = state.canSubmitPassword && !state.hasActiveRisk,
        loading = state.passwordLoggingIn || state.hasActiveRisk,
    ) {
        LoginActionText(
            layout = layout,
            text =
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

private const val PASSWORD_VISIBILITY_ICON_ALPHA = 0.45f
