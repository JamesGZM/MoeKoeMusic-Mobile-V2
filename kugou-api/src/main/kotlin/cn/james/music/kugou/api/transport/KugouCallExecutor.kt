package cn.james.music.kugou.api.transport

import kotlinx.coroutines.delay

fun interface KugouRetryDelayer {
    suspend fun wait(delayMs: Long)
}

class KugouCallExecutor(
    private val requestFactory: KugouRequestFactory,
    private val transport: KugouTransport,
    private val responseDecoder: KugouResponseDecoder = KugouResponseDecoder(),
    private val retryDelayer: KugouRetryDelayer = KugouRetryDelayer { delay(it) },
) {
    suspend fun executeJson(
        spec: KugouRequestSpec,
        context: KugouRequestContext,
    ): KugouProtocolResult {
        require(spec.responseFormat == KugouResponseFormat.Json) { "executeJson requires a JSON response" }
        var attempt = 1
        while (true) {
            val result =
                when (val transportResult = transport.execute(requestFactory.prepare(spec, context))) {
                    is KugouTransportResult.Success -> responseDecoder.decode(transportResult.response)
                    is KugouTransportResult.Failure -> KugouProtocolResult.Failure(transportResult.error)
                }
            val error = (result as? KugouProtocolResult.Failure)?.error
            if (attempt >= MAX_ATTEMPTS || spec.retryMode != KugouRetryMode.IdempotentRead || !shouldRetry(error)) {
                return result
            }
            retryDelayer.wait(BASE_DELAY_MS shl (attempt - 1))
            attempt += 1
        }
    }

    private fun shouldRetry(error: KugouError?): Boolean =
        when (error) {
            KugouError.Network(KugouError.Network.Kind.Timeout) -> true
            is KugouError.Http -> error.statusCode in 500..599
            else -> false
        }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val BASE_DELAY_MS = 250L
    }
}
