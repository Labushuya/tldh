#!/usr/bin/env bash
# Emulator smoke test: share an audio file into tl;dh the way WhatsApp does (ACTION_SEND with a
# content:// stream) and report crashes, the screen text and the service state.
set -u
APK="$1"
PKG=dev.bitsbots.tldh.debug
OUT=smoke-out
mkdir -p "$OUT"

adb wait-for-device
adb shell getprop ro.build.version.release
adb shell getprop ro.product.cpu.abilist
adb install -r -g --abi arm64-v8a "$APK" || adb install -r --abi arm64-v8a "$APK"
adb shell dumpsys package "$PKG" | grep -E "primaryCpuAbi|legacyNativeLibraryDir" | head -3
# Realistic: notification permission NOT granted (revoke what -g granted).
adb shell pm revoke "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

adb push smoke-audio.ogg /data/local/tmp/smoke-audio.ogg >/dev/null
adb push ggml-base-q5_1.bin /data/local/tmp/ggml-base-q5_1.bin >/dev/null
adb shell chmod 644 /data/local/tmp/smoke-audio.ogg /data/local/tmp/ggml-base-q5_1.bin
adb shell run-as "$PKG" mkdir -p cache/updates files/whisper-models
adb shell run-as "$PKG" cp /data/local/tmp/smoke-audio.ogg cache/updates/smoke-audio.ogg
adb shell run-as "$PKG" cp /data/local/tmp/ggml-base-q5_1.bin files/whisper-models/ggml-base-q5_1.bin
adb shell run-as "$PKG" ls -la cache/updates files/whisper-models

screen_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb shell cat /sdcard/ui.xml 2>/dev/null | grep -o 'text="[^"]\+"' | sed 's/text=//' | tr '\n' ' '
  echo
}

shot() {
  echo "== $1 =="
  adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity" | head -2
  echo "screen: $(screen_text)"
  adb shell dumpsys activity services "$PKG" | grep -E "ServiceRecord|isForeground|foregroundServiceType" | head -5
}

adb logcat -c
adb shell am start -W -a android.intent.action.SEND -t "audio/ogg" \
  --eu android.intent.extra.STREAM "content://$PKG.fileprovider/updates/smoke-audio.ogg" \
  -n "$PKG/dev.bitsbots.tldh.MainActivity"
sleep 6;  shot "6 s after share"
FG=$(adb shell dumpsys activity services "$PKG" | grep -E "isForeground=true" | head -1)
echo "foreground service: ${FG:-NOT IN FOREGROUND}"
sleep 20; shot "26 s after share"
adb shell input keyevent KEYCODE_HOME
sleep 15; shot "15 s after HOME (background)"
adb shell am start -W -n "$PKG/dev.bitsbots.tldh.MainActivity" >/dev/null
sleep 5;  shot "after reopening"
adb shell settings put system user_rotation 1 2>/dev/null; adb shell settings put system accelerometer_rotation 0
sleep 6;  shot "after rotation"

adb logcat -d > "$OUT/logcat.txt"
echo "== crashes =="
grep -nE "FATAL EXCEPTION|AndroidRuntime|Process: $PKG|RemoteServiceException|ForegroundService|did not then call|SecurityException" "$OUT/logcat.txt" | head -60
echo "== app log =="
grep -nE "tldh|whisper" "$OUT/logcat.txt" | grep -v "chatty" | head -80
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt" && grep -A3 "FATAL EXCEPTION" "$OUT/logcat.txt" | grep -q "$PKG"; then
  echo "SMOKE RESULT: CRASH"
  grep -A40 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -80
  exit 1
fi
if [ -z "$FG" ]; then
  echo "SMOKE RESULT: no crash, but the work service did not reach the foreground"
  exit 1
fi
echo "SMOKE RESULT: no crash, work service in foreground"
