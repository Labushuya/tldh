# Architecture

## Current v0.6.0 shape

```text
ACTION_SEND Intent
  ↓
MainActivity → SessionManager (wipe previous session)
  ↓
ShareIntentReader → AudioIngestor (format, size, duration guardrails)
  ↓
PcmAudioPreparer (MediaCodec decode → 16 kHz mono PCM16)
  ↓
WhisperCliTranscriber (whisper-cli child process, --output-json-full)
  ↓
WhisperJsonParser → WordAssembler (words + token probabilities + timestamps)
  ↓
TranscriptAnalyzer (SentenceSplitter, CriticalWordDetector, sentence scoring)
  ↓
RuleBasedSummaryEngine (verbatim TL;DR, questions, dates/numbers, tasks, uncertain critical words)
  ↓
ReplySuggester (intent rules + reply templates in three tones) · NoteFormatter
  ↓
Compose Result UI (Prüfen card, key points, replies, highlighted transcript, SnippetPlayer)
```

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
