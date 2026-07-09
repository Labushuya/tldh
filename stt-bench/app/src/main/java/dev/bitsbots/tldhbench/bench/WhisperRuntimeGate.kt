package dev.bitsbots.tldhbench.bench

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes access to the native whisper.cpp wrapper.
 *
 * The current Android wrapper owns native/global state. Running two Whisper calls at the same
 * time, or re-entering immediately after an interrupted run, can leave the app in a state where
 * subsequent benchmarks fail until the process is restarted. The UI already disables parallel
 * buttons, but this process-wide gate protects batch runs, recompositions and future callers too.
 */
internal object WhisperRuntimeGate {
    private val mutex = Mutex()

    suspend fun <T> runExclusive(block: suspend () -> T): T = mutex.withLock { block() }
}
