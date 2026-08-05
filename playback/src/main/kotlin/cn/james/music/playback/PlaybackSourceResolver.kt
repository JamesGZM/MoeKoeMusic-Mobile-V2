package cn.james.music.playback

import android.content.Context
import android.net.Uri
import cn.james.music.core.model.playback.PlaybackItem
import cn.james.music.core.model.playback.PlaybackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

internal sealed interface PlaybackSourceResult {
    data class Resolved(
        val uri: Uri,
    ) : PlaybackSourceResult

    data class Unavailable(
        val error: PlaybackError.SourceUnavailable,
    ) : PlaybackSourceResult
}

internal interface PlaybackSourceResolver {
    suspend fun resolve(item: PlaybackItem): PlaybackSourceResult
}

interface ImportedLocalSourceResolver {
    suspend fun resolve(localMusicId: String): Uri?
}

interface KugouSourceResolver {
    suspend fun resolve(songHash: String): String?
}

@Singleton
internal class DefaultPlaybackSourceResolver
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val importedLocalSourceResolver: ImportedLocalSourceResolver,
        private val kugouSourceResolver: KugouSourceResolver,
    ) : PlaybackSourceResolver {
        override suspend fun resolve(item: PlaybackItem): PlaybackSourceResult =
            when (val source = item.source) {
                PlaybackSource.FoundationDemo -> {
                    PlaybackSourceResult.Resolved(DemoAudioFile.ensure(context))
                }

                is PlaybackSource.ImportedLocal -> {
                    importedLocalSourceResolver.resolve(source.localMusicId)?.let(PlaybackSourceResult::Resolved)
                        ?: PlaybackSourceResult.Unavailable(PlaybackError.SourceUnavailable(item.id))
                }

                is PlaybackSource.Kugou -> {
                    kugouSourceResolver
                        .resolve(source.songHash)
                        ?.let(Uri::parse)
                        ?.takeIf { uri -> uri.scheme == "https" && !uri.host.isNullOrBlank() }
                        ?.let(PlaybackSourceResult::Resolved)
                        ?: PlaybackSourceResult.Unavailable(PlaybackError.SourceUnavailable(item.id))
                }
            }
    }

private object DemoAudioFile {
    private const val SAMPLE_RATE = 22_050
    private const val DURATION_SECONDS = 12
    private const val CHANNEL_COUNT = 1
    private const val BITS_PER_SAMPLE = 16

    suspend fun ensure(context: Context): Uri =
        withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "foundation-audio").apply { mkdirs() }
            val file = File(directory, "moekoe-foundation-demo.wav")
            if (!file.exists() || file.length() == 0L) writeWave(file)
            Uri.fromFile(file)
        }

    private fun writeWave(file: File) {
        val sampleCount = SAMPLE_RATE * DURATION_SECONDS
        val dataSize = sampleCount * CHANNEL_COUNT * (BITS_PER_SAMPLE / 8)
        DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
            output.writeBytes("RIFF")
            output.writeLittleEndianInt(36 + dataSize)
            output.writeBytes("WAVEfmt ")
            output.writeLittleEndianInt(16)
            output.writeLittleEndianShort(1)
            output.writeLittleEndianShort(CHANNEL_COUNT)
            output.writeLittleEndianInt(SAMPLE_RATE)
            output.writeLittleEndianInt(SAMPLE_RATE * CHANNEL_COUNT * (BITS_PER_SAMPLE / 8))
            output.writeLittleEndianShort(CHANNEL_COUNT * (BITS_PER_SAMPLE / 8))
            output.writeLittleEndianShort(BITS_PER_SAMPLE)
            output.writeBytes("data")
            output.writeLittleEndianInt(dataSize)

            val notes = doubleArrayOf(261.63, 329.63, 392.00, 523.25, 440.00, 392.00, 329.63, 293.66)
            repeat(sampleCount) { sampleIndex ->
                val seconds = sampleIndex.toDouble() / SAMPLE_RATE
                val beatPosition = (seconds % 0.75) / 0.75
                val frequency = notes[((seconds / 0.75).toInt()) % notes.size]
                val envelope = minOf(1.0, beatPosition * 10.0) * (1.0 - beatPosition).coerceAtLeast(0.0)
                val fundamental = sin(2.0 * PI * frequency * seconds)
                val harmonic = 0.25 * sin(2.0 * PI * frequency * 2.0 * seconds)
                val sample = ((fundamental + harmonic) * envelope * 0.20 * Short.MAX_VALUE).toInt()
                output.writeLittleEndianShort(sample)
            }
        }
    }

    private fun DataOutputStream.writeLittleEndianInt(value: Int) {
        write(value and 0xFF)
        write(value ushr 8 and 0xFF)
        write(value ushr 16 and 0xFF)
        write(value ushr 24 and 0xFF)
    }

    private fun DataOutputStream.writeLittleEndianShort(value: Int) {
        write(value and 0xFF)
        write(value ushr 8 and 0xFF)
    }
}
