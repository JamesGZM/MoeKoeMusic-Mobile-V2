package cn.james.music.data.kugou

import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.AuthState
import cn.james.music.core.model.auth.MobileCodeLoginResult
import cn.james.music.kugou.api.endpoint.KugouAccountOptionDto
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouAuthenticatedSessionDto
import cn.james.music.kugou.api.endpoint.KugouAuthenticationClient
import cn.james.music.kugou.api.endpoint.KugouMobileLoginResult
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
    ) : KugouAuthenticationClient {
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
