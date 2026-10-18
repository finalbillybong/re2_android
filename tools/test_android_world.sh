#!/bin/sh
# Sanitized integration tests using the same simulation and asset reader as the APK.
set -eu
cd "$(dirname "$0")/.."
mkdir -p build
python3 -I tools/stage_android_voice.py
for name in world actors combat effects story use route; do
    cc -Wall -Wextra -Werror -DRE2_HEADLESS -fsanitize=address,undefined -g \
       -Isource -Iandroid/app/src/main/cpp "tests/${name}_host.c" \
       android/app/src/main/cpp/world.c android/app/src/main/cpp/world_actors.c \
       android/app/src/main/cpp/combat.c android/app/src/main/cpp/story.c android/app/src/main/cpp/actor_assets.c source/script.c source/inventory.c \
       android/app/src/main/cpp/combat_audio.c source/effect.c source/roomfx.c source/enemy.c source/plc.c -lm -o "build/${name}_host"
    ASAN_OPTIONS=detect_leaks=0 "build/${name}_host"
done
