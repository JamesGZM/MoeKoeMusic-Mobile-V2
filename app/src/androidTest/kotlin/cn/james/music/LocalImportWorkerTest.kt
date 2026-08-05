package cn.james.music

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.james.music.core.model.local.ImportCompletionAction
import cn.james.music.core.model.local.LocalImportBatchState
import cn.james.music.core.model.local.LocalImportEntryState
import cn.james.music.core.model.local.LocalImportSource
import cn.james.music.data.local.DebugImportBatch
import cn.james.music.data.local.LocalMusicDebugEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalImportWorkerTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val services: LocalMusicDebugEntryPoint
        get() =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                LocalMusicDebugEntryPoint::class.java,
            )

    @Test
    fun importsEverySupportedFixtureThroughTheRealPipeline() =
        runBlocking {
            val batchId =
                services.localImportGateway().enqueue(
                    FIXTURE_NAMES.map { fixtureUri(it) },
                    LocalImportSource.DocumentPicker,
                    ImportCompletionAction.OpenLocalLibrary,
                )

            val batch = awaitTerminalBatch(batchId)
            assertEquals(LocalImportBatchState.Completed.name, batch.state)
            assertEquals(FIXTURE_NAMES.size, batch.completedCount)
            assertEquals(0, batch.failedCount)
            val entries = services.localImportDebugProbe().entries(batchId)
            assertEquals(FIXTURE_NAMES.size, entries.size)
            entries.forEach { entry ->
                assertTrue(entry.state in setOf(LocalImportEntryState.Imported.name, LocalImportEntryState.Duplicate.name))
                assertNotNull(entry.localMusicId?.takeIf { services.localImportDebugProbe().musicExists(it) })
            }
        }

    @Test
    fun corruptInputFailsWithoutLeavingAFileOrPlaybackItem() =
        runBlocking {
            val musicRoot = musicRoot()
            val filesBefore = musicRoot.audioFiles().toSet()
            val musicCountBefore = services.localImportDebugProbe().musicCount()
            val batchId =
                services.localImportGateway().enqueue(
                    listOf(contentUri("corrupt.mp3")),
                    LocalImportSource.DocumentPicker,
                    ImportCompletionAction.PlayImportedTrack,
                )

            val batch = awaitTerminalBatch(batchId)
            assertTrue(batch.state.startsWith(LocalImportBatchState.Failed.name))
            assertEquals(1, batch.failedCount)
            assertEquals(
                LocalImportEntryState.Failed.name,
                services
                    .localImportDebugProbe()
                    .entries(batchId)
                    .single()
                    .state,
            )
            assertEquals(
                musicCountBefore,
                services.localImportDebugProbe().musicCount(),
            )
            assertEquals(filesBefore, musicRoot.audioFiles().toSet())
            assertTrue(musicRoot.partialFiles().isEmpty())
        }

    @Test
    fun importingTheSameContentTwiceReusesTheExistingCopy() =
        runBlocking {
            val uri = fixtureUri("tone.flac")
            val firstBatch =
                services.localImportGateway().enqueue(
                    listOf(uri),
                    LocalImportSource.DocumentPicker,
                    ImportCompletionAction.OpenLocalLibrary,
                )
            assertEquals(LocalImportBatchState.Completed.name, awaitTerminalBatch(firstBatch).state)
            val musicCount = services.localImportDebugProbe().musicCount()
            val audioFiles = musicRoot().audioFiles().toSet()

            val secondBatch =
                services.localImportGateway().enqueue(
                    listOf(uri),
                    LocalImportSource.DocumentPicker,
                    ImportCompletionAction.OpenLocalLibrary,
                )
            assertEquals(LocalImportBatchState.Completed.name, awaitTerminalBatch(secondBatch).state)
            assertEquals(
                LocalImportEntryState.Duplicate.name,
                services
                    .localImportDebugProbe()
                    .entries(secondBatch)
                    .single()
                    .state,
            )
            assertEquals(musicCount, services.localImportDebugProbe().musicCount())
            assertEquals(audioFiles, musicRoot().audioFiles().toSet())
        }

    @Test
    fun mixedBatchReportsPartialCompletion() =
        runBlocking {
            val batchId =
                services.localImportGateway().enqueue(
                    listOf(fixtureUri("tone.ogg"), contentUri("corrupt.mp3")),
                    LocalImportSource.DocumentPicker,
                    ImportCompletionAction.OpenLocalLibrary,
                )

            val batch = awaitTerminalBatch(batchId)
            assertEquals(LocalImportBatchState.PartiallyCompleted.name, batch.state)
            assertEquals(1, batch.completedCount)
            assertEquals(1, batch.failedCount)
            val entries = services.localImportDebugProbe().entries(batchId)
            assertTrue(entries.first().state in setOf(LocalImportEntryState.Imported.name, LocalImportEntryState.Duplicate.name))
            assertEquals(LocalImportEntryState.Failed.name, entries.last().state)
            assertTrue(musicRoot().partialFiles().isEmpty())
        }

    @Test
    fun cancellingSlowCopyRemovesPartialAndKeepsCompletedItems() =
        runBlocking {
            val batchId =
                services.localImportGateway().enqueue(
                    listOf(fixtureUri("tone.m4a"), contentUri("slow.wav")),
                    LocalImportSource.DocumentPicker,
                    ImportCompletionAction.OpenLocalLibrary,
                )
            withTimeout(TIMEOUT_MS) {
                while (services
                        .localImportDebugProbe()
                        .entries(batchId)
                        .getOrNull(1)
                        ?.state != LocalImportEntryState.Copying.name
                ) {
                    delay(POLL_MS)
                }
            }

            services.localMusicRepository().cancelImport(batchId)
            val batch = awaitTerminalBatch(batchId)
            assertEquals(LocalImportBatchState.Cancelled.name, batch.state)
            assertEquals(1, batch.completedCount)
            val entries = services.localImportDebugProbe().entries(batchId)
            assertTrue(entries.first().state in setOf(LocalImportEntryState.Imported.name, LocalImportEntryState.Duplicate.name))
            assertEquals(LocalImportEntryState.Cancelled.name, entries.last().state)
            withTimeout(TIMEOUT_MS) {
                while (musicRoot().partialFiles().isNotEmpty()) delay(POLL_MS)
            }
            assertTrue(musicRoot().partialFiles().isEmpty())
        }

    @Test
    fun resumedWorkRemovesOrphanedPartialBeforeImporting() =
        runBlocking {
            val orphan = File(musicRoot(), "interrupted-import.partial").apply { writeText("incomplete") }
            val batchId =
                services.localImportGateway().enqueue(
                    listOf(fixtureUri("tone.mp3")),
                    LocalImportSource.DocumentPicker,
                    ImportCompletionAction.OpenLocalLibrary,
                )

            assertEquals(LocalImportBatchState.Completed.name, awaitTerminalBatch(batchId).state)
            assertFalse(orphan.exists())
            assertTrue(musicRoot().partialFiles().isEmpty())
        }

    private suspend fun awaitTerminalBatch(batchId: String): DebugImportBatch =
        withTimeout(TIMEOUT_MS) {
            while (true) {
                val batch = requireNotNull(services.localImportDebugProbe().findBatch(batchId))
                if (batch.state.substringBefore('|') in TERMINAL_STATES) return@withTimeout batch
                delay(POLL_MS)
            }
            error("unreachable")
        }

    private fun musicRoot(): File = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).apply { mkdirs() }

    private fun File.partialFiles(): List<File> = listFiles { file -> file.name.endsWith(".partial") }?.toList().orEmpty()

    private fun File.audioFiles(): List<String> = listFiles { file -> file.name.endsWith(".audio") }?.map(File::getName).orEmpty()

    private fun fixtureUri(name: String) = contentUri("fixtures/$name")

    private fun contentUri(path: String): Uri = Uri.parse("content://cn.james.music.debug.test.audio/$path")

    private companion object {
        val FIXTURE_NAMES = listOf("tone.mp3", "tone.m4a", "tone.flac", "tone.ogg", "tone.wav")
        val TERMINAL_STATES =
            setOf(
                LocalImportBatchState.Completed.name,
                LocalImportBatchState.PartiallyCompleted.name,
                LocalImportBatchState.Failed.name,
                LocalImportBatchState.Cancelled.name,
            )
        const val POLL_MS = 100L
        const val TIMEOUT_MS = 30_000L
    }
}
