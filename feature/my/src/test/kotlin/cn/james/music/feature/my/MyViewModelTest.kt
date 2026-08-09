package cn.james.music.feature.my

import cn.james.music.core.model.account.UserProfile
import cn.james.music.core.model.account.UserProfileError
import cn.james.music.core.model.account.UserProfileRepository
import cn.james.music.core.model.account.UserProfileResult
import cn.james.music.core.model.account.VipSummary
import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.AuthRepository
import cn.james.music.core.model.auth.AuthRiskChallenge
import cn.james.music.core.model.auth.AuthRiskMethodResult
import cn.james.music.core.model.auth.AuthRiskProof
import cn.james.music.core.model.auth.AuthState
import cn.james.music.core.model.auth.MobileCodeLoginResult
import cn.james.music.core.model.auth.PasswordLoginResult
import cn.james.music.core.model.auth.QrLoginCheckResult
import cn.james.music.core.model.auth.QrLoginStartResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: MyViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        profileRepository = FakeProfileRepository()
        authRepository = FakeAuthRepository()
        viewModel = MyViewModel(profileRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successfulRefreshMapsStableProfileState() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Success(FIXTURE_PROFILE))

            viewModel.refresh()
            runCurrent()

            val account = viewModel.state.value.account as MyAccountUiState.Authenticated
            assertEquals("Fixture", account.profile.nickname)
            assertEquals("SVIP", account.profile.vipLabel)
            assertFalse(account.profile.vipUnavailable)
        }

    @Test
    fun refreshFailurePreservesExistingProfile() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Success(FIXTURE_PROFILE))
            profileRepository.results.add(UserProfileResult.Failure(UserProfileError.Timeout))
            viewModel.refresh()
            runCurrent()

            viewModel.refresh()
            runCurrent()

            assertTrue(viewModel.state.value.account is MyAccountUiState.Authenticated)
            assertEquals(MyProfileProblemUi.Timeout, viewModel.state.value.refreshProblem)
        }

    @Test
    fun anonymousResultShowsLoginState() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Anonymous)

            viewModel.refresh()
            runCurrent()

            assertEquals(MyAccountUiState.Anonymous, viewModel.state.value.account)
        }

    @Test
    fun returningToKnownAnonymousStateDoesNotFlashLoading() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Anonymous)
            viewModel.refresh()
            runCurrent()
            profileRepository.deferredResult = CompletableDeferred()

            viewModel.refresh()
            runCurrent()

            assertEquals(MyAccountUiState.Anonymous, viewModel.state.value.account)
            profileRepository.deferredResult?.complete(UserProfileResult.Anonymous)
            runCurrent()
        }

    @Test
    fun logoutRequiresConfirmationAndSwitchesToAnonymousAfterCommit() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Success(FIXTURE_PROFILE))
            viewModel.refresh()
            runCurrent()

            viewModel.requestLogout()
            assertTrue(viewModel.state.value.showLogoutConfirmation)
            assertEquals(0, authRepository.logoutCalls)

            viewModel.confirmLogout()
            runCurrent()

            assertEquals(1, authRepository.logoutCalls)
            assertEquals(MyAccountUiState.Anonymous, viewModel.state.value.account)
        }

    @Test
    fun logoutFailureKeepsAuthenticatedState() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Success(FIXTURE_PROFILE))
            authRepository.logoutResult = AuthActionResult.Failure(AuthError.Storage)
            viewModel.refresh()
            runCurrent()

            viewModel.requestLogout()
            viewModel.confirmLogout()
            runCurrent()

            assertTrue(viewModel.state.value.account is MyAccountUiState.Authenticated)
            assertEquals(MyLogoutProblemUi.Storage, viewModel.state.value.logoutProblem)
            assertFalse(viewModel.state.value.loggingOut)
        }

    @Test
    fun logoutFailureStopsInFlightRefreshAndKeepsAuthenticatedState() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Success(FIXTURE_PROFILE))
            viewModel.refresh()
            runCurrent()
            profileRepository.deferredResult = CompletableDeferred()
            authRepository.logoutResult = AuthActionResult.Failure(AuthError.Storage)

            viewModel.refresh()
            runCurrent()
            assertTrue(viewModel.state.value.refreshing)

            viewModel.requestLogout()
            viewModel.confirmLogout()

            assertFalse(viewModel.state.value.refreshing)
            assertTrue(viewModel.state.value.loggingOut)
            runCurrent()
            assertTrue(viewModel.state.value.account is MyAccountUiState.Authenticated)
            assertEquals(MyLogoutProblemUi.Storage, viewModel.state.value.logoutProblem)
            assertFalse(viewModel.state.value.refreshing)
        }

    @Test
    fun cancelledRefreshCannotReplaceAnonymousStateAfterLogout() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Success(FIXTURE_PROFILE))
            viewModel.refresh()
            runCurrent()
            profileRepository.deferredResult = CompletableDeferred()
            profileRepository.ignoreDeferredCancellation = true

            viewModel.refresh()
            runCurrent()
            viewModel.requestLogout()
            viewModel.confirmLogout()
            runCurrent()
            assertEquals(MyAccountUiState.Anonymous, viewModel.state.value.account)

            profileRepository.deferredResult?.complete(
                UserProfileResult.Success(FIXTURE_PROFILE.copy(nickname = "Stale")),
            )
            runCurrent()

            assertEquals(MyAccountUiState.Anonymous, viewModel.state.value.account)
        }

    @Test
    fun repeatedLogoutConfirmationStartsSingleLogout() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Success(FIXTURE_PROFILE))
            viewModel.refresh()
            runCurrent()

            viewModel.requestLogout()
            viewModel.confirmLogout()
            viewModel.confirmLogout()
            runCurrent()

            assertEquals(1, authRepository.logoutCalls)
        }

    @Test
    fun dismissLogoutHidesConfirmationWithoutCallingLogout() =
        runTest(dispatcher) {
            profileRepository.results.add(UserProfileResult.Success(FIXTURE_PROFILE))
            viewModel.refresh()
            runCurrent()

            viewModel.requestLogout()
            viewModel.dismissLogout()

            assertFalse(viewModel.state.value.showLogoutConfirmation)
            assertEquals(0, authRepository.logoutCalls)
        }

    private class FakeProfileRepository : UserProfileRepository {
        val results = ArrayDeque<UserProfileResult>()
        var deferredResult: CompletableDeferred<UserProfileResult>? = null
        var ignoreDeferredCancellation = false

        override suspend fun load(): UserProfileResult =
            deferredResult?.let { deferred ->
                if (ignoreDeferredCancellation) {
                    withContext(NonCancellable) { deferred.await() }
                } else {
                    deferred.await()
                }
            } ?: results.removeFirst()
    }

    private class FakeAuthRepository : AuthRepository {
        var logoutCalls = 0
        var logoutResult: AuthActionResult = AuthActionResult.Success

        override suspend fun currentState(): AuthState = AuthState.Anonymous

        override suspend fun sendMobileCode(mobile: String): AuthActionResult = AuthActionResult.Success

        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): MobileCodeLoginResult = MobileCodeLoginResult.Failure(AuthError.Protocol)

        override suspend fun loginWithPassword(
            username: String,
            password: String,
        ): PasswordLoginResult = PasswordLoginResult.Failure(AuthError.Protocol)

        override suspend fun createQrLogin(): QrLoginStartResult = QrLoginStartResult.Failure(AuthError.Protocol)

        override suspend fun checkQrLogin(key: String): QrLoginCheckResult = QrLoginCheckResult.Failure(AuthError.Protocol)

        override suspend fun getRiskMethod(challenge: AuthRiskChallenge): AuthRiskMethodResult =
            AuthRiskMethodResult.Failure(AuthError.Protocol)

        override suspend fun verifyRisk(
            challenge: AuthRiskChallenge,
            proof: AuthRiskProof,
        ): AuthActionResult = AuthActionResult.Failure(AuthError.Protocol)

        override suspend fun logout(): AuthActionResult {
            logoutCalls += 1
            return logoutResult
        }
    }

    private companion object {
        val FIXTURE_PROFILE =
            UserProfile(
                userId = "42",
                nickname = "Fixture",
                avatarUrl = null,
                backgroundUrl = null,
                signature = "Hello",
                fans = 8,
                follows = 4,
                listenMinutes = 120,
                vip = VipSummary.Active("SVIP"),
            )
    }
}
