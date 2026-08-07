package cn.james.music.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.AuthRepository
import cn.james.music.core.model.auth.AuthRiskChallenge
import cn.james.music.core.model.auth.AuthRiskMethod
import cn.james.music.core.model.auth.AuthRiskMethodResult
import cn.james.music.core.model.auth.AuthRiskProof
import cn.james.music.core.model.auth.MobileCodeLoginResult
import cn.james.music.core.model.auth.PasswordLoginResult
import cn.james.music.core.model.auth.QrLoginCheckResult
import cn.james.music.core.model.auth.QrLoginSession
import cn.james.music.core.model.auth.QrLoginStartResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class LoginMode {
    MobileCode,
    Password,
    QrCode,
}

internal sealed interface QrLoginUiState {
    data object Generating : QrLoginUiState

    data class Waiting(
        val session: QrLoginSession,
        val remainingSeconds: Int,
    ) : QrLoginUiState {
        override fun toString(): String = "QrLoginUiState.Waiting(session=<redacted>, remainingSeconds=$remainingSeconds)"
    }

    data class Scanned(
        val session: QrLoginSession,
        val nickname: String?,
        val remainingSeconds: Int,
    ) : QrLoginUiState {
        override fun toString(): String =
            "QrLoginUiState.Scanned(session=<redacted>, nicknamePresent=${!nickname.isNullOrBlank()}, " +
                "remainingSeconds=$remainingSeconds)"
    }

    data class Expired(
        val session: QrLoginSession,
    ) : QrLoginUiState {
        override fun toString(): String = "QrLoginUiState.Expired(session=<redacted>)"
    }

    data class Failure(
        val error: AuthError,
    ) : QrLoginUiState
}

internal enum class LoginValidationError {
    InvalidPhone,
    InvalidCode,
    AccountRequired,
    UsernameRequired,
    PasswordRequired,
    RiskCodeRequired,
}

internal sealed interface LoginNotice {
    data object CodeSent : LoginNotice

    data class Validation(
        val error: LoginValidationError,
    ) : LoginNotice

    data class Failure(
        val error: AuthError,
    ) : LoginNotice

    data object PasswordRejected : LoginNotice

    data object PasswordMultipleAccounts : LoginNotice

    data object RiskRejected : LoginNotice

    data object RiskRepeated : LoginNotice

    data class UnsupportedRisk(
        val type: Int,
    ) : LoginNotice
}

internal sealed interface PasswordRiskUiState {
    val challenge: AuthRiskChallenge

    data class Required(
        override val challenge: AuthRiskChallenge,
    ) : PasswordRiskUiState

    data class Sms(
        override val challenge: AuthRiskChallenge,
    ) : PasswordRiskUiState

    data class Tencent(
        override val challenge: AuthRiskChallenge,
        val appId: String,
    ) : PasswordRiskUiState
}

internal sealed interface LoginEffect {
    data object Completed : LoginEffect

    data class LaunchTencentCaptcha(
        val appId: String,
    ) : LoginEffect
}

