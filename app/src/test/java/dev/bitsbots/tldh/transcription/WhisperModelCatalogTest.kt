package dev.bitsbots.tldh.transcription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperModelCatalogTest {
    private val gib = 1024L * 1024L * 1024L

    private val modernCpuinfo = """
        processor	: 0
        Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp
        CPU part	: 0xd46

        processor	: 7
        Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm lrcpc dcpop asimddp sve2
        CPU part	: 0xd4e
    """.trimIndent()

    // Cortex-A53 era phone: plain armv8.0-a, no dotprod/fp16.
    private val oldCpuinfo = """
        processor	: 0
        Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32 cpuid
        CPU part	: 0xd03
    """.trimIndent()

    @Test fun parsesModernCpuFeatures() {
        val features = CpuFeatures.parse(modernCpuinfo)
        assertTrue(features.dotProd)
        assertTrue(features.fp16)
        assertTrue(features.supportsDotProdBuild)
    }

    @Test fun featureMissingOnOneCoreDoesNotCount() {
        val mixed = modernCpuinfo + "\n\nprocessor\t: 8\nFeatures\t: fp asimd evtstrm crc32 cpuid\n"
        assertFalse(CpuFeatures.parse(mixed).supportsDotProdBuild)
    }

    @Test fun parsesOldCpuFeatures() {
        assertFalse(CpuFeatures.parse(oldCpuinfo).supportsDotProdBuild)
        assertFalse(CpuFeatures.parse("").supportsDotProdBuild)
    }

    @Test fun recommendsSmallOnlyForFastCpuWithEnoughRam() {
        val modern = CpuFeatures.parse(modernCpuinfo)
        assertEquals(WhisperModelCatalog.small, WhisperModelCatalog.recommendedFor(DeviceProfile(16 * gib, modern, arm64 = true)))
        assertEquals(WhisperModelCatalog.base, WhisperModelCatalog.recommendedFor(DeviceProfile(4 * gib, modern, arm64 = true)))
        assertEquals(WhisperModelCatalog.base, WhisperModelCatalog.recommendedFor(DeviceProfile(16 * gib, CpuFeatures.parse(oldCpuinfo), arm64 = true)))
    }

    @Test fun modelUrlsArePinnedToARevision() {
        WhisperModelCatalog.models.forEach { spec ->
            assertTrue(spec.url.contains("/resolve/5359861c739e955e79d9a303bcbc70fb988958b1/"))
            assertEquals(64, spec.sha256.length)
        }
    }
}
