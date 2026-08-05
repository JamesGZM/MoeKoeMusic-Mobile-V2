package cn.james.music.playback

import kotlin.math.min

internal class ConsecutivePlaybackErrorPolicy(
    private val maximumFailures: Int = 6,
) {
    private var failureCount = 0

    fun shouldAdvance(queueSize: Int): Boolean {
        failureCount += 1
        return queueSize > 1 && failureCount < min(queueSize, maximumFailures)
    }

    fun reset() {
        failureCount = 0
    }
}
