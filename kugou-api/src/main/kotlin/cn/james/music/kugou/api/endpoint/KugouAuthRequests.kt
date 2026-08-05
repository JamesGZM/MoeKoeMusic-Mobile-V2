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

    fun loginWithPassword(
        username: String,
        password: String,
    ): KugouAuthRequest {
        require(username.isNotBlank()) { "Username must not be blank" }
        require(password.isNotBlank()) { "Password must not be blank" }

        val clientTime = clock.now()
        val temporaryKey = keyProvider.create()
        val encrypted =
            KugouAuthCrypto.encryptJson(
                buildJsonObject {
                    put("pwd", password)
                    put("code", "")
                    put("clienttime_ms", clientTime)
                }.toString(),
                temporaryKey,
            )
        val body =
            buildJsonObject {
                put("plat", 1)
                put("support_multi", 1)
                put("clienttime_ms", clientTime)
                put("t1", PASSWORD_T1)
                put("t2", PASSWORD_T2)
                put("t3", STANDARD_T3)
                put("username", username.trim())
                put("params", encrypted.ciphertextHex)
                put(
                    "pk",
                    KugouAuthCrypto.encryptKeyEnvelope(
                        buildJsonObject {
                            put("clienttime_ms", clientTime)
                            put("key", temporaryKey)
                        }.toString(),
                    ),
                )
            }
        return KugouAuthRequest(
            spec =
                KugouRequestSpec(
                    id = "login_password",
                    method = KugouHttpMethod.Post,
                    path = "/v9/login_by_pwd",
                    headers =
                        mapOf(
                            "Content-Type" to "application/json",
                            "x-router" to "login.user.kugou.com",
                        ),
                    body = Json.encodeToString(body).encodeToByteArray(),
                    serviceErrorPolicy = KugouServiceErrorPolicy.DecodeAuthRiskByEndpoint,
                ),
            temporaryKey = temporaryKey,
        )
    }

    fun getRiskMethod(
        eventId: String,
        userId: String?,
    ): KugouRequestSpec {
        require(eventId.isNotBlank()) { "Risk event id must not be blank" }
        val body =
            buildJsonObject {
                put("eventid", eventId)
                put("userid", userId.asProtocolUserId())
                put("platid", 2)
                put("rtype", 1)
                put("wasm", 1)
                put("i", "")
                put("sid", "")
                put("edt", "")
            }
        return KugouRequestSpec(
            id = "get_risk_method",
            method = KugouHttpMethod.Post,
            path = "/verifyservice/v3/get_verify_info",
            headers = mapOf("Content-Type" to "application/json"),
            body = Json.encodeToString(body).encodeToByteArray(),
            serviceErrorPolicy = KugouServiceErrorPolicy.DecodeByEndpoint,
        )
    }

    fun verifyRisk(
        challenge: KugouRiskChallengeDto,
        proof: KugouRiskProofDto,
        userId: String?,
    ): KugouRequestSpec {
        require(challenge.eventId.isNotBlank()) { "Risk event id must not be blank" }

        val temporaryKey = keyProvider.create()
        val encryptedPayload =
            when (proof) {
                is KugouRiskProofDto.Sms -> {
                    require(proof.code.isNotBlank()) { "Risk SMS code must not be blank" }
                    KugouAuthCrypto
                        .encryptJson(
                            buildJsonObject { put("code", proof.code) }.toString(),
                            temporaryKey,
                        ).ciphertextHex
                }

                is KugouRiskProofDto.Tencent -> {
                    require(proof.ticket.isNotBlank()) { "Tencent ticket must not be blank" }
                    require(proof.randomString.isNotBlank()) { "Tencent random string must not be blank" }
                    require(proof.appId.isNotBlank()) { "Tencent app id must not be blank" }
                    KugouAuthCrypto.encryptJson("{}", temporaryKey).ciphertextHex
                }
            }
        val body =
            buildJsonObject {
                put("eventid", challenge.eventId)
                put("userid", userId.asProtocolUserId())
                put("platid", 2)
                put("v_type", proof.type)
                put("wasm", 1)
                put("i", "")
                put("sid", challenge.sid.orEmpty())
                put("edt", challenge.edt.orEmpty())
                when (proof) {
                    is KugouRiskProofDto.Sms -> {
                        put("code", proof.code)
                    }

                    is KugouRiskProofDto.Tencent -> {
                        put(
                            "verifycode",
                            "KGCodeTX|" +
                                buildJsonObject {
                                    put("ticket", proof.ticket)
                                    put("randstr", proof.randomString)
                                    put("txappid", proof.appId)
                                },
                        )
                    }
                }
                put(
                    "pk",
                    KugouAuthCrypto.encryptKeyEnvelope(
                        buildJsonObject { put("key", temporaryKey) }.toString(),
                    ),
                )
                put("params", encryptedPayload)
            }
        return KugouRequestSpec(
            id = "verify_risk",
            method = KugouHttpMethod.Post,
            baseUrl = "https://verifyservice.kugou.com",
            path = "/v4/verify_user_info",
            params = mapOf("clientver" to RISK_CLIENT_VERSION),
            headers = mapOf("Content-Type" to "application/json"),
            body = Json.encodeToString(body).encodeToByteArray(),
            serviceErrorPolicy = KugouServiceErrorPolicy.DecodeByEndpoint,
        )
    }

    private companion object {
        val MOBILE_PATTERN = Regex("^1\\d{10}$")
        const val STANDARD_T3 = "MCwwLDAsMCwwLDAsMCwwLDA="
        const val LOGIN_USER_AGENT = "Android16-1070-11440-130-0-LOGIN-wifi"
        const val RISK_CLIENT_VERSION = "11510"
        const val PASSWORD_T1 =
            "562a6f12a6e803453647d16a08f5f0c2ff7eee692cba2ab74cc4c8ab47fc467561a7c6b586ce7dc46a63613b246737c03a1dc8f8d162d8ce1d2c71893d19f1d4b797685a4c6d3d81341cbde65e488c4829a9b4d42ef2df470eb102979fa5adcdd9b4eecfea8b909ff7599abeb49867640f10c3c70fc444effca9d15db44a9a6c907731e2bb0f22cd9b3536380169995693e5f0e2424e3378097d3813186e3fe96bbe7023808a0981b4e2b6135a76faac"
        const val PASSWORD_T2 =
            "31c4daf4cf480169ccea1cb7d4a209295865a9d2b788510301694db229b87807469ea0d41b4d4b9173c2151da7294aeebfc9738df154bbdf11a4e117bb5dff6a3af8ce5ce333e681c1f29a44038f27567d58992eb81283e080778ac77db1400fdf49b7cf7e26be2e5af4da7830cc3be4"

        fun randomTemporaryKey(): String =
            buildString(TEMPORARY_KEY_LENGTH) {
                repeat(TEMPORARY_KEY_LENGTH) { append(TEMPORARY_KEY_ALPHABET[SECURE_RANDOM.nextInt(TEMPORARY_KEY_ALPHABET.length)]) }
            }

        val SECURE_RANDOM = SecureRandom()
        const val TEMPORARY_KEY_LENGTH = 16
        const val TEMPORARY_KEY_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}

private fun String?.asProtocolUserId(): Long = this?.toLongOrNull()?.takeIf { it > 0 } ?: 0L
