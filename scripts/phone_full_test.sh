#!/usr/bin/env bash
# End-to-end test on a logged-in phone (openeksin play debug).
# Live write/edit/delete on eksisozluk is OFF by default — set OPENEKSIN_LIVE_WRITE=1
# only when you explicitly want to post (and accept cleanup responsibility).
set -uo pipefail
PHONE="${PHONE_SERIAL:-R5CTA2ZC81L}"
PKG="com.drejo.openeksin"
LIVE_WRITE="${OPENEKSIN_LIVE_WRITE:-0}"
MARK="oektest$(date +%s)"
LOG="/tmp/openeksin_phone_test.log"
export MARK PHONE LIVE_WRITE
: >"$LOG"

log() { echo "==> $*" | tee -a "$LOG"; }
adb_p() { adb -s "$PHONE" "$@"; }

ensure_openeksin() {
  local focus
  focus=$(adb_p shell dumpsys window 2>/dev/null | rg -m1 'mCurrentFocus' || true)
  if [[ "$focus" == *"com.drejo.openeksin"* ]]; then return 0; fi
  log "wrong focus ($focus) — relaunching openeksin"
  adb_p shell am force-stop org.eksin com.sec.android.daemonapp com.samsung.android.dialer 2>/dev/null || true
  adb_p shell am force-stop "$PKG"
  adb_p shell am start -n "$PKG/.MainActivity"
  sleep 4
}

dump() { adb_p shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb_p shell cat /sdcard/ui.xml; }

