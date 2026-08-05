package cn.james.music.feature.login

import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.AuthRepository
import cn.james.music.core.model.auth.AuthRiskChallenge
import cn.james.music.core.model.auth.AuthRiskMethod
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

            viewModel.submitMobileCode()
            runCurrent()

            assertEquals(ACCOUNTS, viewModel.state.value.accounts)
            assertFalse(viewModel.state.value.canSubmitMobileCode)

            viewModel.submitMobileCode()
            assertEquals(LoginNotice.Validation(LoginValidationError.AccountRequired), viewModel.state.value.notice)
            assertEquals(1, repository.loginCalls.size)

            viewModel.selectAccount("20002")
            assertTrue(viewModel.state.value.canSubmitMobileCode)
            viewModel.submitMobileCode()
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

            viewModel.submitMobileCode()
            runCurrent()

            assertEquals(LoginEffect.Completed, effect.await())
        }

    @Test
    fun choosingAnotherAccountClearsThePreviousVerificationCode() =
        runTest(dispatcher) {
            repository.loginResults.add(MobileCodeLoginResult.MultipleAccounts(ACCOUNTS))
            viewModel.updatePhone(VALID_PHONE)
            viewModel.updateCode("123456")
            viewModel.submitMobileCode()
            runCurrent()

            viewModel.chooseOtherAccount()

            assertTrue(
                viewModel.state.value.accounts
                    .isEmpty(),
            )
            assertEquals("", viewModel.state.value.code)
            assertFalse(viewModel.state.value.canSubmitMobileCode)
        }

    @Test
    fun switchingModesClearsInputsFromThePreviousMode() =
        runTest(dispatcher) {
            viewModel.updatePhone(VALID_PHONE)
            viewModel.updateCode("123456")

            viewModel.switchMode(LoginMode.Password)

            assertEquals("", viewModel.state.value.phone)
            assertEquals("", viewModel.state.value.code)
            viewModel.updateUsername("fixture-account")
            viewModel.updatePassword("fixture-password")

            viewModel.switchMode(LoginMode.MobileCode)

            assertEquals("", viewModel.state.value.username)
            assertEquals("", viewModel.state.value.password)
        }

    @Test
    fun passwordAuthenticationEmitsCompletionAndRedactsStateString() =
        runTest(dispatcher) {
            repository.passwordResults.add(PasswordLoginResult.Authenticated)
            enterPasswordCredentials()
            val effect = backgroundScope.async { viewModel.effects.first() }
            runCurrent()

            viewModel.submitPassword()
            runCurrent()

            assertEquals(LoginEffect.Completed, effect.await())
            assertEquals(listOf(PasswordCall("fixture-account", "fixture-password")), repository.passwordCalls)
            assertFalse(
                viewModel.state.value
                    .toString()
                    .contains("fixture-password"),
            )
            assertEquals("", viewModel.state.value.password)
        }

    @Test
    fun rejectedPasswordUsesCredentialSpecificNotice() =
        runTest(dispatcher) {
            repository.passwordResults.add(PasswordLoginResult.Failure(AuthError.Rejected))
            enterPasswordCredentials()

            viewModel.submitPassword()
            runCurrent()

            assertEquals(LoginNotice.PasswordRejected, viewModel.state.value.notice)
            assertEquals("fixture-password", viewModel.state.value.password)
        }

    @Test
    fun smsRiskVerificationRetriesPasswordExactlyOnce() =
        runTest(dispatcher) {
            repository.passwordResults.add(PasswordLoginResult.RiskChallenge(CHALLENGE))
            repository.passwordResults.add(PasswordLoginResult.Authenticated)
            repository.riskMethodResults.add(AuthRiskMethodResult.Available(AuthRiskMethod.Sms))
            repository.verifyResults.add(AuthActionResult.Success)
            enterPasswordCredentials()
            val effect = backgroundScope.async { viewModel.effects.first() }
            runCurrent()

            viewModel.submitPassword()
            runCurrent()
            assertTrue(viewModel.state.value.risk is PasswordRiskUiState.Required)

            viewModel.startRiskVerification()
            runCurrent()
            assertTrue(viewModel.state.value.risk is PasswordRiskUiState.Sms)

            viewModel.updateRiskCode("246810")
            viewModel.verifyRiskCode()
            runCurrent()

            assertEquals(LoginEffect.Completed, effect.await())
            assertEquals(2, repository.passwordCalls.size)
            assertEquals("246810", (repository.riskProofs.single() as AuthRiskProof.Sms).code)
        }

    @Test
    fun repeatedRiskAfterVerificationStopsWithoutLooping() =
        runTest(dispatcher) {
            repository.passwordResults.add(PasswordLoginResult.RiskChallenge(CHALLENGE))
            repository.passwordResults.add(PasswordLoginResult.RiskChallenge(CHALLENGE))
            repository.riskMethodResults.add(AuthRiskMethodResult.Available(AuthRiskMethod.Sms))
            repository.verifyResults.add(AuthActionResult.Success)
            enterPasswordCredentials()

            viewModel.submitPassword()
            runCurrent()
            viewModel.startRiskVerification()
            runCurrent()
            viewModel.updateRiskCode("246810")
            viewModel.verifyRiskCode()
            runCurrent()

            assertEquals(2, repository.passwordCalls.size)
            assertEquals(LoginNotice.RiskRepeated, viewModel.state.value.notice)
            assertEquals(null, viewModel.state.value.risk)
        }

    @Test
    fun tencentRiskSuccessSubmitsTypedProofAndRetriesOnce() =
        runTest(dispatcher) {
            repository.passwordResults.add(PasswordLoginResult.RiskChallenge(CHALLENGE))
            repository.passwordResults.add(PasswordLoginResult.Authenticated)
            repository.riskMethodResults.add(AuthRiskMethodResult.Available(AuthRiskMethod.Tencent("123456789")))
            repository.verifyResults.add(AuthActionResult.Success)
            enterPasswordCredentials()
            val launch = backgroundScope.async { viewModel.effects.first { it is LoginEffect.LaunchTencentCaptcha } }
            runCurrent()

            viewModel.submitPassword()
            runCurrent()
            viewModel.startRiskVerification()
            runCurrent()

            assertEquals(LoginEffect.LaunchTencentCaptcha("123456789"), launch.await())
            viewModel.handleTencentCaptchaResult(TencentCaptchaResult.Success("fixture-ticket", "fixture-random"))
            runCurrent()

            val proof = repository.riskProofs.single() as AuthRiskProof.Tencent
            assertEquals("fixture-ticket", proof.ticket)
            assertEquals("fixture-random", proof.randomString)
            assertEquals("123456789", proof.appId)
            assertEquals(2, repository.passwordCalls.size)
        }

    @Test
    fun cancellingRiskReturnsToPasswordWithoutAutomaticRetry() =
        runTest(dispatcher) {
            repository.passwordResults.add(PasswordLoginResult.RiskChallenge(CHALLENGE))
            enterPasswordCredentials()
            viewModel.submitPassword()
            runCurrent()

            viewModel.cancelRisk()

            assertEquals(null, viewModel.state.value.risk)
            assertEquals(1, repository.passwordCalls.size)
            assertEquals("fixture-password", viewModel.state.value.password)
        }

    @Test
    fun cancellingWhileRiskMethodLoadsIgnoresTheStaleResult() =
        runTest(dispatcher) {
            repository.passwordResults.add(PasswordLoginResult.RiskChallenge(CHALLENGE))
            repository.riskMethodDeferred = CompletableDeferred()
            enterPasswordCredentials()
            viewModel.submitPassword()
            runCurrent()

            viewModel.startRiskVerification()
            runCurrent()
            assertTrue(viewModel.state.value.resolvingRisk)

            viewModel.cancelRisk()
            repository.riskMethodDeferred?.complete(AuthRiskMethodResult.Available(AuthRiskMethod.Sms))
            runCurrent()

            assertEquals(null, viewModel.state.value.risk)
            assertFalse(viewModel.state.value.resolvingRisk)
        }

    private fun enterPasswordCredentials() {
        viewModel.switchMode(LoginMode.Password)
        viewModel.updateUsername("fixture-account")
        viewModel.updatePassword("fixture-password")
    }

    private class FakeAuthRepository : AuthRepository {
        val sentMobiles = mutableListOf<String>()
        val loginCalls = mutableListOf<LoginCall>()
        val loginResults = ArrayDeque<MobileCodeLoginResult>()
        val passwordCalls = mutableListOf<PasswordCall>()
        val passwordResults = ArrayDeque<PasswordLoginResult>()
        val riskMethodResults = ArrayDeque<AuthRiskMethodResult>()
        var riskMethodDeferred: CompletableDeferred<AuthRiskMethodResult>? = null
        val verifyResults = ArrayDeque<AuthActionResult>()
        val riskProofs = mutableListOf<AuthRiskProof>()

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
        ): PasswordLoginResult {
            passwordCalls += PasswordCall(username, password)
            return passwordResults.removeFirst()
        }

        override suspend fun createQrLogin(): QrLoginStartResult = QrLoginStartResult.Failure(AuthError.Protocol)

        override suspend fun checkQrLogin(key: String): QrLoginCheckResult = QrLoginCheckResult.Failure(AuthError.Protocol)

        override suspend fun getRiskMethod(challenge: AuthRiskChallenge): AuthRiskMethodResult =
            riskMethodDeferred?.await() ?: riskMethodResults.removeFirst()

        override suspend fun verifyRisk(
            challenge: AuthRiskChallenge,
            proof: AuthRiskProof,
        ): AuthActionResult {
            riskProofs += proof
            return verifyResults.removeFirst()
        }

        override suspend fun logout(): AuthActionResult = AuthActionResult.Success
    }

    private data class LoginCall(
        val mobile: String,
        val code: String,
        val selectedUserId: String?,
    )

    private data class PasswordCall(
        val username: String,
        val password: String,
    )

    private companion object {
        const val VALID_PHONE = "13800138000"
        val CHALLENGE = AuthRiskChallenge("fixture-event", null, null)
        val ACCOUNTS =
            listOf(
                AuthAccountOption("10001", "Moe", null, null),
                AuthAccountOption("20002", "Koe", null, null),
            )
    }
}
