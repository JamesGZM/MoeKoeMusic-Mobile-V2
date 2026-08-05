package cn.james.music.kugou.api.transport

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class KtorKugouTransport(
    private val client: HttpClient = createKugouHttpClient(),
) : KugouTransport {
    constructor(client: OkHttpClient) : this(createKugouHttpClient(client))

    override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult =
        try {
            val response =
                client.request {
                    method =
                        when (request.method) {
                            KugouHttpMethod.Get -> HttpMethod.Get
                            KugouHttpMethod.Post -> HttpMethod.Post
                        }
                    url {
                        takeFrom(request.baseUrl)
                        protocol = URLProtocol.HTTPS
                        encodedPathSegments = request.path.split('/')
                        request.query.forEach(parameters::append)
                    }
                    headers {
                        request.headers.forEach(::append)
                    }
                    if (request.method == KugouHttpMethod.Post) {
                        setBody(request.body ?: byteArrayOf())
                    }
                }
            KugouTransportResult.Success(
                KugouRawResponse(
                    statusCode = response.status.value,
                    headers = response.headers.entries().associate { (name, values) -> name to values },
                    body = response.bodyAsBytes(),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            KugouTransportResult.Failure(error.toNetworkError())
        }
}

fun createKugouHttpClient(preconfiguredClient: OkHttpClient? = null): HttpClient =
    HttpClient(OkHttp) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                },
            )
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
            requestTimeoutMillis = 20_000
        }
        engine {
            if (preconfiguredClient != null) {
                preconfigured = preconfiguredClient
            } else {
                config {
                    connectTimeout(10, TimeUnit.SECONDS)
                    readTimeout(15, TimeUnit.SECONDS)
                    callTimeout(20, TimeUnit.SECONDS)
                    retryOnConnectionFailure(false)
                }
            }
        }
    }

private fun Exception.toNetworkError(): KugouError.Network =
    KugouError.Network(
        when (this) {
            is HttpRequestTimeoutException, is SocketTimeoutException -> KugouError.Network.Kind.Timeout
            is UnknownHostException -> KugouError.Network.Kind.Offline
            is ConnectException, is IOException -> KugouError.Network.Kind.Connection
            else -> KugouError.Network.Kind.Connection
        },
    )
