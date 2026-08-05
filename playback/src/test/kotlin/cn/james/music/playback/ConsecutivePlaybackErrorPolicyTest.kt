package cn.james.music.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsecutivePlaybackErrorPolicyTest {
    @Test
    fun stopsAfterSixConsecutiveFailuresInLargeQueue() {
        val policy = ConsecutivePlaybackErrorPolicy()
        repeat(5) { assertTrue(policy.shouldAdvance(queueSize = 10)) }
        assertFalse(policy.shouldAdvance(queueSize = 10))
    }

    @Test
    fun successfulPlaybackResetsFailureCount() {
        val policy = ConsecutivePlaybackErrorPolicy(maximumFailures = 2)
        assertTrue(policy.shouldAdvance(queueSize = 3))
        assertFalse(policy.shouldAdvance(queueSize = 3))
        policy.reset()
        assertTrue(policy.shouldAdvance(queueSize = 3))
    }

    @Test
    fun singleItemQueueNeverAutoAdvances() {
        assertFalse(ConsecutivePlaybackErrorPolicy().shouldAdvance(queueSize = 1))
    }
}
