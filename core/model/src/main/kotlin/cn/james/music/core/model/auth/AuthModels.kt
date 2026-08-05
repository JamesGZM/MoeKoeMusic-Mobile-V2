package cn.james.music.core.model.auth

data class AuthAccountOption(
    val userId: String,
    val nickname: String,
    val avatarUrl: String?,
    val grade: String?,
)

sealed interface AuthState {
    data object Anonymous : AuthState

    data class Authenticated(
        val userId: String,
    ) : AuthState

    data class Unavailable(
        val error: AuthError,
    ) : AuthState
}

sealed interface AuthError {
    data object SessionInitialization : AuthError

    data object Offline : AuthError

    data object Timeout : AuthError

    data object Connection : AuthError

    data object ServiceUnavailable : AuthError

    data object Rejected : AuthError

    data object Protocol : AuthError

    data object Storage : AuthError
}

sealed interface AuthActionResult {
    data object Success : AuthActionResult

    data class Failure(
        val error: AuthError,
    ) : AuthActionResult
}

sealed interface MobileCodeLoginResult {
    data object Authenticated : MobileCodeLoginResult

    data class MultipleAccounts(
        val accounts: List<AuthAccountOption>,
    ) : MobileCodeLoginResult

    data class Failure(
        val error: AuthError,
    ) : MobileCodeLoginResult
}

interface AuthRepository {
    suspend fun currentState(): AuthState

    suspend fun sendMobileCode(mobile: String): AuthActionResult

    suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String? = null,
    ): MobileCodeLoginResult

    suspend fun logout(): AuthActionResult
}
