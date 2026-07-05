package dev.bitsbots.tldhbench.models

import android.content.Context
import dev.bitsbots.tldhbench.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class VoskModelManager(private val context: Context) {
    private val modelsRoot: File get() = File(context.filesDir, "models")
    val expectedModelDir: File get() = File(modelsRoot, BuildConfig.EXPECTED_MODEL_DIR)
    val tempZip: File get() = File(context.cacheDir, "vosk-model-small-de.zip")

    fun isInstalled(): Boolean = expectedModelDir.isDirectory && File(expectedModelDir, "conf").exists()

    suspend fun downloadAndInstallSmallGerman(onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        modelsRoot.mkdirs()
        expectedModelDir.deleteRecursively()
        tempZip.delete()
        download(BuildConfig.VOSK_SMALL_DE_URL, tempZip, onProgress)
        unzip(tempZip, modelsRoot)
        tempZip.delete()
        if (!isInstalled()) {
            val candidates = modelsRoot.listFiles()?.filter { it.isDirectory }?.joinToString { it.name }.orEmpty()
            throw IllegalStateException("Vosk-Modell wurde entpackt, aber ${BuildConfig.EXPECTED_MODEL_DIR} wurde nicht gefunden. Gefunden: $candidates")
        }
        expectedModelDir
    }

    private fun download(url: String, target: File, onProgress: (Int) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 120_000
            requestMethod = "GET"
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Download fehlgeschlagen: HTTP ${connection.responseCode}")
        }
        val length = connection.contentLengthLong.takeIf { it > 0L }
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(64 * 1024)
                var read: Int
                var total = 0L
                while (input.read(buffer).also { read = it } >= 0) {
                    output.write(buffer, 0, read)
                    total += read
                    if (length != null) onProgress(((total * 100L) / length).toInt().coerceIn(0, 100))
                }
            }
        }
        onProgress(100)
    }

    private fun unzip(zipFile: File, destination: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val outFile = File(destination, entry.name).canonicalFile
                val destCanonical = destination.canonicalFile
                if (!outFile.path.startsWith(destCanonical.path)) {
                    throw SecurityException("Zip-Eintrag verlässt Zielordner: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
            }
        }
    }
}
