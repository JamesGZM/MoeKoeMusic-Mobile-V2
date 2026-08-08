package cn.james.music.feature.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileUiStateContractTest {
    @Test
    fun profileStateMatrixKeepsOfflineDistinctFromGenericError() {
        assertNotEquals(UserProfileUiState.Error, UserProfileUiState.Offline)
    }

    @Test
    fun emptyPlaylistStateKeepsProfileAndOverviewContent() {
        val state = UserProfileUiState.Content(userProfileDesignPreview.copy(playlists = emptyList()))

        assertEquals(userProfileDesignPreview.nickname, state.value.nickname)
        assertEquals(userProfileDesignPreview.listeningDurationValue, state.value.listeningDurationValue)
        assertTrue(state.value.playlists.isEmpty())
    }

    @Test
    fun profileFixtureUsesStableUniquePlaylistIds() {
        val ids = userProfileDesignPreview.playlists.map(UserPlaylistUi::id)

        assertEquals(ids.size, ids.toSet().size)
    }
}
