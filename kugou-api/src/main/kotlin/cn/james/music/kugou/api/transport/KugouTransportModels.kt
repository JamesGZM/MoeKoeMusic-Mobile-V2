package cn.james.music.kugou.api.transport

import cn.james.music.kugou.api.session.KugouCookies
import kotlinx.serialization.json.JsonElement

enum class KugouHttpMethod {
    Get,
    Post,
}

enum class KugouSignatureMode {
    Android,
    Register,
    Web,
    None,
}

enum class KugouResponseFormat {
    Json,
    Bytes,
}

enum class KugouRetryMode {
    None,
    IdempotentRead,
}

data class KugouRequestContext(
    val mid: String,
    val dfid: String? = null,
    val token: String? = null,
    val userId: String? = null,
    val cookies: KugouCookies = KugouCookies.Empty,
) {
    override fun toString(): String =
        "KugouRequestContext(mid=<redacted>, dfidPresent=${!dfid.isNullOrBlank()}, " +
            "tokenPresent=${!token.isNullOrBlank()}, userIdPresent=${!userId.isNullOrBlank()}, cookies=$cookies)"
}

data class KugouRequestSpec(
    val id: String,
    val method: KugouHttpMethod,
    val path: String,
    val baseUrl: String = "https://gateway.kugou.com",
    val params: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray? = null,
    val signatureMode: KugouSignatureMode = KugouSignatureMode.Android,
    val includeDefaultParams: Boolean = true,
    val generateSignKey: Boolean = false,
    val responseFormat: KugouResponseFormat = KugouResponseFormat.Json,
    val retryMode: KugouRetryMode = KugouRetryMode.None,
) {
    override fun toString(): String =
        "KugouRequestSpec(id=$id, method=$method, path=$path, baseUrl=$baseUrl, " +
            "paramNames=${params.keys.sorted()}, headerNames=${headers.keys.sorted()}, bodyBytes=${body?.size ?: 0}, " +
            "signatureMode=$signatureMode, responseFormat=$responseFormat, retryMode=$retryMode)"
}

data class KugouPreparedRequest(
    val id: String,
    val method: KugouHttpMethod,
    val baseUrl: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: ByteArray?,
    val responseFormat: KugouResponseFormat,
    val retryMode: KugouRetryMode = KugouRetryMode.None,
) {
    override fun toString(): String =
        "KugouPreparedRequest(id=$id, method=$method, baseUrl=$baseUrl, path=$path, " +
            "queryNames=${query.keys.sorted()}, headerNames=${headers.keys.sorted()}, bodyBytes=${body?.size ?: 0}, " +
            "responseFormat=$responseFormat, retryMode=$retryMode)"
}

data class KugouRawResponse(
    val statusCode: Int,
    val headers: Map<String, List<String>>,
    val body: ByteArray,
) {
    override fun toString(): String =
        "KugouRawResponse(statusCode=$statusCode, headerNames=${headers.keys.sorted()}, bodyBytes=${body.size})"
}

sealed interface KugouTransportResult {
    data class Success(
        val response: KugouRawResponse,
    ) : KugouTransportResult

    data class Failure(
        val error: KugouError.Network,
    ) : KugouTransportResult
}

fun interface KugouTransport {
    suspend fun execute(request: KugouPreparedRequest): KugouTransportResult
}

sealed interface KugouError {
    data class Network(
        val kind: Kind,
    ) : KugouError {
        enum class Kind {
            Offline,
            Timeout,
            Connection,
        }
    }

    data class Http(
        val statusCode: Int,
    ) : KugouError

    data class Risk(
        val code: String,
    ) : KugouError

    data class Protocol(
        val reason: Reason,
        val serviceCode: String? = null,
    ) : KugouError {
        enum class Reason {
            MalformedResponse,
            ServiceRejected,
            MissingRequiredField,
        }
    }
}

internal sealed interface KugouProtocolResult {
    data class Success(
        val body: JsonElement,
        val responseCookies: KugouCookies,
    ) : KugouProtocolResult {
        override fun toString(): String = "KugouProtocolResult.Success(bodyType=${body::class.simpleName}, cookies=$responseCookies)"
    }

    data class Failure(
        val error: KugouError,
    ) : KugouProtocolResult
}
