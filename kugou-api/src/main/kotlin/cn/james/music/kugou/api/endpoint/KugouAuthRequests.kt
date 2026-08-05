package cn.james.music.kugou.api.endpoint

import cn.james.music.kugou.api.config.KugouPlatformConfig
import cn.james.music.kugou.api.crypto.KugouAuthCrypto
import cn.james.music.kugou.api.signing.KugouRequestSigner
import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouRequestSpec
import cn.james.music.kugou.api.transport.KugouServiceErrorPolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.SecureRandom

internal fun interface EpochMillisProvider {
    fun now(): Long
}

internal fun interface KugouAuthTemporaryKeyProvider {
    fun create(): String
}

internal data class KugouAuthRequest(
    val spec: KugouRequestSpec,
    val temporaryKey: String,
) {
    override fun toString(): String = "KugouAuthRequest(spec=$spec, temporaryKey=<redacted>)"
}

internal class KugouAuthRequestBuilder(
    private val platform: KugouPlatformConfig = KugouPlatformConfig.Standard,
    private val signer: KugouRequestSigner = KugouRequestSigner(),
    private val clock: EpochMillisProvider = EpochMillisProvider(System::currentTimeMillis),
    private val keyProvider: KugouAuthTemporaryKeyProvider = KugouAuthTemporaryKeyProvider(::randomTemporaryKey),
) {
    fun loginWithMobileCode(
        mobile: String,
        code: String,
        selectedUserId: String? = null,
    ): KugouAuthRequest {
        require(MOBILE_PATTERN.matches(mobile)) { "Mobile number must contain 11 digits and start with 1" }
        require(code.isNotBlank()) { "Verification code must not be blank" }
        selectedUserId?.let { require(it.isNotBlank() && it != "0") { "Selected user id must be valid" } }

        val clientTime = clock.now()
        val temporaryKey = keyProvider.create()
        val encrypted =
            KugouAuthCrypto.encryptJson(
                buildJsonObject {
                    put("mobile", mobile)
                    put("code", code)
                }.toString(),
                temporaryKey,
            )
        val body =
            buildJsonObject {
                put("plat", 1)
                put("support_multi", 1)
                put("t1", 0)
                put("t2", 0)
                put("clienttime_ms", clientTime)
                put("mobile", "${mobile.take(2)}*****${mobile.last()}")
                put("key", signer.signParamsKey(clientTime.toString(), platform.appId, platform.clientVersion))
                put(
                    "pk",
                    KugouAuthCrypto.encryptKeyEnvelope(
                        buildJsonObject {
                            put("clienttime_ms", clientTime)
                            put("key", temporaryKey)
                        }.toString(),
                    ),
                )
                put("params", encrypted.ciphertextHex)
                selectedUserId?.let { put("userid", it) }
                put("t3", STANDARD_T3)
            }
        return KugouAuthRequest(
            spec =
                KugouRequestSpec(
                    id = "login_cellphone",
                    method = KugouHttpMethod.Post,
                    baseUrl = "https://loginserviceretry.kugou.com",
                    path = "/v7/login_by_verifycode",
                    headers =
                        mapOf(
                            "Content-Type" to "application/json",
                            "support-calm" to "1",
                            "User-Agent" to LOGIN_USER_AGENT,
                        ),
                    body = Json.encodeToString(body).encodeToByteArray(),
                    serviceErrorPolicy = KugouServiceErrorPolicy.DecodeByEndpoint,
                ),
            temporaryKey = temporaryKey,
        )
    }

    private companion object {
        val MOBILE_PATTERN = Regex("^1\\d{10}$")
        const val STANDARD_T3 = "MCwwLDAsMCwwLDAsMCwwLDA="
        const val LOGIN_USER_AGENT = "Android16-1070-11440-130-0-LOGIN-wifi"

        fun randomTemporaryKey(): String =
            buildString(TEMPORARY_KEY_LENGTH) {
                repeat(TEMPORARY_KEY_LENGTH) { append(TEMPORARY_KEY_ALPHABET[SECURE_RANDOM.nextInt(TEMPORARY_KEY_ALPHABET.length)]) }
            }

        val SECURE_RANDOM = SecureRandom()
        const val TEMPORARY_KEY_LENGTH = 16
        const val TEMPORARY_KEY_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