internal data class LoginUiState(
    val mode: LoginMode = LoginMode.MobileCode,
    val phone: String = "",
    val code: String = "",
    val sendingCode: Boolean = false,
    val countdownSeconds: Int = 0,
    val loggingIn: Boolean = false,
    val accounts: List<AuthAccountOption> = emptyList(),
    val selectedUserId: String? = null,
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val passwordLoggingIn: Boolean = false,
    val risk: PasswordRiskUiState? = null,
    val riskCode: String = "",
    val resolvingRisk: Boolean = false,
    val verifyingRisk: Boolean = false,
    val riskRetryAttempted: Boolean = false,
    val qrLogin: QrLoginUiState? = null,
    val notice: LoginNotice? = null,
) {
    val hasMultipleAccounts: Boolean get() = mode == LoginMode.MobileCode && accounts.isNotEmpty()
    val hasActiveRisk: Boolean get() = risk != null
    val isBusy: Boolean get() = sendingCode || loggingIn || passwordLoggingIn || resolvingRisk || verifyingRisk
    val canSendCode: Boolean get() = PHONE_PATTERN.matches(phone) && countdownSeconds == 0 && !isBusy
    val canSubmitMobileCode: Boolean
        get() =
            if (hasMultipleAccounts) {
                selectedUserId != null && !isBusy
            } else {
                PHONE_PATTERN.matches(phone) && code.length >= MIN_CODE_LENGTH && !isBusy
            }
    val canSubmitPassword: Boolean get() = username.isNotBlank() && password.isNotEmpty() && !isBusy
    val canSubmitRiskCode: Boolean get() = risk is PasswordRiskUiState.Sms && riskCode.length >= MIN_CODE_LENGTH && !isBusy

    override fun toString(): String =
        "LoginUiState(mode=$mode, phonePresent=${phone.isNotEmpty()}, codePresent=${code.isNotEmpty()}, " +
            "usernamePresent=${username.isNotEmpty()}, passwordPresent=${password.isNotEmpty()}, " +
            "sendingCode=$sendingCode, loggingIn=$loggingIn, passwordLoggingIn=$passwordLoggingIn, " +
            "risk=${risk?.javaClass?.simpleName}, resolvingRisk=$resolvingRisk, verifyingRisk=$verifyingRisk, " +
            "riskRetryAttempted=$riskRetryAttempted, qrLogin=${qrLogin?.javaClass?.simpleName}, notice=$notice)"

    companion object {
        val PHONE_PATTERN = Regex("^1\\d{10}$")
        const val MIN_CODE_LENGTH = 4
    }
}

