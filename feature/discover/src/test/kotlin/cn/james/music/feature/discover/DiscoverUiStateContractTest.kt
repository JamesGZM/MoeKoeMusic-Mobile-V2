package cn.james.music.feature.discover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DiscoverUiStateContractTest {
    @Test
    fun discoverStateMatrixKeepsLoadingEmptyErrorAndContentDistinct() {
        val content = DiscoverUiState.Content(discoverDesignPreview)

        assertNotEquals(DiscoverUiState.Loading, DiscoverUiState.Empty)
        assertNotEquals(DiscoverUiState.Empty, DiscoverUiState.Error)
        assertNotEquals(DiscoverUiState.Error, content)
        assertEquals(discoverDesignPreview, content.value)
    }
}
