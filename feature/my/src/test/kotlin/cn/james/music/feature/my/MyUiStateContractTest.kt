package cn.james.music.feature.my

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyUiStateContractTest {
    @Test
    fun authenticatedAndAnonymousStatesKeepDistinctAccountRules() {
        val authenticated =
            MyAccountUiState.Authenticated(
                MyProfileUi(
                    userId = "fixture-user",
                    nickname = "Fixture",
                    avatarUrl = null,
                    vipLabel = null,
                    vipUnavailable = false,
                ),
            )

        assertNotEquals(authenticated, MyAccountUiState.Anonymous)
        assertTrue(authenticated.profile.userId.isNotBlank())
    }

    @Test
    fun libraryEntriesUseStableIdsAndTypedActions() {
        val library =
            MyLibraryUi(
                quickEntries = myQuickEntries(accountAssetsAvailable = true),
                collectionEntries = myCollectionEntries(accountAssetsAvailable = true),
            )

        assertEquals(8, (library.quickEntries + library.collectionEntries).map(MyEntryUi::id).distinct().size)
        assertEquals(MyEntryActionId.LocalMusic, library.quickEntries.single { it.id == MyEntryId.LocalMusic }.actionId)
        assertTrue(library.collectionEntries.all { it.actionId == MyEntryActionId.AccountAsset })
        assertEquals(MyEntryActionId.LocalMusic, library.actionFor(MyEntryId.LocalMusic))
        assertEquals(MyEntryActionId.AccountAsset, library.actionFor(MyEntryId.SavedAlbums))
    }
}
