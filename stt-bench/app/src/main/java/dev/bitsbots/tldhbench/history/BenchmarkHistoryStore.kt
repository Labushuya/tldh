package dev.bitsbots.tldhbench.history

import android.content.Context
import dev.bitsbots.tldhbench.bench.BenchmarkResult
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

private const val PREFS_NAME = "tldh_stt_bench_history"
private const val KEY_ITEMS = "items"
private const val MAX_ITEMS = 5

data class BenchmarkHistoryItem(
    val timestampMs: Long,
    val engine: String,
    val model: String,
    val modelId: String,
    val audioName: String?,
    val mimeType: String?,
    val durationMs: Long?,
    val totalMs: Long,
    val decodeMs: Long,
    val modelLoadMs: Long,
    val sttMs: Long,
    val rtf: Double?,
    val verdict: String,
    val passed: Boolean?,
    val transcriptPreview: String,
    val transcriptFull: String,
    val warningsCount: Int,
    val werPercent: Double?,
    val cerPercent: Double?,
    val comparisonLabel: String?,
    val comparisonSummary: String?,
    val referencePreview: String?
)

class BenchmarkHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<BenchmarkHistoryItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    add(obj.toHistoryItem())
                }
            }.take(MAX_ITEMS)
        }.getOrElse { emptyList() }
    }

    fun add(result: BenchmarkResult) {
        val next = (listOf(result.toHistoryItem()) + load()).take(MAX_ITEMS)
        save(next)
    }

    fun clear() {
        prefs.edit().remove(KEY_ITEMS).apply()
    }

    private fun save(items: List<BenchmarkHistoryItem>) {
        val array = JSONArray()
        items.take(MAX_ITEMS).forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }
}

private fun BenchmarkResult.toHistoryItem(): BenchmarkHistoryItem {
    val fullTranscript = if (segments.isNotEmpty()) {
        segments.joinToString("\n") { segment ->
            val start = segment.startSec?.let { formatSeconds(it) } ?: "?:??"
            val end = segment.endSec?.let { formatSeconds(it) } ?: "?:??"
            "$start–$end  ${segment.text}"
        }
    } else {
        transcript.trim()
    }.ifBlank { "Kein Transkript erkannt." }

    val preview = fullTranscript
        .replace(Regex("\\s+"), " ")
        .trim()
        .let { if (it.length > 260) it.take(260).trimEnd() + "…" else it }

    val comparison = referenceComparison
    val referencePreview = comparison?.referenceRaw
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.let { if (it.length > 220) it.take(220).trimEnd() + "…" else it }

    return BenchmarkHistoryItem(
        timestampMs = System.currentTimeMillis(),
        engine = engine,
        model = model,
        modelId = modelId,
        audioName = metadata.displayName,
        mimeType = metadata.mimeType,
        durationMs = metadata.durationMs,
        totalMs = timing.totalMs,
        decodeMs = timing.decodeMs,
        modelLoadMs = timing.modelLoadMs,
        sttMs = timing.sttMs,
        rtf = timing.rtf,
        verdict = verdict.message,
        passed = verdict.passed,
        transcriptPreview = preview.ifBlank { "Kein Transkript erkannt." },
        transcriptFull = fullTranscript,
        warningsCount = warnings.size,
        werPercent = comparison?.werPercent,
        cerPercent = comparison?.cerPercent,
        comparisonLabel = comparison?.qualityLabel,
        comparisonSummary = comparison?.summary,
        referencePreview = referencePreview
    )
}

private fun BenchmarkHistoryItem.toJson(): JSONObject = JSONObject()
    .put("timestampMs", timestampMs)
    .put("engine", engine)
    .put("model", model)
    .put("modelId", modelId)
    .put("audioName", audioName)
    .put("mimeType", mimeType)
    .put("durationMs", durationMs)
    .put("totalMs", totalMs)
    .put("decodeMs", decodeMs)
    .put("modelLoadMs", modelLoadMs)
    .put("sttMs", sttMs)
    .put("rtf", rtf)
    .put("verdict", verdict)
    .put("passed", passed)
    .put("transcriptPreview", transcriptPreview)
    .put("transcriptFull", transcriptFull)
    .put("warningsCount", warningsCount)
    .put("werPercent", werPercent)
    .put("cerPercent", cerPercent)
    .put("comparisonLabel", comparisonLabel)
    .put("comparisonSummary", comparisonSummary)
    .put("referencePreview", referencePreview)

private fun JSONObject.toHistoryItem(): BenchmarkHistoryItem = BenchmarkHistoryItem(
    timestampMs = optLong("timestampMs", 0L),
    engine = optString("engine", "Vosk"),
    model = optString("model", "unbekannt"),
    modelId = optString("modelId", "unknown"),
    audioName = optNullableString("audioName"),
    mimeType = optNullableString("mimeType"),
    durationMs = optNullableLong("durationMs"),
    totalMs = optLong("totalMs", 0L),
    decodeMs = optLong("decodeMs", 0L),
    modelLoadMs = optLong("modelLoadMs", 0L),
    sttMs = optLong("sttMs", 0L),
    rtf = optNullableDouble("rtf"),
    verdict = optString("verdict", "kein Verdict gespeichert"),
    passed = optNullableBoolean("passed"),
    transcriptPreview = optString("transcriptPreview", ""),
    transcriptFull = optString("transcriptFull", optString("transcriptPreview", "")),
    warningsCount = optInt("warningsCount", 0),
    werPercent = optNullableDouble("werPercent"),
    cerPercent = optNullableDouble("cerPercent"),
    comparisonLabel = optNullableString("comparisonLabel"),
    comparisonSummary = optNullableString("comparisonSummary"),
    referencePreview = optNullableString("referencePreview")
)

private fun JSONObject.optNullableString(key: String): String? = if (!has(key) || isNull(key)) null else optString(key)
private fun JSONObject.optNullableLong(key: String): Long? = if (!has(key) || isNull(key)) null else optLong(key)
private fun JSONObject.optNullableDouble(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }
private fun JSONObject.optNullableBoolean(key: String): Boolean? = if (!has(key) || isNull(key)) null else optBoolean(key)

private fun formatSeconds(sec: Double): String {
    val total = sec.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun Double.formatRtf(): String = ((this * 100.0).roundToInt() / 100.0).toString()
