package cn.james.music.data.kugou

import cn.james.music.core.model.account.UserProfileError
import cn.james.music.core.model.account.UserProfileResult
import cn.james.music.core.model.account.VipSummary
import cn.james.music.kugou.api.endpoint.KugouApiResult
import cn.james.music.kugou.api.endpoint.KugouUserDetailDto
import cn.james.music.kugou.api.endpoint.KugouUserService
import cn.james.music.kugou.api.endpoint.KugouVipSummaryDto
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouInitializationError
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouSessionProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouRequestContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouUserProfileRepositoryTest {
    @Test
    fun anonymousSessionDoesNotRequestProfile() =
        runBlocking {
            val service = FakeUserService()
            val repository = repository(service, FIXTURE_SESSION)

            assertEquals(UserProfileResult.Anonymous, repository.load())
            assertEquals(0, service.detailCalls)
            assertEquals(0, service.vipCalls)
        }

    @Test
    fun profileSucceedsWhenVipRequestFails() =
        runBlocking {
            val service =
                FakeUserService(
                    detailResult = KugouApiResult.Success(FIXTURE_DETAIL),
                    vipResult = KugouApiResult.Failure(KugouError.Network(KugouError.Network.Kind.Timeout)),
                )

            val result = repository(service, AUTHENTICATED_SESSION).load() as UserProfileResult.Success

            assertEquals("42", result.profile.userId)
            assertEquals("Fixture", result.profile.nickname)
            assertEquals("https://example.test/240/avatar", result.profile.avatarUrl)
            assertEquals("https://example.test/720/background", result.profile.backgroundUrl)
            assertEquals(VipSummary.Unavailable, result.profile.vip)
            assertEquals(1, service.detailCalls)
            assertEquals(1, service.vipCalls)
        }

    @Test
    fun detailAndVipRequestsStartInParallel() =
        runBlocking {
            val vipStarted = CompletableDeferred<Unit>()
            val service =
                object : KugouUserService {
                    override suspend fun fetchUserDetail(context: KugouRequestContext): KugouApiResult<KugouUserDetailDto> {
                        vipStarted.await()
                        return KugouApiResult.Success(FIXTURE_DETAIL)
                    }

                    override suspend fun fetchVipSummary(context: KugouRequestContext): KugouApiResult<KugouVipSummaryDto> {
                        vipStarted.complete(Unit)
                        return KugouApiResult.Success(KugouVipSummaryDto(false, null))
                    }
                }

            val result = withTimeout(1_000) { repository(service, AUTHENTICATED_SESSION).load() }

            assertTrue(result is UserProfileResult.Success)
        }

    @Test
    fun activeAndInactiveVipRemainDistinct() =
        runBlocking {
            val active =
                repository(
                    FakeUserService(
                        detailResult = KugouApiResult.Success(FIXTURE_DETAIL),
                        vipResult = KugouApiResult.Success(KugouVipSummaryDto(true, "SVIP")),
                    ),
                    AUTHENTICATED_SESSION,
                ).load() as UserProfileResult.Success
            val inactive =
                repository(
                    FakeUserService(
                        detailResult = KugouApiResult.Success(FIXTURE_DETAIL),
                        vipResult = KugouApiResult.Success(KugouVipSummaryDto(false, null)),
                    ),
                    AUTHENTICATED_SESSION,
                ).load() as UserProfileResult.Success

            assertEquals(VipSummary.Active("SVIP"), active.profile.vip)
            assertEquals(VipSummary.Inactive, inactive.profile.vip)
        }

    @Test
    fun detailFailureIsTypedAndDoesNotUseVipAsProfile() =
        runBlocking {
            val service =
                FakeUserService(
                    detailResult = KugouApiResult.Failure(KugouError.Network(KugouError.Network.Kind.Offline)),
                    vipResult = KugouApiResult.Success(KugouVipSummaryDto(true, "VIP")),
                )

            val result = repository(service, AUTHENTICATED_SESSION).load()

            assertEquals(UserProfileResult.Failure(UserProfileError.Offline), result)
            assertEquals(1, service.detailCalls)
            assertEquals(1, service.vipCalls)
        }

    @Test
    fun mismatchedResponseUserIdIsRejected() =
        runBlocking {
            val service =
                FakeUserService(
                    detailResult = KugouApiResult.Success(FIXTURE_DETAIL.copy(userId = "99")),
                    vipResult = KugouApiResult.Success(KugouVipSummaryDto(false, null)),
                )

            assertEquals(
                UserProfileResult.Failure(UserProfileError.Protocol),
                repository(service, AUTHENTICATED_SESSION).load(),
            )
        }

    @Test
    fun missingOptionalProfileFieldsStayEmpty() =
        runBlocking {
            val service =
                FakeUserService(
                    detailResult =
                        KugouApiResult.Success(
                            FIXTURE_DETAIL.copy(
                                userId = null,
                                nickname = null,
                                avatarUrl = null,
                                backgroundUrl = null,
                            ),
                        ),
                    vipResult = KugouApiResult.Success(KugouVipSummaryDto(false, null)),
                )

            val profile = (repository(service, AUTHENTICATED_SESSION).load() as UserProfileResult.Success).profile

            assertNull(profile.nickname)
            assertNull(profile.avatarUrl)
            assertNull(profile.backgroundUrl)
            assertFalse(profile.vip is VipSummary.Active)
        }

    @Test
    fun sessionInitializationFailureDoesNotRequestProfile() =
        runBlocking {
            val service = FakeUserService()
            val repository =
                KugouUserProfileRepository(
                    sessionProvider =
                        FakeSessionProvider(
                            KugouInitializationResult.Failure(KugouInitializationError.StorageRead),
                        ),
                    userService = service,
                )

            assertEquals(
                UserProfileResult.Failure(UserProfileError.SessionInitialization),
                repository.load(),
            )
            assertTrue(service.detailCalls == 0 && service.vipCalls == 0)
        }

    private fun repository(
        service: KugouUserService,
        session: KugouSessionSnapshot,
    ) = KugouUserProfileRepository(
        sessionProvider = FakeSessionProvider(KugouInitializationResult.Ready(session)),
        userService = service,
    )

    private class FakeSessionProvider(
        private val result: KugouInitializationResult,
    ) : KugouSessionProvider {
        override suspend fun initialize(): KugouInitializationResult = result
    }

    private class FakeUserService(
        private val detailResult: KugouApiResult<KugouUserDetailDto> =
            KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.ServiceRejected)),
        private val vipResult: KugouApiResult<KugouVipSummaryDto> =
            KugouApiResult.Failure(KugouError.Protocol(KugouError.Protocol.Reason.ServiceRejected)),
    ) : KugouUserService {
        var detailCalls = 0
        var vipCalls = 0

        override suspend fun fetchUserDetail(context: KugouRequestContext): KugouApiResult<KugouUserDetailDto> {
            detailCalls += 1
            return detailResult
        }

        override suspend fun fetchVipSummary(context: KugouRequestContext): KugouApiResult<KugouVipSummaryDto> {
            vipCalls += 1
            return vipResult
        }
    }

    private companion object {
        val FIXTURE_SESSION =
            KugouSessionSnapshot(
                identity = KugouDeviceIdentity("fixture-guid", "fixture-mid"),
                dfid = "fixture-dfid",
                cookies = KugouCookies.from(mapOf("dfid" to "fixture-dfid")),
            )
        val AUTHENTICATED_SESSION =
            FIXTURE_SESSION.copy(
                token = "fixture-token",
                userId = "42",
                cookies = KugouCookies.from(mapOf("token" to "fixture-token", "userid" to "42")),
            )
        val FIXTURE_DETAIL =
            KugouUserDetailDto(
                userId = "42",
                nickname = "Fixture",
                avatarUrl = "http://example.test/{size}/avatar",
                backgroundUrl = "https://example.test/{size}/background",
                signature = "Hello",
                fans = 8,
                follows = 4,
                listenMinutes = 120,
            )
    }
}
