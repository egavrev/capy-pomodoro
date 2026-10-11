#!/usr/bin/env bash
# Drives the installed app on an emulator and captures screenshots + logs into $OUT.
# Used by the "screenshots" CI job; runs on any device reachable with adb.
set -u
OUT=${OUT:-shots}
PKG=com.capypomodoro
mkdir -p "$OUT"

shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }

# Tap the first node whose content-desc or text matches $1 exactly.
tap() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
want = sys.argv[1]
for n in ET.parse('/tmp/ui.xml').iter('node'):
    if want in (n.get('content-desc'), n.get('text')):
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
PY
)
  if [ -z "$xy" ]; then echo "!! not found: $1"; cp /tmp/ui.xml "$OUT/missing-$(echo "$1" | tr -c 'a-zA-Z0-9' _).xml"; return 0; fi
  adb shell input tap $xy
  echo "tap $1 at $xy"
}

adb logcat -c
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell am start -W -n $PKG/.MainActivity
shot 01_focus_idle 5
tap "Start timer";            shot 02_focus_running 4
tap "Mute music";             shot 03_focus_muted 2
tap "Unmute music"
adb shell cmd statusbar expand-notifications; shot 04_notification 3
adb shell cmd statusbar collapse; sleep 1
tap "Skip to next phase";     shot 05_break_running 4
tap "Pause timer";            shot 06_break_paused 2
tap "Long break";             shot 07_long_break 2
tap "Settings";               shot 08_settings_top 3
adb shell input swipe 540 1900 540 700 400; shot 09_settings_middle 2
adb shell input swipe 540 1900 540 700 400; shot 10_settings_bottom 2
tap "Pink gingham";           shot 11_settings_gingham 2
tap "Ocean waves"
tap "Save settings";          shot 12_settings_saved 2
adb shell input keyevent KEYCODE_BACK; shot 13_long_break_gingham 3
tap "Focus";                  shot 14_focus_gingham 3
tap "Settings"; sleep 2
adb shell input swipe 540 1900 540 900 400; sleep 1
tap "Cozy capy room"
adb shell input keyevent KEYCODE_BACK; shot 15_focus_room 3

# A phase that runs out on its own: 1-minute short break, chime, auto-start into Focus round 2.
tap "Settings"; sleep 2
for i in 1 2 3 4; do tap "Decrease Short break"; sleep 0.3; done
shot 16_short_break_1min 1
adb shell input keyevent KEYCODE_BACK; sleep 2
tap "Short break"; sleep 1
tap "Start timer"; shot 17_break_1min_running 3
sleep 60;                     shot 18_after_break_ended 2

# Turning the screen off for a bit must not kill the countdown.
tap "Start timer"; sleep 2
adb shell input keyevent KEYCODE_SLEEP; sleep 20; adb shell input keyevent KEYCODE_WAKEUP; sleep 2
adb shell wm dismiss-keyguard 2>/dev/null || adb shell input keyevent 82
shot 19_after_screen_off 3

adb shell dumpsys activity services $PKG > "$OUT/services.txt" 2>&1
adb logcat -d -v time > "$OUT/logcat_all.txt"
grep -E "AndroidRuntime|FATAL|$PKG|MusicPlayer|ChimePlayer|TimerService|ExoPlayer" "$OUT/logcat_all.txt" > "$OUT/logcat_app.txt" || true
echo "done"
