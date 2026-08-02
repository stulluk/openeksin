#!/usr/bin/env bash
# Monkey-style test: debe tab counts survive refresh and tab switching.
set -euo pipefail

PKG="com.drejo.openeksin"
OUT_DIR="/media/KINGDATA/DREJO-PROJECTS/openeksin/test-screenshots"
mkdir -p "$OUT_DIR"

count_badges() {
  adb shell uiautomator dump /sdcard/ui_debe.xml >/dev/null 2>&1 || true
  local xml n=0
  xml="$(adb shell cat /sdcard/ui_debe.xml 2>/dev/null || true)"
  if [[ -n "$xml" ]]; then
    n="$(printf '%s' "$xml" | rg -o 'resource-id="com.drejo.openeksin:id/topic_count"' | wc -l | tr -d ' ')"
  fi
  echo "$n"
}

tap() {
  adb shell input tap "$1" "$2"
}

adb shell am force-stop "$PKG"
adb shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 2

echo "==> Open debe tab"
tap 309 320
sleep 18
c1="$(count_badges)"
adb exec-out screencap -p > "$OUT_DIR/debe_monkey_1_initial.png"
echo "    badge count after load: $c1"

echo "==> Pull down refresh (scroll to top, then swipe down)"
adb shell input swipe 540 500 540 1200 350
sleep 22
c2="$(count_badges)"
adb exec-out screencap -p > "$OUT_DIR/debe_monkey_2_after_refresh.png"
echo "    badge count after refresh: $c2"

echo "==> Switch to gundem then back to debe"
tap 112 320
sleep 2
tap 309 320
sleep 12
c3="$(count_badges)"
adb exec-out screencap -p > "$OUT_DIR/debe_monkey_3_after_tab_switch.png"
echo "    badge count after tab switch: $c3"

echo "==> Switch to bugun then back to debe"
tap 487 320
sleep 2
tap 309 320
sleep 12
c4="$(count_badges)"
adb exec-out screencap -p > "$OUT_DIR/debe_monkey_4_after_second_switch.png"
echo "    badge count after second tab switch: $c4"

echo "==> Scroll down and refresh again"
adb shell input swipe 540 1600 540 600 300
sleep 1
adb shell input swipe 540 600 540 1400 350
sleep 18
c5="$(count_badges)"
adb exec-out screencap -p > "$OUT_DIR/debe_monkey_5_after_scroll_refresh.png"
echo "    badge count after scroll+refresh: $c5"

fail=0
for c in "$c1" "$c2" "$c3" "$c4" "$c5"; do
  if [[ "$c" -lt 3 ]]; then
    fail=1
  fi
done

if [[ "$fail" -eq 0 ]]; then
  echo "PASS: debe count badges present in all steps ($c1/$c2/$c3/$c4/$c5)"
  exit 0
fi

echo "FAIL: debe count badges missing in one or more steps ($c1/$c2/$c3/$c4/$c5)"
exit 1
