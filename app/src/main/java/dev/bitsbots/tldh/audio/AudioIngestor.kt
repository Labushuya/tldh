package dev.bitsbots.tldh.audio

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import dev.bitsbots.tldh.share.SharedAudio

data class AudioMetadata(
    val uri: Uri,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val format: AudioFormat,
    val headerBytes: ByteArray
)

class AudioIngestor(private val context: Context) {
    fun inspect(sharedAudio: SharedAudio): AudioMetadata {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(sharedAudio.uri)
        val size = querySize(sharedAudio.uri)
        val mime = sharedAudio.mimeType ?: resolver.getType(sharedAudio.uri)
        val header = resolver.openInputStream(sharedAudio.uri)?.use { input ->
            ByteArray(4096).also { buffer ->
                val read = input.read(buffer)
                if (read < 0) return@use ByteArray(0)
                return@use buffer.copyOf(read)
            }
        } ?: ByteArray(0)

        return AudioMetadata(
            uri = sharedAudio.uri,
            displayName = displayName,
            mimeType = mime,
            sizeBytes = size,
            format = AudioFormatDetector.detect(header, displayName, mime),
            headerBytes = header
        )
    }

    private fun queryDisplayName(uri: Uri): String? = query(uri, OpenableColumns.DISPLAY_NAME)

    private fun querySize(uri: Uri): Long? = query(uri, OpenableColumns.SIZE)?.toLongOrNull()

    private fun query(uri: Uri, column: String): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor: Cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(column)
                if (index < 0) null else cursor.getString(index)
            }
        }.getOrNull()
    }
}
