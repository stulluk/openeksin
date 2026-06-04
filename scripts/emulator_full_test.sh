#!/usr/bin/env bash
# openeksin-only emulator end-to-end test.
# Live write/edit/delete on eksisozluk is OFF by default — set OPENEKSIN_LIVE_WRITE=1
# only when you explicitly want to post (and accept cleanup responsibility).
set -uo pipefail
EMU="${EMU_SERIAL:-emulator-5554}"
PKG="com.drejo.openeksin"
LIVE_WRITE="${OPENEKSIN_LIVE_WRITE:-0}"
LOG="/tmp/openeksin_emulator_test.log"
MARK="oektest$(date +%s)"
: >"$LOG"

log() { echo "==> $*" | tee -a "$LOG"; }

adb -s "$EMU" shell am force-stop org.eksin >/dev/null 2>&1 || true
adb -s "$EMU" shell am force-stop com.google.android.calendar >/dev/null 2>&1 || true
adb -s "$EMU" shell am force-stop "$PKG" >/dev/null 2>&1 || true
sleep 1
adb -s "$EMU" shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 3

dump() { adb -s "$EMU" shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb -s "$EMU" shell cat /sdcard/u.xml; }

tap_text() {
  local label="$1"
  local min_y="${2:-0}"
  local coords
  coords=$(dump | python3 -c "
import sys,re,xml.etree.ElementTree as ET
label=sys.argv[1].lower(); miny=int(sys.argv[2])
root=ET.fromstring(sys.stdin.read())
nodes=list(root.iter('node'))

def bounds(n):
    m=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds') or '')
    return tuple(map(int,m.groups())) if m else None

def center(b):
    return (b[0]+b[2])//2,(b[1]+b[3])//2

def inside(inner, outer):
    return outer[0]<=inner[0] and outer[1]<=inner[1] and outer[2]>=inner[2] and outer[3]>=inner[3]

# 1) clickable node with matching text/desc
for n in nodes:
    t=((n.get('text') or '')+' '+(n.get('content-desc') or '')).strip().lower()
    if label in t and n.get('clickable')=='true':
        b=bounds(n)
        if b and b[1]>=miny:
            print(*center(b)); sys.exit(0)
# 2) text node inside smallest clickable container
for n in nodes:
    if (n.get('text') or '').strip().lower()!=label:
        continue
    tb=bounds(n)
    if not tb or tb[1]<miny: continue
    best=None
    for c in nodes:
        if c.get('clickable')!='true': continue
        cb=bounds(c)
        if cb and inside(tb,cb):
            area=(cb[2]-cb[0])*(cb[3]-cb[1])
            if best is None or area<best[0]:
                best=(area,center(cb))
    if best:
        print(*best[1]); sys.exit(0)
    print(*center(tb)); sys.exit(0)
sys.exit(1)
" "$label" "$min_y") || true
  if [[ -n "${coords:-}" ]]; then
    read -r x y <<<"$coords"
    log "tap '$label' @ $x,$y"
    adb -s "$EMU" shell input tap "$x" "$y"
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

focus_openeksin() {
  adb -s "$EMU" shell am start -n "$PKG/.MainActivity" >/dev/null
  sleep 1
}

# --- login check ---
tap_text "menü" 0 || adb -s "$EMU" shell input tap 84 224
sleep 1
if has_text "drejomaster"; then log "SESSION OK: drejomaster"; else log "SESSION FAIL"; fi
adb -s "$EMU" shell input keyevent 4
sleep 1

# --- open cursor topic (read-only unless OPENEKSIN_LIVE_WRITE=1) ---
focus_openeksin
tap_text "ara" 150 || adb -s "$EMU" shell input tap 996 224
sleep 2
adb -s "$EMU" shell input tap 540 200
for _ in $(seq 1 40); do adb -s "$EMU" shell input keyevent 67; done
adb -s "$EMU" shell input text "cursor"
sleep 2
dump | python3 -c "
import sys,re,xml.etree.ElementTree as ET,subprocess
root=ET.fromstring(sys.stdin.read())
for n in root.iter('node'):
    t=(n.get('text') or '')
    if t.startswith('\"cursor\"') and 'başlığına git' in t:
        m=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds'))
        x=(int(m.group(1))+int(m.group(3)))//2; y=(int(m.group(2))+int(m.group(4)))//2
        subprocess.run(['adb','-s','emulator-5554','shell','input','tap',str(x),str(y)])
        print('suggestion',t); break
else:
    # tap clickable row containing cursor title
    for n in root.iter('node'):
        if (n.get('text') or '').lower()=='cursor' and n.get('clickable')=='true':
            m=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds'))
            x=(int(m.group(1))+int(m.group(3)))//2; y=(int(m.group(2))+int(m.group(4)))//2
            subprocess.run(['adb','-s','emulator-5554','shell','input','tap',str(x),str(y)])
            print('row cursor'); break
"
sleep 4
focus_openeksin

if [[ "$LIVE_WRITE" != "1" ]]; then
  log "skipping live write/edit/delete (set OPENEKSIN_LIVE_WRITE=1 to enable)"
else
# --- write entry ---
tap_text "son sayfa" 200 || true
sleep 3
tap_text "menü" 150 || adb -s "$EMU" shell input tap 1020 224
sleep 1
tap_text "yaz" 700
sleep 2
adb -s "$EMU" shell input tap 540 500
adb -s "$EMU" shell input text "$MARK"
sleep 1
tap_text "gönder" 150 || adb -s "$EMU" shell input tap 1020 224
sleep 6
focus_openeksin

if has_text "$MARK"; then log "WRITE OK ($MARK)"; else log "WRITE FAIL ($MARK)"; fi

# --- edit ---
for _ in 1 2 3; do adb -s "$EMU" shell input swipe 540 1800 540 800 400; sleep 0.4; done
# entry 3-dot: right side ~1020, find drejomaster row first
dump | python3 -c "
import sys,re,xml.etree.ElementTree as ET,subprocess
root=ET.fromstring(sys.stdin.read())
mark=sys.argv[1]
# menu near mark or drejomaster
nodes=list(root.iter('node'))
for n in nodes:
    if mark in (n.get('text') or ''):
        mb=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds'))
        my=int(mb.group(2))
        for c in nodes:
            if (c.get('content-desc') or '').lower()=='menü' or (c.get('clickable')=='true' and int(re.search(r'\[(\d+),',c.get('bounds')).group(1))>900):
                cb=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
                cy=int(cb.group(2))
                if abs(cy-my)<250:
                    x=(int(cb.group(1))+int(cb.group(3)))//2; y=(int(cb.group(2))+int(cb.group(4)))//2
                    subprocess.run(['adb','-s','emulator-5554','shell','input','tap',str(x),str(y)])
                    print('entry menu',x,y); sys.exit(0)
# fallback first entry menu on screen
for c in nodes:
    if (c.get('content-desc') or '').lower()=='menü':
        cb=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
        if int(cb.group(2))>500:
            x=(int(cb.group(1))+int(cb.group(3)))//2; y=(int(cb.group(2))+int(cb.group(4)))//2
            subprocess.run(['adb','-s','emulator-5554','shell','input','tap',str(x),str(y)])
            print('fallback menu',x,y); break
" "$MARK"
sleep 1

if has_text "düzenle"; then
  tap_text "düzenle" 700
  sleep 2
  adb -s "$EMU" shell input tap 540 500
  for _ in $(seq 1 25); do adb -s "$EMU" shell input keyevent 67; done
  adb -s "$EMU" shell input text "${MARK}2"
  sleep 1
  tap_text "gönder" 150
  sleep 4
  has_text "${MARK}2" && log "EDIT OK" || log "EDIT FAIL"
  dump | python3 -c "
import sys,re,xml.etree.ElementTree as ET,subprocess
root=ET.fromstring(sys.stdin.read()); mark=sys.argv[1]+'2'
for n in root.iter('node'):
    if mark in (n.get('text') or ''):
        mb=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n.get('bounds'))
        my=int(mb.group(2))
        for c in root.iter('node'):
            if (c.get('content-desc') or '').lower()=='menü':
                cb=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
                if abs(int(cb.group(2))-my)<300:
                    x=(int(cb.group(1))+int(cb.group(3)))//2; y=(int(cb.group(2))+int(cb.group(4)))//2
                    subprocess.run(['adb','-s','emulator-5554','shell','input','tap',str(x),str(y)]); break
        break
" "$MARK"
  sleep 1
else
  log "EDIT SKIP"
fi

if has_text "sil"; then
  tap_text "sil" 700
  sleep 1
  tap_text "sil" 500 || adb -s "$EMU" shell input tap 800 1200
  sleep 3
  has_text "${MARK}2" && log "DELETE FAIL" || log "DELETE OK"
else
  log "DELETE SKIP"
fi

# --- follow/unfollow first visible entry ---
dump | python3 -c "
import sys,re,xml.etree.ElementTree as ET,subprocess
root=ET.fromstring(sys.stdin.read())
for c in root.iter('node'):
    if (c.get('content-desc') or '').lower()=='menü':
        cb=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
        if 500<int(cb.group(2))<1400:
            x=(int(cb.group(1))+int(cb.group(3)))//2; y=(int(cb.group(2))+int(cb.group(4)))//2
            subprocess.run(['adb','-s','emulator-5554','shell','input','tap',str(x),str(y)]); break
"
sleep 1
if has_text "takip et"; then
  tap_text "takip et" 700
  sleep 2
  dump | python3 -c "
import sys,re,xml.etree.ElementTree as ET,subprocess
root=ET.fromstring(sys.stdin.read())
for c in root.iter('node'):
    if (c.get('content-desc') or '').lower()=='menü':
        cb=re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', c.get('bounds'))
        if 500<int(cb.group(2))<1400:
            x=(int(cb.group(1))+int(cb.group(3)))//2; y=(int(cb.group(2))+int(cb.group(4)))//2
            subprocess.run(['adb','-s','emulator-5554','shell','input','tap',str(x),str(y)]); break
"
  sleep 1
  has_text "takibi bırak" && log "FOLLOW OK" || log "FOLLOW UNCERTAIN"
  tap_text "takibi bırak" 700 && log "UNFOLLOW OK"
else
  log "FOLLOW SKIP"
fi

fi

# --- logout / login ---
focus_openeksin
adb -s "$EMU" shell input keyevent 4; sleep 0.5
adb -s "$EMU" shell input keyevent 4; sleep 0.5
tap_text "menü" 0 || adb -s "$EMU" shell input tap 84 224
sleep 1
tap_text "çıkış" 400
sleep 3
if has_text "giriş"; then
  log "LOGOUT OK"
  tap_text "giriş" 400
  sleep 6
  if has_text "verify you are human" || has_text "verification failed" || has_text "turnstile"; then
    log "LOGIN Turnstile blocked (expected on emulator)"
  else
    log "LOGIN screen opened"
  fi
else
  log "LOGOUT FAIL"
fi

log "MARK=$MARK"
log "finished"
