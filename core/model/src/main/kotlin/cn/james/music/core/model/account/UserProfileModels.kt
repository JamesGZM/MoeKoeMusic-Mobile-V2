package cn.james.music.core.model.account

data class UserProfile(
    val userId: String,
    val nickname: String?,
    val avatarUrl: String?,
    val backgroundUrl: String?,
    val signature: String,
    val fans: Long,
    val follows: Long,
    val listenMinutes: Long,
    val vip: VipSummary,
)

sealed interface VipSummary {
    data class Active(
        val label: String,
    ) : VipSummary

    data object Inactive : VipSummary

    data object Unavailable : VipSummary
}

sealed interface UserProfileError {
    data object SessionInitialization : UserProfileError

    data object Offline : UserProfileError

    data object Timeout : UserProfileError

    data object Connection : UserProfileError

    data object ServiceUnavailable : UserProfileError

    data object VerificationRequired : UserProfileError

    data object Rejected : UserProfileError

    data object Protocol : UserProfileError
}

sealed interface UserProfileResult {
    data object Anonymous : UserProfileResult

    data class Success(
        val profile: UserProfile,
    ) : UserProfileResult

    data class Failure(
        val error: UserProfileError,
    ) : UserProfileResult
}

interface UserProfileRepository {
    suspend fun load(): UserProfileResult
}
