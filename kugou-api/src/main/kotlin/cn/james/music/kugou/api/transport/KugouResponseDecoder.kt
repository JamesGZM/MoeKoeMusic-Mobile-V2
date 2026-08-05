package cn.james.music.kugou.api.transport

import cn.james.music.kugou.api.session.KugouCookies
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

class KugouResponseDecoder(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun decode(response: KugouRawResponse): KugouProtocolResult {
        if (response.statusCode !in 200..299) {
            return KugouProtocolResult.Failure(KugouError.Http(response.statusCode))
        }

        response.headerValues("ssa-code").firstOrNull()?.takeIf(String::isNotBlank)?.let {
            return KugouProtocolResult.Failure(KugouError.Risk(it))
        }

        val parsedBody =
            runCatching { json.parseToJsonElement(response.body.decodeToString()) }
                .getOrElse {
                    return KugouProtocolResult.Failure(
                        KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
                    )
                }
        val body =
            parsedBody as? JsonObject
                ?: return KugouProtocolResult.Failure(
                    KugouError.Protocol(KugouError.Protocol.Reason.MalformedResponse),
                )
        serviceError(body)?.let { return KugouProtocolResult.Failure(it) }

        val cookies = KugouCookies.Empty.mergeSetCookie(response.headerValues("set-cookie"))
        return KugouProtocolResult.Success(body = body, responseCookies = cookies)
    }

    private fun serviceError(body: JsonObject): KugouError.Protocol? {
        val status = body.primitive("status")
        val errorCode = body.primitive("error_code")
        val rejected = status?.intOrNull == 0 || errorCode?.intOrNull?.let { it != 0 } == true
        if (!rejected) return null
        val code = errorCode?.contentOrNull ?: status?.contentOrNull
        return KugouError.Protocol(KugouError.Protocol.Reason.ServiceRejected, serviceCode = code)
    }

    private fun JsonObject.primitive(name: String): JsonPrimitive? = get(name) as? JsonPrimitive

    private fun KugouRawResponse.headerValues(name: String): List<String> =
        headers.entries
            .firstOrNull { (key) -> key.equals(name, ignoreCase = true) }
            ?.value
            .orEmpty()
}
