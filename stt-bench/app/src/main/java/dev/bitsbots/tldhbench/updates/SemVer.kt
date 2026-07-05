package dev.bitsbots.tldhbench.updates

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int = compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)
    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val regex = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")

        fun parse(value: String): SemVer? {
            val match = regex.matchEntire(value.trim()) ?: return null
            return SemVer(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt()
            )
        }
    }
}
