package dev.bitsbots.tldhbench.models

import android.content.Context
import dev.bitsbots.tldhbench.bench.VoskModelCatalog
import dev.bitsbots.tldhbench.bench.VoskModelSpec
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

    fun modelDir(spec: VoskModelSpec): File = File(modelsRoot, spec.directoryName)

    fun isInstalled(spec: VoskModelSpec): Boolean =
        modelDir(spec).isDirectory && File(modelDir(spec), "conf").exists()

    fun installedIds(): Set<String> = VoskModelCatalog.models
        .filter { isInstalled(it) }
        .map { it.id }
        .toSet()

    suspend fun downloadAndInstall(spec: VoskModelSpec, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        modelsRoot.mkdirs()
        val targetDir = modelDir(spec)
        val tempZip = File(context.cacheDir, "${spec.id}.zip")
        targetDir.deleteRecursively()
        tempZip.delete()
        download(spec.url, tempZip, onProgress)
        unzip(tempZip, modelsRoot)
        tempZip.delete()
        if (!isInstalled(spec)) {
            val candidates = modelsRoot.listFiles()?.filter { it.isDirectory }?.joinToString { it.name }.orEmpty()
            throw IllegalStateException("Vosk-Modell wurde entpackt, aber ${spec.directoryName} wurde nicht gefunden. Gefunden: $candidates")
        }
        targetDir
    }

    suspend fun delete(spec: VoskModelSpec): Unit = withContext(Dispatchers.IO) {
        modelDir(spec).deleteRecursively()
        File(context.cacheDir, "${spec.id}.zip").delete()
    }

    private fun download(url: String, target: File, onProgress: (Int) -> Unit) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 180_000
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
        val destCanonical = destination.canonicalFile
        ZipInputStream(zipFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val outFile = File(destination, entry.name).canonicalFile
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
