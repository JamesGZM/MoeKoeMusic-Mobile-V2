package cn.james.music

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

class TestAudioProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {
        if (uri.lastPathSegment == SLOW_FILE_NAME) return openSlowWave()
        val file = resolveFile(uri)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor =
        MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply {
            val displayName = uri.lastPathSegment ?: DEFAULT_FILE_NAME
            val size = if (displayName == SLOW_FILE_NAME) SLOW_FILE_SIZE else resolveFile(uri).length()
            addRow(arrayOf<Any?>(displayName, size))
        }

    override fun getType(uri: Uri): String =
        when (uri.lastPathSegment?.substringAfterLast('.')) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            else -> "audio/wav"
        }

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun resolveFile(uri: Uri): File {
        val context = requireNotNull(context)
        val displayName = uri.lastPathSegment ?: DEFAULT_FILE_NAME
        val file = File(context.cacheDir, "test-audio/$displayName")
        file.parentFile?.mkdirs()
        when {
            uri.pathSegments.firstOrNull() == "fixtures" -> {
                if (!file.exists()) {
                    context.assets.open("local-music-fixtures/$displayName").use { input ->
                        file.outputStream().use(input::copyTo)
                    }
                }
            }

            displayName == CORRUPT_FILE_NAME -> {
                if (!file.exists()) file.writeBytes("not an audio file".encodeToByteArray())
            }

            file.length() != EXPECTED_FILE_SIZE -> {
                writeWave(file)
            }
        }
        return file
    }

    private fun openSlowWave(): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            try {
                DataOutputStream(ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).buffered()).use { output ->
                    writeWaveHeader(output, SLOW_DATA_SIZE)
                    val buffer = ByteArray(8 * 1024)
                    var remaining = SLOW_DATA_SIZE
                    while (remaining > 0) {
                        val count = minOf(buffer.size, remaining)
                        output.write(buffer, 0, count)
                        output.flush()
                        remaining -= count
                        Thread.sleep(10)
                    }
                }
            } catch (_: IOException) {
                // The importer closes the read side when a batch is cancelled.
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }.apply {
            name = "MoeKoeSlowAudioProvider"
            isDaemon = true
            start()
        }
        return pipe[0]
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun writeWave(file: File) {
        val sampleRate = 8_000
        val durationSeconds = 10
        val dataSize = sampleRate * durationSeconds * 2
        DataOutputStream(file.outputStream().buffered()).use { output ->
            writeWaveHeader(output, dataSize)
            repeat(sampleRate * durationSeconds) { output.writeLeShort(0) }
        }
    }

    private fun writeWaveHeader(
        output: DataOutputStream,
        dataSize: Int,
    ) {
        val sampleRate = 8_000
        output.writeBytes("RIFF")
        output.writeLeInt(36 + dataSize)
        output.writeBytes("WAVEfmt ")
        output.writeLeInt(16)
        output.writeLeShort(1)
        output.writeLeShort(1)
        output.writeLeInt(sampleRate)
        output.writeLeInt(sampleRate * 2)
        output.writeLeShort(2)
        output.writeLeShort(16)
        output.writeBytes("data")
        output.writeLeInt(dataSize)
    }

    private fun DataOutputStream.writeLeInt(value: Int) {
        repeat(4) { write(value ushr (it * 8) and 0xff) }
    }

    private fun DataOutputStream.writeLeShort(value: Int) {
        repeat(2) { write(value ushr (it * 8) and 0xff) }
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "tone.wav"
        const val CORRUPT_FILE_NAME = "corrupt.mp3"
        const val SLOW_FILE_NAME = "slow.wav"
        const val EXPECTED_FILE_SIZE = 160_044L
        const val SLOW_DATA_SIZE = 8 * 1024 * 1024
        const val SLOW_FILE_SIZE = SLOW_DATA_SIZE + 44L
    }
}
