package dev.bitsbots.tldhbench.corpus

import android.content.Context
import android.net.Uri
import dev.bitsbots.tldhbench.share.SharedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ReferenceCorpusManager(private val context: Context) {
    private val corpusRoot: File get() = File(context.filesDir, "reference-corpus")

    fun sampleFile(sample: ReferenceSample): File = File(corpusRoot, sample.fileName)

    fun isInstalled(sample: ReferenceSample): Boolean =
        sampleFile(sample).isFile && sampleFile(sample).length() > 0L

    fun installedIds(): Set<String> = BuiltInReferenceCorpus.samples
        .filter { isInstalled(it) }
        .map { it.id }
        .toSet()

    fun sharedAudio(sample: ReferenceSample): SharedAudio {
        val file = sampleFile(sample)
        if (!file.isFile) throw IllegalStateException("Testaudio ist noch nicht heruntergeladen: ${sample.id}")
        return SharedAudio(uri = Uri.fromFile(file), mimeType = sample.mimeType)
    }

    suspend fun download(sample: ReferenceSample, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        corpusRoot.mkdirs()
        val target = sampleFile(sample)
        val temp = File(context.cacheDir, "${sample.id}.download")
        temp.delete()
        downloadFile(sample.audioUrl, temp, onProgress)
        if (temp.length() <= 0L) throw IllegalStateException("Download lieferte keine Daten: ${sample.id}")
        target.delete()
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
        target
    }

    suspend fun downloadAll(samples: List<ReferenceSample>, onStatus: (String, Int) -> Unit): Unit = withContext(Dispatchers.IO) {
        for ((index, sample) in samples.withIndex()) {
            if (isInstalled(sample)) {
                onStatus("Vorhanden ${sample.id}", ((index + 1) * 100) / samples.size)
                continue
            }
            download(sample) { sampleProgress ->
                val overall = ((index * 100) + sampleProgress) / samples.size
                onStatus("Lade ${sample.id}", overall.coerceIn(0, 100))
            }
        }
        onStatus("Goldstandard-Testaudios bereit", 100)
    }

    suspend fun delete(sample: ReferenceSample): Unit = withContext(Dispatchers.IO) {
        sampleFile(sample).delete()
    }

    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        corpusRoot.deleteRecursively()
    }

    private fun downloadFile(url: String, target: File, onProgress: (Int) -> Unit) {
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
}
