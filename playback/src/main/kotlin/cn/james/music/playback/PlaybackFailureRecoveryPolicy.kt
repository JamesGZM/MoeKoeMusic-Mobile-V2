package cn.james.music.playback

internal enum class PlaybackFailureRecoveryAction {
    Pause,
    Advance,
}

internal class PlaybackFailureRecoveryPolicy(
    private val consecutiveErrorPolicy: ConsecutivePlaybackErrorPolicy = ConsecutivePlaybackErrorPolicy(),
) {
    fun onUnrecoverableError(
        autoSkipFailedPlayback: Boolean,
        queueSize: Int,
    ): PlaybackFailureRecoveryAction {
        if (!autoSkipFailedPlayback) {
            consecutiveErrorPolicy.reset()
            return PlaybackFailureRecoveryAction.Pause
        }
        return if (consecutiveErrorPolicy.shouldAdvance(queueSize)) {
            PlaybackFailureRecoveryAction.Advance
        } else {
            PlaybackFailureRecoveryAction.Pause
        }
    }

    fun onPlaybackReady() {
        consecutiveErrorPolicy.reset()
    }
}
