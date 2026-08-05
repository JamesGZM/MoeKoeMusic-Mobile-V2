package cn.james.music.kugou.api

import cn.james.music.kugou.api.crypto.KugouPlaylistCrypto
import cn.james.music.kugou.api.session.KugouAnonymousSessionInitializer
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouDeviceIdentityFactory
import cn.james.music.kugou.api.session.KugouDeviceProfile
import cn.james.music.kugou.api.session.KugouDeviceProfileProvider
import cn.james.music.kugou.api.session.KugouGuidProvider
import cn.james.music.kugou.api.session.KugouInitializationError
import cn.james.music.kugou.api.session.KugouInitializationResult
import cn.james.music.kugou.api.session.KugouRegistrationCodec
import cn.james.music.kugou.api.session.KugouRegistrationKeyProvider
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.session.KugouSessionStorageException
import cn.james.music.kugou.api.session.KugouSessionStore
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouPreparedRequest
import cn.james.music.kugou.api.transport.KugouRawResponse
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouTransport
import cn.james.music.kugou.api.transport.KugouTransportResult
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class KugouAnonymousSessionInitializerTest {
    @Test
    fun registrationPersistsDfidAndCoalescesConcurrentInitialization() =
        runBlocking {
            val store = MemoryStore()
            val transport = RegistrationTransport(delayMs = 25)
            val initializer = initializer(store, transport)

            val first = async { initializer.initialize() }
            val second = async { initializer.initialize() }

            val results = listOf(first.await(), second.await())
            assertTrue(results.all { it is KugouInitializationResult.Ready })
            assertEquals(1, transport.calls)
            assertEquals(FIXTURE_DFID, store.snapshot?.dfid)
            assertEquals(FIXTURE_DFID, store.snapshot?.cookies?.value("dfid"))
            assertFalse(store.snapshot.toString().contains(FIXTURE_DFID))
        }

    @Test
    fun restoredRegisteredSessionDoesNotCallTransport() =
        runBlocking {
            val stored = KugouSessionSnapshot(identity = FIXTURE_IDENTITY, dfid = FIXTURE_DFID)
            val store = MemoryStore(stored)
            val transport = RegistrationTransport()

            val result = initializer(store, transport).initialize()

            assertEquals(KugouInitializationResult.Ready(stored), result)
            assertEquals(0, transport.calls)
        }

    @Test
    fun malformedRegistrationResponseIsTypedAndKeepsGeneratedIdentity() =
        runBlocking {
            val store = MemoryStore()
            val transport = RegistrationTransport(responseBody = "not encrypted".encodeToByteArray())

            val result = initializer(store, transport).initialize()

            assertEquals(
                KugouInitializationResult.Failure(
                    KugouInitializationError.Protocol(KugouInitializationError.Protocol.Reason.MalformedResponse),
                ),
                result,
            )
            assertEquals(FIXTURE_IDENTITY, store.snapshot?.identity)
            assertEquals(null, store.snapshot?.dfid)
        }

    @Test
    fun storageFailureStopsBeforeNetwork() =
        runBlocking {
            val store = MemoryStore(failWrites = true)
            val transport = RegistrationTransport()

            val result = initializer(store, transport).initialize()

            assertEquals(KugouInitializationResult.Failure(KugouInitializationError.StorageWrite), result)
            assertEquals(0, transport.calls)
        }

    @Test
    fun deviceProfileFailureIsTypedAndStopsBeforeNetwork() =
        runBlocking {
            val store = MemoryStore()
            val transport = RegistrationTransport()
            val initializer =
                KugouAnonymousSessionInitializer(
                    store = store,
                    identityFactory = KugouDeviceIdentityFactory(KugouGuidProvider { FIXTURE_GUID }),
                    profileProvider = KugouDeviceProfileProvider { error("fixture") },
                    requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L }),
                    transport = transport,
                )

            val result = initializer.initialize()

            assertEquals(
                KugouInitializationResult.Failure(
                    KugouInitializationError.Protocol(KugouInitializationError.Protocol.Reason.DeviceProfileUnavailable),
                ),
                result,
            )
            assertEquals(0, transport.calls)
        }

    private fun initializer(
        store: KugouSessionStore,
        transport: KugouTransport,
    ) = KugouAnonymousSessionInitializer(
        store = store,
        identityFactory = KugouDeviceIdentityFactory(KugouGuidProvider { FIXTURE_GUID }),
        profileProvider = KugouDeviceProfileProvider { FIXTURE_PROFILE },
        requestFactory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L }),
        transport = transport,
        registrationCodec =
            KugouRegistrationCodec(
                keyProvider = KugouRegistrationKeyProvider { FIXTURE_RESPONSE_KEY },
            ),
    )

    private class MemoryStore(
        initial: KugouSessionSnapshot? = null,
        private val failWrites: Boolean = false,
    ) : KugouSessionStore {
        var snapshot: KugouSessionSnapshot? = initial

        override suspend fun read(): KugouSessionSnapshot? = snapshot

        override suspend fun write(snapshot: KugouSessionSnapshot) {
            if (failWrites) throw KugouSessionStorageException(IllegalStateException("fixture"))
            this.snapshot = snapshot
        }

        override suspend fun clear() {
            snapshot = null
        }
    }

    private class RegistrationTransport(
        private val responseBody: ByteArray = successfulRegistrationBody(),
        private val delayMs: Long = 0,
    ) : KugouTransport {
        var calls = 0

        override suspend fun execute(request: KugouPreparedRequest): KugouTransportResult {
            calls += 1
            if (delayMs > 0) delay(delayMs)
            return KugouTransportResult.Success(
                KugouRawResponse(
                    statusCode = 200,
                    headers = mapOf("Set-Cookie" to listOf("kg_session=fixture; Path=/")),
                    body = responseBody,
                ),
            )
        }
    }

    private companion object {
        const val FIXTURE_GUID = "00000000-0000-4000-8000-000000000001"
        const val FIXTURE_DFID = "fixture-dfid"
        const val FIXTURE_RESPONSE_KEY = "abc123"
        val FIXTURE_IDENTITY = KugouDeviceIdentityFactory(KugouGuidProvider { FIXTURE_GUID }).create()
        val FIXTURE_PROFILE =
            KugouDeviceProfile(
                availableRamBytes = 4_000_000_000,
                availableInternalBytes = 32_000_000_000,
                availableExternalBytes = 0,
                brand = "MoeKoe",
                buildSerial = "unknown",
                device = "android-test",
                manufacturer = "MoeKoe",
            )

        fun successfulRegistrationBody(): ByteArray {
            val encrypted =
                KugouPlaylistCrypto.encryptBase64(
                    """{"status":1,"data":{"dfid":"$FIXTURE_DFID"}}""",
                    FIXTURE_RESPONSE_KEY,
                )
            return Base64.getDecoder().decode(encrypted)
        }
    }
}
