package cn.james.music.kugou.api.session

import cn.james.music.kugou.api.endpoint.KugouEndpoints
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface KugouSessionProvider {
    suspend fun initialize(): KugouInitializationResult
}

interface KugouSessionObserver {
    val sessions: StateFlow<KugouSessionSnapshot?>
}

class KugouAnonymousSessionInitializer internal constructor(
    private val store: KugouSessionStore,
    private val identityFactory: KugouDeviceIdentityFactory,
    private val profileProvider: KugouDeviceProfileProvider,
    private val requestFactory: KugouRequestFactory,
    private val transport: KugouTransport,
    private val registrationCodec: KugouRegistrationCodec = KugouRegistrationCodec(),
) : KugouSessionProvider,
    KugouSessionObserver,
    KugouSessionMutator {
    constructor(
        store: KugouSessionStore,
        identityFactory: KugouDeviceIdentityFactory,
        profileProvider: KugouDeviceProfileProvider,
        requestFactory: KugouRequestFactory,
        transport: KugouTransport,
    ) : this(
        store = store,
        identityFactory = identityFactory,
        profileProvider = profileProvider,
        requestFactory = requestFactory,
        transport = transport,
        registrationCodec = KugouRegistrationCodec(),
    )

    private val mutex = Mutex()
    private val mutableSessions = MutableStateFlow<KugouSessionSnapshot?>(null)
    private var current: KugouSessionSnapshot? = null
    private var inFlight: CompletableDeferred<KugouInitializationResult>? = null

    override val sessions: StateFlow<KugouSessionSnapshot?> = mutableSessions.asStateFlow()

    override suspend fun initialize(): KugouInitializationResult {
        val pending =
            mutex.withLock {
                current?.let { return KugouInitializationResult.Ready(it) }
                inFlight?.let { return@withLock Pending(it, leader = false) }
                Pending(CompletableDeferred<KugouInitializationResult>().also { inFlight = it }, leader = true)
            }
        if (!pending.leader) return pending.result.await()

        return try {
            val result = initializeOnce()
            mutex.withLock {
                if (result is KugouInitializationResult.Ready) {
                    current = result.session
                    mutableSessions.value = result.session
                }
                inFlight = null
            }
            pending.result.complete(result)
            result
        } catch (cancellation: CancellationException) {
            mutex.withLock { inFlight = null }
            pending.result.cancel(cancellation)
            throw cancellation
        } catch (failure: Throwable) {
            mutex.withLock { inFlight = null }
            pending.result.completeExceptionally(failure)
            throw failure
        }
    }

    override suspend fun replace(snapshot: KugouSessionSnapshot): KugouSessionMutationResult =
        mutex.withLock {
            val active = current
            if (active != null) {
                require(active.identity == snapshot.identity) { "Session replacement cannot change device identity" }
            }
            try {
                store.write(snapshot)
                current = snapshot
                mutableSessions.value = snapshot
                KugouSessionMutationResult.Updated(snapshot)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: KugouSessionStorageException) {
                KugouSessionMutationResult.StorageFailure
            }
        }

    suspend fun clear() {
        mutex.withLock {
            store.clear()
            current = null
            inFlight?.cancel()
            inFlight = null
            mutableSessions.value = null
        }
    }

    private suspend fun initializeOnce(): KugouInitializationResult {
        val stored =
            try {
                store.read()
            } catch (_: KugouSessionStorageException) {
                return KugouInitializationResult.Failure(KugouInitializationError.StorageRead)
            }
        val session = stored ?: KugouSessionSnapshot(identityFactory.create())
        if (stored == null && !write(session)) {
            return KugouInitializationResult.Failure(KugouInitializationError.StorageWrite)
        }
        if (!session.dfid.isNullOrBlank()) return KugouInitializationResult.Ready(session)

        val profile =
            try {
                profileProvider.load()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                return KugouInitializationResult.Failure(
                    KugouInitializationError.Protocol(KugouInitializationError.Protocol.Reason.DeviceProfileUnavailable),
                )
            }
        val registration =
            try {
                registrationCodec.encode(profile, session)
            } catch (_: Throwable) {
                return KugouInitializationResult.Failure(
                    KugouInitializationError.Protocol(KugouInitializationError.Protocol.Reason.EncryptionFailed),
                )
            }
        val spec = KugouEndpoints.registerDevice(registration.encryptedBody, registration.encryptedIdentity)
        val response =
            when (val result = transport.execute(requestFactory.prepare(spec, session.requestContext()))) {
                is KugouTransportResult.Failure -> return KugouInitializationResult.Failure(KugouInitializationError.Network(result.error))
                is KugouTransportResult.Success -> result.response
            }
        if (response.statusCode !in 200..299) {
            return KugouInitializationResult.Failure(KugouInitializationError.Http(response.statusCode))
        }
        return when (val decoded = registrationCodec.decode(response.body, registration.responseKey)) {
            is KugouRegistrationDecodeResult.Failure -> {
                KugouInitializationResult.Failure(KugouInitializationError.Protocol(decoded.reason))
            }

            is KugouRegistrationDecodeResult.Success -> {
                val responseCookies = response.headerValues("set-cookie")
                val mergedCookies =
                    session.cookies
                        .mergeSetCookie(responseCookies)
                        .asMap()
                        .toMutableMap()
                        .apply { put("dfid", decoded.dfid) }
                        .let(KugouCookies::from)
                val ready = session.copy(dfid = decoded.dfid, cookies = mergedCookies)
                if (!write(ready)) {
                    KugouInitializationResult.Failure(KugouInitializationError.StorageWrite)
                } else {
                    KugouInitializationResult.Ready(ready)
                }
            }
        }
    }

    private suspend fun write(snapshot: KugouSessionSnapshot): Boolean =
        try {
            store.write(snapshot)
            true
        } catch (_: KugouSessionStorageException) {
            false
        }

    private fun cn.james.music.kugou.api.transport.KugouRawResponse.headerValues(name: String): List<String> =
        headers.entries
            .firstOrNull { (key) -> key.equals(name, ignoreCase = true) }
            ?.value
            .orEmpty()

    private data class Pending(
        val result: CompletableDeferred<KugouInitializationResult>,
        val leader: Boolean,
    )
}
