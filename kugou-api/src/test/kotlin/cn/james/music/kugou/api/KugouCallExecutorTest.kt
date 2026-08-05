package cn.james.music.kugou.api

import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCallExecutor
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouProtocolResult
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestContext
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouRequestSpec
import cn.james.music.kugou.api.transport.KugouRetryDelayer
import cn.james.music.kugou.api.transport.KugouRetryMode
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouCallExecutorTest {
    @Test
    fun timeoutRetriesTwiceWithExponentialDelay() =
        runBlocking {
            val timeout = KugouTransportResult.Failure(KugouError.Network(KugouError.Network.Kind.Timeout))
            val transport = QueueTransport(timeout, timeout, success())
            val delays = mutableListOf<Long>()
            val executor = executor(transport, delays)

            val result = executor.executeJson(retryableSpec(), context())

            assertTrue(result is KugouProtocolResult.Success)
            assertEquals(3, transport.callCount)
            assertEquals(listOf(250L, 500L), delays)
        }

    @Test
    fun serverFailureRetriesButClientAndConnectionFailuresDoNot() =
        runBlocking {
            val server = QueueTransport(raw(status = 503), success())
            val client = QueueTransport(raw(status = 400), success())
            val connection =
                QueueTransport(
                    KugouTransportResult.Failure(KugouError.Network(KugouError.Network.Kind.Connection)),
                    success(),
                )

            assertTrue(executor(server).executeJson(retryableSpec(), context()) is KugouProtocolResult.Success)
            assertTrue(executor(client).executeJson(retryableSpec(), context()) is KugouProtocolResult.Failure)
            assertTrue(executor(connection).executeJson(retryableSpec(), context()) is KugouProtocolResult.Failure)
            assertEquals(2, server.callCount)
            assertEquals(1, client.callCount)
            assertEquals(1, connection.callCount)
        }

    @Test
    fun nonRetryableRequestNeverRepeatsTimeout() =
        runBlocking {
            val transport = QueueTransport(KugouTransportResult.Failure(KugouError.Network(KugouError.Network.Kind.Timeout)), success())
            val result = executor(transport).executeJson(retryableSpec().copy(retryMode = KugouRetryMode.None), context())

            assertTrue(result is KugouProtocolResult.Failure)
            assertEquals(1, transport.callCount)
        }

    private fun executor(
        transport: QueueTransport,
        delays: MutableList<Long> = mutableListOf(),
    ) = KugouCallExecutor(
        requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L }),
        transport = transport,
        retryDelayer = KugouRetryDelayer(delays::add),
    )

    private fun retryableSpec() =
        KugouRequestSpec(
            id = "fixture",
            method = KugouHttpMethod.Get,
            path = "/fixture",
            retryMode = KugouRetryMode.IdempotentRead,
        )

    private fun context() = KugouRequestContext(mid = "fixture-mid")

    private fun success() = raw(status = 200)

    private fun raw(status: Int) =
        KugouTransportResult.Success(
            KugouRawResponse(
                statusCode = status,
                headers = emptyMap(),
                body = """{"status":1}""".encodeToByteArray(),
            ),
        )

    private class QueueTransport(
        vararg results: KugouTransportResult,
    ) : KugouTransport {
        private val results = ArrayDeque(results.toList())
        var callCount = 0
            private set

        override suspend fun execute(request: cn.james.music.kugou.api.transport.KugouPreparedRequest): KugouTransportResult {
            callCount += 1
            return results.removeFirst()
        }
    }
}
