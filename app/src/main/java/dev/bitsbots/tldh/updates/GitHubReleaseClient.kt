package dev.bitsbots.tldh.updates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

class GitHubReleaseClient(
    private val repositorySlug: String
) {
    suspend fun fetchStableReleases(): List<GitHubRelease> = withContext(Dispatchers.IO) {
        require(repositorySlug.contains('/')) { "GitHub Repository ist nicht konfiguriert." }
        val response = getText("https://api.github.com/repos/$repositorySlug/releases?per_page=30")
        val releases = JSONArray(response)
        buildList {
            for (index in 0 until releases.length()) {
                val releaseJson = releases.getJSONObject(index)
                val assetsJson = releaseJson.getJSONArray("assets")
                val checksumUrl = (0 until assetsJson.length())
                    .asSequence()
                    .map { assetsJson.getJSONObject(it) }
                    .firstOrNull { it.optString("name") == "SHA256SUMS.txt" }
                    ?.optString("browser_download_url")
                    ?.takeIf { it.isNotBlank() }
                val checksums = checksumUrl?.let { parseChecksumFile(getText(it)) }.orEmpty()
                val assets = buildList {
                    for (assetIndex in 0 until assetsJson.length()) {
                        val assetJson = assetsJson.getJSONObject(assetIndex)
                        val name = assetJson.optString("name")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            add(
                                ReleaseAsset(
                                    name = name,
                                    downloadUrl = assetJson.optString("browser_download_url"),
                                    sha256 = checksums[name],
                                    sizeBytes = assetJson.optLong("size", -1).takeIf { it >= 0 }
                                )
                            )
                        }
                    }
                }
                val body = releaseJson.optString("body", "")
                add(
                    GitHubRelease(
                        tagName = releaseJson.optString("tag_name"),
                        draft = releaseJson.optBoolean("draft", true),
                        prerelease = releaseJson.optBoolean("prerelease", true),
                        yanked = body.contains("yanked: true", ignoreCase = true) || body.contains("[yanked]", ignoreCase = true),
                        assets = assets
                    )
                )
            }
        }
    }

    suspend fun downloadAsset(asset: ReleaseAsset, destinationDir: File, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        require(asset.downloadUrl.startsWith("https://")) { "Ungültige Download-URL." }
        destinationDir.mkdirs()
        val target = File(destinationDir, asset.name)
        val tmp = File(destinationDir, "${asset.name}.part")
        if (tmp.exists()) tmp.delete()
        if (target.exists()) target.delete()
        var connection: HttpURLConnection? = null
        try {
            connection = openConnection(asset.downloadUrl)
            val expectedBytes = asset.sizeBytes?.takeIf { it > 0 }
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (expectedBytes != null) {
                            progress((written.toFloat() / max(expectedBytes, 1L)).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            val actual = Checksum.sha256(tmp)
            val expected = asset.sha256 ?: error("Release enthält keine SHA256-Prüfsumme.")
            if (!actual.equals(expected, ignoreCase = true)) {
                tmp.delete()
                error("SHA256-Prüfung fehlgeschlagen. Download wurde verworfen.")
            }
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            progress(1f)
            target
        } catch (io: IOException) {
            tmp.delete()
            throw IllegalStateException(
                "Download wurde unterbrochen. tl;dh hält das Gerät während des Downloads wach; lasse die App trotzdem geöffnet und starte den Download erneut, falls Android oder die Netzwerkverbindung ihn beendet hat.",
                io
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun getText(url: String): String {
        val connection = openConnection(url)
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun openConnection(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 120_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "tldh-android-updater")
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorText = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val hint = when (code) {
                404 -> " Repository nicht gefunden oder nicht öffentlich erreichbar. Für den App-Updater muss das Release-Repo öffentlich sein oder ein späterer authentifizierter Update-Kanal implementiert werden."
                403 -> " Zugriff wurde begrenzt oder verweigert. Später erneut versuchen oder GitHub-Rate-Limit prüfen."
                else -> ""
            }
            error("GitHub antwortete mit HTTP $code.$hint $errorText")
        }
        return connection
    }

    private fun parseChecksumFile(text: String): Map<String, String> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split(Regex("\\s+"), limit = 2)
            val sha = parts.getOrNull(0)?.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) } ?: return@mapNotNull null
            val name = parts.getOrNull(1)?.trim()?.trimStart('*') ?: return@mapNotNull null
            name to sha.lowercase()
        }
        .toMap()
}