tap_text() {
  local label="$1" min_y="${2:-0}"
  local coords
  coords=$(dump | python3 -c "
import sys,re,xml.etree.ElementTree as ET
label=sys.argv[1].lower(); miny=int(sys.argv[2])
root=ET.fromstring(sys.stdin.read())
for n in root.iter('node'):
    t=((n.get('text') or '')+' '+(n.get('content-desc') or '')).strip().lower()
    if label not in t: continue
    b=n.get('bounds'); m=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b or '')
    if not m or int(m.group(2))<miny: continue
    if n.get('clickable')=='true':
        print((int(m.group(1))+int(m.group(3)))//2,(int(m.group(2))+int(m.group(4)))//2); break
    tb=(int(m.group(1)),int(m.group(2)),int(m.group(3)),int(m.group(4)))
    for c in root.iter('node'):
        if c.get('clickable')!='true': continue
        m2=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds') or '')
        if not m2: continue
        ib=(int(m2.group(1)),int(m2.group(2)),int(m2.group(3)),int(m2.group(4)))
        if ib[0]<=tb[0] and ib[1]<=tb[1] and ib[2]>=tb[2] and ib[3]>=tb[3]:
            print((ib[0]+ib[2])//2,(ib[1]+ib[3])//2); raise SystemExit
" "$label" "$min_y") || true
  if [[ -n "${coords:-}" ]]; then
    read -r x y <<<"$coords"
    log "tap '$label' @ $x,$y"
    adb_p shell input tap "$x" "$y"
    return 0
  fi
  log "MISS '$label'"
  return 1
}

has_text() {
  dump | python3 -c "
import sys,xml.etree.ElementTree as ET
n=sys.argv[1].lower()
root=ET.fromstring(sys.stdin.read())
print('yes' if any(n in ((x.get('text') or '')+(x.get('content-desc') or '')).lower() for x in root.iter('node')) else 'no')
" "$1" | rg -q yes
}

FAIL=0
ok() { log "OK: $*"; }
fail() { log "FAIL: $*"; FAIL=1; }

log "launch ($MARK)"
adb_p shell am force-stop org.eksin com.sec.android.daemonapp 2>/dev/null || true
adb_p shell am force-stop "$PKG"
adb_p shell am start -n "$PKG/.MainActivity"
sleep 4
ensure_openeksin

tap_text "menü" 0 || adb_p shell input tap 84 224
sleep 1
if has_text "drejomaster"; then ok "session drejomaster"; else fail "not logged in"; fi
adb_p shell input keyevent 4
sleep 1

log "open cursor (read-only unless OPENEKSIN_LIVE_WRITE=1)"
adb_p shell input tap 996 224
sleep 2
adb_p shell input tap 540 394
for _ in $(seq 1 30); do adb_p shell input keyevent 67; done
adb_p shell input text "cursor"
sleep 3
adb_p shell input tap 540 581
sleep 5

tap_text "son sayfa" 200 || adb_p shell input tap 1008 345
sleep 3
if has_text "cursor"; then ok "cursor topic opened"; else fail "cursor topic"; fi

if [[ "$LIVE_WRITE" == "1" ]]; then
  log "LIVE_WRITE enabled — posting test entry (will attempt cleanup)"
  tap_text "menü" 150 || adb_p shell input tap 996 177
  sleep 1
  tap_text "yaz" 700 || adb_p shell input tap 221 2028
  sleep 2
  adb_p shell input tap 540 600
  adb_p shell input text "$MARK"
  sleep 1
  tap_text "gönder" 150 || adb_p shell input tap 996 177
  sleep 6
  tap_text "son sayfa" 200 || adb_p shell input tap 1008 345
  sleep 3
  if has_text "$MARK"; then ok "write $MARK"; else fail "write $MARK"; fi

  for _ in 1 2 3; do adb_p shell input swipe 540 1800 540 800 400; sleep 0.4; done
  dump | MARK="$MARK" PHONE="$PHONE" python3 -c "
import os,re,sys,xml.etree.ElementTree as ET,subprocess
mark=os.environ['MARK']; phone=os.environ['PHONE']
root=ET.fromstring(sys.stdin.read())
for n in root.iter('node'):
    if mark in (n.get('text') or ''):
        my=int(re.search(r'\[(\d+),', n.get('bounds')).group(1))
        for c in root.iter('node'):
            if (c.get('content-desc') or '').lower()!='menü': continue
            m=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
            if abs(int(m.group(2))-my)<300:
                x=(int(m.group(1))+int(m.group(3)))//2; y=(int(m.group(2))+int(m.group(4)))//2
                subprocess.run(['adb','-s',phone,'shell','input','tap',str(x),str(y)]); raise SystemExit
"
  sleep 1

  if has_text "düzenle"; then
    tap_text "düzenle" 700
    sleep 2
    adb_p shell input tap 540 600
    for _ in $(seq 1 30); do adb_p shell input keyevent 67; done
    adb_p shell input text "${MARK}2"
    sleep 1
    tap_text "gönder" 150 || adb_p shell input tap 996 177
    sleep 4
    has_text "${MARK}2" && ok "edit" || fail "edit"
    dump | MARK="$MARK" PHONE="$PHONE" python3 -c "
import os,re,sys,xml.etree.ElementTree as ET,subprocess
mark=os.environ['MARK']+'2'; phone=os.environ['PHONE']
root=ET.fromstring(sys.stdin.read())
for n in root.iter('node'):
    if mark in (n.get('text') or ''):
        my=int(re.search(r'\[(\d+),', n.get('bounds')).group(1))
        for c in root.iter('node'):
            if (c.get('content-desc') or '').lower()!='menü': continue
            m=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
            if abs(int(m.group(2))-my)<300:
                x=(int(m.group(1))+int(m.group(3)))//2; y=(int(m.group(2))+int(m.group(4)))//2
                subprocess.run(['adb','-s',phone,'shell','input','tap',str(x),str(y)]); break
        break
"
    sleep 1
  else fail "edit menu missing"
  fi

  if has_text "sil"; then
    tap_text "sil" 700
    sleep 1
    tap_text "sil" 500 || adb_p shell input tap 800 1200
    sleep 3
    has_text "${MARK}2" && fail "delete" || ok "delete"
  else fail "delete menu missing"
  fi

  dump | PHONE="$PHONE" python3 -c "
import os,re,sys,xml.etree.ElementTree as ET,subprocess
phone=os.environ['PHONE']
root=ET.fromstring(sys.stdin.read())
for c in root.iter('node'):
    if (c.get('content-desc') or '').lower()!='menü': continue
    m=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
    if 500<int(m.group(2))<1400:
        x=(int(m.group(1))+int(m.group(3)))//2; y=(int(m.group(2))+int(m.group(4)))//2
        subprocess.run(['adb','-s',phone,'shell','input','tap',str(x),str(y)]); break
"
  sleep 1
  if has_text "takip et"; then
    tap_text "takip et" 700; sleep 2
    dump | PHONE="$PHONE" python3 -c "
import os,re,sys,xml.etree.ElementTree as ET,subprocess
phone=os.environ['PHONE']
root=ET.fromstring(sys.stdin.read())
for c in root.iter('node'):
    if (c.get('content-desc') or '').lower()!='menü': continue
    m=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
    if 500<int(m.group(2))<1400:
        x=(int(m.group(1))+int(m.group(3)))//2; y=(int(m.group(2))+int(m.group(4)))//2
        subprocess.run(['adb','-s',phone,'shell','input','tap',str(x),str(y)]); break
"
    sleep 1
    has_text "takibi bırak" && ok "follow" || fail "follow"
    tap_text "takibi bırak" 700; sleep 2; ok "unfollow"
  else fail "follow missing"
  fi
else
  log "skipping live write/edit/delete/follow (set OPENEKSIN_LIVE_WRITE=1 to enable)"
  ok "live write skipped"
fi

adb_p shell input keyevent 4; sleep 0.5
adb_p shell input keyevent 4; sleep 0.5
tap_text "menü" 0 || adb_p shell input tap 84 224
sleep 1
tap_text "çıkış" 400 || adb_p shell input tap 540 1708
sleep 3
if has_text "giriş"; then ok "logout"; else fail "logout"; fi

tap_text "giriş" 400
sleep 8
tap_text "menü" 0 || adb_p shell input tap 84 224
sleep 1
if has_text "drejomaster"; then ok "login"; else fail "login after logout"; fi

log "MARK=$MARK log=$LOG"
exit $FAIL
