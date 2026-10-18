# NTSC-U asset conversion

The converters accept the original US Leon disc (`SLUS_004.21`) alongside
the upstream PAL disc (`SLES_009.72`). Disc selection reads `SYSTEM.CNF`;
do not rename the US executable to the PAL filename.

The supported US executable has SHA-256
`1cd618bc4390db4bd60735b0db1e91534f6134ede56fae454eb580be2aa686ef`.
Other US revisions, Dual Shock releases, and Claire discs are not supported
by this profile. Unknown US executable revisions fail before conversion.
PAL retains upstream offsets and filename identification; it has not been
retested with a PAL disc in this checkout.

## Build and check

From the repository root, after extracting the user's BIN/CUE archive:

```sh
python3 -I tools/extract_psx_iso.py \
  'build/disc-images/Resident Evil 2 (USA) (Disc 1) (Track 1).bin' build/disc1-usa
tools/build_assets.sh build/disc1-usa
RE2_DISC_DIR=build/disc1-usa python3 -m unittest discover -s tests -p 'test_disc_profile.py' -v
```

The converter uses FFmpeg, its C development libraries, a C compiler, and
Python 3, as upstream does. Generated assets go in `nitrofiles/`; extracted
disc data and intermediate files go in `build/`. Both are ignored by git.

The compatibility suite passed all eight tests with this disc. The existing
script harness checked all 125 converted rooms with zero reported problems
and no undefined-behaviour diagnostics. The enemy simulation and the room 101
Kendo cutscene simulation (1,200 ticks) exited successfully without missing
asset reports. These harnesses exercise selected game paths, not a complete
playthrough.

To repeat the host checks after conversion:

```sh
cc -O1 -g -fsanitize=undefined -Isource tests/script_host.c source/inventory.c -o build/script_host
build/script_host nitrofiles/room/room[0-9][0-9a-f][0-9a-f]0.bin
cc -Itests/stubs -Isource tests/enemy_host.c source/script.c source/inventory.c source/enemy.c source/plc.c -o build/enemy_host -lm
build/enemy_host
cc -Itests/stubs -Isource tests/cutscene_host.c source/script.c source/inventory.c source/enemy.c source/plc.c -o build/cutscene_host -lm
build/cutscene_host nitro:/room/room1010.bin 0x16 1200
```

Inspect the script harness's summary and diagnostics; upstream returns zero
even when its summary reports problems.

## Verified layout differences

These are executable **file offsets**, including the PS-X EXE header.

| Table | PAL | NTSC-U | Evidence used for NTSC-U |
| --- | --- | --- | --- |
| Enemy model entries | `0x83a8c` | `0x83ddc` | TIM headers and EMD animation/mesh parsing in CDEMD0.EMS |
| Item names | `0x871d4` | `0x8741c` | 139 decoded names, beginning with Knife and Hand Gun |
| Enemy sound pairs | `0x8fccc` | `0x900b8` | 72 pairs followed by the `0xff` terminator |
| Enemy sound parts | `0x90390` | `0x9077c` | All 72 bank ranges and VAB headers match ENEMSE.VBS |
| First room per stage | `0x90910` | `0x90cfc` | Stage ranges contain all extracted rooms |
| Common voice clips | `0x83038` | `0x83388` | 13 US entries reference valid XA sector ranges |

The US enemy table ends at type `0x5a` (55 nonempty model entries).
Reading through upstream's `0x5f` limit would interpret unrelated bytes as
entries. The US common voice table has 13 entries, rather than PAL's 19.

Stage overlays store their voice-table pointer at RAM `0x800c66b4` on US
instead of `0x800c6184` on PAL. The converter resolves the full MIPS store
address, including the base register. All seven overlays and their referenced
XA ranges are checked against the extracted disc.

US core effects use `COMMON/DATA/TEX.TIM`; upstream requested `TEX_PAL.TIM`.
The US TIM has a 32 by 19 CLUT at VRAM (256, 480), covering the palettes
referenced by CORE00.ESP. The converter checks that each effect's starting
palette lies inside the selected TIM.

## Limits and Android work

Conversion and host simulations do not prove full game completion, correct
audio playback, or rendering on a Thor. The engine still has upstream's
missing gameplay features. It also still uses the upstream 25 Hz PAL game
clock; native NTSC timing (30 Hz) needs to be handled when porting the runtime,
with animation and cutscene synchronisation checked together.

The common converted assets retain DS-compatible formats. Android separately
stages the original decoded 320×240 RGB backgrounds without the DS palette
reduction or resize. Keep `build/bg` and `build/room` after conversion.

The [Android exploration build](../android/README.md) loads all 125 Leon-disc
rooms on demand, renders Leon above the shared C inventory, and connects doors,
floor heights, pickups and examination text. Its exploration policy bypasses
unfinished cinematic door events. It runs at 30 Hz, with exported door programs
at NTSC 60 Hz; full story/script timing, actors, audio, storage and Leon A/B
scenario integration remain unfinished.
