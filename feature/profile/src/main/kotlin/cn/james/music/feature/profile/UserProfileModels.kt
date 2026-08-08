package cn.james.music.feature.profile

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState

    data object Error : UserProfileUiState

    data object Offline : UserProfileUiState

    data class Content(
        val value: UserProfileUi,
    ) : UserProfileUiState
}

@Immutable
internal data class UserProfileUi(
    val id: String,
    val nickname: String,
    val signature: String,
    val vipLabel: String?,
    val levelLabel: String,
    val followingCount: String,
    val followerCount: String,
    val friendCount: String,
    val listeningDurationValue: String,
    val musicAgeValue: String,
    @param:DrawableRes val avatarRes: Int,
    val playlists: List<UserPlaylistUi>,
)

@Immutable
internal data class UserPlaylistUi(
    val id: String,
    val title: String,
    val songCountLabel: String,
    @param:DrawableRes val artworkRes: Int,
)

internal val userProfileDesignPreview =
    UserProfileUi(
        id = "design-preview-user",
        nickname = "阿珏",
        signature = "欢迎回来，今天也要听好音乐",
        vipLabel = "VIP",
        levelLabel = "Lv.8",
        followingCount = "28",
        followerCount = "126",
        friendCount = "12",
        listeningDurationValue = "328",
        musicAgeValue = "5",
        avatarRes = R.drawable.profile_avatar,
        playlists =
            listOf(
                UserPlaylistUi("liked", "我喜欢", "126 首", R.drawable.profile_playlist_liked),
                UserPlaylistUi("acg", "ACG 收藏", "82 首", R.drawable.profile_playlist_acg),
                UserPlaylistUi("night", "夜间电台", "45 首", R.drawable.profile_playlist_night),
            ),
    )
