package cn.james.music.feature.my

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.james.music.core.model.account.UserProfileError
import cn.james.music.core.model.auth.AuthError

@Composable
internal fun UserProfileError.profileMessage(): String =
    stringResource(
        when (this) {
            UserProfileError.SessionInitialization -> R.string.my_error_session
            UserProfileError.Offline -> R.string.my_error_offline
            UserProfileError.Timeout -> R.string.my_error_timeout
            UserProfileError.Connection -> R.string.my_error_connection
            UserProfileError.ServiceUnavailable -> R.string.my_error_service
            UserProfileError.VerificationRequired -> R.string.my_error_verification
            UserProfileError.Rejected -> R.string.my_error_rejected
            UserProfileError.Protocol -> R.string.my_error_protocol
        },
    )

@Composable
internal fun AuthError.logoutMessage(): String =
    stringResource(
        when (this) {
            AuthError.Storage, AuthError.SessionInitialization -> R.string.my_logout_error_storage
            AuthError.Offline, AuthError.Timeout, AuthError.Connection, AuthError.ServiceUnavailable -> R.string.my_logout_error_retry
            AuthError.Rejected, AuthError.Protocol -> R.string.my_logout_error_retry
        },
    )
