package cn.james.music.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.auth.AuthAccountOption
import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.AuthRepository
import cn.james.music.core.model.auth.MobileCodeLoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal enum class LoginValidationError {
    InvalidPhone,
    InvalidCode,
    AccountRequired,
}

internal sealed interface LoginNotice {
    data object CodeSent : LoginNotice

    data class Validation(
        val error: LoginValidationError,
    ) : LoginNotice

    data class Failure(
        val error: AuthError,
    ) : LoginNotice
}

internal sealed interface LoginEffect {
    data object Completed : LoginEffect
}

internal data class LoginUiState(
    val phone: String = "",
    val code: String = "",
    val sendingCode: Boolean = false,
    val countdownSeconds: Int = 0,
    val loggingIn: Boolean = false,
    val accounts: List<AuthAccountOption> = emptyList(),
    val selectedUserId: String? = null,
    val notice: LoginNotice? = null,
) {
    val hasMultipleAccounts: Boolean get() = accounts.isNotEmpty()
    val canSendCode: Boolean get() = PHONE_PATTERN.matches(phone) && countdownSeconds == 0 && !sendingCode
    val canSubmit: Boolean
        get() =
            if (hasMultipleAccounts) {
                selectedUserId != null && !loggingIn
            } else {
                PHONE_PATTERN.matches(phone) && code.length >= MIN_CODE_LENGTH && !loggingIn
            }

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

        fun submit() {
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
            if (current.loggingIn) return
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

        private companion object {
            const val MAX_PHONE_LENGTH = 11
            const val MAX_CODE_LENGTH = 6
            const val COUNTDOWN_SECONDS = 60
        }
    }
