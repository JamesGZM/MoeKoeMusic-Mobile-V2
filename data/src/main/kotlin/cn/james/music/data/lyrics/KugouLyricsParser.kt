package cn.james.music.data.lyrics

import cn.james.music.core.model.lyrics.LyricsDocument
import cn.james.music.core.model.lyrics.LyricsLine
import cn.james.music.core.model.lyrics.LyricsSyllable
import cn.james.music.core.model.lyrics.LyricsVocalAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeAlignment
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.parser.KugouKrcParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal sealed interface KugouLyricsParseResult {
    data class Success(
        val document: LyricsDocument,
    ) : KugouLyricsParseResult

    data object Failure : KugouLyricsParseResult
}

internal class KugouLyricsParser(
    private val dispatcher: CoroutineDispatcher,
) {
    suspend fun parse(krcText: String): KugouLyricsParseResult =
        withContext(dispatcher) {
            try {
                currentCoroutineContext().ensureActive()
                if (!KugouKrcParser.canParse(krcText)) return@withContext KugouLyricsParseResult.Failure
                val lines =
                    KugouKrcParser
                        .parse(krcText)
                        .lines
                        .mapNotNull { line -> (line as? KaraokeLine.MainKaraokeLine)?.toDomainOrNull() }
                        .sortedBy(LyricsLine::startTimeMs)
                currentCoroutineContext().ensureActive()
                if (lines.isEmpty()) {
                    KugouLyricsParseResult.Failure
                } else {
                    KugouLyricsParseResult.Success(LyricsDocument(lines))
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                KugouLyricsParseResult.Failure
            }
        }

    private fun KaraokeLine.MainKaraokeLine.toDomainOrNull(): LyricsLine? {
        val mappedSyllables = syllables.mapNotNull { syllable -> syllable.toDomainOrNull() }
        if (mappedSyllables.joinToString(separator = "", transform = LyricsSyllable::content).isBlank()) return null
        return runCatching {
            LyricsLine(
                syllables = mappedSyllables,
                translation = translation?.trim()?.takeIf(String::isNotBlank),
                vocalAlignment = alignment.toDomain(),
                backgroundLines =
                    accompanimentLines
                        .orEmpty()
                        .mapNotNull { line -> line.toDomainOrNull() },
            )
        }.getOrNull()
    }

    private fun KaraokeLine.AccompanimentKaraokeLine.toDomainOrNull(): LyricsLine? {
        val mappedSyllables = syllables.mapNotNull { syllable -> syllable.toDomainOrNull() }
        if (mappedSyllables.joinToString(separator = "", transform = LyricsSyllable::content).isBlank()) return null
        return runCatching {
            LyricsLine(
                syllables = mappedSyllables,
                translation = translation?.trim()?.takeIf(String::isNotBlank),
                vocalAlignment = alignment.toDomain(),
            )
        }.getOrNull()
    }

    private fun KaraokeSyllable.toDomainOrNull(): LyricsSyllable? {
        if (content.isEmpty() || start < 0 || end <= start) return null
        return LyricsSyllable(
            content = content,
            startTimeMs = start.toLong(),
            endTimeMs = end.toLong(),
            phonetic = phonetic?.trim()?.takeIf(String::isNotBlank),
        )
    }

    private fun KaraokeAlignment.toDomain(): LyricsVocalAlignment =
        when (this) {
            KaraokeAlignment.Start -> LyricsVocalAlignment.Leading
            KaraokeAlignment.End -> LyricsVocalAlignment.Trailing
            KaraokeAlignment.Unspecified -> LyricsVocalAlignment.Unspecified
        }
}
