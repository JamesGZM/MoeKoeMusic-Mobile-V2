package cn.james.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.james.music.core.model.lyrics.LyricsDocument
import cn.james.music.core.model.lyrics.LyricsError
import cn.james.music.core.model.lyrics.LyricsRepository
import cn.james.music.core.model.lyrics.LyricsResult
import cn.james.music.core.model.playback.PlaybackSource
import cn.james.music.feature.player.PlayerLyricLineUi
import cn.james.music.feature.player.PlayerLyricSyllableUi
import cn.james.music.feature.player.PlayerLyricsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-side adapter between the lyrics domain and the pure player feature. It deliberately has no
 * playback command dependency: seeking stays owned by [AppPlaybackViewModel].
 */
@HiltViewModel
class AppPlayerLyricsViewModel
    @Inject
    constructor(
        private val lyricsRepository: LyricsRepository,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow<PlayerLyricsUiState>(PlayerLyricsUiState.Unrequested)
        val state: StateFlow<PlayerLyricsUiState> = mutableState.asStateFlow()

        private var successfulKey: LyricsRequestKey? = null
        private var successfulContent: PlayerLyricsUiState.Content? = null
        private var requestJob: Job? = null
        private var requestKey: LyricsRequestKey? = null
        private var lyricsPageVisible = false
        private var generation = 0L

        fun updateRequest(
            itemId: String?,
            source: PlaybackSource?,
            lyricsPageVisible: Boolean,
        ) {
            val nextKey = itemId?.let { id -> source?.let { LyricsRequestKey(id, it) } }
            val changed = requestKey != nextKey
            this.lyricsPageVisible = lyricsPageVisible
            if (changed) {
                cancelAndInvalidate()
                requestKey = nextKey
                if (successfulKey != nextKey) {
                    successfulKey = null
                    successfulContent = null
                }
                mutableState.value = PlayerLyricsUiState.Unrequested
            }
            if (!lyricsPageVisible) {
                cancelAndInvalidate()
                mutableState.value = PlayerLyricsUiState.Unrequested
                return
            }
            val key =
                requestKey ?: run {
                    mutableState.value = PlayerLyricsUiState.Empty
                    return
                }
            successfulContent?.takeIf { successfulKey == key }?.let { cached ->
                mutableState.value = cached
                return
            }
            if (requestJob?.isActive != true && mutableState.value == PlayerLyricsUiState.Unrequested) load(key)
        }

        fun retry() {
            val key = requestKey ?: return
            if (lyricsPageVisible) load(key, force = true)
        }

        private fun load(
            key: LyricsRequestKey,
            force: Boolean = false,
        ) {
            if (!force && requestJob?.isActive == true) return
            requestJob?.cancel()
            val requestGeneration = ++generation
            mutableState.value = PlayerLyricsUiState.Loading
            requestJob =
                viewModelScope.launch {
                    val result =
                        try {
                            lyricsRepository.getLyrics(key.source)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        }
                    if (requestGeneration != generation || !lyricsPageVisible || requestKey != key) return@launch
                    val state = result.toPlayerLyricsUiState()
                    if (state is PlayerLyricsUiState.Content) {
                        successfulKey = key
                        successfulContent = state
                    }
                    mutableState.value = state
                }
        }

        private fun cancelAndInvalidate() {
            requestJob?.cancel()
            requestJob = null
            generation++
        }
    }

private data class LyricsRequestKey(
    val itemId: String,
    val source: PlaybackSource,
)

internal fun LyricsResult.toPlayerLyricsUiState(): PlayerLyricsUiState =
    when (this) {
        is LyricsResult.Success -> {
            document.toPlayerLyricsContent()
        }

        LyricsResult.NotFound -> {
            PlayerLyricsUiState.Empty
        }

        is LyricsResult.Failure -> {
            when (error) {
                LyricsError.UnsupportedSource -> PlayerLyricsUiState.Empty

                LyricsError.Offline -> PlayerLyricsUiState.Offline

                LyricsError.Timeout,
                LyricsError.Connection,
                LyricsError.ServiceUnavailable,
                LyricsError.Protocol,
                -> PlayerLyricsUiState.Error
            }
        }
    }

internal fun LyricsDocument.toPlayerLyricsContent(): PlayerLyricsUiState.Content =
    PlayerLyricsUiState.Content(
        lines =
            lines.map { line ->
                PlayerLyricLineUi(
                    original = line.text,
                    secondary =
                        line.translation ?: line.syllables
                            .mapNotNull { it.phonetic?.takeIf(String::isNotBlank) }
                            .joinToString(separator = "")
                            .takeIf(String::isNotBlank),
                    startTimeMs = line.startTimeMs,
                    endTimeMs = line.endTimeMs,
                    syllables =
                        line.syllables.map { syllable ->
                            PlayerLyricSyllableUi(
                                content = syllable.content,
                                startTimeMs = syllable.startTimeMs,
                                endTimeMs = syllable.endTimeMs,
                            )
                        },
                )
            },
    )
