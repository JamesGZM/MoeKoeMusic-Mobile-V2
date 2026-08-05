package cn.james.music.feature.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.account.UserProfile
import cn.james.music.core.model.account.UserProfileError
import cn.james.music.core.model.account.UserProfileRepository
import cn.james.music.core.model.account.UserProfileResult
import cn.james.music.core.model.account.VipSummary
import cn.james.music.core.model.auth.AuthActionResult
import cn.james.music.core.model.auth.AuthError
import cn.james.music.core.model.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class MyProfileUi(
    val userId: String,
    val nickname: String?,
    val avatarUrl: String?,
    val vipLabel: String?,
    val vipUnavailable: Boolean,
)

internal sealed interface MyAccountUiState {
    data object Loading : MyAccountUiState

    data object Anonymous : MyAccountUiState

    data class Authenticated(
        val profile: MyProfileUi,
    ) : MyAccountUiState

    data class Failure(
        val error: UserProfileError,
    ) : MyAccountUiState
}

internal data class MyUiState(
    val account: MyAccountUiState = MyAccountUiState.Loading,
    val refreshing: Boolean = false,
    val refreshError: UserProfileError? = null,
    val showLogoutConfirmation: Boolean = false,
    val loggingOut: Boolean = false,
    val logoutError: AuthError? = null,
)

@HiltViewModel
internal class MyViewModel
    @Inject
    constructor(
        private val profileRepository: UserProfileRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(MyUiState())
        val state: StateFlow<MyUiState> = mutableState.asStateFlow()
        private var refreshJob: Job? = null

        fun refresh() {
            if (mutableState.value.loggingOut) return
            refreshJob?.cancel()
            refreshJob =
                viewModelScope.launch {
                    val previousAccount = mutableState.value.account
                    mutableState.value =
                        mutableState.value.copy(
                            account = if (previousAccount is MyAccountUiState.Authenticated) previousAccount else MyAccountUiState.Loading,
                            refreshing = previousAccount is MyAccountUiState.Authenticated,
                            refreshError = null,
                            logoutError = null,
                        )
                    when (val result = profileRepository.load()) {
                        UserProfileResult.Anonymous -> {
                            mutableState.value = MyUiState(account = MyAccountUiState.Anonymous)
                        }

                        is UserProfileResult.Failure -> {
                            mutableState.value =
                                if (previousAccount is MyAccountUiState.Authenticated) {
                                    mutableState.value.copy(
                                        account = previousAccount,
                                        refreshing = false,
                                        refreshError = result.error,
                                    )
                                } else {
                                    mutableState.value.copy(
                                        account = MyAccountUiState.Failure(result.error),
                                        refreshing = false,
                                    )
                                }
                        }

                        is UserProfileResult.Success -> {
                            mutableState.value =
                                mutableState.value.copy(
                                    account = MyAccountUiState.Authenticated(result.profile.toUi()),
                                    refreshing = false,
                                )
                        }
                    }
                }
        }

        fun requestLogout() {
            if (mutableState.value.account !is MyAccountUiState.Authenticated || mutableState.value.loggingOut) return
            mutableState.value = mutableState.value.copy(showLogoutConfirmation = true, logoutError = null)
        }

        fun dismissLogout() {
            if (mutableState.value.loggingOut) return
            mutableState.value = mutableState.value.copy(showLogoutConfirmation = false)
        }

        fun confirmLogout() {
            if (mutableState.value.account !is MyAccountUiState.Authenticated || mutableState.value.loggingOut) return
            refreshJob?.cancel()
            viewModelScope.launch {
                mutableState.value =
                    mutableState.value.copy(
                        showLogoutConfirmation = false,
                        loggingOut = true,
                        logoutError = null,
                    )
                when (val result = authRepository.logout()) {
                    AuthActionResult.Success -> {
                        mutableState.value = MyUiState(account = MyAccountUiState.Anonymous)
                    }

                    is AuthActionResult.Failure -> {
                        mutableState.value = mutableState.value.copy(loggingOut = false, logoutError = result.error)
                    }
                }
            }
        }

        private fun UserProfile.toUi(): MyProfileUi {
            val activeVip = vip as? VipSummary.Active
            return MyProfileUi(
                userId = userId,
                nickname = nickname,
                avatarUrl = avatarUrl,
                vipLabel = activeVip?.label,
                vipUnavailable = vip is VipSummary.Unavailable,
            )
        }
    }
