# Benchmark Plan

## v0.3.3 focus

Keep Vosk as the executable baseline and start the practical whisper.cpp path with model-preflight: download, storage, cleanup and candidate selection for tiny/base/small before enabling Native/JNI transcription.

## Test flow

1. Install/select Vosk model.
2. Share audio into `tl;dh STT Bench`, select a gold-standard sample, or generate a 30s/90s/4min longform WAV profile.
3. Confirm that the reference field contains the expected transcript when WER/CER is needed.
4. Run benchmark.
5. Review:
   - total time
   - RTF
   - transcript with timestamps
   - WER / CER
   - S/I/D and total word errors
   - first word-level deviations
   - reference vs recognized text
6. Compare the last 5 benchmark runs in history.
7. For model-level comparison, run the installed gold-standard corpus as a batch and copy the Markdown report.
8. For realistic long audio behavior, generate one of the single-file longform profiles and run it as an Einzelbenchmark.
9. For aggregate model comparison, select 3×, 8×, or 20× Corpus repeats before starting the batch. These are repeated corpus runs, not single concatenated audios.

## Interpretation

- Speed passes when total time stays within the target class. For 180-380s longform clips, the target is faster-than-listening (`RTF <= 1.0`).
- Recognition quality is considered usable only if WER/CER are low enough for TL;DR extraction and critical terms such as negations, times, dates, names and quantities are correctly recognized.

## Longform interpretation

- The generated longform WAV is one actual audio file, not just a UI-level loop.
- WER/CER compares the joined reference transcript against the recognized Vosk transcript.
- 30s approximates a short voice message; 90s approximates a longer real message; 4min stress-tests local speed and memory behavior.

## Batch interpretation

- Average RTF shows whether the model has product-level speed headroom.
- Average WER/CER shows whether the model is improving beyond anecdotal single clips.
- Worst WER sample identifies which sentence type currently breaks the model.
- Speed-pass count is only evaluated for samples with a defined target duration class.

## Next candidates

If Vosk small remains too inaccurate, compare the new non-speech-reduction results against previous runs, then continue with context vocabulary, larger Vosk models, or other STT engines.


## UX notes

The app is split into sections instead of one long scroll stack. The active-audio card must always make clear whether the source is Android Share, Goldstandard or Longform. Buttons use full-width responsive action rows to avoid cramped labels on phone screens. After single or batch benchmark completion, the app routes to **Ergebnisse** so the user does not have to hunt for the output. The **Updates** section restores the manual in-app installer for benchmark releases.


## Android crash guard

`Big DE TUDA 0.6` is intentionally blocked for on-device Android execution in this benchmark build. The model is extremely large and can terminate the app at native Vosk model-load time before Kotlin error handling can show a clean failure. Treat it as a later Tower/LAN quality-mode candidate.


## v0.3.0 engine comparison policy

Vosk remains the active executable baseline. Do not integrate Vosk into the main tl;dh app as an automatic TL;DR source until real-audio WER is much lower. The next benchmark milestone is to add whisper.cpp as a second executable engine and run the same gold-standard and real WhatsApp audios against the same reference transcripts.

Product-readiness thresholds used by the app:

- <= 15% WER and low deletion risk: product candidate.
- <= 25% WER and RTF <= 1.0: only with visible guardrails and full transcript review.
- > 25-35% WER: preview-only / not enough for automatic TL;DR.
- High deletion count: risky even if CER looks acceptable, because missing negations, times, names or numbers can invert meaning.


## v0.3.3 whisper.cpp preflight protocol

1. Open **Engines**.
2. Download `Whisper tiny` first. Confirm the model reaches `bereit`.
3. Repeat with `Whisper base` if storage and download time are acceptable.
4. Download `Whisper small` only as a deliberate quality-candidate preparation step.
5. No Whisper benchmark is expected yet in v0.3.3. Vosk remains the executable engine until the Native/JNI adapter lands.
6. v0.3.3 target: use the already prepared PCM file and reference-comparison pipeline, run whisper.cpp, then compare WER/CER/S/I/D and RTF against the same Vosk reports.
