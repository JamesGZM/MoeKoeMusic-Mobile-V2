package cn.james.music.feature.my

import androidx.compose.runtime.Immutable

@Immutable
internal data class MyProfileUi(
    val userId: String,
    val nickname: String?,
    val avatarUrl: String?,
    val vipLabel: String?,
    val vipUnavailable: Boolean,
)

@Immutable
internal enum class MyEntryId {
    Liked,
    Recent,
    LocalMusic,
    Cloud,
    SavedPlaylists,
    SavedAlbums,
    FollowedArtists,
    FollowedFriends,
}

@Immutable
internal enum class MyEntryActionId {
    AccountAsset,
    LocalMusic,
}

@Immutable
internal data class MyEntryUi(
    val id: MyEntryId,
    val supportingText: String?,
    val actionId: MyEntryActionId,
    val available: Boolean,
)

@Immutable
internal data class MyLibraryUi(
    val quickEntries: List<MyEntryUi> = myQuickEntries(accountAssetsAvailable = false),
    val collectionEntries: List<MyEntryUi> = myCollectionEntries(accountAssetsAvailable = false),
    val playlists: List<MyPlaylistUi> = emptyList(),
)

@Immutable
internal data class MyPlaylistUi(
    val id: String,
    val title: String,
    val supportingText: String,
    val artwork: MyPlaylistArtwork,
)

@Immutable
internal enum class MyPlaylistArtwork {
    Liked,
    Acg,
    NightRadio,
}

@Immutable
internal enum class MyProfileProblemUi {
    SessionInitialization,
    Offline,
    Timeout,
    Connection,
    ServiceUnavailable,
    VerificationRequired,
    Rejected,
    Protocol,
}

@Immutable
internal enum class MyLogoutProblemUi {
    Storage,
    Retry,
}

@Immutable
internal sealed interface MyAccountUiState {
    data object Loading : MyAccountUiState

    data object Anonymous : MyAccountUiState

    data class Authenticated(
        val profile: MyProfileUi,
    ) : MyAccountUiState

    data class Failure(
        val problem: MyProfileProblemUi,
    ) : MyAccountUiState
}

@Immutable
internal data class MyUiState(
    val account: MyAccountUiState = MyAccountUiState.Loading,
    val library: MyLibraryUi = MyLibraryUi(),
    val refreshing: Boolean = false,
    val refreshProblem: MyProfileProblemUi? = null,
    val showLogoutConfirmation: Boolean = false,
    val loggingOut: Boolean = false,
    val logoutProblem: MyLogoutProblemUi? = null,
)

internal fun myQuickEntries(
    accountAssetsAvailable: Boolean,
    supportingText: Map<MyEntryId, String> = emptyMap(),
): List<MyEntryUi> =
    listOf(
        MyEntryUi(MyEntryId.Liked, supportingText[MyEntryId.Liked], MyEntryActionId.AccountAsset, accountAssetsAvailable),
        MyEntryUi(MyEntryId.Recent, supportingText[MyEntryId.Recent], MyEntryActionId.AccountAsset, accountAssetsAvailable),
        MyEntryUi(MyEntryId.LocalMusic, supportingText[MyEntryId.LocalMusic], MyEntryActionId.LocalMusic, true),
        MyEntryUi(MyEntryId.Cloud, supportingText[MyEntryId.Cloud], MyEntryActionId.AccountAsset, accountAssetsAvailable),
    )

internal fun myCollectionEntries(
    accountAssetsAvailable: Boolean,
    supportingText: Map<MyEntryId, String> = emptyMap(),
): List<MyEntryUi> =
    listOf(
        MyEntryUi(MyEntryId.SavedPlaylists, supportingText[MyEntryId.SavedPlaylists], MyEntryActionId.AccountAsset, accountAssetsAvailable),
        MyEntryUi(MyEntryId.SavedAlbums, supportingText[MyEntryId.SavedAlbums], MyEntryActionId.AccountAsset, accountAssetsAvailable),
        MyEntryUi(
            MyEntryId.FollowedArtists,
            supportingText[MyEntryId.FollowedArtists],
            MyEntryActionId.AccountAsset,
            accountAssetsAvailable,
        ),
        MyEntryUi(
            MyEntryId.FollowedFriends,
            supportingText[MyEntryId.FollowedFriends],
            MyEntryActionId.AccountAsset,
            accountAssetsAvailable,
        ),
    )

internal fun MyLibraryUi.withAccountAssetsAvailable(available: Boolean): MyLibraryUi =
    copy(
        quickEntries = quickEntries.map { if (it.actionId == MyEntryActionId.AccountAsset) it.copy(available = available) else it },
        collectionEntries = collectionEntries.map { it.copy(available = available) },
    )

internal fun MyLibraryUi.actionFor(id: MyEntryId): MyEntryActionId? =
    quickEntries.firstOrNull { it.id == id && it.available }?.actionId
        ?: collectionEntries.firstOrNull { it.id == id && it.available }?.actionId
