package dev.bitsbots.tldhbench.bench

enum class SttEngineReadiness {
    ACTIVE,
    NEXT_CANDIDATE,
    PLANNED,
    EXTERNAL
}

data class SttEngineSpec(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val readiness: SttEngineReadiness,
    val localMode: String,
    val expectedStrength: String,
    val expectedRisk: String,
    val nextStep: String
)

object SttEngineCatalog {
    val engines: List<SttEngineSpec> = listOf(
        SttEngineSpec(
            id = "vosk",
            displayName = "Vosk Android",
            shortLabel = "aktiv",
            readiness = SttEngineReadiness.ACTIVE,
            localMode = "On-device · offline · bereits benchmarkfähig",
            expectedStrength = "Sehr schnell auf dem HONOR Magic V2; gute Fast-Preview-Basis.",
            expectedRisk = "Reale WhatsApp-Audios lagen bisher bei ca. 40–47 % WER und sind damit zu riskant für automatische TL;DRs.",
            nextStep = "Als Baseline behalten und gegen weitere Engines auf denselben Audios vergleichen."
        ),
        SttEngineSpec(
            id = "whisper-cpp",
            displayName = "whisper.cpp",
            shortLabel = "ausführbar",
            readiness = SttEngineReadiness.ACTIVE,
            localMode = "On-device · offline · Modell-Prep + erster ausführbarer whisper.cpp Runner",
            expectedStrength = "Voraussichtlich deutlich robuster bei freier deutscher Sprache, Hintergrundgeräuschen und spontaner WhatsApp-Sprache.",
            expectedRisk = "Wahrscheinlich langsamer und größer als Vosk; tiny/base/small müssen real gegen dieselben Referenzen gemessen werden.",
            nextStep = "v0.3.4: erste echte Whisper-Transkription gegen dieselbe Audio-/Referenz-/WER-Pipeline testen."
        ),
        SttEngineSpec(
            id = "sherpa-onnx",
            displayName = "sherpa-onnx",
            shortLabel = "geplant",
            readiness = SttEngineReadiness.PLANNED,
            localMode = "On-device · offline · ONNX Runtime / Native Assets",
            expectedStrength = "Potentiell guter Mobile-Kompromiss aus Streaming, Offline-Betrieb und moderner Modellpipeline.",
            expectedRisk = "Deutsches Modell, Lizenz, Paketgröße und Android-Performance müssen erst belastbar geprüft werden.",
            nextStep = "Nach whisper.cpp als zweiter Nicht-Vosk-Kandidat einbauen, sofern ein passendes deutsches Modell feststeht."
        ),
        SttEngineSpec(
            id = "lan-whisper",
            displayName = "LAN/Tower Whisper",
            shortLabel = "Quality Mode",
            readiness = SttEngineReadiness.EXTERNAL,
            localMode = "Lokales Heimnetz · kein Cloud-Zwang · Server/Tower statt Handy",
            expectedStrength = "Realistischer Qualitätsmodus für lange oder wichtige Audios, wenn das Handy-Modell zu schwach ist.",
            expectedRisk = "Nicht mehr rein on-device; benötigt lokalen Dienst, Queue, Netzwerkstatus und Datenschutz-Guardrails.",
            nextStep = "Später als separater LAN-Quality-Bench gegen dieselben Referenztexte messen."
        )
    )

    val activeEngine: SttEngineSpec = engines.first { it.id == "vosk" }
}
