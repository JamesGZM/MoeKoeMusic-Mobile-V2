package cn.james.music.playback

import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val state: StateFlow<PlaybackState>
    val progress: StateFlow<PlaybackProgress>

    suspend fun replaceQueue(
        items: List<PlaybackItem>,
        startIndex: Int = 0,
        play: Boolean = true,
    ): PlaybackCommandResult

    suspend fun append(items: List<PlaybackItem>): PlaybackCommandResult

    suspend fun playNext(item: PlaybackItem): PlaybackCommandResult

    suspend fun remove(index: Int): PlaybackCommandResult

    suspend fun move(
        fromIndex: Int,
        toIndex: Int,
    ): PlaybackCommandResult

    suspend fun clear(): PlaybackCommandResult

    suspend fun play(): PlaybackCommandResult

    suspend fun pause(): PlaybackCommandResult

    suspend fun seekTo(positionMs: Long): PlaybackCommandResult

    suspend fun skipNext(): PlaybackCommandResult

    suspend fun skipPrevious(): PlaybackCommandResult

    suspend fun setMode(mode: PlaybackMode): PlaybackCommandResult
}
