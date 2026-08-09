package cn.james.music.feature.login

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.james.music.core.designsystem.component.MoeVerticalDivider

@Composable
internal fun LoginMobileCodeContent(
    layout: LoginLayoutSpec,
    state: LoginUiState,
    onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onSubmit: () -> Unit,
) {
    LoginFormColumn(
        layout = layout,
    ) {
        Spacer(Modifier.height(layout.form.selectorHeight + layout.form.selectorToTitle))
        Text(
            text = stringResource(R.string.login_phone_title),
            modifier = Modifier.padding(start = layout.form.titleStart),
            style =
                TextStyle(
                    fontSize = layout.typography.titleSize,
                    lineHeight = layout.typography.titleLineHeight,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
        Spacer(Modifier.height(layout.form.titleToField))
        LoginOutlinedField(
            layout = layout,
            value = state.phone,
            onValueChange = onPhoneChange,
            enabled = !state.sendingCode && !state.loggingIn,
            leadingIcon = {
                FieldLeadingIcon(layout) { Icon(Icons.Filled.Phone, contentDescription = null) }
            },
            prefix = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LoginFieldActionText(layout = layout, text = "+86", color = MaterialTheme.colorScheme.onSurface)
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(layout.form.countryDropDownIconSize),
                    )
                    MoeVerticalDivider(
                        modifier =
                            Modifier
                                .padding(start = layout.dp(8f), end = layout.dp(20f))
                                .height(layout.form.countryDividerHeight),
                    )
                }
            },
            placeholder = stringResource(R.string.login_phone_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        Spacer(Modifier.height(layout.form.fieldSpacing))
        LoginOutlinedField(
            layout = layout,
            value = state.code,
            onValueChange = onCodeChange,
            enabled = !state.loggingIn,
            leadingIcon = {
                FieldLeadingIcon(layout) { Icon(Icons.Filled.VerifiedUser, contentDescription = null) }
            },
            placeholder = stringResource(R.string.login_code_hint),
            trailingIcon = {
                TextButton(
                    onClick = onSendCode,
                    enabled = state.canSendCode,
                    modifier = Modifier.width(layout.form.codeActionWidth),
                    contentPadding = PaddingValues(0.dp),
                    colors =
                        ButtonDefaults.textButtonColors(
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
                        ),
                ) {
                    LoginFieldActionText(
                        layout = layout,
                        text =
                            when {
                            state.sendingCode -> stringResource(R.string.login_sending_code)
                            state.countdownSeconds > 0 -> {
                                stringResource(R.string.login_resend_seconds, state.countdownSeconds)
                            }

                            else -> stringResource(R.string.login_send_code)
                        },
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        Spacer(Modifier.height(layout.form.mobileNoticeTop))
        LoginInlineFeedback(
            layout = layout,
            notice = state.notice,
            reservedHeight = layout.form.mobileNoticeHeight,
        )
        Spacer(Modifier.height(layout.form.mobileNoticeToButton))
        LoginPrimaryButton(
            layout = layout,
            onClick = onSubmit,
            enabled = state.canSubmitMobileCode,
            loading = state.loggingIn,
        ) {
            LoginActionText(
                layout = layout,
                text = if (state.loggingIn) stringResource(R.string.login_submitting) else stringResource(R.string.login_submit),
            )
        }
        Spacer(Modifier.height(layout.form.buttonToFooter))
        LoginFooter(layout)
    }
}
