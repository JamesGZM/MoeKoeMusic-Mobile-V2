package cn.james.music.core.model.lyrics

import cn.james.music.core.model.playback.PlaybackSource

enum class LyricsVocalAlignment {
    Leading,
    Trailing,
    Unspecified,
}

data class LyricsSyllable(
    val content: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val phonetic: String? = null,
) {
    init {
        require(content.isNotEmpty()) { "Lyrics syllable content must not be empty" }
        require(startTimeMs >= 0) { "Lyrics syllable start time must not be negative" }
        require(endTimeMs > startTimeMs) { "Lyrics syllable must have a positive duration" }
    }

    override fun toString(): String = "LyricsSyllable(startTimeMs=$startTimeMs, endTimeMs=$endTimeMs, phoneticPresent=${phonetic != null})"
}

data class LyricsLine(
    val syllables: List<LyricsSyllable>,
    val translation: String? = null,
    val vocalAlignment: LyricsVocalAlignment = LyricsVocalAlignment.Unspecified,
    val backgroundLines: List<LyricsLine> = emptyList(),
) {
    init {
        require(syllables.isNotEmpty()) { "Lyrics line must contain at least one syllable" }
        require(syllables.joinToString(separator = "", transform = LyricsSyllable::content).isNotBlank()) {
            "Lyrics line content must not be blank"
        }
        require(syllables.zipWithNext().all { (first, second) -> first.startTimeMs <= second.startTimeMs }) {
            "Lyrics syllables must be ordered by start time"
        }
    }

    val text: String
        get() = syllables.joinToString(separator = "", transform = LyricsSyllable::content)

    val startTimeMs: Long
        get() = syllables.first().startTimeMs

    val endTimeMs: Long
        get() = syllables.maxOf(LyricsSyllable::endTimeMs)

    override fun toString(): String =
        "LyricsLine(startTimeMs=$startTimeMs, endTimeMs=$endTimeMs, syllableCount=${syllables.size}, " +
            "translationPresent=${translation != null}, vocalAlignment=$vocalAlignment, " +
            "backgroundLineCount=${backgroundLines.size})"
}

data class LyricsDocument(
    val lines: List<LyricsLine>,
) {
    init {
        require(lines.isNotEmpty()) { "Lyrics document must contain at least one line" }
        require(lines.zipWithNext().all { (first, second) -> first.startTimeMs <= second.startTimeMs }) {
            "Lyrics lines must be ordered by start time"
        }
    }

    override fun toString(): String = "LyricsDocument(lineCount=${lines.size})"
}

sealed interface LyricsError {
    data object Offline : LyricsError

    data object Timeout : LyricsError

    data object Connection : LyricsError

    data object ServiceUnavailable : LyricsError

    data object Protocol : LyricsError

    data object UnsupportedSource : LyricsError
}

sealed interface LyricsResult {
    data class Success(
        val document: LyricsDocument,
    ) : LyricsResult

    data object NotFound : LyricsResult

    data class Failure(
        val error: LyricsError,
    ) : LyricsResult
}

interface LyricsRepository {
    suspend fun getLyrics(source: PlaybackSource): LyricsResult
}
