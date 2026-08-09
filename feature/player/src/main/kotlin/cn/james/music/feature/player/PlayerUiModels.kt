package cn.james.music.feature.player

import androidx.compose.runtime.Immutable

@Immutable
data class PlayerItemUiModel(
    val id: String,
    val title: String,
    val artist: String,
    val artwork: PlayerArtworkUiModel = PlayerArtworkUiModel.None,
) {
    init {
        require(id.isNotBlank()) { "Player item id must not be blank" }
        require(title.isNotBlank()) { "Player item title must not be blank" }
        require(artist.isNotBlank()) { "Player item artist must not be blank" }
    }
}

@Immutable
sealed interface PlayerArtworkUiModel {
    data object None : PlayerArtworkUiModel

    data class Remote(
        val httpsUrl: String,
    ) : PlayerArtworkUiModel {
        init {
            require(httpsUrl.startsWith("https://")) { "Player remote artwork must use HTTPS" }
        }
    }

    data class AppStorage(
        val ref: PlayerArtworkStorageRef,
    ) : PlayerArtworkUiModel

    data class BundledResource(
        val resourceId: Int,
    ) : PlayerArtworkUiModel {
        init {
            require(resourceId != 0) { "Player bundled artwork resource must be valid" }
        }
    }
}

@Immutable
@JvmInline
value class PlayerArtworkStorageRef(
    val key: String,
) {
    init {
        require(key.isSafeArtworkStorageKey()) { "Player artwork storage key must be safe and app-scoped" }
    }
}

enum class PlayerPlaybackModeUi {
    Sequential,
    RepeatAll,
    RepeatOne,
    Shuffle,
}

@Immutable
data class PlayerUiState(
    val item: PlayerItemUiModel? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val controlsEnabled: Boolean = true,
    val mode: PlayerPlaybackModeUi = PlayerPlaybackModeUi.RepeatAll,
)

@Immutable
data class PlayerProgressUiState(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
)

enum class PlayerPage {
    Cover,
    Lyrics,
}

@Immutable
data class PlayerLyricLineUi(
    val original: String,
    val secondary: String? = null,
    val startTimeMs: Long = 0,
    val endTimeMs: Long = startTimeMs + 1,
    val syllables: List<PlayerLyricSyllableUi> = emptyList(),
)

internal fun PlayerLyricLineUi.secondaryForDisplay(showSupplementalText: Boolean): String? = secondary?.takeIf { showSupplementalText }

@Immutable
data class PlayerLyricSyllableUi(
    val content: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
)

@Immutable
data class PlayerLyricsProgressUiState(
    val activeLineIndex: Int = -1,
    val highlightedPrefixCharacterCount: Int = 0,
)

enum class PlayerLyricsTextSize {
    Standard,
    Large,
    Largest,
}

@Immutable
sealed interface PlayerLyricsUiState {
    data object Unrequested : PlayerLyricsUiState

    data object Loading : PlayerLyricsUiState

    data object Empty : PlayerLyricsUiState

    data object Offline : PlayerLyricsUiState

    data object Error : PlayerLyricsUiState

    data class Content(
        val lines: List<PlayerLyricLineUi>,
        val textSize: PlayerLyricsTextSize = PlayerLyricsTextSize.Standard,
    ) : PlayerLyricsUiState
}

fun PlayerLyricsUiState.toPlayerLyricsProgressUiState(positionMs: Long): PlayerLyricsProgressUiState {
    val content = this as? PlayerLyricsUiState.Content ?: return PlayerLyricsProgressUiState()
    val activeIndex = content.lines.upperBoundByStart(positionMs) - 1
    if (activeIndex == -1) return PlayerLyricsProgressUiState()
    val line = content.lines[activeIndex]
    return PlayerLyricsProgressUiState(
        activeLineIndex = activeIndex,
        highlightedPrefixCharacterCount = line.highlightedPrefixCharacterCountAt(positionMs),
    )
}

internal fun PlayerLyricLineUi.highlightedPrefixCharacterCountAt(positionMs: Long): Int =
    syllables
        .sumOf { syllable ->
            when {
                positionMs <= syllable.startTimeMs -> {
                    0
                }

                positionMs >= syllable.endTimeMs -> {
                    syllable.content.length
                }

                else -> {
                    (
                        (syllable.content.length * (positionMs - syllable.startTimeMs)) /
                            (syllable.endTimeMs - syllable.startTimeMs)
                    ).toInt()
                        .coerceIn(1, syllable.content.length)
                }
            }
        }.coerceIn(0, original.length)

private fun List<PlayerLyricLineUi>.upperBoundByStart(positionMs: Long): Int {
    var low = 0
    var high = size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (this[middle].startTimeMs <= positionMs) low = middle + 1 else high = middle
    }
    return low
}

private fun String.isSafeArtworkStorageKey(): Boolean =
    isNotBlank() &&
        !startsWith('/') &&
        !startsWith('\\') &&
        !contains('\\') &&
        !Regex("^[A-Za-z]:").containsMatchIn(this) &&
        split('/').none { segment -> segment.isBlank() || segment == "." || segment == ".." }
