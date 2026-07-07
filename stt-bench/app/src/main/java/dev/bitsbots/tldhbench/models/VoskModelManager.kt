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

        // Always clear previous partial state first. This is important for models whose
        // previous installation failed validation, because they otherwise cannot be
        // removed through the old "installed only" delete button.
        deleteLocalArtifacts(spec)

        download(spec.url, tempZip, onProgress)
        unzip(tempZip, modelsRoot)
        tempZip.delete()

        normalizeExtractedModel(spec, targetDir)

        val resolved = locateUsableModelDir(spec)
        if (resolved == null) {
            val diagnostics = modelDiagnostics(spec)
            throw IllegalStateException(
                "Vosk-Modell wurde entpackt, aber kein plausibles Vosk-Modellverzeichnis für ${spec.directoryName} gefunden. " +
                    "$diagnostics Nutze im Modelle-Tab 'Lokale Reste bereinigen' und lade danach erneut."
            )
        }
        resolved
    }

    suspend fun delete(spec: VoskModelSpec): Unit = withContext(Dispatchers.IO) {
        deleteLocalArtifacts(spec)
    }

    private fun deleteLocalArtifacts(spec: VoskModelSpec) {
        File(modelsRoot, spec.directoryName).deleteRecursively()
        File(modelsRoot, "${spec.directoryName}.normalized").deleteRecursively()
        File(context.cacheDir, "${spec.id}.zip").delete()
    }

    private fun locateUsableModelDir(spec: VoskModelSpec): File? {
        val target = File(modelsRoot, spec.directoryName)
        if (isUsableVoskModelDir(target)) return target
        if (target.exists()) {
            target.walkTopDown()
                .maxDepth(7)
                .filter { it.isDirectory && isUsableVoskModelDir(it) }
                .minByOrNull { it.absolutePath.length }
                ?.let { return it }
        }

        // Defensive fallback for archives that do not unpack exactly into the documented
        // directory name. Keep this constrained to name matches to avoid accidentally
        // resolving another already-installed German model.
        return modelsRoot.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory && (it.name == spec.directoryName || it.name.contains(spec.directoryName, ignoreCase = true)) }
            ?.flatMap { root -> root.walkTopDown().maxDepth(7).filter { it.isDirectory }.asSequence() }
            ?.filter { isUsableVoskModelDir(it) }
            ?.minByOrNull { it.absolutePath.length }
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

        // The official models are Vosk-compatible, but older/repackaged models can differ
        // slightly in layout. Do not reject a model only because one exact marker path is
        // different; let the Vosk native Model loader be the final authority at benchmark time.
        val hasAcousticModel = File(dir, "am/final.mdl").isFile ||
            File(dir, "final.mdl").isFile ||
            dir.walkTopDown().maxDepth(4).any { it.isFile && it.name == "final.mdl" }

        val graphDir = File(dir, "graph")
        val hasGraph = File(graphDir, "HCLG.fst").isFile ||
            (File(graphDir, "HCLr.fst").isFile && File(graphDir, "Gr.fst").isFile) ||
            graphDir.isDirectory ||
            dir.walkTopDown().maxDepth(5).any { it.isFile && (it.name == "HCLG.fst" || it.name == "HCLr.fst" || it.name == "Gr.fst") }

        val confDir = File(dir, "conf")
        val hasConfig = File(confDir, "mfcc.conf").isFile ||
            File(confDir, "model.conf").isFile ||
            File(dir, "mfcc.conf").isFile ||
            File(dir, "model.conf").isFile ||
            confDir.isDirectory

        return hasAcousticModel && hasGraph && hasConfig
    }

    private fun modelDiagnostics(spec: VoskModelSpec): String {
        val target = File(modelsRoot, spec.directoryName)
        val candidates = modelsRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.joinToString { root ->
                val markers = listOf(
                    "am/final.mdl",
                    "final.mdl",
                    "conf/mfcc.conf",
                    "conf/model.conf",
                    "mfcc.conf",
                    "model.conf",
                    "graph/HCLG.fst",
                    "graph/HCLr.fst",
                    "graph/Gr.fst"
                ).filter { File(root, it).exists() }
                    .joinToString(prefix = "[", postfix = "]")
                "${root.name}$markers"
            }
            .orEmpty()
        val nested = if (target.exists()) {
            target.walkTopDown()
                .maxDepth(3)
                .filter { it.isDirectory }
                .take(24)
                .joinToString { it.relativeTo(target).path.ifBlank { "." } }
        } else {
            "Zielordner fehlt"
        }
        return "Gefundene Modellordner: $candidates. Unterordner in Zielstruktur: $nested."
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
