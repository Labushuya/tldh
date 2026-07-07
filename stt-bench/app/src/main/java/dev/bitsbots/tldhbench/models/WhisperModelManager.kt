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

class WhisperModelManager(private val context: Context) {
    private val modelsRoot: File get() = File(context.filesDir, "whisper-models")

    fun modelFile(spec: WhisperModelSpec): File = File(modelsRoot, spec.fileName)

    fun isInstalled(spec: WhisperModelSpec): Boolean {
        val file = modelFile(spec)
        return file.isFile && file.length() > 1024L * 1024L
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
        downloadToFile(spec.url, partial, onProgress)
        if (partial.length() <= 1024L * 1024L) {
            partial.delete()
            throw IllegalStateException("Whisper-Modell-Download ist unplausibel klein. Download abgebrochen oder falsche Datei erhalten.")
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

    private fun downloadToFile(url: String, target: File, onProgress: (Int) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 240_000
            requestMethod = "GET"
            instanceFollowRedirects = true
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Download fehlgeschlagen: HTTP ${connection.responseCode}")
        }
        val length = connection.contentLengthLong.takeIf { it > 0L }
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(128 * 1024)
                var read: Int
                var total = 0L
                while (input.read(buffer).also { read = it } >= 0) {
                    output.write(buffer, 0, read)
                    total += read
                    if (length != null) onProgress(((total * 100L) / length).toInt().coerceIn(0, 100))
                }
            }
        }
    }
}