@HiltViewModel
internal class LoginViewModel
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(LoginUiState())
        val state: StateFlow<LoginUiState> = mutableState.asStateFlow()
        private val mutableEffects = MutableSharedFlow<LoginEffect>(extraBufferCapacity = 1)
        val effects: SharedFlow<LoginEffect> = mutableEffects.asSharedFlow()
        private var countdownJob: Job? = null
        private var qrLoginJob: Job? = null
        private val noticesByMode = mutableMapOf<LoginMode, LoginNotice?>()

        fun switchMode(mode: LoginMode) {
            val current = mutableState.value
            if (mode == current.mode || current.isBusy || current.hasActiveRisk) return
            noticesByMode[current.mode] = current.notice
            mutableState.value = current.copy(mode = mode, notice = noticesByMode[mode])
            if (mode == LoginMode.QrCode && current.qrLogin == null) startQrLogin()
        }

        fun refreshQrLogin() {
            if (mutableState.value.mode != LoginMode.QrCode) return
            startQrLogin()
        }

        fun updatePhone(value: String) {
            if (mutableState.value.hasMultipleAccounts) return
            mutableState.value =
                mutableState.value.copy(
                    phone = value.filter(Char::isDigit).take(MAX_PHONE_LENGTH),
                    notice = null,
                )
        }

        fun updateCode(value: String) {
            if (mutableState.value.hasMultipleAccounts) return
            mutableState.value =
                mutableState.value.copy(
                    code = value.filter(Char::isDigit).take(MAX_CODE_LENGTH),
                    notice = null,
                )
        }

        fun updateUsername(value: String) {
            if (mutableState.value.hasActiveRisk) return
            mutableState.value = mutableState.value.copy(username = value.take(MAX_USERNAME_LENGTH), notice = null)
        }

        fun updatePassword(value: String) {
            if (mutableState.value.hasActiveRisk) return
            mutableState.value = mutableState.value.copy(password = value.take(MAX_PASSWORD_LENGTH), notice = null)
        }

        fun togglePasswordVisibility() {
            val current = mutableState.value
            if (current.isBusy || current.hasActiveRisk) return
            mutableState.value = current.copy(passwordVisible = !current.passwordVisible)
        }

        fun updateRiskCode(value: String) {
            if (mutableState.value.risk !is PasswordRiskUiState.Sms) return
            mutableState.value =
                mutableState.value.copy(
                    riskCode = value.filter(Char::isDigit).take(MAX_RISK_CODE_LENGTH),
                    notice = null,
                )
        }

        fun sendCode() {
            val current = mutableState.value
            if (!LoginUiState.PHONE_PATTERN.matches(current.phone)) {
                mutableState.value = current.copy(notice = LoginNotice.Validation(LoginValidationError.InvalidPhone))
                return
            }
            if (current.sendingCode || current.countdownSeconds > 0) return
            viewModelScope.launch {
                mutableState.value = mutableState.value.copy(sendingCode = true, notice = null)
                when (val result = repository.sendMobileCode(current.phone)) {
                    is AuthActionResult.Failure -> {
                        mutableState.value = mutableState.value.copy(sendingCode = false, notice = LoginNotice.Failure(result.error))
                    }

                    AuthActionResult.Success -> {
                        mutableState.value =
                            mutableState.value.copy(
                                sendingCode = false,
                                countdownSeconds = COUNTDOWN_SECONDS,
                                notice = LoginNotice.CodeSent,
                            )
                        startCountdown()
                    }
                }
            }
        }

        fun submitMobileCode() {
            val current = mutableState.value
            if (current.hasMultipleAccounts && current.selectedUserId == null) {
                mutableState.value = current.copy(notice = LoginNotice.Validation(LoginValidationError.AccountRequired))
                return
            }
            if (!LoginUiState.PHONE_PATTERN.matches(current.phone)) {
                mutableState.value = current.copy(notice = LoginNotice.Validation(LoginValidationError.InvalidPhone))
                return
            }
            if (current.code.length < LoginUiState.MIN_CODE_LENGTH) {
                mutableState.value = current.copy(notice = LoginNotice.Validation(LoginValidationError.InvalidCode))
                return
            }
            if (current.isBusy) return
            viewModelScope.launch {
                mutableState.value = mutableState.value.copy(loggingIn = true, notice = null)
                when (
                    val result =
                        repository.loginWithMobileCode(
                            mobile = current.phone,
                            code = current.code,
                            selectedUserId = current.selectedUserId,
                        )
                ) {
                    MobileCodeLoginResult.Authenticated -> {
                        mutableState.value = mutableState.value.copy(loggingIn = false, code = "")
                        mutableEffects.emit(LoginEffect.Completed)
                    }

                    is MobileCodeLoginResult.Failure -> {
                        mutableState.value = mutableState.value.copy(loggingIn = false, notice = LoginNotice.Failure(result.error))
                    }

                    is MobileCodeLoginResult.MultipleAccounts -> {
                        mutableState.value =
                            mutableState.value.copy(
                                loggingIn = false,
                                accounts = result.accounts,
                                selectedUserId = null,
                            )
                    }
                }
            }
        }

        fun submitPassword() {
            val current = mutableState.value
            if (current.username.isBlank()) {
                mutableState.value = current.copy(notice = LoginNotice.Validation(LoginValidationError.UsernameRequired))
                return
            }
            if (current.password.isEmpty()) {
                mutableState.value = current.copy(notice = LoginNotice.Validation(LoginValidationError.PasswordRequired))
                return
            }
            if (current.isBusy || current.hasActiveRisk) return
            viewModelScope.launch { performPasswordLogin(afterVerification = false) }
        }

        fun startRiskVerification() {
            val risk = mutableState.value.risk as? PasswordRiskUiState.Required ?: return
            if (mutableState.value.isBusy) return
            viewModelScope.launch {
                mutableState.value = mutableState.value.copy(resolvingRisk = true, notice = null)
                val result = repository.getRiskMethod(risk.challenge)
                if (mutableState.value.risk != risk) return@launch
                when (result) {
                    is AuthRiskMethodResult.Failure -> {
                        mutableState.value =
                            mutableState.value.copy(
                                resolvingRisk = false,
                                notice = LoginNotice.Failure(result.error),
                            )
                    }

                    is AuthRiskMethodResult.Available -> {
                        when (val method = result.method) {
                            AuthRiskMethod.Sms -> {
                                mutableState.value =
                                    mutableState.value.copy(
                                        risk = PasswordRiskUiState.Sms(risk.challenge),
                                        riskCode = "",
                                        resolvingRisk = false,
                                        notice = null,
                                    )
                            }

                            is AuthRiskMethod.Tencent -> {
                                mutableState.value =
                                    mutableState.value.copy(
                                        risk = PasswordRiskUiState.Tencent(risk.challenge, method.appId),
                                        resolvingRisk = false,
                                        notice = null,
                                    )
                                mutableEffects.emit(LoginEffect.LaunchTencentCaptcha(method.appId))
                            }

                            is AuthRiskMethod.Unsupported -> {
                                mutableState.value =
                                    mutableState.value.copy(
                                        risk = null,
                                        resolvingRisk = false,
                                        notice = LoginNotice.UnsupportedRisk(method.type),
                                    )
                            }
                        }
                    }
                }
            }
        }

        fun verifyRiskCode() {
            val current = mutableState.value
            val risk = current.risk as? PasswordRiskUiState.Sms ?: return
            if (current.riskCode.length < LoginUiState.MIN_CODE_LENGTH) {
                mutableState.value = current.copy(notice = LoginNotice.Validation(LoginValidationError.RiskCodeRequired))
                return
            }
            if (current.isBusy) return
            viewModelScope.launch {
                mutableState.value = mutableState.value.copy(verifyingRisk = true, notice = null)
                when (val result = repository.verifyRisk(risk.challenge, AuthRiskProof.Sms(current.riskCode))) {
                    is AuthActionResult.Failure -> {
                        mutableState.value =
                            mutableState.value.copy(
                                verifyingRisk = false,
                                notice =
                                    if (result.error == AuthError.Rejected) {
                                        LoginNotice.RiskRejected
                                    } else {
                                        LoginNotice.Failure(result.error)
                                    },
                            )
                    }

                    AuthActionResult.Success -> {
                        mutableState.value =
                            mutableState.value.copy(
                                risk = null,
                                riskCode = "",
                                verifyingRisk = false,
                                riskRetryAttempted = true,
                            )
                        performPasswordLogin(afterVerification = true)
                    }
                }
            }
        }

        fun retryTencentVerification() {
            val risk = mutableState.value.risk as? PasswordRiskUiState.Tencent ?: return
            if (mutableState.value.isBusy) return
            viewModelScope.launch { mutableEffects.emit(LoginEffect.LaunchTencentCaptcha(risk.appId)) }
        }

        fun handleTencentCaptchaResult(result: TencentCaptchaResult) {
            val current = mutableState.value
            val risk = current.risk as? PasswordRiskUiState.Tencent ?: return
            when (result) {
                TencentCaptchaResult.Cancelled -> {
                    cancelRisk()
                }

                TencentCaptchaResult.Failure -> {
                    mutableState.value = current.copy(notice = LoginNotice.RiskRejected)
                }

                is TencentCaptchaResult.Success -> {
                    if (current.isBusy) return
                    viewModelScope.launch {
                        mutableState.value = mutableState.value.copy(verifyingRisk = true, notice = null)
                        when (
                            val verification =
                                repository.verifyRisk(
                                    risk.challenge,
                                    AuthRiskProof.Tencent(result.ticket, result.randomString, risk.appId),
                                )
                        ) {
                            is AuthActionResult.Failure -> {
                                mutableState.value =
                                    mutableState.value.copy(
                                        verifyingRisk = false,
                                        notice =
                                            if (verification.error == AuthError.Rejected) {
                                                LoginNotice.RiskRejected
                                            } else {
                                                LoginNotice.Failure(verification.error)
                                            },
                                    )
                            }

                            AuthActionResult.Success -> {
                                mutableState.value =
                                    mutableState.value.copy(
                                        risk = null,
                                        verifyingRisk = false,
                                        riskRetryAttempted = true,
                                    )
                                performPasswordLogin(afterVerification = true)
                            }
                        }
                    }
                }
            }
        }

        fun cancelRisk() {
            val current = mutableState.value
            if (current.verifyingRisk || current.passwordLoggingIn) return
            mutableState.value =
                current.copy(
                    risk = null,
                    riskCode = "",
                    resolvingRisk = false,
                    riskRetryAttempted = false,
                    notice = null,
                )
        }

        fun selectAccount(userId: String) {
            if (mutableState.value.accounts.none { it.userId == userId }) return
            mutableState.value = mutableState.value.copy(selectedUserId = userId, notice = null)
        }

        fun chooseOtherAccount() {
            mutableState.value =
                mutableState.value.copy(
                    code = "",
                    accounts = emptyList(),
                    selectedUserId = null,
                    notice = null,
                )
        }

        private suspend fun performPasswordLogin(afterVerification: Boolean) {
            val current = mutableState.value
            mutableState.value = current.copy(passwordLoggingIn = true, notice = null)
            when (val result = repository.loginWithPassword(current.username.trim(), current.password)) {
                PasswordLoginResult.Authenticated -> {
                    mutableState.value =
                        mutableState.value.copy(
                            password = "",
                            passwordLoggingIn = false,
                            riskRetryAttempted = false,
                        )
                    mutableEffects.emit(LoginEffect.Completed)
                }

                is PasswordLoginResult.Failure -> {
                    mutableState.value =
                        mutableState.value.copy(
                            passwordLoggingIn = false,
                            notice =
                                if (result.error == AuthError.Rejected) {
                                    LoginNotice.PasswordRejected
                                } else {
                                    LoginNotice.Failure(result.error)
                                },
                        )
                }

                is PasswordLoginResult.MultipleAccounts -> {
                    mutableState.value =
                        mutableState.value.copy(
                            passwordLoggingIn = false,
                            notice = LoginNotice.PasswordMultipleAccounts,
                        )
                }

                is PasswordLoginResult.RiskChallenge -> {
                    if (afterVerification || current.riskRetryAttempted) {
                        mutableState.value =
                            mutableState.value.copy(
                                passwordLoggingIn = false,
                                risk = null,
                                riskRetryAttempted = false,
                                notice = LoginNotice.RiskRepeated,
                            )
                    } else {
                        mutableState.value =
                            mutableState.value.copy(
                                passwordLoggingIn = false,
                                risk = PasswordRiskUiState.Required(result.challenge),
                                notice = null,
                            )
                    }
                }
            }
        }

        private fun startCountdown() {
            countdownJob?.cancel()
            countdownJob =
                viewModelScope.launch {
                    while (mutableState.value.countdownSeconds > 0) {
                        delay(1_000)
                        mutableState.value =
                            mutableState.value.copy(
                                countdownSeconds = (mutableState.value.countdownSeconds - 1).coerceAtLeast(0),
                            )
                    }
                }
        }

        private fun startQrLogin() {
            qrLoginJob?.cancel()
            mutableState.value =
                mutableState.value.copy(
                    qrLogin = QrLoginUiState.Generating,
                    notice = null,
                )
            qrLoginJob =
                viewModelScope.launch {
                    when (val start = repository.createQrLogin()) {
                        is QrLoginStartResult.Failure -> {
                            mutableState.value = mutableState.value.copy(qrLogin = QrLoginUiState.Failure(start.error))
                        }

                        is QrLoginStartResult.Ready -> {
                            mutableState.value =
                                mutableState.value.copy(
                                    qrLogin = QrLoginUiState.Waiting(start.session, QR_LIFETIME_SECONDS),
                                )
                            coroutineScope {
                                val countdown = launch { tickQrCountdown(start.session) }
                                try {
                                    pollQrLogin(start.session)
                                } finally {
                                    countdown.cancel()
                                }
                            }
                        }
                    }
                }
        }

        private suspend fun tickQrCountdown(session: QrLoginSession) {
            while (mutableState.value.qrLogin.belongsTo(session)) {
                delay(QR_COUNTDOWN_INTERVAL_MS)
                val current = mutableState.value.qrLogin
                if (!current.belongsTo(session)) return
                val remainingSeconds = (current.remainingSeconds() - 1).coerceAtLeast(0)
                if (remainingSeconds == 0) {
                    mutableState.value = mutableState.value.copy(qrLogin = QrLoginUiState.Expired(session))
                    return
                }
                mutableState.value =
                    mutableState.value.copy(
                        qrLogin =
                            when (current) {
                                is QrLoginUiState.Scanned -> current.copy(remainingSeconds = remainingSeconds)
                                is QrLoginUiState.Waiting -> current.copy(remainingSeconds = remainingSeconds)
                                else -> return
                            },
                    )
            }
        }

        private suspend fun pollQrLogin(session: QrLoginSession) {
            var consecutiveFailures = 0
            while (mutableState.value.qrLogin.belongsTo(session)) {
                delay(QR_POLL_INTERVAL_MS)
                if (!mutableState.value.qrLogin.belongsTo(session)) return
                when (val result = repository.checkQrLogin(session.key)) {
                    is QrLoginCheckResult.Failure -> {
                        consecutiveFailures += 1
                        if (consecutiveFailures >= MAX_QR_CHECK_FAILURES) {
                            mutableState.value = mutableState.value.copy(qrLogin = QrLoginUiState.Failure(result.error))
                            return
                        }
                    }

                    QrLoginCheckResult.Waiting -> {
                        consecutiveFailures = 0
                        val current = mutableState.value.qrLogin
                        mutableState.value =
                            mutableState.value.copy(
                                qrLogin =
                                    if (current is QrLoginUiState.Scanned) {
                                        current
                                    } else {
                                        QrLoginUiState.Waiting(session, current.remainingSeconds())
                                    },
                            )
                    }

                    is QrLoginCheckResult.Scanned -> {
                        consecutiveFailures = 0
                        mutableState.value =
                            mutableState.value.copy(
                                qrLogin =
                                    QrLoginUiState.Scanned(
                                        session = session,
                                        nickname = result.nickname,
                                        remainingSeconds = mutableState.value.qrLogin.remainingSeconds(),
                                    ),
                            )
                    }

                    QrLoginCheckResult.Expired -> {
                        mutableState.value = mutableState.value.copy(qrLogin = QrLoginUiState.Expired(session))
                        return
                    }

                    QrLoginCheckResult.Authenticated -> {
                        mutableEffects.emit(LoginEffect.Completed)
                        return
                    }
                }
            }
        }

        private companion object {
            const val MAX_PHONE_LENGTH = 11
            const val MAX_CODE_LENGTH = 6
            const val MAX_RISK_CODE_LENGTH = 8
            const val MAX_USERNAME_LENGTH = 128
            const val MAX_PASSWORD_LENGTH = 128
            const val COUNTDOWN_SECONDS = 60
        }
    }

private fun QrLoginUiState?.remainingSeconds(): Int =
    when (this) {
        is QrLoginUiState.Waiting -> remainingSeconds
        is QrLoginUiState.Scanned -> remainingSeconds
        else -> QR_LIFETIME_SECONDS
    }

private fun QrLoginUiState?.belongsTo(session: QrLoginSession): Boolean =
    when (this) {
        is QrLoginUiState.Waiting -> this.session.key == session.key
        is QrLoginUiState.Scanned -> this.session.key == session.key
        else -> false
    }

private const val QR_LIFETIME_SECONDS = 120
private const val QR_COUNTDOWN_INTERVAL_MS = 1_000L
private const val QR_POLL_INTERVAL_MS = 2_000L
private const val MAX_QR_CHECK_FAILURES = 3
