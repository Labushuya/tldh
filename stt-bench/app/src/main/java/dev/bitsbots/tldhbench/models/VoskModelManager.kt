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

    fun modelDir(spec: VoskModelSpec): File = locateUsableModelDir(spec) ?: File(modelsRoot, spec.directoryName)

    fun isInstalled(spec: VoskModelSpec): Boolean = locateUsableModelDir(spec) != null

    fun installedIds(): Set<String> = VoskModelCatalog.models
        .filter { isInstalled(it) }
        .map { it.id }
        .toSet()

    suspend fun downloadAndInstall(spec: VoskModelSpec, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        modelsRoot.mkdirs()
        val targetDir = File(modelsRoot, spec.directoryName)
        val tempZip = File(context.cacheDir, "${spec.id}.zip")
        targetDir.deleteRecursively()
        tempZip.delete()
        download(spec.url, tempZip, onProgress)
        unzip(tempZip, modelsRoot)
        tempZip.delete()

        normalizeExtractedModel(spec, targetDir)

        val resolved = locateUsableModelDir(spec)
        if (resolved == null) {
            val diagnostics = modelDiagnostics(spec)
            throw IllegalStateException(
                "Vosk-Modell wurde entpackt, aber kein gültiges Vosk-Modellverzeichnis für ${spec.directoryName} gefunden. $diagnostics"
            )
        }
        resolved
    }

    suspend fun delete(spec: VoskModelSpec): Unit = withContext(Dispatchers.IO) {
        File(modelsRoot, spec.directoryName).deleteRecursively()
        File(context.cacheDir, "${spec.id}.zip").delete()
    }

    private fun locateUsableModelDir(spec: VoskModelSpec): File? {
        val target = File(modelsRoot, spec.directoryName)
        if (isUsableVoskModelDir(target)) return target
        if (!target.exists()) return null
        return target
            .walkTopDown()
            .maxDepth(6)
            .filter { it.isDirectory && isUsableVoskModelDir(it) }
            .minByOrNull { it.absolutePath.length }
    }

    private fun normalizeExtractedModel(spec: VoskModelSpec, targetDir: File) {
        val resolved = locateUsableModelDir(spec) ?: return
        if (resolved.canonicalFile == targetDir.canonicalFile) return

        val normalized = File(modelsRoot, "${spec.directoryName}.normalized")
        normalized.deleteRecursively()
        resolved.copyRecursively(normalized, overwrite = true)
        targetDir.deleteRecursively()
        if (!normalized.renameTo(targetDir)) {
            normalized.copyRecursively(targetDir, overwrite = true)
            normalized.deleteRecursively()
        }
    }

    private fun isUsableVoskModelDir(dir: File): Boolean {
        if (!dir.isDirectory) return false
        val hasAm = File(dir, "am/final.mdl").isFile
        val hasConf = File(dir, "conf/mfcc.conf").isFile || File(dir, "conf/model.conf").isFile
        val hasGraph = File(dir, "graph/HCLG.fst").isFile ||
            (File(dir, "graph/HCLr.fst").isFile && File(dir, "graph/Gr.fst").isFile)
        return hasAm && hasConf && hasGraph
    }

    private fun modelDiagnostics(spec: VoskModelSpec): String {
        val target = File(modelsRoot, spec.directoryName)
        val candidates = modelsRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.joinToString { root ->
                val direct = listOf("am/final.mdl", "conf/mfcc.conf", "conf/model.conf", "graph/HCLG.fst")
                    .filter { File(root, it).exists() }
                    .joinToString(prefix = "[", postfix = "]")
                "${root.name}$direct"
            }
            .orEmpty()
        val nested = if (target.exists()) {
            target.walkTopDown()
                .maxDepth(3)
                .filter { it.isDirectory }
                .take(20)
                .joinToString { it.relativeTo(target).path.ifBlank { "." } }
        } else {
            "Zielordner fehlt"
        }
        return "Gefundene Modellordner: $candidates. Unterordner in Zielstruktur: $nested"
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
