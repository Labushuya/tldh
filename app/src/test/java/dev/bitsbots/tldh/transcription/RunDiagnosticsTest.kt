package dev.bitsbots.tldh.transcription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunDiagnosticsTest {
    private val clusters = listOf(
        CpuCluster(listOf(0, 1, 2), 2_016_000),
        CpuCluster(listOf(3, 4, 5, 6), 2_803_200),
        CpuCluster(listOf(7), 3_187_200)
    )

    /** 60 s visible at 3x real time, then 120 s display off at 1x, on a 5 min note. */
    private fun run(): List<PerfSample> {
        val samples = mutableListOf<PerfSample>()
        var progress = 0.0
        for (t in 0..30) {
            samples += PerfSample(
                atMs = t * 2_000L,
                phase = RunPhase.VISIBLE,
                progress = progress.toInt(),
                threadCpus = listOf(3, 4, 5, 6, 7),
                allowedCpus = "0-7",
                cpuset = "/top-app",
                clusterKhz = listOf(1_200_000, 2_803_200, 3_187_200)
            )
            progress += 100.0 * 6 / 300 // 6 s of audio per 2 s
        }
        val visibleEnd = progress
        for (t in 1..60) {
            samples += PerfSample(
                atMs = 60_000L + t * 2_000L,
                phase = RunPhase.DISPLAY_OFF,
                progress = (visibleEnd + t * 100.0 * 2 / 300).toInt().coerceAtMost(100),
                threadCpus = listOf(0, 3, 4, 5, 6),
                allowedCpus = "0-6",
                cpuset = "/foreground",
                appCpuset = "/background",
                clusterKhz = listOf(1_100_000, 1_600_000, if (t % 2 == 0) null else 800_000)
            )
        }
        return samples
    }

    @Test
    fun speedPerPhase() {
        val stats = RunDiagnostics.summarize(run(), audioDurationMs = 300_000)
        assertEquals(listOf(RunPhase.VISIBLE, RunPhase.DISPLAY_OFF), stats.map { it.phase })
        val visible = stats[0]
        val off = stats[1]
        assertEquals(62_000L, visible.wallMs)
        assertEquals(118_000L, off.wallMs)
        assertEquals(3.0, visible.speed!!, 0.25)
        assertEquals(1.0, off.speed!!, 0.15)
    }

    @Test
    fun coresGroupsAndClockPerPhase() {
        val stats = RunDiagnostics.summarize(run(), audioDurationMs = 300_000)
        val visible = stats[0]
        val off = stats[1]
        assertEquals(listOf(3, 4, 5, 6, 7), visible.cores)
        assertEquals("/top-app", visible.cpuset)
        assertEquals(listOf(0, 3, 4, 5, 6), off.cores)
        assertEquals("0-6", off.allowedCpus)
        assertEquals(listOf<Long?>(1_100_000, 1_600_000, 800_000), off.clusterKhz)
    }

    @Test
    fun describesEachPhaseOnOneLine() {
        val lines = RunDiagnostics.describe(RunDiagnostics.summarize(run(), audioDurationMs = 300_000), clusters)
        assertEquals(2, lines.size)
        assertTrue(lines[0], lines[0].startsWith("Display an, App offen: 62 s, 3,"))
        assertTrue(lines[0], "Kerne 3–7" in lines[0])
        assertTrue(lines[0], "erlaubt 0-7, whisper top-app ·" in lines[0])
        assertTrue(lines[1], lines[1].startsWith("Display aus: 118 s, 1,"))
        assertTrue(lines[1], "Kerne 0,3–6" in lines[1])
        assertTrue(lines[1], "erlaubt 0-6, whisper foreground, App background" in lines[1])
        assertTrue(lines[1], "Takt aktuell/max GHz: 0–2 1,1/2,0, 3–6 1,6/2,8, 7 0,8/3,2" in lines[1])
    }

    @Test
    fun shortPhasesAndEmptyRunsAreLeftOut() {
        val blip = listOf(
            PerfSample(0, RunPhase.VISIBLE, 0),
            PerfSample(2_000, RunPhase.BACKGROUND, 5),
            PerfSample(4_000, RunPhase.VISIBLE, 10),
            PerfSample(10_000, RunPhase.VISIBLE, 30)
        )
        val stats = RunDiagnostics.summarize(blip, audioDurationMs = 60_000)
        assertEquals(listOf(RunPhase.VISIBLE), stats.map { it.phase })
        assertTrue(RunDiagnostics.summarize(blip.take(1), 60_000).isEmpty())
        assertTrue(RunDiagnostics.summarize(blip, 0).isEmpty())
    }

    @Test
    fun withoutProcessDataOnlySpeedIsShown() {
        val samples = (0..5).map { PerfSample(it * 2_000L, RunPhase.BACKGROUND, it * 10) }
        val line = RunDiagnostics.describe(RunDiagnostics.summarize(samples, 60_000), emptyList()).single()
        assertEquals("Display an, App im Hintergrund: 10 s, 3,0× Echtzeit", line)
    }

    @Test
    fun noProgressIsSaidPlainly() {
        val samples = listOf(PerfSample(0, RunPhase.DISPLAY_OFF, 0), PerfSample(8_000, RunPhase.DISPLAY_OFF, 0))
        val stats = RunDiagnostics.summarize(samples, 60_000)
        assertNull(stats.single().speed)
        assertTrue(RunDiagnostics.describe(stats, emptyList()).single().endsWith("kein Fortschritt gemessen"))
    }

    @Test
    fun aPauseOfTheProbeCountsForNoPhase() {
        val samples = listOf(
            PerfSample(0, RunPhase.BACKGROUND, 0),
            PerfSample(2_000, RunPhase.BACKGROUND, 0),
            // 30 s without a sample: tl;dh itself did not run.
            PerfSample(32_000, RunPhase.DISPLAY_OFF, 40),
            PerfSample(34_000, RunPhase.DISPLAY_OFF, 45),
            PerfSample(36_000, RunPhase.DISPLAY_OFF, 50),
            PerfSample(38_000, RunPhase.DISPLAY_OFF, 55)
        )
        val stats = RunDiagnostics.summarize(samples, 100_000)
        assertEquals(listOf(RunPhase.DISPLAY_OFF), stats.map { it.phase })
        assertEquals(6_000L, stats.single().wallMs)
        assertEquals(30_000L, RunDiagnostics.gapMs(samples))
        val lines = RunDiagnostics.describe(stats, emptyList(), RunDiagnostics.gapMs(samples))
        assertEquals("Messlücke: 30 s, in der Zeit kam tl;dh selbst nicht zum Messen", lines.last())
    }

    @Test
    fun clustersWithoutARealClockAreLeftOut() {
        val samples = listOf(
            PerfSample(0, RunPhase.VISIBLE, 0, threadCpus = listOf(0, 1), clusterKhz = listOf(1, 0)),
            PerfSample(4_000, RunPhase.VISIBLE, 20, threadCpus = listOf(0, 1), clusterKhz = listOf(1, 0))
        )
        val line = RunDiagnostics.describe(RunDiagnostics.summarize(samples, 60_000), listOf(CpuCluster(listOf(0), 1), CpuCluster(listOf(1), 0))).single()
        assertEquals("Display an, App offen: 4 s, 3,0× Echtzeit · Kerne 0–1", line)
    }

    @Test
    fun aWindowsProgressIsSpreadOverTheTimeItTook() {
        // whisper loads and decodes its first window while visible, but reports it only in the background.
        val samples = (0..4).map { PerfSample(it * 2_000L, RunPhase.VISIBLE, 0) } +
            (5..10).map { PerfSample(it * 2_000L, RunPhase.BACKGROUND, if (it == 10) 50 else 0) }
        val stats = RunDiagnostics.summarize(samples, 100_000)
        assertEquals(listOf(RunPhase.VISIBLE, RunPhase.BACKGROUND), stats.map { it.phase })
        assertEquals(10_000L, stats[0].wallMs)
        assertEquals(10_000L, stats[1].wallMs)
        assertEquals(2.5, stats[0].speed!!, 0.01)
        assertEquals(2.5, stats[1].speed!!, 0.01)
    }

    @Test
    fun coreRanges() {
        assertEquals("3–7", RunDiagnostics.ranges(listOf(7, 3, 4, 5, 6)))
        assertEquals("0,3–6", RunDiagnostics.ranges(listOf(0, 3, 4, 5, 6)))
        assertEquals("2", RunDiagnostics.ranges(listOf(2, 2)))
        assertEquals("0–1,4,6–7", RunDiagnostics.ranges(listOf(0, 1, 4, 6, 7)))
    }

    @Test
    fun parsesThreadStatWithSpacesInName() {
        val line = "4242 (ggml worker 3) R 4200 4200 0 0 -1 4194368 120 0 0 0 1500 30 0 0 20 0 6 0 98765 " +
            "123456789 4567 18446744073709551615 1 1 0 0 0 0 0 0 0 0 0 0 17 6 0 0 0 0 0"
        assertEquals(ProcStat('R', 6), ProcStat.parse(line))
        assertNull(ProcStat.parse("garbage"))
    }
}
