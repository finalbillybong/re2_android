# RE2 Android — AYN Thor

An experimental native Android port of **Resident Evil 2**, focused on the
**AYN Thor**: gameplay on the upper display and touch inventory management on
the lower display, with physical controller support.

This repository is the Android project. It is **based on the Nintendo DS
[re2ds project by eray1605](https://github.com/eray1605/re2ds)** and retains its
engine sources, asset converters and research. The Android application,
dual-display integration and NTSC-U conversion support are developed here.
It is not an official Capcom release or a finished port.

**Current version: 0.9 — world foundation / exploration preview.**
Completable Leon A and Leon B campaigns are the intended goal; neither is
implemented end to end yet. The presence of all room assets does not mean the
full game is playable.

## What works on Android

- All **125 converted Leon-disc rooms** can be loaded on demand. The pause
  menu includes a paged room browser for exploration and testing.
- Original **320×240 backgrounds**, displayed in a full-height 4:3 viewport
  on the Thor's upper screen (1440×1080 with side bars). Models render at the
  viewport resolution. No replacement artwork is used.
- Leon's movement, camera zones, collision, floor heights, stairs and slopes.
- Door connections across the room catalog, with original door models and
  animation programs. **Pause → Door animations: On/Off** saves the preference;
  Off changes rooms instantly.
- A themed lower-screen inventory with the original item artwork, selection,
  equipment, inspection and combination backed by shared native C rules.
- Visible item pickups with **“Will you take the …?”** confirmation. Declining
  leaves the item; accepting adds it and removes its world model.
- Controller focus after lower-screen touches, dual-display routing, screen
  swapping, and a single-display fallback.

## What is unfinished

This remains an exploration preview. Android story/cutscene actors, enemies,
combat, health/healing, item boxes, in-game saving, audio, movies, puzzles and
complete progression are not connected. Scenario initialization, A/B-specific
flags, cross-scenario choices, endings and results still need implementation
and validation.

Exploration currently uses converted door definitions even when a story event
would replace them. This bypass must be removed when story integration works.
Script smoke tests do not prove that story events or either campaign complete.
Some upstream features, including most enemy types/bosses and scenario support,
are themselves unfinished and require new engine work.

See the [Android implementation notes](android/README.md) for controls,
validation, known limitations and the integration sequence. [ROADMAP.md](ROADMAP.md)
is the inherited **DS upstream roadmap**, not a list of working Android features.

## Controls

| Input | Action |
| --- | --- |
| Left stick / D-pad | Move forward/back and turn |
| Hold B while moving forward | Run |
| A | Interact with a nearby door, item or scenery |
| Start / tap gameplay | Open the pause menu |
| D-pad / stick, then A | Navigate and select a pause-menu entry |
| B / Start in the pause menu | Resume |
| Touch a lower-screen inventory slot | Pause and manage inventory |
| A / X / Y in inventory | Equip / combine / inspect |
| Start / lower-screen Exit | Return to gameplay |

Pickup prompts accept touch or left/right plus A; B/Start cancels.
Room warps, test supplies and reset are development tools in the pause menu.

## Build locally

**No game data is included.** Supply your own original NTSC-U Leon disc dump
(`SLUS-00421`, supported executable revision). Claire discs, Dual Shock releases
and other US revisions are not supported by the current conversion profile.
The converters retain upstream PAL support, but this Android build has been
validated with the supported NTSC-U disc.

Requirements:

- Python 3, a C compiler, FFmpeg development libraries and `pkg-config` for conversion.
- JDK 17 and Android SDK platform 35, Build Tools 34.0.0.
- NDK 27.0.12077973 and CMake 3.22.1.
- Gradle 8.9 through the included wrapper; Android Gradle Plugin 8.7.3.

The app builds for **arm64-v8a**, requires Android 8.0/API 26 or newer, and has
been tested on the Thor running Android 13. Set `JAVA_HOME` and `ANDROID_HOME`
to your installed toolchains; the build script also supports a local toolchain
under `build/android-tools`.

```sh
git clone https://github.com/finalbillybong/re2_android.git
cd re2_android

# Extract and convert your own Leon disc; keep generated files under build/.
python3 -I tools/extract_psx_iso.py '/path/to/Leon Disc 1 Track 1.bin' build/disc1-usa
tools/build_assets.sh build/disc1-usa

# Stage local assets, build the APK, run JVM tests and Android lint.
tools/build_android.sh

adb install -r build/android/re2-thor-exploration.apk
adb shell am start -W -n org.re2ds.thor/.MainActivity
```

Read the [NTSC-U conversion notes](docs/ntsc-u.md) for the supported executable
fingerprint and conversion checks. The Android asset stage uses original decoded
backgrounds from `build/bg`, converted assets from `nitrofiles`, and room script
metadata from `build/room`; keep these outputs after conversion.

The locally built APK **contains game data**. Do not commit or distribute it,
extracted disc files or converted assets. An asset-import workflow for a
source-only distributable app remains future work.

## Validation

Version 0.9 passes 14 JVM tests, Android lint, and native address/undefined-behaviour
checks. Asset tests decode all 125 rooms, 1,253 camera definitions and 106 door
file/variant combinations covering 279 recorded doorways. Native smoke tests run
scripts in all 125 rooms. The build is installed on a Thor; prior hardware checks
confirmed dual-screen controls, inventory focus, pickup confirmation, and the
door-animation toggle. The expanded room browser has also loaded room 103 on-device.

These checks validate the exploration foundation, not complete A/B playthroughs.
Activity restoration retains some runtime state; it is not an in-game save system.

## Repository layout and credits

| Path | Purpose |
| --- | --- |
| `android/` | Android app, GLES renderer, dual-screen UI, JNI bridge and JVM tests |
| `source/` | Inherited DS engine and shared C modules used by the Android port |
| `tools/` | Inherited and adapted disc converters, Android staging/build scripts |
| `tests/` | Native engine and conversion checks |
| `docs/` | Upstream research and NTSC-U conversion notes |
| `Makefile`, `start.command` | Retained upstream DS build tooling; not the Android build entry point |

The original engine, reverse-engineering work and conversion pipeline come from
[eray1605/re2ds](https://github.com/eray1605/re2ds) and its contributors. This Android
project builds on that work; it does not claim authorship of the upstream engine.
Resident Evil / Biohazard and the original game assets belong to Capcom.
