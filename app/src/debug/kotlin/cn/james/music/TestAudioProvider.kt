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

class TestAudioProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor {
        val file = File(requireNotNull(context).cacheDir, "external-view-test.wav")
        file.parentFile?.mkdirs()
        if (file.length() != EXPECTED_FILE_SIZE) writeWave(file)
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
            addRow(arrayOf<Any?>("external-view-test.wav", EXPECTED_FILE_SIZE))
        }

    override fun getType(uri: Uri): String = "audio/wav"

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

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
            repeat(sampleRate * durationSeconds) { output.writeLeShort(0) }
        }
    }

    private fun DataOutputStream.writeLeInt(value: Int) {
        repeat(4) { write(value ushr (it * 8) and 0xff) }
    }

    private fun DataOutputStream.writeLeShort(value: Int) {
        repeat(2) { write(value ushr (it * 8) and 0xff) }
    }

    private companion object {
        const val EXPECTED_FILE_SIZE = 160_044L
    }
}
