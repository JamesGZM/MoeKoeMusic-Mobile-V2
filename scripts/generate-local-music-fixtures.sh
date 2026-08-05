#!/usr/bin/env bash

set -euo pipefail

fixture_dir="${1:-build/local-music-fixtures}"
mkdir -p "$fixture_dir"

common_input=(
  -f lavfi
  -i "sine=frequency=440:sample_rate=44100:duration=1"
  -map_metadata -1
  -fflags +bitexact
  -flags:a +bitexact
  -y
  -loglevel error
)

ffmpeg "${common_input[@]}" -codec:a libmp3lame -b:a 128k "$fixture_dir/tone.mp3"
ffmpeg "${common_input[@]}" -codec:a aac -b:a 128k "$fixture_dir/tone.m4a"
ffmpeg "${common_input[@]}" -codec:a flac "$fixture_dir/tone.flac"
ffmpeg "${common_input[@]}" -codec:a libopus -b:a 96k "$fixture_dir/tone.ogg"
ffmpeg "${common_input[@]}" -codec:a pcm_s16le "$fixture_dir/tone.wav"

shasum -a 256 "$fixture_dir"/tone.*
