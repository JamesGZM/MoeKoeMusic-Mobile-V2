package cn.james.music.kugou.api.transport

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class OkHttpKugouTransport(
    private val client: OkHttpClient = createKugouHttpClient(),
) : KugouTransport {
    override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request.toOkHttpRequest())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(KugouTransportResult.Failure(e.toNetworkError()))
                        }
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use {
                            val result =
                                runCatching {
                                    KugouRawResponse(
                                        statusCode = response.code,
                                        headers = response.headers.toMultimap(),
                                        body = response.body.bytes(),
                                    )
                                }.fold(
                                    onSuccess = KugouTransportResult::Success,
                                    onFailure = { error ->
                                        KugouTransportResult.Failure(
                                            (error as? IOException ?: IOException(error)).toNetworkError(),
                                        )
                                    },
                                )
                            if (continuation.isActive) continuation.resume(result)
                        }
                    }
                },
            )
        }

    private fun KugouPreparedRequest.toOkHttpRequest(): Request {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder().encodedPath(path)
        query.forEach(urlBuilder::addQueryParameter)
        val builder = Request.Builder().url(urlBuilder.build())
        headers.forEach(builder::header)
        when (method) {
            KugouHttpMethod.Get -> {
                require(body == null) { "GET requests cannot contain a body" }
                builder.get()
            }

            KugouHttpMethod.Post -> {
                val contentType = headers.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value
                builder.post((body ?: byteArrayOf()).toRequestBody(contentType?.toMediaTypeOrNull()))
            }
        }
        return builder.build()
    }
}

fun createKugouHttpClient(): OkHttpClient =
    OkHttpClient
        .Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

private fun IOException.toNetworkError(): KugouError.Network =
    KugouError.Network(
        when (this) {
            is SocketTimeoutException -> KugouError.Network.Kind.Timeout
            is UnknownHostException -> KugouError.Network.Kind.Offline
            is ConnectException -> KugouError.Network.Kind.Connection
            else -> KugouError.Network.Kind.Connection
        },
    )
