package cn.james.music.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlayerUiStateContractTest {
    @Test
    fun playerStateMatrixKeepsEmptyPlaybackAndLyricsFailuresDistinct() {
        val item = PlayerItemUiModel("fixture", "Title", "Artist")
        val content = PlayerUiState(item = item, isPlaying = true)

        assertNotEquals(PlayerUiState(), content)
        assertNotEquals(PlayerLyricsUiState.Loading, PlayerLyricsUiState.Empty)
        assertNotEquals(PlayerLyricsUiState.Empty, PlayerLyricsUiState.Offline)
        assertNotEquals(PlayerLyricsUiState.Offline, PlayerLyricsUiState.Error)
        assertEquals(item, content.item)
    }

    @Test
    fun queueStateKeepsEmptyAndCurrentSelectionDistinct() {
        val item = PlayerQueueItemUi("fixture", "Title", "Artist")
        val empty = PlayerQueueUiState(emptyList(), null, PlayerPlaybackModeUi.RepeatAll)
        val content = PlayerQueueUiState(listOf(item), item.id, PlayerPlaybackModeUi.RepeatAll)

        assertNotEquals(empty, content)
        assertEquals(item.id, content.currentItemId)
    }

    @Test
    fun appStorageArtworkOnlyAcceptsValidatedOpaqueReferences() {
        val ref = PlayerArtworkStorageRef("artwork/track.webp")

        assertEquals(ref, PlayerArtworkUiModel.AppStorage(ref).ref)
        assertEquals("artwork/track.webp", ref.key)

        listOf("", "/data/user/0/cover.webp", "../cover.webp", "artwork/../cover.webp", "C:/cover.webp", "artwork\\cover.webp")
            .forEach { invalid ->
                assertThrows(IllegalArgumentException::class.java) { PlayerArtworkStorageRef(invalid) }
            }
    }
}
