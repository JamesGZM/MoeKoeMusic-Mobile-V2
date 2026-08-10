package cn.james.music.core.model.account

/**
 * Opaque capability issued only after a successful or already-completed daily claim. Callers must
 * return it to the same [DailyVipClaimRepository] for one optional upgrade attempt.
 */
interface DailyVipUpgradeAuthorization

interface DailyVipClaimRepository {
    suspend fun claim(): DailyVipClaimResult

    suspend fun upgrade(authorization: DailyVipUpgradeAuthorization): DailyVipUpgradeResult
}

sealed interface DailyVipClaimResult {
    data class Claimed(
        val upgradeAuthorization: DailyVipUpgradeAuthorization,
    ) : DailyVipClaimResult

    data class AlreadyClaimed(
        val upgradeAuthorization: DailyVipUpgradeAuthorization,
    ) : DailyVipClaimResult

    data class Failure(
        val error: DailyVipClaimError,
    ) : DailyVipClaimResult
}

sealed interface DailyVipUpgradeResult {
    data object Upgraded : DailyVipUpgradeResult

    data object AlreadyClaimed : DailyVipUpgradeResult

    data class Failure(
        val error: DailyVipClaimError,
    ) : DailyVipUpgradeResult
}

sealed interface DailyVipClaimError {
    data object SessionInitialization : DailyVipClaimError

    data object AuthenticationRequired : DailyVipClaimError

    data object StaleSession : DailyVipClaimError

    data object InvalidUpgradeAuthorization : DailyVipClaimError

    data object Offline : DailyVipClaimError

    data object Timeout : DailyVipClaimError

    data object Connection : DailyVipClaimError

    data class Http(
        val statusCode: Int,
    ) : DailyVipClaimError

    data object RiskBlocked : DailyVipClaimError

    data object Rejected : DailyVipClaimError

    data object Protocol : DailyVipClaimError
}
