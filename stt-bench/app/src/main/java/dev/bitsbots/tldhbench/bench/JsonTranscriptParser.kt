package dev.bitsbots.tldhbench.bench

import org.json.JSONObject

internal object JsonTranscriptParser {
    fun parseSegments(jsonObjects: List<String>): Pair<String, List<TranscriptSegment>> {
        val segments = mutableListOf<TranscriptSegment>()
        val transcriptParts = mutableListOf<String>()

        jsonObjects.forEach { raw ->
            val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return@forEach
            val text = obj.optString("text", "").trim()
            if (text.isBlank()) return@forEach
            transcriptParts += text

            val words = mutableListOf<TranscriptWord>()
            val arr = obj.optJSONArray("result")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val wordObj = arr.optJSONObject(i) ?: continue
                    val word = wordObj.optString("word", "").trim()
                    if (word.isBlank()) continue
                    words += TranscriptWord(
                        word = word,
                        startSec = wordObj.optDoubleOrNull("start"),
                        endSec = wordObj.optDoubleOrNull("end"),
                        confidence = wordObj.optDoubleOrNull("conf")
                    )
                }
            }

            segments += TranscriptSegment(
                startSec = words.firstOrNull()?.startSec,
                endSec = words.lastOrNull()?.endSec,
                text = text,
                words = words
            )
        }

        return transcriptParts.joinToString(" ").trim() to segments
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name) else null
}
