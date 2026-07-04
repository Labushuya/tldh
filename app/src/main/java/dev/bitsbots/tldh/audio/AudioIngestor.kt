package dev.bitsbots.tldh.audio

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import dev.bitsbots.tldh.share.SharedAudio

private const val UNKNOWN_SIZE: Long = -1L

data class AudioMetadata(
    val uriString: String,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val format: AudioFormat,
    val headerBytes: ByteArray,
    val extension: String?,
    val headerProbeBytes: Int,
    val validation: AudioValidation
)

class AudioIngestor(private val context: Context) {
    fun inspect(sharedAudio: SharedAudio): AudioMetadata {
        val resolver = context.contentResolver
        val displayName = queryDisplayName(sharedAudio.uri) ?: sharedAudio.uri.lastPathSegment?.substringAfterLast('/')
        val size = querySize(sharedAudio.uri)?.takeIf { it >= 0L }
        val mime = sharedAudio.mimeType ?: resolver.getType(sharedAudio.uri)
        val header = resolver.openInputStream(sharedAudio.uri)?.use { input ->
            val buffer = ByteArray(AudioIngestPolicy.MAX_HEADER_PROBE_BYTES)
            val read = input.read(buffer)
            if (read < 0) ByteArray(0) else buffer.copyOf(read)
        } ?: ByteArray(0)
        val format = AudioFormatDetector.detect(header, displayName, mime)
        val metadataWithoutValidation = AudioMetadata(
            uriString = sharedAudio.uri.toString(),
            displayName = displayName,
            mimeType = mime,
            sizeBytes = size,
            format = format,
            headerBytes = header,
            extension = displayName?.substringAfterLast('.', missingDelimiterValue = "")?.takeIf { it.isNotBlank() }?.lowercase(),
            headerProbeBytes = header.size,
            validation = AudioValidation(accepted = false, rejectReasons = emptyList(), warnings = emptyList())
        )
        val validation = AudioIngestPolicy.validate(metadataWithoutValidation)
        val metadata = metadataWithoutValidation.copy(validation = validation)
        if (!validation.accepted) throw AudioIngestException(metadata, validation.rejectReasons)
        return metadata
    }

    private fun queryDisplayName(uri: Uri): String? = query(uri, OpenableColumns.DISPLAY_NAME)

    private fun querySize(uri: Uri): Long? = query(uri, OpenableColumns.SIZE)?.toLongOrNull() ?: UNKNOWN_SIZE

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
