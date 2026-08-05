package cn.james.music.kugou.api.transport

import cn.james.music.kugou.api.config.KugouPlatformConfig
import cn.james.music.kugou.api.signing.KugouRequestSigner
import java.net.URI

fun interface EpochSecondsProvider {
    fun now(): Long
}

class KugouRequestFactory(
    private val platform: KugouPlatformConfig = KugouPlatformConfig.Standard,
    private val signer: KugouRequestSigner = KugouRequestSigner(),
    private val clock: EpochSecondsProvider = EpochSecondsProvider { System.currentTimeMillis() / 1_000L },
) {
    fun prepare(
        spec: KugouRequestSpec,
        context: KugouRequestContext,
    ): KugouPreparedRequest {
        require(spec.id.isNotBlank()) { "Request id must not be blank" }
        require(spec.path.startsWith('/') && '?' !in spec.path && '#' !in spec.path) { "Request path must be absolute and query-free" }
        require(context.mid.isNotBlank()) { "MID must not be blank" }
        val baseUrl = normalizeBaseUrl(spec.baseUrl)

        val params = linkedMapOf<String, String>()
        if (spec.includeDefaultParams) {
            params += defaultParams(context)
        }
        params += spec.params
        if (spec.generateSignKey) {
            val hash = requireNotNull(params["hash"]?.takeIf(String::isNotBlank)) { "signKey requires hash" }
            params["key"] =
                signer.signKey(
                    hash = hash,
                    mid = requireNotNull(params["mid"]),
                    userId = params["userid"] ?: "0",
                    appId = requireNotNull(params["appid"]),
                )
        }
        if ("signature" !in params && spec.signatureMode != KugouSignatureMode.None) {
            params["signature"] = signature(spec.signatureMode, params, spec.body.orEmpty())
        }

        val clientTime = requireNotNull(params["clienttime"]) { "clienttime is required" }
        val dfid = context.dfid?.takeIf(String::isNotBlank) ?: "-"
        val headers =
            linkedMapOf(
                "User-Agent" to platform.userAgent,
            ).apply {
                spec.headers.forEach(::putHeader)
                putHeader("dfid", dfid)
                putHeader("clienttime", clientTime)
                putHeader("mid", context.mid)
                putHeader("kg-rc", "1")
                putHeader("kg-thash", "5d816a0")
                putHeader("kg-rec", "1")
                putHeader("kg-rf", "B9EDA08A64250DEFFBCADDEE00F8F25F")
                context.cookies
                    .headerValue()
                    .takeIf(String::isNotEmpty)
                    ?.let { putHeader("Cookie", it) }
            }

        return KugouPreparedRequest(
            id = spec.id,
            method = spec.method,
            baseUrl = baseUrl,
            path = spec.path,
            query = params.toMap(),
            headers = headers.toMap(),
            body = spec.body?.copyOf(),
            responseFormat = spec.responseFormat,
        )
    }

    private fun defaultParams(context: KugouRequestContext): Map<String, String> =
        linkedMapOf(
            "dfid" to (context.dfid?.takeIf(String::isNotBlank) ?: "-"),
            "mid" to context.mid,
            "uuid" to "-",
            "appid" to platform.appId,
            "clientver" to platform.clientVersion,
            "clienttime" to clock.now().toString(),
        ).apply {
            context.token?.takeIf(String::isNotBlank)?.let { put("token", it) }
            context.userId?.takeIf { it.isNotBlank() && it != "0" }?.let { put("userid", it) }
        }

    private fun signature(
        mode: KugouSignatureMode,
        params: Map<String, String>,
        body: ByteArray,
    ): String =
        when (mode) {
            KugouSignatureMode.Android -> signer.androidSignature(params, body)
            KugouSignatureMode.Register -> signer.registerSignature(params)
            KugouSignatureMode.Web -> signer.webSignature(params)
            KugouSignatureMode.None -> error("None is handled before signing")
        }

    private fun normalizeBaseUrl(value: String): String {
        val uri = runCatching { URI(value) }.getOrElse { throw IllegalArgumentException("Invalid base URL", it) }
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) { "Only HTTPS endpoints are allowed" }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null && (uri.path.isNullOrEmpty() || uri.path == "/")) {
            "Base URL must contain only an HTTPS origin"
        }
        return value.removeSuffix("/")
    }
}

private fun ByteArray?.orEmpty(): ByteArray = this ?: byteArrayOf()

private fun MutableMap<String, String>.putHeader(
    name: String,
    value: String,
) {
    require(name.isNotBlank() && name.all(::isHttpTokenCharacter)) { "Header name contains invalid characters" }
    require(value.none { it == '\r' || it == '\n' }) { "Header value contains invalid characters" }
    keys.firstOrNull { it.equals(name, ignoreCase = true) }?.let(::remove)
    put(name, value)
}

private fun isHttpTokenCharacter(character: Char): Boolean = character.isLetterOrDigit() || character in "!#$%&'*+-.^_`|~"
