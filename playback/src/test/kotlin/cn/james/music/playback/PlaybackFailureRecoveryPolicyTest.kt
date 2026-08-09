package cn.james.music.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFailureRecoveryPolicyTest {
    @Test
    fun disabledAutoSkipPausesWithoutConsumingFailureBudget() {
        val policy = PlaybackFailureRecoveryPolicy(ConsecutivePlaybackErrorPolicy(maximumFailures = 2))

        assertEquals(PlaybackFailureRecoveryAction.Pause, policy.onUnrecoverableError(autoSkipFailedPlayback = false, queueSize = 3))
        assertEquals(PlaybackFailureRecoveryAction.Advance, policy.onUnrecoverableError(autoSkipFailedPlayback = true, queueSize = 3))
        assertEquals(PlaybackFailureRecoveryAction.Pause, policy.onUnrecoverableError(autoSkipFailedPlayback = true, queueSize = 3))
    }

    @Test
    fun enabledAutoSkipKeepsSingleItemAndSixFailureLimits() {
        val singleItem = PlaybackFailureRecoveryPolicy()
        assertEquals(PlaybackFailureRecoveryAction.Pause, singleItem.onUnrecoverableError(autoSkipFailedPlayback = true, queueSize = 1))

        val queue = PlaybackFailureRecoveryPolicy()
        repeat(5) {
            assertEquals(PlaybackFailureRecoveryAction.Advance, queue.onUnrecoverableError(autoSkipFailedPlayback = true, queueSize = 10))
        }
        assertEquals(PlaybackFailureRecoveryAction.Pause, queue.onUnrecoverableError(autoSkipFailedPlayback = true, queueSize = 10))
    }

    @Test
    fun nextErrorUsesCurrentSettingWithoutChangingEarlierDecision() {
        val policy = PlaybackFailureRecoveryPolicy()

        assertEquals(PlaybackFailureRecoveryAction.Advance, policy.onUnrecoverableError(autoSkipFailedPlayback = true, queueSize = 2))
        assertEquals(PlaybackFailureRecoveryAction.Pause, policy.onUnrecoverableError(autoSkipFailedPlayback = false, queueSize = 2))
    }

    @Test
    fun readyPlaybackResetsConsecutiveFailureCount() {
        val policy = PlaybackFailureRecoveryPolicy(ConsecutivePlaybackErrorPolicy(maximumFailures = 2))

        assertEquals(PlaybackFailureRecoveryAction.Advance, policy.onUnrecoverableError(autoSkipFailedPlayback = true, queueSize = 3))
        policy.onPlaybackReady()
        assertEquals(PlaybackFailureRecoveryAction.Advance, policy.onUnrecoverableError(autoSkipFailedPlayback = true, queueSize = 3))
    }
}
