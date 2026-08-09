package cn.james.music.feature.my

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
internal fun MyProfileProblemUi.profileMessage(): String =
    stringResource(
        when (this) {
            MyProfileProblemUi.SessionInitialization -> R.string.my_error_session
            MyProfileProblemUi.Offline -> R.string.my_error_offline
            MyProfileProblemUi.Timeout -> R.string.my_error_timeout
            MyProfileProblemUi.Connection -> R.string.my_error_connection
            MyProfileProblemUi.ServiceUnavailable -> R.string.my_error_service
            MyProfileProblemUi.VerificationRequired -> R.string.my_error_verification
            MyProfileProblemUi.Rejected -> R.string.my_error_rejected
            MyProfileProblemUi.Protocol -> R.string.my_error_protocol
        },
    )

@Composable
internal fun MyLogoutProblemUi.logoutMessage(): String =
    stringResource(
        when (this) {
            MyLogoutProblemUi.Storage -> R.string.my_logout_error_storage
            MyLogoutProblemUi.Retry -> R.string.my_logout_error_retry
        },
    )
