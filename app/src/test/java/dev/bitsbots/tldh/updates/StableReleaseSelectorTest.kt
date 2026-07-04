package dev.bitsbots.tldh.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StableReleaseSelectorTest {
    @Test
    fun selectsHighestStableReleaseWithApkAndChecksum() {
        val releases = listOf(
            GitHubRelease("v0.3.0", draft = false, prerelease = true, assets = listOf(asset("tldh.apk", "abc"))),
            GitHubRelease("v0.2.0", draft = false, prerelease = false, assets = listOf(asset("tldh.apk", "abc"))),
            GitHubRelease("v0.4.0", draft = false, prerelease = false, yanked = true, assets = listOf(asset("tldh.apk", "abc"))),
            GitHubRelease("v0.5.0", draft = false, prerelease = false, assets = listOf(asset("tldh.apk", null)))
        )

        val selected = StableReleaseSelector().select("0.1.0", releases)

        assertEquals(SemVer(0, 2, 0), selected?.version)
    }

    @Test
    fun prefersConfiguredUpdaterAssetWhenRequested() {
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

        val selected = StableReleaseSelector(assetNameContains = "tldh-updater-").select("0.2.0", releases)

        assertEquals("tldh-updater-0.2.1.apk", selected?.apk?.name)
    }

    @Test
    fun returnsNullWhenNothingNewIsStable() {
        val releases = listOf(GitHubRelease("v0.1.0", draft = false, prerelease = false, assets = listOf(asset("tldh.apk", "abc"))))
        assertNull(StableReleaseSelector().select("0.1.0", releases))
    }

    private fun asset(name: String, sha: String?) = ReleaseAsset(name = name, downloadUrl = "https://example.invalid/$name", sha256 = sha, sizeBytes = 1)
}
