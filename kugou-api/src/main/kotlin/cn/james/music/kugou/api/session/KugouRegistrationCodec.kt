package cn.james.music.kugou.api.session

import cn.james.music.kugou.api.crypto.KugouPlaylistCrypto
import cn.james.music.kugou.api.crypto.KugouRsa
import cn.james.music.kugou.api.crypto.toHex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.security.SecureRandom
import java.util.Base64

data class KugouDeviceProfile(
    val availableRamBytes: Long,
    val availableInternalBytes: Long,
    val availableExternalBytes: Long,
    val brand: String,
    val buildSerial: String,
    val device: String,
    val manufacturer: String,
)

fun interface KugouDeviceProfileProvider {
    suspend fun load(): KugouDeviceProfile
}

internal data class KugouRegistrationRequest(
    val encryptedBody: ByteArray,
    val encryptedIdentity: String,
    val responseKey: String,
) {
    override fun toString(): String =
        "KugouRegistrationRequest(bodyBytes=${encryptedBody.size}, identityBytes=${encryptedIdentity.length / 2}, key=<redacted>)"
}

internal sealed interface KugouRegistrationDecodeResult {
    data class Success(
        val dfid: String,
    ) : KugouRegistrationDecodeResult

    data class Failure(
        val reason: KugouInitializationError.Protocol.Reason,
    ) : KugouRegistrationDecodeResult
}

internal fun interface KugouRegistrationKeyProvider {
    fun create(): String
}

internal class KugouRegistrationCodec(
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val keyProvider: KugouRegistrationKeyProvider = KugouRegistrationKeyProvider(::randomRegistrationKey),
) {
    fun encode(
        profile: KugouDeviceProfile,
        session: KugouSessionSnapshot,
    ): KugouRegistrationRequest {
        val responseKey = keyProvider.create()
        require(responseKey.length == 6) { "Registration key must contain six characters" }
        val body = deviceBody(profile, session.identity).toString()
        val encryptedBody = KugouPlaylistCrypto.encryptBase64(body, responseKey).encodeToByteArray()
        val identityBody =
            buildJsonObject {
                put("aes", responseKey)
                put("uid", session.userId?.toLongOrNull() ?: 0L)
                put("token", session.token.orEmpty())
            }.toString()
        val encryptedIdentity = KugouRsa.encryptPkcs1(identityBody.encodeToByteArray(), STANDARD_REGISTER_PUBLIC_KEY).toHex()
        return KugouRegistrationRequest(encryptedBody, encryptedIdentity, responseKey)
    }

    fun decode(
        encryptedBody: ByteArray,
        responseKey: String,
    ): KugouRegistrationDecodeResult =
        runCatching {
            val base64 = Base64.getEncoder().encodeToString(encryptedBody)
            val plaintext = KugouPlaylistCrypto.decryptBase64(base64, responseKey)
            val body = json.parseToJsonElement(plaintext).jsonObject
            if (body["status"]?.jsonPrimitive?.intOrNull != 1) {
                return KugouRegistrationDecodeResult.Failure(
                    KugouInitializationError.Protocol.Reason.RegistrationRejected,
                )
            }
            val dfid =
                body["data"]
                    ?.jsonObject
                    ?.get("dfid")
                    ?.jsonPrimitive
                    ?.contentOrNull
            if (dfid.isNullOrBlank()) {
                KugouRegistrationDecodeResult.Failure(KugouInitializationError.Protocol.Reason.MissingDfid)
            } else {
                KugouRegistrationDecodeResult.Success(dfid)
            }
        }.getOrElse {
            KugouRegistrationDecodeResult.Failure(KugouInitializationError.Protocol.Reason.MalformedResponse)
        }

    private fun deviceBody(
        profile: KugouDeviceProfile,
        identity: KugouDeviceIdentity,
    ) = buildJsonObject {
        put("availableRamSize", profile.availableRamBytes)
        put("availableRomSize", profile.availableInternalBytes)
        put("availableSDSize", profile.availableExternalBytes)
        put("basebandVer", "")
        put("batteryLevel", 100)
        put("batteryStatus", 3)
        put("brand", profile.brand)
        put("buildSerial", profile.buildSerial)
        put("device", profile.device)
        put("imei", identity.guid)
        put("imsi", "")
        put("manufacturer", profile.manufacturer)
        put("uuid", identity.guid)
        SENSOR_NAMES.forEach { name ->
            put(name, false)
            put("${name}Value", "")
        }
    }

    private companion object {
        val SENSOR_NAMES =
            listOf("accelerometer", "gravity", "gyroscope", "light", "magnetic", "orientation", "pressure", "step_counter", "temperature")

        const val STANDARD_REGISTER_PUBLIC_KEY =
            """-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDIAG7QOELSYoIJvTFJhMpe1s/gbjDJX51HBNnEl5HXqTW6lQ7LC8jr9fWZTwusknp+sVGzwd40MwP6U5yDE27M/X1+UR4tvOGOqp94TJtQ1EPnWGWXngpeIW5GxoQGao1rmYWAu6oi1z9XkChrsUdC6DJE5E221wf/4WLFxwAtRQIDAQAB
-----END PUBLIC KEY-----"""

        fun randomRegistrationKey(): String {
            val random = SecureRandom()
            return buildString(6) {
                repeat(6) { append(REGISTRATION_KEY_ALPHABET[random.nextInt(REGISTRATION_KEY_ALPHABET.length)]) }
            }
        }

        const val REGISTRATION_KEY_ALPHABET = "1234567890abcdefghijklmnopqrstuvwxyz"
    }
}
