package dev.bitsbots.tldh.engine

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import dev.bitsbots.tldh.transcription.CpuCluster
import dev.bitsbots.tldh.transcription.PerfSample
import dev.bitsbots.tldh.transcription.ProcStat
import dev.bitsbots.tldh.transcription.RunPhase
import java.io.File

/**
 * Looks at the running whisper-cli process every few seconds: is the display on, is tl;dh on
 * screen, which cores do whisper's threads run on, which cores may it use and how fast do the
 * clusters clock. Reads only the process's own /proc entries and cpufreq, no permission needed.
 * Whatever Android doesn't let an app read stays empty.
 */
class PerformanceProbe(context: Context, private val isVisible: () -> Boolean) {
    private val power = context.getSystemService(PowerManager::class.java)
    val clusters: List<CpuCluster> = readClusters()
    private val samples = mutableListOf<PerfSample>()

    @Volatile
    var pid: Int? = null

    @Synchronized
    fun sample(progress: Int) {
        val pid = pid
        samples += PerfSample(
            atMs = SystemClock.elapsedRealtime(),
            phase = phase(),
            progress = progress,
            threadCpus = pid?.let { threadCpus(it) }.orEmpty(),
            allowedCpus = pid?.let { status(it, "Cpus_allowed_list") },
            cpuset = pid?.let { readLine(File("/proc/$it/cpuset")) },
            clusterKhz = clusters.map { readLong(File("/sys/devices/system/cpu/cpu${it.cpus.first()}/cpufreq/scaling_cur_freq")) }
        )
    }

    @Synchronized
    fun samples(): List<PerfSample> = samples.toList()

    private fun phase(): RunPhase = when {
        power?.isInteractive == false -> RunPhase.DISPLAY_OFF
        isVisible() -> RunPhase.VISIBLE
        else -> RunPhase.BACKGROUND
    }

    private fun threadCpus(pid: Int): List<Int> {
        val tasks = File("/proc/$pid/task").listFiles().orEmpty()
        val threads = tasks.mapNotNull { task -> readLine(File(task, "stat"))?.let(ProcStat::parse) }
        val running = threads.filter { it.state == 'R' }
        return (running.ifEmpty { threads }).map { it.cpu }
    }

    private fun status(pid: Int, key: String): String? = runCatching {
        File("/proc/$pid/status").useLines { lines ->
            lines.firstOrNull { it.startsWith("$key:") }?.substringAfter(':')?.trim()
        }
    }.getOrNull()

    companion object {
        /** Android's Process prints "Process[pid=1234, hasExited=false]"; else search /proc. */
        fun pidOf(process: Process, executable: File): Int? =
            Regex("""pid=(\d+)""").find(process.toString())?.groupValues?.get(1)?.toIntOrNull()
                ?: findByCommand(executable.absolutePath)

        private fun findByCommand(path: String): Int? =
            File("/proc").listFiles { file -> file.name.all(Char::isDigit) }.orEmpty().firstOrNull { dir ->
                runCatching { File(dir, "cmdline").readText().startsWith(path) }.getOrDefault(false)
            }?.name?.toIntOrNull()

        private fun readClusters(): List<CpuCluster> {
            val cpus = File("/sys/devices/system/cpu").listFiles { file -> file.name.matches(Regex("cpu\\d+")) }.orEmpty()
                .mapNotNull { it.name.removePrefix("cpu").toIntOrNull() }
                .sorted()
            val seen = mutableSetOf<Int>()
            return cpus.mapNotNull { cpu ->
                if (cpu in seen) return@mapNotNull null
                val dir = File("/sys/devices/system/cpu/cpu$cpu/cpufreq")
                val related = readLine(File(dir, "related_cpus"))
                    ?.split(Regex("\\s+"))?.mapNotNull { it.toIntOrNull() }?.sorted()
                    ?.takeIf { it.isNotEmpty() } ?: listOf(cpu)
                val max = readLong(File(dir, "cpuinfo_max_freq")) ?: return@mapNotNull null
                seen += related
                CpuCluster(related, max)
            }
        }

        private fun readLine(file: File): String? = runCatching { file.readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }

        private fun readLong(file: File): Long? = readLine(file)?.toLongOrNull()
    }
}
