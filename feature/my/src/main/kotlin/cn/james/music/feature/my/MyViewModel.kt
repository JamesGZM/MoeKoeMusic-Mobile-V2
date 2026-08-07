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

internal data class MyLibraryUi(
    val likedCount: String? = null,
    val recentCount: String? = null,
    val localCount: String? = null,
    val cloudSize: String? = null,
    val savedPlaylistCount: String? = null,
    val savedAlbumCount: String? = null,
    val followedArtistCount: String? = null,
    val followedFriendCount: String? = null,
    val playlists: List<MyPlaylistUi> = emptyList(),
)

internal data class MyPlaylistUi(
    val title: String,
    val supportingText: String,
    val artwork: MyPlaylistArtwork,
)

internal enum class MyPlaylistArtwork {
    Liked,
    Acg,
    NightRadio,
}

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
    val library: MyLibraryUi = MyLibraryUi(),
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
                            refreshError = null,
                            logoutError = null,
                        )
                    val result = profileRepository.load()
                    if (generation != refreshGeneration) return@launch
                    when (result) {
                        UserProfileResult.Anonymous -> {
                            mutableState.value = MyUiState(account = MyAccountUiState.Anonymous)
                        }

                        is UserProfileResult.Failure -> {
                            mutableState.value =
                                if (hasStableContent) {
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
            refreshGeneration += 1
            refreshJob?.cancel()
            mutableState.value =
                mutableState.value.copy(
                    refreshing = false,
                    showLogoutConfirmation = false,
                    loggingOut = true,
                    logoutError = null,
                )
            viewModelScope.launch {
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
