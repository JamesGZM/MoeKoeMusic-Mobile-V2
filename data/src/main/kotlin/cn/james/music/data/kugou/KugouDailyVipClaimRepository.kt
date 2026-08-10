package cn.james.music.data.kugou

import cn.james.music.core.model.account.DailyVipClaimError
import cn.james.music.core.model.account.DailyVipClaimRepository
import cn.james.music.core.model.account.DailyVipClaimResult
import cn.james.music.core.model.account.DailyVipUpgradeAuthorization
import cn.james.music.core.model.account.DailyVipUpgradeResult
import cn.james.music.kugou.api.endpoint.KugouDailyVipDayResult
import cn.james.music.kugou.api.endpoint.KugouDailyVipService
import cn.james.music.kugou.api.endpoint.KugouDailyVipUpgradeResult
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionObserver
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.KugouError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class KugouDailyVipClaimRepository
    @Inject
    constructor(
        private val sessionProvider: KugouSessionProvider,
        private val sessionObserver: KugouSessionObserver,
        private val dailyVipService: KugouDailyVipService,
        private val utcDateProvider: UtcDateProvider = UtcDateProvider(),
    ) : DailyVipClaimRepository {
        private val mutationMutex = Mutex()
        private val authorizationOwner = Any()

        override suspend fun claim(): DailyVipClaimResult =
            mutationMutex.withLock {
                when (val current = initializeCurrentScope()) {
                    is ScopeLoadResult.Failure -> DailyVipClaimResult.Failure(current.error)
                    is ScopeLoadResult.Ready -> claimFor(current.scope)
                }
            }

        override suspend fun upgrade(authorization: DailyVipUpgradeAuthorization): DailyVipUpgradeResult =
            mutationMutex.withLock {
                val issuedAuthorization =
                    authorization as? IssuedUpgradeAuthorization
                        ?: return@withLock DailyVipUpgradeResult.Failure(DailyVipClaimError.InvalidUpgradeAuthorization)
                if (issuedAuthorization.owner !== authorizationOwner || issuedAuthorization.consumed) {
                    return@withLock DailyVipUpgradeResult.Failure(DailyVipClaimError.InvalidUpgradeAuthorization)
                }
                issuedAuthorization.consumed = true
                when (val current = initializeCurrentScope()) {
                    is ScopeLoadResult.Failure -> {
                        DailyVipUpgradeResult.Failure(current.error)
                    }

                    is ScopeLoadResult.Ready -> {
                        if (!current.scope.matches(issuedAuthorization.scope)) {
                            DailyVipUpgradeResult.Failure(DailyVipClaimError.StaleSession)
                        } else {
                            upgradeFor(current.scope)
                        }
                    }
                }
            }

        private suspend fun claimFor(scope: CurrentScope): DailyVipClaimResult {
            val result = dailyVipService.claimDay(scope.date.toString(), scope.session.requestContext())
            scope.validateCurrent()?.let { return DailyVipClaimResult.Failure(it) }
            return when (result) {
                KugouDailyVipDayResult.Claimed -> {
                    DailyVipClaimResult.Claimed(issueAuthorization(scope))
                }

                KugouDailyVipDayResult.AlreadyClaimed -> {
                    DailyVipClaimResult.AlreadyClaimed(issueAuthorization(scope))
                }

                is KugouDailyVipDayResult.Failure -> {
                    DailyVipClaimResult.Failure(result.error.toDailyVipClaimError())
                }
            }
        }

        private suspend fun upgradeFor(scope: CurrentScope): DailyVipUpgradeResult {
            val result = dailyVipService.upgrade(scope.session.requestContext())
            scope.validateCurrent()?.let { return DailyVipUpgradeResult.Failure(it) }
            return when (result) {
                KugouDailyVipUpgradeResult.Upgraded -> {
                    DailyVipUpgradeResult.Upgraded
                }

                KugouDailyVipUpgradeResult.AlreadyClaimed -> {
                    DailyVipUpgradeResult.AlreadyClaimed
                }

                is KugouDailyVipUpgradeResult.Failure -> {
                    DailyVipUpgradeResult.Failure(result.error.toDailyVipClaimError())
                }
            }
        }

        private suspend fun initializeCurrentScope(): ScopeLoadResult {
            val session =
                when (val initialization = sessionProvider.initialize()) {
                    is KugouInitializationResult.Failure -> return ScopeLoadResult.Failure(DailyVipClaimError.SessionInitialization)
                    is KugouInitializationResult.Ready -> initialization.session
                }
            val userId = session.authenticatedUserId() ?: return ScopeLoadResult.Failure(DailyVipClaimError.AuthenticationRequired)
            val observed = sessionObserver.state.value
            if (observed.session != session || observed.session.authenticatedUserId() != userId) {
                return ScopeLoadResult.Failure(DailyVipClaimError.StaleSession)
            }
            return ScopeLoadResult.Ready(
                CurrentScope(
                    session = session,
                    generation = observed.generation,
                    userId = userId,
                    date = utcDateProvider.today(),
                ),
            )
        }

        private fun issueAuthorization(scope: CurrentScope): DailyVipUpgradeAuthorization =
            IssuedUpgradeAuthorization(
                owner = authorizationOwner,
                scope = UpgradeScope(scope.generation, scope.userId, scope.date),
            )

        private fun CurrentScope.validateCurrent(): DailyVipClaimError? {
            val observed = sessionObserver.state.value
            val observedUserId = observed.session.authenticatedUserId()
            val error =
                when {
                    observedUserId == null -> {
                        DailyVipClaimError.AuthenticationRequired
                    }

                    observed.generation != generation || observedUserId != userId || utcDateProvider.today() != date -> {
                        DailyVipClaimError.StaleSession
                    }

                    else -> {
                        null
                    }
                }
            return error
        }

        private fun KugouSessionSnapshot?.authenticatedUserId(): String? {
            val snapshot = this ?: return null
            return snapshot.userId?.takeIf {
                !snapshot.token.isNullOrBlank() && it.toLongOrNull()?.let { value -> value > 0 } == true
            }
        }

        private fun KugouError.toDailyVipClaimError(): DailyVipClaimError =
            when (this) {
                is KugouError.Network -> {
                    when (kind) {
                        KugouError.Network.Kind.Offline -> DailyVipClaimError.Offline
                        KugouError.Network.Kind.Timeout -> DailyVipClaimError.Timeout
                        KugouError.Network.Kind.Connection -> DailyVipClaimError.Connection
                    }
                }

                is KugouError.Http -> {
                    DailyVipClaimError.Http(statusCode)
                }

                is KugouError.Risk -> {
                    DailyVipClaimError.RiskBlocked
                }

                is KugouError.Protocol -> {
                    if (reason == KugouError.Protocol.Reason.ServiceRejected) {
                        DailyVipClaimError.Rejected
                    } else {
                        DailyVipClaimError.Protocol
                    }
                }
            }

        private sealed interface ScopeLoadResult {
            data class Ready(
                val scope: CurrentScope,
            ) : ScopeLoadResult

            data class Failure(
                val error: DailyVipClaimError,
            ) : ScopeLoadResult
        }

        private data class CurrentScope(
            val session: KugouSessionSnapshot,
            val generation: Long,
            val userId: String,
            val date: LocalDate,
        ) {
            fun matches(authorization: UpgradeScope): Boolean =
                generation == authorization.generation && userId == authorization.userId && date == authorization.date
        }

        private data class UpgradeScope(
            val generation: Long,
            val userId: String,
            val date: LocalDate,
        )

        private class IssuedUpgradeAuthorization(
            val owner: Any,
            val scope: UpgradeScope,
            var consumed: Boolean = false,
        ) : DailyVipUpgradeAuthorization {
            override fun toString(): String = "DailyVipUpgradeAuthorization(<redacted>)"
        }
    }

internal open class UtcDateProvider(
    private val clock: Clock = Clock.systemUTC(),
) {
    open fun today(): LocalDate = LocalDate.now(clock)
}
