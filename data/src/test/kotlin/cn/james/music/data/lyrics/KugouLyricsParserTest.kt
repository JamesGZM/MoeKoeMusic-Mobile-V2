package cn.james.music.data.lyrics

import cn.james.music.core.model.lyrics.LyricsVocalAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class KugouLyricsParserTest {
    private val parser = KugouLyricsParser(Dispatchers.Unconfined)

    @Test
    fun mapsSyllablesTranslationPhoneticsDuetAndBackgroundVocals() =
        runBlocking {
            val result = parser.parse(krcFixture())

            assertTrue(result is KugouLyricsParseResult.Success)
            val document = (result as KugouLyricsParseResult.Success).document
            assertEquals(2, document.lines.size)

            val first = document.lines.first()
            assertEquals("Moe Koe", first.text)
            assertEquals("萌音", first.translation)
            assertEquals(LyricsVocalAlignment.Leading, first.vocalAlignment)
            assertEquals(listOf("Moe", null, "Koe"), first.syllables.map { it.phonetic })
            assertEquals("和声", first.backgroundLines.single().text)

            val second = document.lines.last()
            assertEquals(1_000L, second.startTimeMs)
            assertEquals(2_000L, second.endTimeMs)
            assertEquals(LyricsVocalAlignment.Trailing, second.vocalAlignment)
            assertEquals("回应", second.translation)
        }

    @Test
    fun filtersZeroDurationSyllablesWithoutDestroyingIntentionalSpaces() =
        runBlocking {
            val result =
                parser.parse(
                    """
                    [0,1000]<0,300,0>You<300,0,0>bad<300,200,0> <500,500,0>run
                    """.trimIndent(),
                )

            val line = (result as KugouLyricsParseResult.Success).document.lines.single()
            assertEquals("You run", line.text)
            assertFalse(line.syllables.any { it.content == "bad" })
        }

    @Test
    fun malformedOrEmptyKrcReturnsFailureWithoutLeakingThirdPartyModels() =
        runBlocking {
            val malformed = parser.parse("[0,1000]plain text without syllable timing")
            val onlyZeroDuration = parser.parse("[0,1000]<0,0,0>invalid")

            assertEquals(KugouLyricsParseResult.Failure, malformed)
            assertEquals(KugouLyricsParseResult.Failure, onlyZeroDuration)
        }

    @Test
    fun diagnosticsDoNotContainLyricsText() =
        runBlocking {
            val result = parser.parse(krcFixture()) as KugouLyricsParseResult.Success
            val document = result.document

            assertFalse(document.toString().contains("Moe"))
            assertFalse(
                document.lines
                    .first()
                    .toString()
                    .contains("Moe"),
            )
            assertFalse(
                document.lines
                    .first()
                    .syllables
                    .first()
                    .toString()
                    .contains("Moe"),
            )
        }

    private fun krcFixture(): String {
        val metadata =
            """
            {
              "content": [
                {"type":1,"lyricContent":[["萌音"],["回应"]]},
                {"type":0,"lyricContent":[[["Moe"],[" "],["Koe"]],[["colon"],["hui ying"]]]}
              ]
            }
            """.trimIndent()
        val encodedMetadata = Base64.getEncoder().encodeToString(metadata.encodeToByteArray())
        return """
            [language:$encodedMetadata]
            [0,1000]<0,300,0>Moe<300,100,0> <400,600,0>Koe
            [bg:<200,300,0>(和<500,300,0>声)]
            [1000,1000]<0,100,0>:<100,900,0>回应
            """.trimIndent()
    }
}
