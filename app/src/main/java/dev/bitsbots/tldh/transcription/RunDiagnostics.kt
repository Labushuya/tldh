package dev.bitsbots.tldh.transcription

import java.util.Locale

/** Where the phone was while whisper ran: these are the states Android schedules differently. */
enum class RunPhase(val label: String) {
    VISIBLE("Display an, App offen"),
    BACKGROUND("Display an, App im Hintergrund"),
    DISPLAY_OFF("Display aus")
}

/** A group of cores that share one clock (a cpufreq policy), e.g. 3-6 on a 1+4+3 chip. */
data class CpuCluster(val cpus: List<Int>, val maxKhz: Long)

/** One look at the running whisper process, taken every few seconds. */
data class PerfSample(
    val atMs: Long,
    val phase: RunPhase,
    /** whisper's own progress, 0..100. */
    val progress: Int,
    /** The core each whisper thread is on (running threads; all threads if none was running). */
    val threadCpus: List<Int> = emptyList(),
    /** Cores the process may use, as the kernel lists them ("0-6"). */
    val allowedCpus: String? = null,
    /** Android's CPU group for the process, e.g. "/top-app" or "/foreground". */
    val cpuset: String? = null,
    /** Current clock per cluster, same order as the clusters. */
    val clusterKhz: List<Long?> = emptyList()
)

data class PhaseStats(
    val phase: RunPhase,
    val wallMs: Long,
    val audioMs: Long,
    val cores: List<Int>,
    val allowedCpus: String?,
    val cpuset: String?,
    /** Median clock per cluster while in this phase. */
    val clusterKhz: List<Long?>
) {
    /** Seconds of audio per second of work; 3.0 means a 3 min note takes 1 min. */
    val speed: Double? get() = if (wallMs > 0 && audioMs > 0) audioMs.toDouble() / wallMs else null
}

/**
 * Turns the samples of one run into per-phase numbers. Time between two samples counts for the
 * phase of the earlier one; audio progress in that time is credited to the same phase. whisper
 * reports progress per 30 s window, so short phases are rough.
 */
object RunDiagnostics {
    const val SAMPLE_INTERVAL_MS = 2_000L
    private const val MIN_PHASE_MS = 3_000L
    private const val MIN_CORE_SHARE = 0.05

    fun summarize(samples: List<PerfSample>, audioDurationMs: Long): List<PhaseStats> {
        if (samples.size < 2 || audioDurationMs <= 0) return emptyList()
        val wall = mutableMapOf<RunPhase, Long>()
        val audio = mutableMapOf<RunPhase, Long>()
        samples.zipWithNext().forEach { (a, b) ->
            val dt = (b.atMs - a.atMs).coerceAtLeast(0)
            val dp = (b.progress - a.progress).coerceAtLeast(0)
            wall[a.phase] = (wall[a.phase] ?: 0) + dt
            audio[a.phase] = (audio[a.phase] ?: 0) + dp * audioDurationMs / 100
        }
        return RunPhase.entries.mapNotNull { phase ->
            val wallMs = wall[phase] ?: 0
            if (wallMs < MIN_PHASE_MS) return@mapNotNull null
            val inPhase = samples.filter { it.phase == phase }
            PhaseStats(
                phase = phase,
                wallMs = wallMs,
                audioMs = audio[phase] ?: 0,
                cores = usedCores(inPhase),
                allowedCpus = mostCommon(inPhase.mapNotNull { it.allowedCpus }),
                cpuset = mostCommon(inPhase.mapNotNull { it.cpuset }),
                clusterKhz = medianPerCluster(inPhase)
            )
        }
    }

    /** One line per phase for the technical details of a result. */
    fun describe(stats: List<PhaseStats>, clusters: List<CpuCluster>): List<String> = stats.map { s ->
        val speed = s.speed?.let { "%.1f× Echtzeit".format(Locale.GERMANY, it) } ?: "kein Fortschritt gemessen"
        buildList {
            add("${s.phase.label}: ${"%.0f".format(Locale.GERMANY, s.wallMs / 1000.0)} s, $speed")
            if (s.cores.isNotEmpty()) add("Kerne ${ranges(s.cores)}")
            val group = listOfNotNull(s.allowedCpus?.let { "erlaubt $it" }, s.cpuset?.trim('/')?.takeIf { it.isNotBlank() })
            if (group.isNotEmpty()) add(group.joinToString(", "))
            clock(s.clusterKhz, clusters)?.let { add("Takt aktuell/max GHz: $it") }
        }.joinToString(" · ")
    }

    /** Cores whisper's threads were seen on in at least 5 % of the observations. */
    internal fun usedCores(samples: List<PerfSample>): List<Int> {
        val all = samples.flatMap { it.threadCpus }
        if (all.isEmpty()) return emptyList()
        return all.groupingBy { it }.eachCount()
            .filter { (_, count) -> count >= all.size * MIN_CORE_SHARE }
            .keys.sorted()
    }

    internal fun ranges(cpus: List<Int>): String {
        val sorted = cpus.distinct().sorted()
        val parts = mutableListOf<String>()
        var start = sorted.first()
        var previous = start
        for (cpu in sorted.drop(1) + Int.MIN_VALUE) {
            if (cpu == previous + 1) {
                previous = cpu
                continue
            }
            parts += if (start == previous) "$start" else "$start–$previous"
            start = cpu
            previous = cpu
        }
        return parts.joinToString(",")
    }

    private fun clock(khz: List<Long?>, clusters: List<CpuCluster>): String? {
        if (clusters.isEmpty() || khz.none { it != null }) return null
        return clusters.mapIndexedNotNull { i, cluster ->
            val current = khz.getOrNull(i) ?: return@mapIndexedNotNull null
            "${ranges(cluster.cpus)} ${ghz(current)}/${ghz(cluster.maxKhz)}"
        }.joinToString(", ")
    }

    private fun ghz(khz: Long): String = "%.1f".format(Locale.GERMANY, khz / 1_000_000.0)

    private fun medianPerCluster(samples: List<PerfSample>): List<Long?> {
        val width = samples.maxOfOrNull { it.clusterKhz.size } ?: 0
        return List(width) { i ->
            val values = samples.mapNotNull { it.clusterKhz.getOrNull(i) }.sorted()
            if (values.isEmpty()) null else values[values.size / 2]
        }
    }

    private fun mostCommon(values: List<String>): String? =
        values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
}

/** The two fields of /proc/<pid>/task/<tid>/stat the probe needs. */
internal data class ProcStat(val state: Char, val cpu: Int) {
    companion object {
        /** The thread name in parentheses may contain spaces; fields after it start at "state". */
        fun parse(line: String): ProcStat? {
            val fields = line.substringAfterLast(')').trim().split(' ')
            val state = fields.getOrNull(0)?.firstOrNull() ?: return null
            // Field 39 of stat (processor) is index 36 after the name.
            val cpu = fields.getOrNull(36)?.toIntOrNull() ?: return null
            return ProcStat(state, cpu)
        }
    }
}
