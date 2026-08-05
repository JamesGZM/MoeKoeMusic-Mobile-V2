package cn.james.music.data.kugou

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.session.KugouDeviceIdentity
import cn.james.music.kugou.api.session.KugouSessionSnapshot
import cn.james.music.kugou.api.session.KugouSessionStorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

class EncryptedKugouSessionStoreTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun encryptedRoundTripDoesNotPersistSensitivePlaintext() =
        withFixture {
            store.write(FIXTURE_SESSION)

            assertEquals(FIXTURE_SESSION, store.read())
            val storedBytes = file.readBytes().decodeToString()
            SENSITIVE_VALUES.forEach { value -> assertFalse("Found sensitive plaintext in DataStore", storedBytes.contains(value)) }
        }

    @Test
    fun unreadableCiphertextIsRemovedAndKeyIsReset() =
        withFixture {
            dataStore.edit { preferences -> preferences[EncryptedKugouSessionStore.ENCRYPTED_SESSION] = "invalid-envelope" }

            assertNull(store.read())
            assertNull(dataStore.data.first()[EncryptedKugouSessionStore.ENCRYPTED_SESSION])
            assertEquals(1, cipher.clearCalls)
        }

    @Test
    fun decryptionFailureRecoversAsSignedOutSession() =
        withFixture {
            store.write(FIXTURE_SESSION)
            cipher.failDecryption = true

            assertNull(store.read())
            assertEquals(1, cipher.clearCalls)
        }

    @Test
    fun clearRemovesCiphertextAndKey() =
        withFixture {
            store.write(FIXTURE_SESSION)

            store.clear()

            assertNull(store.read())
            assertEquals(1, cipher.clearCalls)
        }

    @Test
    fun encryptionFailureIsMappedToStorageError() {
        val fixture = fixture()
        fixture.cipher.failEncryption = true
        try {
            assertThrows(KugouSessionStorageException::class.java) {
                runBlocking { fixture.store.write(FIXTURE_SESSION) }
            }
        } finally {
            fixture.scope.cancel()
        }
    }

    @Test
    fun coroutineCancellationIsNotMappedToStorageError() {
        val fixture = fixture()
        fixture.cipher.cancelEncryption = true
        try {
            assertThrows(CancellationException::class.java) {
                runBlocking { fixture.store.write(FIXTURE_SESSION) }
            }
        } finally {
            fixture.scope.cancel()
        }
    }

    private fun withFixture(block: suspend Fixture.() -> Unit) {
        val fixture = fixture()
        try {
            runBlocking { fixture.block() }
        } finally {
            fixture.scope.cancel()
        }
    }

    private fun fixture(): Fixture {
        val file = temporaryFolder.newFile("session-${UUID.randomUUID()}.preferences_pb")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val cipher = FakeCipher()
        return Fixture(
            scope = scope,
            file = file,
            dataStore = dataStore,
            cipher = cipher,
            store = EncryptedKugouSessionStore(dataStore, cipher),
        )
    }

    private data class Fixture(
        val scope: CoroutineScope,
        val file: java.io.File,
        val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        val cipher: FakeCipher,
        val store: EncryptedKugouSessionStore,
    )

    private class FakeCipher : KugouSessionCipher {
        var failEncryption = false
        var failDecryption = false
        var cancelEncryption = false
        var clearCalls = 0

        override fun encrypt(plaintext: ByteArray): EncryptedSessionPayload {
            if (cancelEncryption) throw CancellationException("fixture")
            if (failEncryption) throw KugouSessionCipherException(IllegalStateException("fixture"))
            return EncryptedSessionPayload(
                initializationVector = ByteArray(12) { it.toByte() },
                ciphertext = plaintext.map { byte -> (byte.toInt() xor MASK).toByte() }.toByteArray(),
            )
        }

        override fun decrypt(payload: EncryptedSessionPayload): ByteArray {
            if (failDecryption) throw KugouSessionCipherException(IllegalStateException("fixture"))
            return payload.ciphertext.map { byte -> (byte.toInt() xor MASK).toByte() }.toByteArray()
        }

        override fun clearKey() {
            clearCalls += 1
        }

        private companion object {
            const val MASK = 0x5A
        }
    }

    private companion object {
        val FIXTURE_SESSION =
            KugouSessionSnapshot(
                identity = KugouDeviceIdentity(guid = "fixture-guid-sensitive", mid = "fixture-mid-sensitive"),
                dfid = "fixture-dfid-sensitive",
                token = "fixture-token-sensitive",
                userId = "123456",
                cookies = KugouCookies.from(mapOf("session" to "fixture-cookie-sensitive")),
            )
        val SENSITIVE_VALUES =
            listOf(
                FIXTURE_SESSION.identity.guid,
                FIXTURE_SESSION.identity.mid,
                requireNotNull(FIXTURE_SESSION.dfid),
                requireNotNull(FIXTURE_SESSION.token),
                requireNotNull(FIXTURE_SESSION.cookies.value("session")),
            )
    }
}
