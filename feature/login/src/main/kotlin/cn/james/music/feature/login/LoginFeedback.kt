package cn.james.music.feature.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import cn.james.music.core.designsystem.MoeKoeTheme
import cn.james.music.core.model.auth.AuthError

@Composable
internal fun LoginNoticeText(
    notice: LoginNotice?,
    modifier: Modifier = Modifier,
    style: TextStyle? = null,
) {
    if (notice == null) return
    val isError = notice !is LoginNotice.CodeSent
    Text(
        text = notice.message(),
        modifier = modifier,
        style = style ?: MaterialTheme.typography.bodyMedium,
        color = if (isError) MaterialTheme.colorScheme.error else MoeKoeTheme.extraColors.success,
    )
}

/** Keeps transient success and error feedback on one shared baseline without moving actions below it. */
@Composable
internal fun LoginInlineFeedback(
    layout: LoginLayoutSpec,
    notice: LoginNotice?,
    reservedHeight: Dp,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(reservedHeight),
        contentAlignment = Alignment.TopStart,
    ) {
        if (notice == null) return@Box
        val isError = notice !is LoginNotice.CodeSent
        Text(
            text = notice.message(),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style =
                TextStyle(
                    fontSize = layout.typography.fieldSize,
                    lineHeight = layout.typography.fieldLineHeight,
                    fontWeight = FontWeight.Normal,
                ),
            color = if (isError) MaterialTheme.colorScheme.error else MoeKoeTheme.extraColors.success,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun LoginNotice.message(): String =
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
internal fun AuthError.message(): String =
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
