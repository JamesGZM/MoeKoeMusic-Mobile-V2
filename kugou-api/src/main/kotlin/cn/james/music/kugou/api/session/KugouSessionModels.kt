package cn.james.music.kugou.api.session

import cn.james.music.kugou.api.crypto.KugouMid
import cn.james.music.kugou.api.transport.KugouError
import cn.james.music.kugou.api.transport.KugouRequestContext
import java.util.UUID

data class KugouDeviceIdentity(
    val guid: String,
    val mid: String,
) {
    override fun toString(): String = "KugouDeviceIdentity(<redacted>)"
}

data class KugouSessionSnapshot(
    val identity: KugouDeviceIdentity,
    val dfid: String? = null,
    val token: String? = null,
    val userId: String? = null,
    val cookies: KugouCookies = KugouCookies.Empty,
) {
    fun requestContext(): KugouRequestContext =
        KugouRequestContext(
            mid = identity.mid,
            dfid = dfid,
            token = token,
            userId = userId,
            cookies = cookies,
        )

    override fun toString(): String =
        "KugouSessionSnapshot(identity=$identity, dfidPresent=${!dfid.isNullOrBlank()}, " +
            "tokenPresent=${!token.isNullOrBlank()}, userIdPresent=${!userId.isNullOrBlank()}, cookies=$cookies)"
}

interface KugouSessionStore {
    suspend fun read(): KugouSessionSnapshot?

    suspend fun write(snapshot: KugouSessionSnapshot)

    suspend fun clear()
}

class KugouSessionStorageException(
    cause: Throwable,
) : Exception("Kugou session storage failed", cause)

fun interface KugouGuidProvider {
    fun create(): String
}

class KugouDeviceIdentityFactory(
    private val guidProvider: KugouGuidProvider = KugouGuidProvider { UUID.randomUUID().toString() },
) {
    fun create(): KugouDeviceIdentity {
        val guid = guidProvider.create()
        require(guid.isNotBlank()) { "Generated GUID must not be blank" }
        return KugouDeviceIdentity(guid = guid, mid = KugouMid.fromGuid(guid))
    }
}

sealed interface KugouInitializationResult {
    data class Ready(
        val session: KugouSessionSnapshot,
    ) : KugouInitializationResult

    data class Failure(
        val error: KugouInitializationError,
    ) : KugouInitializationResult
}

sealed interface KugouInitializationError {
    data object StorageRead : KugouInitializationError

    data object StorageWrite : KugouInitializationError

    data class Network(
        val error: KugouError.Network,
    ) : KugouInitializationError

    data class Http(
        val statusCode: Int,
    ) : KugouInitializationError

    data class Protocol(
        val reason: Reason,
    ) : KugouInitializationError {
        enum class Reason {
            DeviceProfileUnavailable,
            EncryptionFailed,
            MalformedResponse,
            RegistrationRejected,
            MissingDfid,
        }
    }
}
