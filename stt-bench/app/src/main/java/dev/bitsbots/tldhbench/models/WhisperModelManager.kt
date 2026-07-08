package dev.bitsbots.tldhbench.models

import android.content.Context
import dev.bitsbots.tldhbench.bench.WhisperModelCatalog
import dev.bitsbots.tldhbench.bench.WhisperModelSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class WhisperModelManager(private val context: Context) {
    private val modelsRoot: File get() = File(context.filesDir, "whisper-models")

    fun modelFile(spec: WhisperModelSpec): File = File(modelsRoot, spec.fileName)

    fun isInstalled(spec: WhisperModelSpec): Boolean {
        val file = modelFile(spec)
        val minimumPlausibleSize = expectedBytes(spec)?.let { (it * 0.85).toLong() } ?: (1024L * 1024L)
        return file.isFile && file.length() >= minimumPlausibleSize
    }

    fun installedIds(): Set<String> = WhisperModelCatalog.models
        .filter { isInstalled(it) }
        .map { it.id }
        .toSet()

    fun modelSizeBytes(spec: WhisperModelSpec): Long? = modelFile(spec).takeIf { it.isFile }?.length()

    suspend fun download(spec: WhisperModelSpec, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        modelsRoot.mkdirs()
        val target = modelFile(spec)
        val partial = File(modelsRoot, "${spec.fileName}.part")
        partial.delete()
        target.delete()
        onProgress(1)
        downloadToFile(spec, partial, onProgress)
        val expected = expectedBytes(spec)
        if (partial.length() <= 1024L * 1024L) {
            partial.delete()
            throw IllegalStateException("Whisper-Modell-Download ist unplausibel klein. Download abgebrochen oder falsche Datei erhalten.")
        }
        if (expected != null && partial.length() < (expected * 0.85).toLong()) {
            val actual = formatMiB(partial.length())
            val targetSize = formatMiB(expected)
            partial.delete()
            throw IllegalStateException("Whisper-Modell ist unvollständig ($actual von erwartet ca. $targetSize). Bitte erneut laden.")
        }
        if (!partial.renameTo(target)) {
            partial.copyTo(target, overwrite = true)
            partial.delete()
        }
        onProgress(100)
        target
    }

    suspend fun delete(spec: WhisperModelSpec): Unit = withContext(Dispatchers.IO) {
        modelFile(spec).delete()
        File(modelsRoot, "${spec.fileName}.part").delete()
    }

    private fun downloadToFile(spec: WhisperModelSpec, target: File, onProgress: (Int) -> Unit) {
        val connection = (URL(spec.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 300_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "tldh-stt-bench/0.3.8")
            setRequestProperty("Accept", "application/octet-stream,*/*")
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Download fehlgeschlagen: HTTP ${connection.responseCode}")
        }
        val advertisedLength = connection.contentLengthLong.takeIf { it > 0L }
        val expectedLength = advertisedLength ?: expectedBytes(spec)
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(256 * 1024)
                var read: Int
                var total = 0L
                var lastProgress = 1
                onProgress(lastProgress)
                while (input.read(buffer).also { read = it } >= 0) {
                    output.write(buffer, 0, read)
                    total += read
                    val nextProgress = if (expectedLength != null && expectedLength > 0L) {
                        ((total * 100L) / expectedLength).toInt().coerceIn(1, 99)
                    } else {
                        // Fallback for hosts/CDNs without Content-Length: keep visible movement
                        // instead of showing a permanent 0% while hundreds of MiB are arriving.
                        (1 + (total / (8L * 1024L * 1024L))).toInt().coerceIn(1, 95)
                    }
                    if (nextProgress != lastProgress) {
                        lastProgress = nextProgress
                        onProgress(nextProgress)
                    }
                }
                output.fd.sync()
            }
        }
    }

    private fun expectedBytes(spec: WhisperModelSpec): Long? = when (spec.id) {
        "tiny" -> 75L * 1024L * 1024L
        "base" -> 142L * 1024L * 1024L
        "small" -> 466L * 1024L * 1024L
        else -> null
    }

    private fun formatMiB(bytes: Long): String = "%.1f MiB".format(Locale.GERMANY, bytes / 1024.0 / 1024.0)
}
