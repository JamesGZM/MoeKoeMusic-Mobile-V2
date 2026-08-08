package cn.james.music.feature.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaylistDetailUiStateContractTest {
    @Test
    fun playlistDetailStateMatrixKeepsLoadingEmptyErrorOfflineAndContentDistinct() {
        val content = PlaylistDetailUiState.Content(playlistDetailDesignPreview)

        assertNotEquals(PlaylistDetailUiState.Loading, PlaylistDetailUiState.Empty)
        assertNotEquals(PlaylistDetailUiState.Empty, PlaylistDetailUiState.Error)
        assertNotEquals(PlaylistDetailUiState.Error, PlaylistDetailUiState.Offline)
        assertNotEquals(PlaylistDetailUiState.Offline, content)
        assertEquals(playlistDetailDesignPreview, content.value)
    }
}
