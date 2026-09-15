#!/usr/bin/env bash
# Read-only ADB probe: checks whether a Fold8 exposes what Folduo depends on.
# Changes nothing on the phone. Usage: tools/fold8-probe.sh [--watch SECONDS] [--serial SERIAL]
set -euo pipefail

WATCH=0
ADB=(adb)
while [[ $# -gt 0 ]]; do
  case "$1" in
    --watch) WATCH="$2"; shift 2 ;;
    --serial) ADB=(adb -s "$2"); shift 2 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done

command -v adb >/dev/null || { echo "adb not found: brew install android-platform-tools" >&2; exit 1; }
sh() { "${ADB[@]}" shell "$@" 2>/dev/null | tr -d '\r'; }

echo "== Device"
echo "model:   $(sh getprop ro.product.model)"
echo "device:  $(sh getprop ro.product.device)"
echo "android: $(sh getprop ro.build.version.release) (SDK $(sh getprop ro.build.version.sdk))"
echo "build:   $(sh getprop ro.build.display.id)"
echo "one ui:  $(sh getprop ro.build.version.oneui)"

echo
echo "== Required packages"
for pkg in moe.shizuku.privileged.api com.samsung.android.wallpaper.live com.samsung.android.wallpaper.res; do
  if sh pm path "$pkg" | grep -q package:; then echo "present  $pkg"; else echo "MISSING  $pkg"; fi
done

echo
echo "== Device states (Folduo needs CONCURRENT_INNER_DEFAULT and CONCURRENT_OUTER_DEFAULT)"
sh dumpsys device_state | grep -E 'DeviceState\{|CONCURRENT|mCommittedState|mBaseState' | head -30

echo
echo "== Displays (Folduo assumes built-in logical display ids 0 and 1)"
sh dumpsys display | grep -E '^ *mDisplayId=|mBaseDisplayInfo=' | sed -E 's/(real [0-9]+ x [0-9]+).*/\1/' | head -10

echo
echo "== Hinge / fold sensors (type 36 = public hinge angle; 65686-65690 = Samsung)"
sh dumpsys sensorservice | grep -iE 'hinge|fold|angle|65686|65687|65688|65689|65690' | head -20

echo
echo "== Wallpaper (Folduo reads fine angles from FoldInteractive playing video_002.mp4)"
sh dumpsys wallpaper | grep -iE 'FoldInteractive|video_0|sub_wallpaper|mWallpaperComponent|which=' | head -20

if [[ "$WATCH" -gt 0 ]]; then
  echo
  echo "== Watching FoldInteractive log for ${WATCH}s: slowly fold and unfold the phone now"
  "${ADB[@]}" logcat -c 2>/dev/null || true
  "${ADB[@]}" logcat -v time -s 'SprWallpaper|FoldInteractive:*' &
  pid=$!
  sleep "$WATCH"
  kill "$pid" 2>/dev/null || true
fi
