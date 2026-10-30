package dev.bitsbots.tldhbench.models

import android.content.Context
import dev.bitsbots.tldhbench.bench.WhistleModel
import dev.bitsbots.tldhbench.updates.Checksum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class WhistleModelManager(private val context: Context) {
    private val modelsRoot: File get() = File(context.filesDir, "whistle-models")

    fun modelFile(): File = File(modelsRoot, WhistleModel.fileName)

    /** The bundled needle executable, extracted by Android from lib/arm64-v8a into the native library dir. */
    fun runtimeFile(): File = File(context.applicationInfo.nativeLibraryDir, WhistleModel.runtimeLibraryName)

    fun isRuntimeAvailable(): Boolean = runtimeFile().let { it.isFile && it.canExecute() }

    fun isInstalled(): Boolean = modelFile().let { it.isFile && it.length() == WhistleModel.sizeBytes }

    suspend fun download(onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        modelsRoot.mkdirs()
        val target = modelFile()
        val partial = File(modelsRoot, "${WhistleModel.fileName}.part")
        partial.delete()
        target.delete()
        onProgress(1)
        downloadToFile(partial, onProgress)
        if (partial.length() != WhistleModel.sizeBytes) {
            val actual = partial.length()
            partial.delete()
            throw IllegalStateException("Whistle-Modell ist unvollständig ($actual von ${WhistleModel.sizeBytes} Byte). Bitte erneut laden.")
        }
        val actualSha = Checksum.sha256(partial)
        if (!actualSha.equals(WhistleModel.sha256, ignoreCase = true)) {
            partial.delete()
            throw IllegalStateException("Whistle-Modell: SHA-256 stimmt nicht (erwartet ${WhistleModel.sha256.take(12)}…, erhalten ${actualSha.take(12)}…).")
        }
        if (!partial.renameTo(target)) {
            partial.copyTo(target, overwrite = true)
            partial.delete()
        }
        onProgress(100)
        target
    }

    suspend fun delete(): Unit = withContext(Dispatchers.IO) {
        modelFile().delete()
        File(modelsRoot, "${WhistleModel.fileName}.part").delete()
    }

    private fun downloadToFile(target: File, onProgress: (Int) -> Unit) {
        val connection = (URL(WhistleModel.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "tldh-stt-bench/0.6.0")
            setRequestProperty("Accept", "application/octet-stream,*/*")
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Download fehlgeschlagen: HTTP ${connection.responseCode}")
        }
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(256 * 1024)
                var read: Int
                var total = 0L
                var lastProgress = 1
                while (input.read(buffer).also { read = it } >= 0) {
                    output.write(buffer, 0, read)
                    total += read
                    val nextProgress = ((total * 100L) / WhistleModel.sizeBytes).toInt().coerceIn(1, 99)
                    if (nextProgress != lastProgress) {
                        lastProgress = nextProgress
                        onProgress(nextProgress)
                    }
                }
                output.fd.sync()
            }
        }
    }
}
