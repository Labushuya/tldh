# Benchmark Plan

## v0.2.8 focus

Measure Vosk model speed and recognition quality on real German audio with explicit active-audio source tracking, word-level deviation analysis, conservative non-speech reduction, generated 30s/90s/4min longform profiles, and repeatable batch reports.

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

- Speed passes when total time stays within the target class.
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
