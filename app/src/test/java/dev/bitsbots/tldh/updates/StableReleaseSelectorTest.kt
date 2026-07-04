package dev.bitsbots.tldh.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StableReleaseSelectorTest {
    @Test
    fun selectsHighestStableSingleApkReleaseWithChecksum() {
        val releases = listOf(
            GitHubRelease("v0.3.0", draft = false, prerelease = true, assets = listOf(asset("tldh-0.3.0.apk", "abc"))),
            GitHubRelease("v0.2.0", draft = false, prerelease = false, assets = listOf(asset("tldh-0.2.0.apk", "abc"))),
            GitHubRelease("v0.4.0", draft = false, prerelease = false, yanked = true, assets = listOf(asset("tldh-0.4.0.apk", "abc"))),
            GitHubRelease("v0.5.0", draft = false, prerelease = false, assets = listOf(asset("tldh-0.5.0.apk", null)))
        )

        val selected = StableReleaseSelector().select("0.1.0", releases)

        assertEquals(SemVer(0, 2, 0), selected?.version)
        assertEquals("tldh-0.2.0.apk", selected?.apk?.name)
    }

    @Test
    fun ignoresLegacySplitFlavorAssets() {
        val releases = listOf(
            GitHubRelease(
                "v0.2.1",
                draft = false,
                prerelease = false,
                assets = listOf(
                    asset("tldh-offline-0.2.1.apk", "abc"),
                    asset("tldh-updater-0.2.1.apk", "def")
                )
            )
        )

        val selected = StableReleaseSelector().select("0.2.0", releases)

        assertNull(selected)
    }

    @Test
    fun returnsNullWhenNothingNewIsStable() {
        val releases = listOf(GitHubRelease("v0.1.0", draft = false, prerelease = false, assets = listOf(asset("tldh-0.1.0.apk", "abc"))))
        assertNull(StableReleaseSelector().select("0.1.0", releases))
    }

    private fun asset(name: String, sha: String?) = ReleaseAsset(name = name, downloadUrl = "https://example.invalid/$name", sha256 = sha, sizeBytes = 1)
}
