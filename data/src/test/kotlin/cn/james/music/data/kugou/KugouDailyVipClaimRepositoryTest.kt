package cn.james.music.data.kugou

import cn.james.music.core.model.account.DailyVipClaimError
import cn.james.music.core.model.account.DailyVipClaimResult
import cn.james.music.core.model.account.DailyVipUpgradeAuthorization
import cn.james.music.core.model.account.DailyVipUpgradeResult
import cn.james.music.kugou.api.endpoint.KugouDailyVipDayResult
import cn.james.music.kugou.api.endpoint.KugouDailyVipService
import cn.james.music.kugou.api.endpoint.KugouDailyVipUpgradeResult
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouInitializationError
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionObserver
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.session.KugouSessionState
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouRequestContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class KugouDailyVipClaimRepositoryTest {
    @Test
    fun initializationFailureAndAnonymousSessionDoNotCallService() =
        runBlocking {
            val service = FakeDailyVipService()
            val initializationFailure =
                repository(
                    provider = FakeSessionProvider(KugouInitializationResult.Failure(KugouInitializationError.StorageRead)),
                    observer = FakeSessionObserver(null),
                    service = service,
                )
            val anonymousSession = FIXTURE_SESSION.copy(token = null, userId = null)
            val anonymous =
                repository(
                    provider = FakeSessionProvider(KugouInitializationResult.Ready(anonymousSession)),
                    observer = FakeSessionObserver(anonymousSession),
                    service = service,
                )

            assertEquals(
                DailyVipClaimResult.Failure(DailyVipClaimError.SessionInitialization),
                initializationFailure.claim(),
            )
            assertEquals(
                DailyVipClaimResult.Failure(DailyVipClaimError.AuthenticationRequired),
                anonymous.claim(),
            )
            assertEquals(0, service.dayCalls)
            assertEquals(0, service.upgradeCalls)
        }

    @Test
    fun claimUsesUtcDateAcrossLocalDateBoundaryAndMapsClaimedAndAlready() =
        runBlocking {
            val service =
                FakeDailyVipService(
                    dayResults = ArrayDeque(listOf(KugouDailyVipDayResult.Claimed, KugouDailyVipDayResult.AlreadyClaimed)),
                )
            val repository =
                repository(
                    service = service,
                    dateProvider =
                        UtcDateProvider(
                            Clock.fixed(Instant.parse("2026-08-10T00:30:00Z"), ZoneOffset.UTC),
                        ),
                )

            val claimed = repository.claim()
            val already = repository.claim()

            assertTrue(claimed is DailyVipClaimResult.Claimed)
            assertTrue(already is DailyVipClaimResult.AlreadyClaimed)
            assertFalse((already as DailyVipClaimResult.AlreadyClaimed).upgradeAuthorization.toString().contains("42"))
            assertEquals(listOf("2026-08-10", "2026-08-10"), service.receiveDays)
        }

    @Test
    fun protocolNetworkHttpAndRiskFailuresMapWithoutAuthorization() =
        runBlocking {
            val cases =
                listOf(
                    KugouError.Network(KugouError.Network.Kind.Offline) to DailyVipClaimError.Offline,
                    KugouError.Network(KugouError.Network.Kind.Timeout) to DailyVipClaimError.Timeout,
                    KugouError.Network(KugouError.Network.Kind.Connection) to DailyVipClaimError.Connection,
                    KugouError.Http(503) to DailyVipClaimError.Http(503),
                    KugouError.Risk("fixture-risk") to DailyVipClaimError.RiskBlocked,
                    KugouError.Protocol(KugouError.Protocol.Reason.ServiceRejected) to DailyVipClaimError.Rejected,
                    KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse) to DailyVipClaimError.Protocol,
                )

            cases.forEach { (protocolError, domainError) ->
                val repository = repository(service = FakeDailyVipService(dayResult = KugouDailyVipDayResult.Failure(protocolError)))

                assertEquals(DailyVipClaimResult.Failure(domainError), repository.claim())
            }
        }

    @Test
    fun upgradeMapsProtocolNetworkHttpAndRiskFailures() =
        runBlocking {
            val cases =
                listOf(
                    KugouError.Network(KugouError.Network.Kind.Offline) to DailyVipClaimError.Offline,
                    KugouError.Network(KugouError.Network.Kind.Timeout) to DailyVipClaimError.Timeout,
                    KugouError.Network(KugouError.Network.Kind.Connection) to DailyVipClaimError.Connection,
                    KugouError.Http(429) to DailyVipClaimError.Http(429),
                    KugouError.Risk("fixture-risk") to DailyVipClaimError.RiskBlocked,
                    KugouError.Protocol(KugouError.Protocol.Reason.ServiceRejected) to DailyVipClaimError.Rejected,
                    KugouError.Protocol(KugouError.Protocol.Reason.MissingRequiredField) to DailyVipClaimError.Protocol,
                )

            cases.forEach { (protocolError, domainError) ->
                val repository =
                    repository(
                        service =
                            FakeDailyVipService(
                                upgradeResult = KugouDailyVipUpgradeResult.Failure(protocolError),
                            ),
                    )
                val authorization = (repository.claim() as DailyVipClaimResult.Claimed).upgradeAuthorization

                assertEquals(DailyVipUpgradeResult.Failure(domainError), repository.upgrade(authorization))
            }
        }

    @Test
    fun authorizationIsOwnerBoundSingleUseAndCannotBeForged() =
        runBlocking {
            val firstService = FakeDailyVipService(upgradeResult = KugouDailyVipUpgradeResult.Upgraded)
            val first = repository(service = firstService)
            val authorization = (first.claim() as DailyVipClaimResult.Claimed).upgradeAuthorization
            val secondService = FakeDailyVipService()
            val second = repository(service = secondService)

            assertEquals(
                DailyVipUpgradeResult.Failure(DailyVipClaimError.InvalidUpgradeAuthorization),
                first.upgrade(object : DailyVipUpgradeAuthorization {}),
            )
            assertEquals(
                DailyVipUpgradeResult.Failure(DailyVipClaimError.InvalidUpgradeAuthorization),
                second.upgrade(authorization),
            )
            assertEquals(DailyVipUpgradeResult.Upgraded, first.upgrade(authorization))
            assertEquals(
                DailyVipUpgradeResult.Failure(DailyVipClaimError.InvalidUpgradeAuthorization),
                first.upgrade(authorization),
            )
            assertEquals(1, firstService.upgradeCalls)
            assertEquals(0, secondService.upgradeCalls)
            assertFalse(authorization.toString().contains("42"))
        }

    @Test
    fun authorizationRejectsGenerationAccountAndDateChangesBeforeUpgrade() =
        runBlocking {
            val observer = FakeSessionObserver(FIXTURE_SESSION)
            val dateProvider = MutableDateProvider(LocalDate.parse("2026-08-10"))
            val service = FakeDailyVipService()
            val repository = repository(observer = observer, service = service, dateProvider = dateProvider)

            val generationAuthorization = (repository.claim() as DailyVipClaimResult.Claimed).upgradeAuthorization
            observer.publish(FIXTURE_SESSION)
            assertEquals(
                DailyVipUpgradeResult.Failure(DailyVipClaimError.StaleSession),
                repository.upgrade(generationAuthorization),
            )

            val accountAuthorization = (repository.claim() as DailyVipClaimResult.Claimed).upgradeAuthorization
            observer.publish(AUTHENTICATED_OTHER_SESSION)
            assertEquals(
                DailyVipUpgradeResult.Failure(DailyVipClaimError.StaleSession),
                repository.upgrade(accountAuthorization),
            )

            observer.publish(FIXTURE_SESSION)
            val dateAuthorization = (repository.claim() as DailyVipClaimResult.Claimed).upgradeAuthorization
            dateProvider.date = LocalDate.parse("2026-08-11")
            assertEquals(
                DailyVipUpgradeResult.Failure(DailyVipClaimError.StaleSession),
                repository.upgrade(dateAuthorization),
            )
            assertEquals(0, service.upgradeCalls)
        }

    @Test
    fun lateClaimAfterLogoutReturnsAuthenticationFailureWithoutAuthorization() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val observer = FakeSessionObserver(FIXTURE_SESSION)
            val service =
                FakeDailyVipService(
                    onClaim = {
                        started.complete(Unit)
                        release.await()
                        KugouDailyVipDayResult.Claimed
                    },
                )
            val repository = repository(observer = observer, service = service)
            val pending = async { repository.claim() }

            started.await()
            observer.publish(FIXTURE_SESSION.copy(token = null, userId = null))
            release.complete(Unit)

            assertEquals(DailyVipClaimResult.Failure(DailyVipClaimError.AuthenticationRequired), pending.await())
        }

    @Test
    fun mutationsAreSerializedAndCancellationConsumesUpgradeAuthorization() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val service =
                FakeDailyVipService(
                    onClaim = {
                        if (serviceCallNumber(started)) release.await()
                        KugouDailyVipDayResult.Claimed
                    },
                )
            val repository = repository(service = service)
            val first = async { repository.claim() }

            started.await()
            val second = async { repository.claim() }
            yield()
            assertEquals(1, service.dayCalls)
            release.complete(Unit)
            first.await()
            second.await()
            assertEquals(2, service.dayCalls)

            val cancellationService =
                FakeDailyVipService(
                    upgradeResult = null,
                    onUpgrade = { throw CancellationException("fixture cancellation") },
                )
            val cancellationRepository = repository(service = cancellationService)
            val authorization = (cancellationRepository.claim() as DailyVipClaimResult.Claimed).upgradeAuthorization

            try {
                cancellationRepository.upgrade(authorization)
                throw AssertionError("Expected CancellationException")
            } catch (_: CancellationException) {
                // Expected: the write may have reached transport, but this capability is consumed.
            }
            assertEquals(
                DailyVipUpgradeResult.Failure(DailyVipClaimError.InvalidUpgradeAuthorization),
                cancellationRepository.upgrade(authorization),
            )
            assertEquals(1, cancellationService.upgradeCalls)
        }

    @Test(expected = CancellationException::class)
    fun claimCancellationPropagatesWithoutIssuingAuthorization() {
        runBlocking {
            repository(
                service = FakeDailyVipService(onClaim = { throw CancellationException("fixture cancellation") }),
            ).claim()
        }
    }

    private fun serviceCallNumber(started: CompletableDeferred<Unit>): Boolean {
        started.complete(Unit)
        return true
    }

    private fun repository(
        provider: KugouSessionProvider = FakeSessionProvider(KugouInitializationResult.Ready(FIXTURE_SESSION)),
        observer: FakeSessionObserver = FakeSessionObserver(FIXTURE_SESSION),
        service: FakeDailyVipService = FakeDailyVipService(),
        dateProvider: UtcDateProvider = UtcDateProvider(Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC)),
    ) = KugouDailyVipClaimRepository(
        sessionProvider = provider,
        sessionObserver = observer,
        dailyVipService = service,
        utcDateProvider = dateProvider,
    )

    private class FakeSessionProvider(
        private val result: KugouInitializationResult,
    ) : KugouSessionProvider {
        override suspend fun initialize(): KugouInitializationResult = result
    }

    private class FakeSessionObserver(
        session: KugouSessionSnapshot?,
    ) : KugouSessionObserver {
        private val mutableState = MutableStateFlow(KugouSessionState(session = session, generation = 1))

        override val state: StateFlow<KugouSessionState> = mutableState

        fun publish(session: KugouSessionSnapshot?) {
            mutableState.value = KugouSessionState(session = session, generation = mutableState.value.generation + 1)
        }
    }

    private class MutableDateProvider(
        var date: LocalDate,
    ) : UtcDateProvider(Clock.systemUTC()) {
        override fun today(): LocalDate = date
    }

    private class FakeDailyVipService(
        private val dayResults: ArrayDeque<KugouDailyVipDayResult> = ArrayDeque(),
        private val dayResult: KugouDailyVipDayResult = KugouDailyVipDayResult.Claimed,
        private val upgradeResult: KugouDailyVipUpgradeResult? = KugouDailyVipUpgradeResult.Upgraded,
        private val onClaim: (suspend (String) -> KugouDailyVipDayResult)? = null,
        private val onUpgrade: (suspend () -> KugouDailyVipUpgradeResult)? = null,
    ) : KugouDailyVipService {
        var dayCalls = 0
            private set
        var upgradeCalls = 0
            private set
        val receiveDays = mutableListOf<String>()

        override suspend fun claimDay(
            receiveDay: String,
            context: KugouRequestContext,
        ): KugouDailyVipDayResult {
            dayCalls += 1
            receiveDays += receiveDay
            return onClaim?.invoke(receiveDay) ?: dayResults.removeFirstOrNull() ?: dayResult
        }

        override suspend fun upgrade(context: KugouRequestContext): KugouDailyVipUpgradeResult {
            upgradeCalls += 1
            return onUpgrade?.invoke() ?: requireNotNull(upgradeResult)
        }
    }

    private companion object {
        val FIXTURE_SESSION =
            KugouSessionSnapshot(
                identity = KugouDeviceIdentity(guid = "fixture-guid", mid = "fixture-mid"),
                dfid = "fixture-dfid",
                token = "fixture-token",
                userId = "42",
            )
        val AUTHENTICATED_OTHER_SESSION = FIXTURE_SESSION.copy(token = "other-token", userId = "99")
    }
}
