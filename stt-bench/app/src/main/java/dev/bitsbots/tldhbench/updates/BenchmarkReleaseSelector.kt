package dev.bitsbots.tldhbench.updates

data class BenchmarkUpdate(
    val version: SemVer,
    val apk: ReleaseAsset,
    val sourceTag: String
)

class BenchmarkReleaseSelector {
    fun select(currentVersion: String, releases: List<GitHubRelease>): BenchmarkUpdate? {
        val current = SemVer.parse(currentVersion.substringBefore('-')) ?: return null
        return releases.asSequence()
            .filterNot { it.draft || it.prerelease || it.yanked }
            .filter { it.tagName.startsWith("stt-bench-v") }
            .mapNotNull { release ->
                val versionText = release.tagName.removePrefix("stt-bench-")
                val version = SemVer.parse(versionText) ?: return@mapNotNull null
                val preferredName = "tldh-stt-bench-${version}.apk"
                val apk = release.assets.firstOrNull { asset ->
                    asset.name.equals(preferredName, ignoreCase = true) && !asset.sha256.isNullOrBlank()
                } ?: return@mapNotNull null
                BenchmarkUpdate(version = version, apk = apk, sourceTag = release.tagName)
            }
            .filter { it.version > current }
            .sortedByDescending { it.version }
            .firstOrNull()
    }
}
