package dev.bitsbots.tldh.transcription

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dev.bitsbots.tldh.updates.Checksum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object DeviceProfiler {
    fun read(context: Context): DeviceProfile {
        val memoryInfo = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
        val cpuinfo = runCatching { File("/proc/cpuinfo").readText() }.getOrDefault("")
        return DeviceProfile(
            totalRamBytes = memoryInfo.totalMem,
            cpu = CpuFeatures.parse(cpuinfo),
            arm64 = "arm64-v8a" in Build.SUPPORTED_ABIS,
            coreMaxFreqKhz = readCoreMaxFrequencies()
        )
    }

    private fun readCoreMaxFrequencies(): List<Long> {
        val cores = File("/sys/devices/system/cpu").listFiles { file -> file.name.matches(Regex("cpu\\d+")) }.orEmpty()
        return cores.mapNotNull { core ->
            runCatching { File(core, "cpufreq/cpuinfo_max_freq").readText().trim().toLong() }.getOrNull()
        }
    }
}

/**
 * Whisper models live in filesDir/whisper-models, outside the per-share session folders, so the
 * session wipe never deletes them. The model is only downloaded when the user taps the button.
 */
class WhisperModelStore(context: Context) {
    private val modelsDir = File(context.filesDir, "whisper-models")
    private val prefs = context.getSharedPreferences("tldh_settings", Context.MODE_PRIVATE)

    fun modelFile(spec: WhisperModelSpec): File = File(modelsDir, spec.fileName)

    fun isInstalled(spec: WhisperModelSpec): Boolean = modelFile(spec).let { it.isFile && it.length() == spec.sizeBytes }

    fun installedModels(): List<WhisperModelSpec> = WhisperModelCatalog.models.filter { isInstalled(it) }

    /** The model the user picked, falling back to any installed one. */
    fun activeModel(): WhisperModelSpec? {
        val chosen = WhisperModelCatalog.byId(prefs.getString(KEY_ACTIVE_MODEL, null))
        return chosen?.takeIf { isInstalled(it) } ?: installedModels().firstOrNull()
    }

    fun setActiveModel(spec: WhisperModelSpec) {
        prefs.edit().putString(KEY_ACTIVE_MODEL, spec.id).apply()
    }

    suspend fun download(spec: WhisperModelSpec, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        modelsDir.mkdirs()
        val target = modelFile(spec)
        val partial = File(modelsDir, "${spec.fileName}.part")
        partial.delete()
        try {
            val connection = (URL(spec.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 120_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "tldh-android")
            }
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Modell-Download fehlgeschlagen: HTTP ${connection.responseCode}")
            }
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var total = 0L
                    var lastReported = -1
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        total += read
                        val percent = ((total * 100) / spec.sizeBytes).toInt()
                        if (percent != lastReported) {
                            lastReported = percent
                            onProgress((total.toFloat() / spec.sizeBytes).coerceIn(0f, 1f))
                        }
                    }
                    output.fd.sync()
                }
            }
            if (partial.length() != spec.sizeBytes) {
                throw IllegalStateException("Modell unvollständig (${partial.length()} von ${spec.sizeBytes} Byte). Bitte erneut laden.")
            }
            if (!Checksum.sha256(partial).equals(spec.sha256, ignoreCase = true)) {
                throw IllegalStateException("Modell-Prüfsumme stimmt nicht. Download verworfen.")
            }
            target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
            }
            target
        } finally {
            partial.delete()
        }
    }

    fun delete(spec: WhisperModelSpec) {
        modelFile(spec).delete()
    }

    private companion object {
        const val KEY_ACTIVE_MODEL = "whisper_model_id"
    }
}
