package cn.james.music.feature.player

import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import cn.james.music.core.model.playback.PlaybackSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlayerUiStateContractTest {
    @Test
    fun playerStateMatrixKeepsEmptyPlaybackAndLyricsFailuresDistinct() {
        val item = PlaybackItem("fixture", "Title", "Artist", source = PlaybackSource.FoundationDemo)
        val content = PlayerUiState(item = item, isPlaying = true)

        assertNotEquals(PlayerUiState(), content)
        assertNotEquals(PlayerLyricsUiState.Loading, PlayerLyricsUiState.Empty)
        assertNotEquals(PlayerLyricsUiState.Empty, PlayerLyricsUiState.Offline)
        assertNotEquals(PlayerLyricsUiState.Offline, PlayerLyricsUiState.Error)
        assertEquals(item, content.item)
    }

    @Test
    fun queueStateKeepsEmptyAndCurrentSelectionDistinct() {
        val item = PlaybackItem("fixture", "Title", "Artist", source = PlaybackSource.FoundationDemo)
        val empty = PlayerQueueUiState(emptyList(), -1, PlaybackMode.RepeatAll)
        val content = PlayerQueueUiState(listOf(PlayerQueueItemUi(item)), 0, PlaybackMode.RepeatAll)

        assertNotEquals(empty, content)
        assertEquals(0, content.currentIndex)
    }
}
