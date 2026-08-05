package cn.james.music.playback

import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackMode

enum class PlaybackConnectionState {
    Connecting,
    Connected,
    Disconnected,
}

enum class PlaybackStatus {
    Idle,
    Buffering,
    Ready,
    Ended,
}

sealed interface PlaybackError {
    val recoverable: Boolean

    data class SourceUnavailable(
        val itemId: String,
        val reason: PlaybackSourceError,
        override val recoverable: Boolean = true,
    ) : PlaybackError

    data class PlayerFailure(
        val itemId: String?,
        val errorCode: Int,
        override val recoverable: Boolean,
    ) : PlaybackError

    data object ControllerUnavailable : PlaybackError {
        override val recoverable = true
    }

    data object InvalidCommand : PlaybackError {
        override val recoverable = false
    }
}

sealed interface PlaybackSourceError {
    data object NoCopyright : PlaybackSourceError

    data object VipRequired : PlaybackSourceError

    data object Offline : PlaybackSourceError

    data object Timeout : PlaybackSourceError

    data object Connection : PlaybackSourceError

    data object VerificationRequired : PlaybackSourceError

    data object AuthenticationRequired : PlaybackSourceError

    data object ServiceUnavailable : PlaybackSourceError

    data object Protocol : PlaybackSourceError

    data object SessionInitialization : PlaybackSourceError

    data object InvalidSource : PlaybackSourceError
}

sealed interface RemotePlaybackSourceResult {
    data class Resolved(
        val url: String,
    ) : RemotePlaybackSourceResult

    data class Unavailable(
        val error: PlaybackSourceError,
    ) : RemotePlaybackSourceResult
}

sealed interface PlaybackCommandResult {
    data object Accepted : PlaybackCommandResult

    data class Rejected(
        val error: PlaybackError,
    ) : PlaybackCommandResult
}

data class PlaybackState(
    val connection: PlaybackConnectionState = PlaybackConnectionState.Connecting,
    val queue: List<PlaybackItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val mode: PlaybackMode = PlaybackMode.RepeatAll,
    val error: PlaybackError? = null,
) {
    val currentItem: PlaybackItem?
        get() = queue.getOrNull(currentIndex)
}

data class PlaybackProgress(
    val positionMs: Long = 0,
    val bufferedPositionMs: Long = 0,
    val durationMs: Long = 0,
)

data class PlaybackSnapshot(
    val queue: List<PlaybackItem>,
    val currentIndex: Int,
    val positionMs: Long,
    val mode: PlaybackMode,
    val updatedAtEpochMs: Long,
)

interface PlaybackSnapshotStore {
    suspend fun load(): PlaybackSnapshot?

    suspend fun save(snapshot: PlaybackSnapshot)

    suspend fun clear()
}
