package dev.bitsbots.tldh.session

import java.io.File
import java.util.UUID

class SessionManager(
    private val filesDir: File,
    private val cacheDir: File
) {
    private var currentSessionId: String? = null

    fun newSessionId(): String {
        val id = "tldh-session-${UUID.randomUUID()}"
        currentSessionId = id
        File(cacheDir, id).mkdirs()
        return id
    }

    fun wipeCurrentSession() {
        currentSessionId?.let { id ->
            listOf(File(cacheDir, id), File(filesDir, id)).forEach { it.deleteRecursively() }
        }
        currentSessionId = null
    }

    fun wipeOrphanedSessions() {
        listOf(cacheDir, filesDir).forEach { root ->
            root.listFiles()
                ?.filter { it.name.startsWith("tldh-session-") }
                ?.forEach { it.deleteRecursively() }
        }
    }
}
