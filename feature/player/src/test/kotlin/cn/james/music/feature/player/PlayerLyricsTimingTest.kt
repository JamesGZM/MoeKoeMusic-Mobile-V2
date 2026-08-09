package cn.james.music.feature.player

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLyricsTimingTest {
    private val content =
        PlayerLyricsUiState.Content(
            lines =
                listOf(
                    PlayerLyricLineUi(
                        original = "abcdef",
                        startTimeMs = 1_000,
                        endTimeMs = 3_000,
                        syllables =
                            listOf(
                                PlayerLyricSyllableUi("abc", 1_000, 2_000),
                                PlayerLyricSyllableUi("def", 2_000, 3_000),
                            ),
                    ),
                    PlayerLyricLineUi("next", startTimeMs = 4_000, endTimeMs = 5_000),
                ),
        )

    @Test
    fun timingUsesOrderedBoundariesAndPrefixHighlight() {
        assertEquals(PlayerLyricsProgressUiState(), content.toPlayerLyricsProgressUiState(999))
        assertEquals(PlayerLyricsProgressUiState(0, 1), content.toPlayerLyricsProgressUiState(1_500))
        assertEquals(PlayerLyricsProgressUiState(0, 4), content.toPlayerLyricsProgressUiState(2_500))
        assertEquals(PlayerLyricsProgressUiState(0, 6), content.toPlayerLyricsProgressUiState(3_000))
        assertEquals(PlayerLyricsProgressUiState(1, 0), content.toPlayerLyricsProgressUiState(4_000))
    }

    @Test
    fun visibilityCoordinatorOnlyEmitsActualTransitionsAndDispose() {
        val coordinator = PlayerLyricsPageVisibilityCoordinator()

        assertEquals(false, coordinator.onSettledPage(PlayerPage.Cover.ordinal))
        assertEquals(null, coordinator.onSettledPage(PlayerPage.Cover.ordinal))
        assertEquals(true, coordinator.onSettledPage(PlayerPage.Lyrics.ordinal))
        assertEquals(false, coordinator.onDisposed())
        assertEquals(null, coordinator.onDisposed())
    }

    @Test
    fun supplementalTextVisibilityOnlyChangesTheRenderedSecondaryText() {
        val line = PlayerLyricLineUi(original = "原文", secondary = "translation")

        assertEquals("translation", line.secondaryForDisplay(showSupplementalText = true))
        assertEquals(null, line.secondaryForDisplay(showSupplementalText = false))
        assertEquals("translation", line.secondary)
    }

    @Test
    fun textSizeIsIndependentFromLyricsDocumentAndPreservesConfirmedOffsets() {
        val document = PlayerLyricsUiState.Content(lines = content.lines)

        assertEquals((-29).dp, PlayerLyricsTextSize.Large.contentOffsetFor(document.lines, showSupplementalText = true))
        assertEquals((-44).dp, PlayerLyricsTextSize.Largest.contentOffsetFor(document.lines, showSupplementalText = true))
        assertEquals((-65).dp, PlayerLyricsTextSize.Standard.contentOffsetFor(document.lines, showSupplementalText = true))
        assertEquals(content.lines, document.lines)
    }

    @Test
    fun standardOffsetUsesOriginalOnlyGeometryWhenSupplementalTextIsHidden() {
        val lines =
            List(5) { index ->
                PlayerLyricLineUi(
                    original = "原文$index",
                    secondary = "translation$index",
                    startTimeMs = index * 1_000L,
                    endTimeMs = (index + 1) * 1_000L,
                )
            }
        val document = PlayerLyricsUiState.Content(lines = lines)

        assertEquals(18.dp, PlayerLyricsTextSize.Standard.contentOffsetFor(document.lines, showSupplementalText = true))
        assertEquals((-65).dp, PlayerLyricsTextSize.Standard.contentOffsetFor(document.lines, showSupplementalText = false))
        assertEquals(lines, document.lines)
        assertEquals("translation0", document.lines.first().secondary)
    }
}
