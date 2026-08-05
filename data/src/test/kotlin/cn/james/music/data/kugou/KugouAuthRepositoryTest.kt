package cn.james.music.data.kugou

import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.AuthRiskChallenge
import cn.james.music.core.model.auth.AuthRiskMethod
import cn.james.music.core.model.auth.AuthRiskMethodResult
import cn.james.music.core.model.auth.AuthRiskProof
import cn.james.music.core.model.auth.AuthState
import cn.james.music.core.model.auth.MobileCodeLoginResult
import cn.james.music.core.model.auth.PasswordLoginResult
import cn.james.music.kugou.api.endpoint.KugouAccountOptionDto
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouAuthenticatedSessionDto
import cn.james.music.kugou.api.endpoint.KugouAuthenticationClient
import cn.james.music.kugou.api.endpoint.KugouMobileLoginResult
import cn.james.music.kugou.api.endpoint.KugouPasswordLoginResult
import cn.james.music.kugou.api.endpoint.KugouRiskChallengeDto
import cn.james.music.kugou.api.endpoint.KugouRiskMethodDto
import cn.james.music.kugou.api.endpoint.KugouRiskProofDto
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionMutationResult
import cn.james.music.kugou.api.session.KugouSessionMutator
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.KugouRequestContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouAuthRepositoryTest {
    @Test
    fun authenticatedResultCommitsBeforeReportingSuccess() =
        runBlocking {
            val mutator = RecordingMutator()
            val repository = repository(authenticatedClient(), mutator)

            val result = repository.loginWithMobileCode("13800000000", "246810")

            assertEquals(MobileCodeLoginResult.Authenticated, result)
            val committed = requireNotNull(mutator.replacement)
            assertEquals("fixture-token", committed.token)
            assertEquals("42", committed.userId)
            assertEquals("fixture-dfid", committed.dfid)
            assertEquals(FIXTURE_SESSION.identity, committed.identity)
            assertEquals("keep", committed.cookies.value("anonymous"))
            assertEquals("fixture-token", committed.cookies.value("token"))
        }

    @Test
    fun storageFailureNeverReportsAuthenticated() =
        runBlocking {
            val repository = repository(authenticatedClient(), RecordingMutator(fail = true))

            val result = repository.loginWithMobileCode("13800000000", "246810")

            assertEquals(MobileCodeLoginResult.Failure(AuthError.Storage), result)
        }

    @Test
    fun multipleAccountsMapWithoutMutatingSession() =
        runBlocking {
            val client =
                FakeAuthClient(
                    loginResult =
                        KugouMobileLoginResult.MultipleAccounts(
                            listOf(KugouAccountOptionDto("42", "Fixture", "https://example.test/avatar", "7")),
                        ),
                )
            val mutator = RecordingMutator()
            val result = repository(client, mutator).loginWithMobileCode("13800000000", "246810")

            assertTrue(result is MobileCodeLoginResult.MultipleAccounts)
            assertEquals("42", (result as MobileCodeLoginResult.MultipleAccounts).accounts.single().userId)
            assertNull(mutator.replacement)
        }

    @Test
    fun passwordAuthenticationCommitsSessionBeforeReportingSuccess() =
        runBlocking {
            val mutator = RecordingMutator()
            val client = FakeAuthClient(passwordResult = authenticatedPasswordResult())

            val result = repository(client, mutator).loginWithPassword("fixture-account", "fixture-password")

            assertEquals(PasswordLoginResult.Authenticated, result)
            assertEquals("fixture-token", requireNotNull(mutator.replacement).token)
        }

    @Test
    fun passwordRiskChallengeDoesNotMutateSessionAndRemainsRedacted() =
        runBlocking {
            val challenge = KugouRiskChallengeDto("fixture-event", "fixture-sid", "fixture-edt")
            val mutator = RecordingMutator()
            val result =
                repository(
                    FakeAuthClient(passwordResult = KugouPasswordLoginResult.RiskChallenge(challenge)),
                    mutator,
                ).loginWithPassword("fixture-account", "fixture-password")

            assertTrue(result is PasswordLoginResult.RiskChallenge)
            val domainChallenge = (result as PasswordLoginResult.RiskChallenge).challenge
            assertEquals("fixture-event", domainChallenge.eventId)
            assertFalse(domainChallenge.toString().contains("fixture-sid"))
            assertNull(mutator.replacement)
        }

    @Test
    fun riskMethodAndProofStayTypedAcrossRepositoryBoundary() =
        runBlocking {
            val client =
                FakeAuthClient(
                    riskMethodResult = KugouApiResult.Success(KugouRiskMethodDto.Tencent("fixture-app")),
                    verifyResult = KugouApiResult.Success(Unit),
                )
            val repository = repository(client, RecordingMutator())
            val challenge = AuthRiskChallenge("fixture-event", "fixture-sid", "fixture-edt")

            assertEquals(
                AuthRiskMethodResult.Available(AuthRiskMethod.Tencent("fixture-app")),
                repository.getRiskMethod(challenge),
            )
            assertEquals(
                AuthActionResult.Success,
                repository.verifyRisk(
                    challenge,
                    AuthRiskProof.Tencent("fixture-ticket", "fixture-random", "fixture-app"),
                ),
            )

            val proof = client.lastRiskProof as KugouRiskProofDto.Tencent
            assertEquals("fixture-app", proof.appId)
            assertFalse(proof.toString().contains("fixture-ticket"))
        }

    @Test
    fun logoutPreservesAnonymousIdentityAndDfid() =
        runBlocking {
            val authenticated =
                FIXTURE_SESSION.copy(
                    token = "fixture-token",
                    userId = "42",
                    cookies =
                        KugouCookies.from(
                            mapOf(
                                "dfid" to "fixture-dfid",
                                "anonymous" to "keep",
                                "token" to "fixture-token",
                                "userid" to "42",
                                "vip_type" to "1",
                            ),
                        ),
                )
            val mutator = RecordingMutator()
            val repository = repository(FakeAuthClient(), mutator, authenticated)

            assertEquals(AuthState.Authenticated("42"), repository.currentState())
            assertEquals(AuthActionResult.Success, repository.logout())

            val committed = requireNotNull(mutator.replacement)
            assertEquals(authenticated.identity, committed.identity)
            assertEquals("fixture-dfid", committed.dfid)
            assertNull(committed.token)
            assertNull(committed.userId)
            assertEquals("keep", committed.cookies.value("anonymous"))
            assertNull(committed.cookies.value("token"))
            assertNull(committed.cookies.value("vip_type"))
        }

    private fun repository(
        client: KugouAuthenticationClient,
        mutator: KugouSessionMutator,
        session: KugouSessionSnapshot = FIXTURE_SESSION,
    ) = KugouAuthRepository(
        sessionProvider = ReadyProvider(session),
        sessionMutator = mutator,
        authClient = client,
    )

    private fun authenticatedClient() =
        FakeAuthClient(
            loginResult =
                KugouMobileLoginResult.Authenticated(
                    KugouAuthenticatedSessionDto(
                        token = "fixture-token",
                        userId = "42",
                        cookies = KugouCookies.from(mapOf("token" to "fixture-token", "userid" to "42")),
                    ),
                ),
        )

    private fun authenticatedPasswordResult() =
        KugouPasswordLoginResult.Authenticated(
            KugouAuthenticatedSessionDto(
                token = "fixture-token",
                userId = "42",
                cookies = KugouCookies.from(mapOf("token" to "fixture-token", "userid" to "42")),
            ),
        )

    private class ReadyProvider(
        private val session: KugouSessionSnapshot,
    ) : KugouSessionProvider {
        override suspend fun initialize(): KugouInitializationResult = KugouInitializationResult.Ready(session)
    }

    private class RecordingMutator(
        private val fail: Boolean = false,
    ) : KugouSessionMutator {
        var replacement: KugouSessionSnapshot? = null

        override suspend fun replace(snapshot: KugouSessionSnapshot): KugouSessionMutationResult {
            replacement = snapshot
            return if (fail) KugouSessionMutationResult.StorageFailure else KugouSessionMutationResult.Updated(snapshot)
        }
    }

    private class FakeAuthClient(
        private val sendResult: KugouApiResult<Unit> = KugouApiResult.Success(Unit),
        private val loginResult: KugouMobileLoginResult =
            KugouMobileLoginResult.Failure(
                cn.james.music.kugou.api.transport.KugouError.Protocol(
                    cn.james.music.kugou.api.transport.KugouError.Protocol.Reason.ServiceRejected,
                ),
            ),
        private val passwordResult: KugouPasswordLoginResult =
            KugouPasswordLoginResult.Failure(
                cn.james.music.kugou.api.transport.KugouError.Protocol(
                    cn.james.music.kugou.api.transport.KugouError.Protocol.Reason.ServiceRejected,
                ),
            ),
        private val riskMethodResult: KugouApiResult<KugouRiskMethodDto> =
            KugouApiResult.Failure(
                cn.james.music.kugou.api.transport.KugouError.Protocol(
                    cn.james.music.kugou.api.transport.KugouError.Protocol.Reason.ServiceRejected,
                ),
            ),
        private val verifyResult: KugouApiResult<Unit> =
            KugouApiResult.Failure(
                cn.james.music.kugou.api.transport.KugouError.Protocol(
                    cn.james.music.kugou.api.transport.KugouError.Protocol.Reason.ServiceRejected,
                ),
            ),
    ) : KugouAuthenticationClient {
        var lastRiskProof: KugouRiskProofDto? = null

        override suspend fun sendMobileCode(
            mobile: String,
            context: KugouRequestContext,
        ): KugouApiResult<Unit> = sendResult

        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
            context: KugouRequestContext,
        ): KugouMobileLoginResult = loginResult

        override suspend fun loginWithPassword(
            username: String,
            password: String,
            context: KugouRequestContext,
        ): KugouPasswordLoginResult = passwordResult

        override suspend fun getRiskMethod(
            eventId: String,
            context: KugouRequestContext,
        ): KugouApiResult<KugouRiskMethodDto> = riskMethodResult

        override suspend fun verifyRisk(
            challenge: KugouRiskChallengeDto,
            proof: KugouRiskProofDto,
            context: KugouRequestContext,
        ): KugouApiResult<Unit> {
            lastRiskProof = proof
            return verifyResult
        }
    }

    private companion object {
        val FIXTURE_SESSION =
            KugouSessionSnapshot(
                identity = KugouDeviceIdentity("fixture-guid", "fixture-mid"),
                dfid = "fixture-dfid",
                cookies = KugouCookies.from(mapOf("dfid" to "fixture-dfid", "anonymous" to "keep")),
            )
    }
}
