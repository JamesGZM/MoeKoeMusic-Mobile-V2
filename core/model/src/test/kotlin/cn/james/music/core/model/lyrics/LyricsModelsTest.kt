package cn.james.music.core.model.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class LyricsModelsTest {
    @Test
    fun lineDerivesTextAndBoundsWhilePreservingTimedSpaces() {
        val line =
            LyricsLine(
                syllables =
                    listOf(
                        LyricsSyllable("Moe", startTimeMs = 100, endTimeMs = 300, phonetic = "moe"),
                        LyricsSyllable(" ", startTimeMs = 300, endTimeMs = 350),
                        LyricsSyllable("Koe", startTimeMs = 350, endTimeMs = 800, phonetic = "koe"),
                    ),
                translation = "萌音",
            )

        assertEquals("Moe Koe", line.text)
        assertEquals(100L, line.startTimeMs)
        assertEquals(800L, line.endTimeMs)
        assertFalse(line.toString().contains("Moe"))
    }

    @Test
    fun invalidDurationsBlankLinesAndUnorderedDocumentsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LyricsSyllable("invalid", startTimeMs = 100, endTimeMs = 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LyricsLine(listOf(LyricsSyllable(" ", startTimeMs = 0, endTimeMs = 100)))
        }

        val later = line("later", startTimeMs = 1_000)
        val earlier = line("earlier", startTimeMs = 0)
        assertThrows(IllegalArgumentException::class.java) {
            LyricsDocument(listOf(later, earlier))
        }
    }

    private fun line(
        content: String,
        startTimeMs: Long,
    ) = LyricsLine(
        listOf(
            LyricsSyllable(
                content = content,
                startTimeMs = startTimeMs,
                endTimeMs = startTimeMs + 100,
            ),
        ),
    )
}
