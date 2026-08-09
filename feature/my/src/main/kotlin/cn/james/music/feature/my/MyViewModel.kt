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
        private var refreshGeneration = 0L

        fun refresh() {
            if (mutableState.value.loggingOut) return
            refreshJob?.cancel()
            val generation = ++refreshGeneration
            refreshJob =
                viewModelScope.launch {
                    val previousAccount = mutableState.value.account
                    val hasStableContent =
                        previousAccount is MyAccountUiState.Authenticated || previousAccount is MyAccountUiState.Anonymous
                    mutableState.value =
                        mutableState.value.copy(
                            account = if (hasStableContent) previousAccount else MyAccountUiState.Loading,
                            refreshing = previousAccount is MyAccountUiState.Authenticated,
                            refreshProblem = null,
                            logoutProblem = null,
                        )
                    val result = profileRepository.load()
                    if (generation != refreshGeneration) return@launch
                    when (result) {
                        UserProfileResult.Anonymous -> {
                            mutableState.value =
                                MyUiState(
                                    account = MyAccountUiState.Anonymous,
                                    library = mutableState.value.library.withAccountAssetsAvailable(true),
                                )
                        }

                        is UserProfileResult.Failure -> {
                            mutableState.value =
                                if (hasStableContent) {
                                    mutableState.value.copy(
                                        account = previousAccount,
                                        refreshing = false,
                                        refreshProblem = result.error.toUi(),
                                    )
                                } else {
                                    mutableState.value.copy(
                                        account = MyAccountUiState.Failure(result.error.toUi()),
                                        refreshing = false,
                                    )
                                }
                        }

                        is UserProfileResult.Success -> {
                            mutableState.value =
                                mutableState.value.copy(
                                    account = MyAccountUiState.Authenticated(result.profile.toUi()),
                                    library = mutableState.value.library.withAccountAssetsAvailable(false),
                                    refreshing = false,
                                )
                        }
                    }
                }
        }

        fun requestLogout() {
            if (mutableState.value.account !is MyAccountUiState.Authenticated || mutableState.value.loggingOut) return
            mutableState.value = mutableState.value.copy(showLogoutConfirmation = true, logoutProblem = null)
        }

        fun dismissLogout() {
            if (mutableState.value.loggingOut) return
            mutableState.value = mutableState.value.copy(showLogoutConfirmation = false)
        }

        fun confirmLogout() {
            if (mutableState.value.account !is MyAccountUiState.Authenticated || mutableState.value.loggingOut) return
            refreshGeneration += 1
            refreshJob?.cancel()
            mutableState.value =
                mutableState.value.copy(
                    refreshing = false,
                    showLogoutConfirmation = false,
                    loggingOut = true,
                    logoutProblem = null,
                )
            viewModelScope.launch {
                when (val result = authRepository.logout()) {
                    AuthActionResult.Success -> {
                        mutableState.value =
                            MyUiState(
                                account = MyAccountUiState.Anonymous,
                                library = mutableState.value.library.withAccountAssetsAvailable(true),
                            )
                    }

                    is AuthActionResult.Failure -> {
                        mutableState.value = mutableState.value.copy(loggingOut = false, logoutProblem = result.error.toLogoutUi())
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

        private fun UserProfileError.toUi(): MyProfileProblemUi =
            when (this) {
                UserProfileError.SessionInitialization -> MyProfileProblemUi.SessionInitialization
                UserProfileError.Offline -> MyProfileProblemUi.Offline
                UserProfileError.Timeout -> MyProfileProblemUi.Timeout
                UserProfileError.Connection -> MyProfileProblemUi.Connection
                UserProfileError.ServiceUnavailable -> MyProfileProblemUi.ServiceUnavailable
                UserProfileError.VerificationRequired -> MyProfileProblemUi.VerificationRequired
                UserProfileError.Rejected -> MyProfileProblemUi.Rejected
                UserProfileError.Protocol -> MyProfileProblemUi.Protocol
            }

        private fun AuthError.toLogoutUi(): MyLogoutProblemUi =
            when (this) {
                AuthError.Storage,
                AuthError.SessionInitialization,
                -> MyLogoutProblemUi.Storage

                AuthError.Offline,
                AuthError.Timeout,
                AuthError.Connection,
                AuthError.ServiceUnavailable,
                AuthError.Rejected,
                AuthError.Protocol,
                -> MyLogoutProblemUi.Retry
            }
    }
