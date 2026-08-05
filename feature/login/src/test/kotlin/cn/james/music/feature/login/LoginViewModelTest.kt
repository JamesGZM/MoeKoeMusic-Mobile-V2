package cn.james.music.feature.login

import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthRepository
import cn.james.music.core.model.auth.AuthRiskChallenge
import cn.james.music.core.model.auth.AuthRiskMethodResult
import cn.james.music.core.model.auth.AuthRiskProof
import cn.james.music.core.model.auth.AuthState
import cn.james.music.core.model.auth.MobileCodeLoginResult
import cn.james.music.core.model.auth.PasswordLoginResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
        viewModel = LoginViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invalidPhoneDoesNotSendCode() =
        runTest(dispatcher) {
            viewModel.updatePhone("123")

            viewModel.sendCode()

            assertEquals(LoginNotice.Validation(LoginValidationError.InvalidPhone), viewModel.state.value.notice)
            assertTrue(repository.sentMobiles.isEmpty())
        }

    @Test
    fun successfulSendStartsCountdown() =
        runTest(dispatcher) {
            viewModel.updatePhone(VALID_PHONE)

            viewModel.sendCode()
            runCurrent()

            assertEquals(listOf(VALID_PHONE), repository.sentMobiles)
            assertEquals(60, viewModel.state.value.countdownSeconds)
            assertFalse(viewModel.state.value.canSendCode)

            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(59, viewModel.state.value.countdownSeconds)
        }

    @Test
    fun multiAccountRequiresSelectionAndRetriesWithSelectedId() =
        runTest(dispatcher) {
            repository.loginResults.add(MobileCodeLoginResult.MultipleAccounts(ACCOUNTS))
            repository.loginResults.add(MobileCodeLoginResult.Authenticated)
            viewModel.updatePhone(VALID_PHONE)
            viewModel.updateCode("123456")

            viewModel.submit()
            runCurrent()

            assertEquals(ACCOUNTS, viewModel.state.value.accounts)
            assertFalse(viewModel.state.value.canSubmit)

            viewModel.submit()
            assertEquals(LoginNotice.Validation(LoginValidationError.AccountRequired), viewModel.state.value.notice)
            assertEquals(1, repository.loginCalls.size)

            viewModel.selectAccount("20002")
            assertTrue(viewModel.state.value.canSubmit)
            viewModel.submit()
            runCurrent()

            assertEquals("20002", repository.loginCalls.last().selectedUserId)
        }

    @Test
    fun authenticatedResultEmitsCompletedEffect() =
        runTest(dispatcher) {
            repository.loginResults.add(MobileCodeLoginResult.Authenticated)
            viewModel.updatePhone(VALID_PHONE)
            viewModel.updateCode("123456")
            val effect = backgroundScope.async { viewModel.effects.first() }
            runCurrent()

            viewModel.submit()
            runCurrent()

            assertEquals(LoginEffect.Completed, effect.await())
        }

    @Test
    fun choosingAnotherAccountClearsThePreviousVerificationCode() =
        runTest(dispatcher) {
            repository.loginResults.add(MobileCodeLoginResult.MultipleAccounts(ACCOUNTS))
            viewModel.updatePhone(VALID_PHONE)
            viewModel.updateCode("123456")
            viewModel.submit()
            runCurrent()

            viewModel.chooseOtherAccount()

            assertTrue(
                viewModel.state.value.accounts
                    .isEmpty(),
            )
            assertEquals("", viewModel.state.value.code)
            assertFalse(viewModel.state.value.canSubmit)
        }

    private class FakeAuthRepository : AuthRepository {
        val sentMobiles = mutableListOf<String>()
        val loginCalls = mutableListOf<LoginCall>()
        val loginResults = ArrayDeque<MobileCodeLoginResult>()

        override suspend fun currentState(): AuthState = AuthState.Anonymous

        override suspend fun sendMobileCode(mobile: String): AuthActionResult {
            sentMobiles += mobile
            return AuthActionResult.Success
        }

        override suspend fun loginWithMobileCode(
            mobile: String,
            code: String,
            selectedUserId: String?,
        ): MobileCodeLoginResult {
            loginCalls += LoginCall(mobile, code, selectedUserId)
            return loginResults.removeFirst()
        }

        override suspend fun loginWithPassword(
            username: String,
            password: String,
        ): PasswordLoginResult = PasswordLoginResult.Failure(cn.james.music.core.model.auth.AuthError.Rejected)

        override suspend fun getRiskMethod(challenge: AuthRiskChallenge): AuthRiskMethodResult =
            AuthRiskMethodResult.Failure(cn.james.music.core.model.auth.AuthError.Rejected)

        override suspend fun verifyRisk(
            challenge: AuthRiskChallenge,
            proof: AuthRiskProof,
        ): AuthActionResult = AuthActionResult.Failure(cn.james.music.core.model.auth.AuthError.Rejected)

        override suspend fun logout(): AuthActionResult = AuthActionResult.Success
    }

    private data class LoginCall(
        val mobile: String,
        val code: String,
        val selectedUserId: String?,
    )

    private companion object {
        const val VALID_PHONE = "13800138000"
        val ACCOUNTS =
            listOf(
                AuthAccountOption("10001", "Moe", null, null),
                AuthAccountOption("20002", "Koe", null, null),
            )
    }
}
