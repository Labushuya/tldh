package dev.bitsbots.tldh.updates

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sha256: String?,
    val sizeBytes: Long?
)

data class GitHubRelease(
    val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val yanked: Boolean = false,
    val assets: List<ReleaseAsset>
)

data class StableUpdate(
    val version: SemVer,
    val apk: ReleaseAsset,
    val sourceTag: String
)
