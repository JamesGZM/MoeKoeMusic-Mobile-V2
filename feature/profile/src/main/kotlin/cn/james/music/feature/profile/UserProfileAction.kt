package cn.james.music.feature.profile

internal sealed interface UserProfileAction {
    data object Share : UserProfileAction

    data object More : UserProfileAction

    data object Edit : UserProfileAction

    data object Following : UserProfileAction

    data object Followers : UserProfileAction

    data object Friends : UserProfileAction

    data object ViewAllPlaylists : UserProfileAction

    data class OpenPlaylist(
        val id: String,
    ) : UserProfileAction

    data class OpenPlaylistMore(
        val id: String,
    ) : UserProfileAction

    data object Retry : UserProfileAction
}
