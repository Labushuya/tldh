# Benchmark Plan

## v0.3.4 focus

Keep Vosk as the speed baseline and activate the first executable whisper.cpp path. The same decoded/preprocessed audio, reference text, WER/CER/S/I/D metrics, history and reports must be used so Vosk and Whisper can be compared fairly.

## Test flow

1. Install/select either a Vosk model or a Whisper model.
2. Share audio into `tl;dh STT Bench`, select a gold-standard sample, or generate a 30s/90s/4min longform WAV profile.
3. Confirm that the reference field contains the expected transcript when WER/CER is needed.
4. Run benchmark.
5. Review:
   - total time
   - RTF
   - transcript; Vosk has timestamps, the first Whisper path has full text only
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
- WER/CER compares the joined reference transcript against the recognized transcript of the active engine.
- 30s approximates a short voice message; 90s approximates a longer real message; 4min stress-tests local speed and memory behavior.

## Batch interpretation

- Average RTF shows whether the model has product-level speed headroom.
- Average WER/CER shows whether the model is improving beyond anecdotal single clips.
- Worst WER sample identifies which sentence type currently breaks the model.
- Speed-pass count is only evaluated for samples with a defined target duration class.

## Next candidates

If Vosk remains too inaccurate, compare Whisper tiny/base/small on the same real audios. Only consider tl;dh integration when WER drops meaningfully below the previous 40-47% Vosk real-audio baseline and RTF remains acceptable.


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


## v0.3.4 whisper.cpp test protocol

1. Open **Engines**.
2. Download `Whisper tiny` first and set whisper.cpp active.
3. Run the same real Shared-Audio + reference text used for the Vosk reports.
4. Repeat with `Whisper base`.
5. Test `Whisper small` only if tiny/base are stable and storage/battery/time are acceptable.
6. Compare RTF, WER, CER, S/I/D and deletion risk against Vosk Small DE 0.15 and Big DE 0.21.
7. Treat crashes, very long hangs or RTF > 1.0 on 4-6 minute audios as strong signals for LAN/Tower quality mode instead of phone-only product integration.


## v0.6.0 Whistle test protocol

Desktop pre-test before integration (Linux x86-64 `needle` CLI, 49 Kerstin gold-standard sentences ≈ 4:10 min, strict WER as in `ReferenceTextComparator`; "WhatsApp-like" = Opus 16 kbit/s VoIP, "noisy" = additionally pink noise at 10 dB SNR):

| Engine | clean | WhatsApp-like | noisy | 4:26 min clean (deletions) |
|---|---:|---:|---:|---:|
| Whisper base (faster-whisper int8) | 14.5 % | 15.0 % | 21.0 % | 14.7 % (4) |
| Whistle, engine windows | 15.7 % | 16.0 % | 21.2 % | 20.2 % (42) |
| Whistle, app pause chunking ≤ 25 s | – | – | – | 15.7 % (8) |
| Whisper small (faster-whisper int8) | 10.0 % | 10.3 % | 11.6 % | 8.6 % (4) |

Takeaways: Whistle ≈ Whisper base at one eighth of the size; Whisper small is the only clear quality step. Whistle's own 30 s windowing dropped three consecutive sentences at one window boundary (reproducible), which app-side pause chunking avoids. Desktop speed does not transfer to the phone (x86 vs. ARM), so speed must be measured on the Magic V2.

On-device protocol:

1. **Engines** → load the Whistle model (16.9 MB) and activate Whistle.
2. Run the gold-standard batch (1× corpus) with **Pausen-Chunking**. Compare WER/CER/S/I/D and RTF against whisper.cpp base on the same corpus.
3. Generate the **4 min longform** audio and run it once with **Pausen-Chunking** and once with **Engine-Fenster**. Deletions should stay low with pause chunking; a block of missing sentences in the engine-window run confirms the windowing issue on device.
4. Repeat step 2 with **Pausen-Chunking · Tiefe 4** to see whether the faster decoder is worth the extra errors on the phone.
5. Run the same real WhatsApp audios + reference texts used for the Vosk/Whisper reports.
6. Whistle is only interesting for tl;dh if it is clearly faster than whisper.cpp base on the phone at similar WER; otherwise whisper.cpp small (quality) or base (speed) remain the candidates.
