# Architecture

## Current v0.7.1 shape

```text
ACTION_SEND intent (share)        ACTION_OPEN_DOCUMENT ("Audiodatei öffnen")
  ↓                                 ↓
MainActivity (handles the intent once) → TldhEngine (process-wide; state machine
                                          Idle → NeedsModel → Processing → Result | Error)
  ↓                                        ↘ TldhWorkService (foreground service + wake lock while work runs)
  ↓
SessionManager (wipe previous session, new session folder)
  ↓
AudioIngestor (format detection, size and duration limits)
  ↓
PcmAudioPreparer (MediaCodec decode → 16 kHz mono PCM16) → WaveformSampler (loudness bars)
  ↓
WhisperCliTranscriber (whisper-cli child process; progress from stderr, live segments from stdout,
                       --output-json-full for words)
  ↓
WhisperJsonParser → WordAssembler (words + token probabilities + timestamps)
  ↓
TranscriptAnalyzer (SentenceSplitter, CriticalWordDetector, sentence scoring)
  ↓
RuleBasedSummaryEngine (verbatim TL;DR, questions, dates/numbers, tasks, uncertain critical words)
  ↓
ReplySuggester (intent rules + reply templates in three tones) · NoteFormatter
  ↓
Compose screens: Start · Setup · Processing · Result · Settings · Error
  ↓
Close result → session wipe → Start with confirmation
```

`engine/TldhEngine.kt` owns the pipeline, model downloads and the update download as `StateFlow`s, so configuration changes and leaving the app never interrupt them; `engine/TldhWorkService.kt` keeps the process alive with a progress notification while work runs. UI code lives in `ui/`: `TldhApp.kt` (observes the engine, navigation), `screens/` (one file per screen), `components/` (glass, controls, surfaces, waveform, icons) and `theme/` (tokens from [DESIGN.md](../DESIGN.md)). Pure text formatting for the screens is in `ui/UiText.kt` and unit-tested.

Glass: `Modifier.glassSource` records the scrolling content into a `GraphicsLayer`; `GlassPane` draws that layer again with a `BlurEffect`, clipped to its shape, under a tint and a hairline. On Android < 12 the pane falls back to a near-opaque tint.

`whisper-cli` is built from `whisper.cpp` source by `scripts/build-whisper-android.sh` (Gradle task `buildWhisperCli`) and packaged as `lib/arm64-v8a/libwhisper_cli*.so`, so Android extracts it into the native library dir, the one app location it may execute from.

## Target architecture

```text
Share Intent
  ↓
SessionManager
  ↓
AudioIngestor
  ↓
AudioDecoder / Normalizer
  ↓
whisper.cpp transcription (child process)
  ↓
Local Summarizer
  ↓
Structured Result UI
  ↓
Session Wipe
```

## Key constraints

- Content-URI first.
- No broad storage permissions.
- No runtime network for audio processing.
- No database in MVP.
- HONOR Magic V2 is the primary hardware target.
