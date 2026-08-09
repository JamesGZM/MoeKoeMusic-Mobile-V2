package cn.james.music.feature.discover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DiscoverUiStateContractTest {
    @Test
    fun discoverStateMatrixKeepsLoadingEmptyErrorAndContentDistinct() {
        val content = DiscoverUiState.Content(discoverDefaultContent)

        assertNotEquals(DiscoverUiState.Loading, DiscoverUiState.Empty)
        assertNotEquals(DiscoverUiState.Empty, DiscoverUiState.Error)
        assertNotEquals(DiscoverUiState.Error, content)
        assertEquals(discoverDefaultContent, content.value)
    }

    @Test
    fun contentUsesStableIdsAndSemanticTone() {
        val content = discoverDefaultContent

        assertEquals(
            content.tabs.size,
            content.tabs
                .map(DiscoverTabUi::id)
                .distinct()
                .size,
        )
        assertEquals(
            content.categories.size,
            content.categories
                .map(DiscoverCategoryUi::id)
                .distinct()
                .size,
        )
        assertEquals(
            listOf("pop", "rock", "acg", "light", "electronic", "healing"),
            content.categories.map(DiscoverCategoryUi::id),
        )
        assertEquals(
            content.rankings.size,
            content.rankings
                .map(DiscoverRankingUi::id)
                .distinct()
                .size,
        )
        assertEquals(DiscoverRankingToneUi.Rising, content.rankings.first().tone)
    }
}
