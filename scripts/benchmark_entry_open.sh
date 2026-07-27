#!/usr/bin/env bash
# Measures tap-to-entry-list-visible latency on gündem (3 topics).
set -euo pipefail

DEVICE="${ADB_DEVICE:-R5CTA2ZC81L}"
ADB=(adb -s "$DEVICE")
# Topic row taps (1080x2340): below toolbar + tab bar
TAPS=(550 650 750)
X=540

measure_openeksin() {
  local pkg="com.drejo.openeksin"
  "${ADB[@]}" logcat -c
  "${ADB[@]}" shell am force-stop "$pkg"
  "${ADB[@]}" shell am start -n "$pkg/.MainActivity" >/dev/null
  sleep 3

  local i=0
  for y in "${TAPS[@]}"; do
    i=$((i + 1))
    "${ADB[@]}" shell input tap "$X" "$y"
    sleep 4
    "${ADB[@]}" logcat -d -s OpenEksinPerf:I | tail -5
    echo "--- topic $i ---"
    "${ADB[@]}" shell input keyevent KEYCODE_BACK
    sleep 1.5
  done
}

measure_eksin_ui() {
  local pkg="org.eksin"
  local tmp="/data/local/tmp/eksin_ui.xml"
  "${ADB[@]}" shell am force-stop "$pkg"
  "${ADB[@]}" shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || \
    "${ADB[@]}" shell am start -n "$pkg/.ui.main.MainActivity" >/dev/null 2>&1
  sleep 3

  local i=0
  for y in "${TAPS[@]}"; do
    i=$((i + 1))
    local start
    start=$("${ADB[@]}" shell date +%s%3N | tr -d '\r')
    "${ADB[@]}" shell input tap "$X" "$y"

    local elapsed=-1
    local attempt
    for _ in $(seq 1 40); do
      "${ADB[@]}" shell uiautomator dump "$tmp" >/dev/null 2>&1 || true
      if "${ADB[@]}" shell cat "$tmp" 2>/dev/null | rg -q "entry-item-list|entry_body|EntryList|content.*entry"; then
        local now
        now=$("${ADB[@]}" shell date +%s%3N | tr -d '\r')
        elapsed=$((now - start))
        break
      fi
      sleep 0.05
    done
    echo "eksin topic $i: ${elapsed}ms (ui dump marker)"
    "${ADB[@]}" shell input keyevent KEYCODE_BACK
    sleep 1.5
  done
}

echo "=== openeksin (logcat OpenEksinPerf) ==="
measure_openeksin

echo ""
echo "=== org.eksin (uiautomator poll) ==="
measure_eksin_ui
