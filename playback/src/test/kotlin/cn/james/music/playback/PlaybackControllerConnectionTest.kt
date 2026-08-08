package cn.james.music.playback

import com.google.common.util.concurrent.SettableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackControllerConnectionTest {
    @Test
    fun inFlightConnectionIsShared() {
        val pending = SettableFuture.create<String>()
        var createCalls = 0
        val connection =
            PlaybackControllerConnection(
                createFuture = {
                    createCalls += 1
                    pending
                },
                callbackExecutor = Runnable::run,
                onConnected = {},
                onConnectionFailed = {},
            )

        assertSame(pending, connection.ensure())
        assertSame(pending, connection.ensure())
        assertEquals(1, createCalls)
    }

    @Test
    fun failedConnectionIsClearedSoTheNextCommandCanRetry() {
        val first = SettableFuture.create<String>()
        val second = SettableFuture.create<String>()
        val futures = ArrayDeque(listOf(first, second))
        val connected = mutableListOf<String>()
        var failures = 0
        val connection =
            PlaybackControllerConnection(
                createFuture = { futures.removeFirst() },
                callbackExecutor = Runnable::run,
                onConnected = connected::add,
                onConnectionFailed = { failures += 1 },
            )

        assertSame(first, connection.ensure())
        first.setException(IllegalStateException("fixture failure"))
        assertEquals(1, failures)

        assertSame(second, connection.ensure())
        second.set("controller")
        assertEquals(listOf("controller"), connected)
    }
}
