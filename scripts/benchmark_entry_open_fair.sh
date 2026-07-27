#!/usr/bin/env bash
# Fair tap-to-entry comparison: openeksin (OpenEksinPerf) vs org.eksin (ActivityManager Displayed).
set -euo pipefail

DEVICE="${ADB_DEVICE:-R5CTA2ZC81L}"
ADB=(adb -s "$DEVICE")
X=540
# Three distinct topic rows on gündem (1080x2340)
TAPS=(520 600 680)

run_openeksin_round() {
  local pkg="com.drejo.openeksin"
  "${ADB[@]}" logcat -c
  "${ADB[@]}" shell am force-stop "$pkg"
  "${ADB[@]}" shell am start -n "$pkg/.MainActivity" >/dev/null
  sleep 3

  local i=0
  for y in "${TAPS[@]}"; do
    i=$((i + 1))
    "${ADB[@]}" shell input tap "$X" "$y"
    sleep 2.5
    local visible
    visible=$("${ADB[@]}" logcat -d -s OpenEksinPerf:I | rg "entry_list_visible" | tail -1 | rg -o "dt=[0-9]+" | cut -d= -f2 || true)
    echo "openeksin topic $i: ${visible:-timeout}ms"
    "${ADB[@]}" logcat -c
    "${ADB[@]}" shell input keyevent KEYCODE_BACK
    sleep 1.2
  done
}

run_eksin_round() {
  local pkg="org.eksin"
  "${ADB[@]}" shell am force-stop "$pkg"
  "${ADB[@]}" shell am start -n "$pkg/com.eksin.activity.TopicIndexActivity" >/dev/null
  sleep 3

  local i=0
  for y in "${TAPS[@]}"; do
    i=$((i + 1))
    "${ADB[@]}" logcat -c
    "${ADB[@]}" shell input tap "$X" "$y"
    sleep 2.5
    local displayed
    displayed=$("${ADB[@]}" logcat -d | rg "Displayed ${pkg}/com.eksin.activity.EntryBrowseActivity" | tail -1 | rg -o "[0-9]+ms \+[0-9]+ms" | head -1 || true)
    if [[ -z "$displayed" ]]; then
      displayed=$("${ADB[@]}" logcat -d | rg "Displayed ${pkg}/com.eksin.activity.EntryBrowseActivity" | tail -1 | rg -o "[0-9]+ms" | head -1 || true)
    fi
    echo "eksin topic $i: ${displayed:-timeout}"
    "${ADB[@]}" shell input keyevent KEYCODE_BACK
    sleep 1.2
  done
}

echo "=== openeksin ==="
run_openeksin_round
echo ""
echo "=== org.eksin ==="
run_eksin_round
