package dev.bitsbots.tldh.updates

class StableReleaseSelector {
    fun select(currentVersion: String, releases: List<GitHubRelease>): StableUpdate? {
        val current = SemVer.parse(currentVersion.substringBefore('-')) ?: return null
        return releases.asSequence()
            .filterNot { it.draft || it.prerelease || it.yanked }
            .mapNotNull { release ->
                val version = SemVer.parse(release.tagName) ?: return@mapNotNull null
                val preferredName = "tldh-${version}.apk"
                val apk = release.assets.firstOrNull { asset ->
                    asset.name.equals(preferredName, ignoreCase = true) &&
                        !asset.sha256.isNullOrBlank()
                } ?: release.assets.firstOrNull { asset ->
                    asset.name.equals("tldh.apk", ignoreCase = true) &&
                        !asset.sha256.isNullOrBlank()
                } ?: return@mapNotNull null
                StableUpdate(version = version, apk = apk, sourceTag = release.tagName)
            }
            .filter { it.version > current }
            .sortedByDescending { it.version }
            .firstOrNull()
    }
}
