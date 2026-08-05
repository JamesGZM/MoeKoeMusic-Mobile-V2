package cn.james.music.data.kugou

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.session.KugouSessionStorageException
import cn.james.music.kugou.api.session.KugouSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedKugouSessionStore
    @Inject
    constructor(
        @param:KugouSessionDataStore private val dataStore: DataStore<Preferences>,
        private val cipher: KugouSessionCipher,
    ) : KugouSessionStore {
        private val json = Json { ignoreUnknownKeys = true }
        private val mutex = Mutex()

        override suspend fun read(): KugouSessionSnapshot? =
            mutex.withLock {
                val envelope = storage { dataStore.data.first()[ENCRYPTED_SESSION] } ?: return null
                try {
                    decodeSnapshot(cipher.decrypt(decodeEnvelope(envelope)).decodeToString())
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    recoverUnreadableSession()
                    null
                }
            }

        override suspend fun write(snapshot: KugouSessionSnapshot) =
            mutex.withLock {
                val plaintext = encodeSnapshot(snapshot).encodeToByteArray()
                try {
                    val envelope = storage { encodeEnvelope(cipher.encrypt(plaintext)) }
                    storage { dataStore.edit { preferences -> preferences[ENCRYPTED_SESSION] = envelope } }
                } finally {
                    plaintext.fill(0)
                }
                Unit
            }

        override suspend fun clear() =
            mutex.withLock {
                storage { dataStore.edit { preferences -> preferences.remove(ENCRYPTED_SESSION) } }
                storage { cipher.clearKey() }
            }

        private suspend fun recoverUnreadableSession() {
            storage { dataStore.edit { preferences -> preferences.remove(ENCRYPTED_SESSION) } }
            storage { cipher.clearKey() }
        }

        private fun encodeSnapshot(snapshot: KugouSessionSnapshot): String =
            buildJsonObject {
                put("version", SNAPSHOT_VERSION)
                put(
                    "identity",
                    buildJsonObject {
                        put("guid", snapshot.identity.guid)
                        put("mid", snapshot.identity.mid)
                    },
                )
                put("dfid", snapshot.dfid)
                put("token", snapshot.token)
                put("userId", snapshot.userId)
                put(
                    "cookies",
                    buildJsonObject {
                        snapshot.cookies.asMap().forEach { (name, value) -> put(name, value) }
                    },
                )
            }.toString()

        private fun decodeSnapshot(value: String): KugouSessionSnapshot {
            val body = json.parseToJsonElement(value).jsonObject
            require(body["version"]?.jsonPrimitive?.content == SNAPSHOT_VERSION.toString()) { "Unsupported session version" }
            val identity = requireNotNull(body["identity"]).jsonObject
            val guid = requireNotNull(identity["guid"]?.jsonPrimitive?.contentOrNull).also { require(it.isNotBlank()) }
            val mid = requireNotNull(identity["mid"]?.jsonPrimitive?.contentOrNull).also { require(it.isNotBlank()) }
            val cookies =
                body["cookies"]
                    ?.jsonObject
                    ?.mapValues { (_, element) -> element.jsonPrimitive.content }
                    .orEmpty()
            return KugouSessionSnapshot(
                identity = KugouDeviceIdentity(guid = guid, mid = mid),
                dfid = body.optionalString("dfid"),
                token = body.optionalString("token"),
                userId = body.optionalString("userId"),
                cookies = KugouCookies.from(cookies),
            )
        }

        private fun kotlinx.serialization.json.JsonObject.optionalString(name: String): String? =
            this[name]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)

        private fun encodeEnvelope(payload: EncryptedSessionPayload): String =
            listOf(
                ENVELOPE_VERSION.toString(),
                BASE64_ENCODER.encodeToString(payload.initializationVector),
                BASE64_ENCODER.encodeToString(payload.ciphertext),
            ).joinToString(ENVELOPE_SEPARATOR)

        private fun decodeEnvelope(value: String): EncryptedSessionPayload {
            val parts = value.split(ENVELOPE_SEPARATOR)
            require(parts.size == 3 && parts[0] == ENVELOPE_VERSION.toString()) { "Unsupported encrypted envelope" }
            return EncryptedSessionPayload(
                initializationVector = BASE64_DECODER.decode(parts[1]),
                ciphertext = BASE64_DECODER.decode(parts[2]),
            )
        }

        private suspend fun <T> storage(block: suspend () -> T): T =
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: KugouSessionStorageException) {
                throw failure
            } catch (failure: Exception) {
                throw KugouSessionStorageException(failure)
            }

        internal companion object {
            val ENCRYPTED_SESSION = stringPreferencesKey("encrypted_session_v1")
            const val SNAPSHOT_VERSION = 1
            const val ENVELOPE_VERSION = 1
            const val ENVELOPE_SEPARATOR = "."
            val BASE64_ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
            val BASE64_DECODER: Base64.Decoder = Base64.getUrlDecoder()
        }
    }
