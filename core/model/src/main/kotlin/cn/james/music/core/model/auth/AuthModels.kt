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

data class AuthRiskChallenge(
    val eventId: String,
    val sid: String?,
    val edt: String?,
) {
    override fun toString(): String = "AuthRiskChallenge(eventId=<redacted>, sid=<redacted>, edt=<redacted>)"
}

sealed interface PasswordLoginResult {
    data object Authenticated : PasswordLoginResult

    data class RiskChallenge(
        val challenge: AuthRiskChallenge,
    ) : PasswordLoginResult

    data class MultipleAccounts(
        val accounts: List<AuthAccountOption>,
    ) : PasswordLoginResult

    data class Failure(
        val error: AuthError,
    ) : PasswordLoginResult
}

data class QrLoginSession(
    val key: String,
    val loginUrl: String,
) {
    override fun toString(): String = "QrLoginSession(key=<redacted>, loginUrl=<redacted>)"
}

sealed interface QrLoginStartResult {
    data class Ready(
        val session: QrLoginSession,
    ) : QrLoginStartResult

    data class Failure(
        val error: AuthError,
    ) : QrLoginStartResult
}

sealed interface QrLoginCheckResult {
    data object Waiting : QrLoginCheckResult

    data class Scanned(
        val nickname: String?,
    ) : QrLoginCheckResult

    data object Expired : QrLoginCheckResult

    data object Authenticated : QrLoginCheckResult

    data class Failure(
        val error: AuthError,
    ) : QrLoginCheckResult
}

sealed interface AuthRiskMethod {
    data object Sms : AuthRiskMethod

    data class Tencent(
        val appId: String,
    ) : AuthRiskMethod

    data class Unsupported(
        val type: Int,
    ) : AuthRiskMethod
}

sealed interface AuthRiskMethodResult {
    data class Available(
        val method: AuthRiskMethod,
    ) : AuthRiskMethodResult

    data class Failure(
        val error: AuthError,
    ) : AuthRiskMethodResult
}

sealed interface AuthRiskProof {
    class Sms(
        val code: String,
    ) : AuthRiskProof {
        override fun toString(): String = "AuthRiskProof.Sms(code=<redacted>)"
    }

    class Tencent(
        val ticket: String,
        val randomString: String,
        val appId: String,
    ) : AuthRiskProof {
        override fun toString(): String = "AuthRiskProof.Tencent(ticket=<redacted>, randomString=<redacted>, appId=<redacted>)"
    }
}

interface AuthRepository {
    suspend fun currentState(): AuthState

    suspend fun sendMobileCode(mobile: String): AuthActionResult

    suspend fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String? = null,
    ): MobileCodeLoginResult

    suspend fun loginWithPassword(
        username: String,
        password: String,
    ): PasswordLoginResult

    suspend fun createQrLogin(): QrLoginStartResult

    suspend fun checkQrLogin(key: String): QrLoginCheckResult

    suspend fun getRiskMethod(challenge: AuthRiskChallenge): AuthRiskMethodResult

    suspend fun verifyRisk(
        challenge: AuthRiskChallenge,
        proof: AuthRiskProof,
    ): AuthActionResult

    suspend fun logout(): AuthActionResult
}
