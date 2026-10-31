#!/usr/bin/env bash
# End-to-end on an emulator: share a ~3 min German voice note, leave the app, turn the display
# off, force Doze, come back, rotate, then wait for the result and walk through it. Fails on any
# crash, ANR, app death or missing result.
set -u
APK="$1"
PKG=dev.bitsbots.tldh.debug
OUT=e2e-out
mkdir -p "$OUT"

adb wait-for-device
echo "Android $(adb shell getprop ro.build.version.release), CPU flags: $(adb shell grep -m1 -o -E 'avx2|fma|f16c' /proc/cpuinfo | sort -u | tr '\n' ' ')"
adb install -r "$APK" >/dev/null
adb shell dumpsys package "$PKG" | grep -m1 primaryCpuAbi
adb push e2e-audio.ogg /data/local/tmp/e2e-audio.ogg >/dev/null
adb push ggml-base-q5_1.bin /data/local/tmp/ggml-base-q5_1.bin >/dev/null
adb shell chmod 644 /data/local/tmp/e2e-audio.ogg /data/local/tmp/ggml-base-q5_1.bin
adb shell run-as "$PKG" mkdir -p cache/updates files/whisper-models
adb shell run-as "$PKG" cp /data/local/tmp/e2e-audio.ogg cache/updates/e2e-audio.ogg
adb shell run-as "$PKG" cp /data/local/tmp/ggml-base-q5_1.bin files/whisper-models/ggml-base-q5_1.bin

dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb shell cat /sdcard/ui.xml 2>/dev/null; }
screen_text() { dump | grep -o 'text="[^"]\+"' | sed 's/text=//' | tr '\n' ' '; echo; }
tap_text() {
  local b
  b=$(dump | tr '>' '\n' | grep -F "text=\"$1\"" | head -1 | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | tr '\n' ' ')
  # shellcheck disable=SC2086
  set -- $b
  if [ $# -eq 4 ]; then adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )); echo "tapped"; else echo "not found"; fi
}
alive() { adb shell pidof "$PKG" >/dev/null 2>&1 && echo "process alive (pid $(adb shell pidof "$PKG"))" || echo "PROCESS NOT RUNNING"; }
fg() { adb shell dumpsys activity services "$PKG" | grep -m1 -o "isForeground=[a-z]* foregroundId=[0-9]* types=[0-9a-fx]*" || echo "service not running"; }
shot() { echo "== $1 =="; alive; fg; echo "screen: $(screen_text)"; }

adb logcat -c
adb shell am start -W -a android.intent.action.SEND -t "audio/ogg" \
  --eu android.intent.extra.STREAM "content://$PKG.fileprovider/updates/e2e-audio.ogg" \
  -n "$PKG/dev.bitsbots.tldh.MainActivity" >/dev/null
sleep 8;  shot "8 s after share"
echo "notification dialog: allow -> $(tap_text "Allow")"
sleep 4;  shot "processing visible"
adb shell input keyevent KEYCODE_HOME
sleep 25; shot "25 s in background"
adb shell input keyevent KEYCODE_SLEEP
adb shell dumpsys deviceidle force-idle >/dev/null 2>&1
sleep 45; shot "45 s display off + Doze"
adb shell dumpsys deviceidle unforce >/dev/null 2>&1
adb shell input keyevent KEYCODE_WAKEUP; adb shell wm dismiss-keyguard >/dev/null 2>&1; adb shell input keyevent 82
adb shell am start -W -n "$PKG/dev.bitsbots.tldh.MainActivity" >/dev/null
sleep 5;  shot "reopened"
adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1
sleep 8;  shot "rotated"
adb shell settings put system user_rotation 0
sleep 4

RESULT=none
for i in $(seq 1 90); do
  text=$(screen_text)
  case "$text" in
    *"Kurz gesagt"*) RESULT=ok; break ;;
    *"Das hat nicht geklappt"*|*"Diese Datei geht leider nicht"*|*"nicht möglich"*) RESULT=error; break ;;
    *"keeps stopping"*|*"isn't responding"*) RESULT=crash; break ;;
  esac
  if ! adb shell pidof "$PKG" >/dev/null 2>&1; then RESULT=died; break; fi
  [ $((i % 4)) -eq 0 ] && echo "waiting ($((i * 15)) s): $(echo "$text" | grep -o '"[0-9]* %"' | head -1) $(alive)"
  sleep 15
done
shot "final state: $RESULT"

if [ "$RESULT" = ok ]; then
  for i in 1 2 3 4 5 6 7 8; do
    adb shell input swipe 160 520 160 140 350; sleep 1
    echo "-- scroll $i: $(screen_text | cut -c1-600)"
  done
  echo "expand transcript -> $(tap_text "Ganz anzeigen")"; sleep 2
  echo "tone -> $(tap_text "förmlich")"; sleep 1
  for i in 1 2 3; do adb shell input swipe 160 520 160 140 350; sleep 1; done
  shot "after interacting with the result"
  echo "close -> $(tap_text "Session löschen")"; sleep 3
  shot "after closing"
fi

adb logcat -d > "$OUT/logcat.txt"
echo "== crashes, ANRs, kills =="
grep -nE "FATAL EXCEPTION|ANR in|Process $PKG .*has died|Killing [0-9]+:$PKG|lowmemorykiller.*$PKG|Fatal signal|tombstone|Foreground service refused" "$OUT/logcat.txt" | head -40
grep -A30 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60
echo "== whisper / tldh =="
grep -nE "TldhWorkService|whisper-cli|libwhisper" "$OUT/logcat.txt" | head -20

if grep -qE "FATAL EXCEPTION|ANR in $PKG|Fatal signal" "$OUT/logcat.txt" || [ "$RESULT" != ok ]; then
  echo "E2E RESULT: FAILED ($RESULT)"
  exit 1
fi
echo "E2E RESULT: result shown, no crash"
