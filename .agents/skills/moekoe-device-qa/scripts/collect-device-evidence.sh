#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <device-serial> <application-id> <scenario>" >&2
  exit 2
fi

device_serial="$1"
application_id="$2"
scenario="$3"
[[ "$scenario" =~ ^[a-z0-9][a-z0-9._-]*$ ]] || { echo "invalid scenario" >&2; exit 2; }
adb devices -l | awk 'NR > 1 && $2 == "device" { print $1 }' | grep -Fxq "$device_serial" || { echo "specified device is not online" >&2; exit 1; }
[[ "$(adb -s "$device_serial" shell getprop ro.boot.qemu | tr -d '\r')" != "1" ]] || { echo "emulators are forbidden" >&2; exit 1; }

output_dir="build/reports/device-qa/$scenario"
mkdir -p "$output_dir"
adb -s "$device_serial" shell uiautomator dump /sdcard/window.xml >/dev/null
adb -s "$device_serial" pull /sdcard/window.xml "$output_dir/window.xml" >/dev/null
adb -s "$device_serial" exec-out screencap -p > "$output_dir/screen.png"
adb -s "$device_serial" logcat -d --pid="$(adb -s "$device_serial" shell pidof -s "$application_id" | tr -d '\r')" '*:W' > "$output_dir/logcat-warnings.txt" 2>/dev/null || true
model="$(adb -s "$device_serial" shell getprop ro.product.model | tr -d '\r')"
api="$(adb -s "$device_serial" shell getprop ro.build.version.sdk | tr -d '\r')"
printf 'scenario=%s\napplicationId=%s\nmodel=%s\napi=%s\n' "$scenario" "$application_id" "$model" "$api" > "$output_dir/manifest.properties"
echo "$output_dir"
