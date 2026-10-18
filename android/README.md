# RE2 Thor: exploration and classic inventory

Version 0.9 makes all 125 converted Leon-disc rooms available through an indexed,
on-demand room loader. Backgrounds, object geometry and camera masks belong only
to the loaded room; Leon's models and menu assets are shared. Door destinations
carry stage, room and player height. Collision, camera zones and interaction reach
use the current floor; slopes/stairs adapt the upstream height rules. The pause
menu's Explore rooms → All rooms provides a paged inspection shortcut.

The door catalog covers 279 recorded doorways and 106 original file/variant
combinations. It loads at most two animation tracks into its cache. Staging reads
the actual SCD tail bytes: byte 12 is the DO2 file and byte 13 its variant. The old
JSON field called `door_type` is byte 13, so v0.8 incorrectly used the variant as
the file number. The gun shop now correctly uses DOOR0B in variants 0/1; the rear
door uses DOOR02. The existing On/Off preference is retained.

This is an **exploration build**, not story progression. NPCs, enemies,
combat, cinematic movement/cameras, voice and audio are not connected.
The script bridge deliberately leaves cinematic hooks inactive. It uses
converted door definitions even where a story event replaces a door's AOT,
so unfinished cutscenes cannot trap the player in a room. Event triggers are
not exposed. These policies must be removed when actor/cutscene integration
is complete; this build cannot validate story flags or cutscene timing.
Healing and in-game save files remain unfinished. Leon A/B scenario selection,
scenario initialization, zapping data, endings and completion are not implemented.
Room object models now use the original converted meshes/textures and
scripted placement, rotations and hiding state, including foreground depth
occlusion.

## Controls

- Stick / D-pad: forward/back and turning. Hold B while moving forward to run.
- A, or Pause → Interact: use a nearby door, inspect an item,
  or examine nearby scenery. The lower-screen hint changes when something is in reach.
- Items ask “Will you take the …?” with the original item picture. Select
  Yes/No by touch or left/right, then A to confirm. B/Start cancels. Accepting
  returns control directly to Leon; a full inventory shows a message and
  leaves the item in place.
- Start (or tap gameplay): open the pause menu. D-pad/stick navigates; A selects;
  B/Start resumes. Pause → Inventory manages items with the controller;
  touching a lower-screen slot still opens inventory directly.
  A/B/Start/Exit dismiss a pickup or examination message
  and return control to Leon without touching the gameplay display.
- Touch an inventory slot: select it and pause movement.
- Equip (or A while managing inventory): equip/put away the selected weapon.
- Combine: select the first item, press Combine, then select the second item.
  With a controller, X begins combining; use D-pad to select the second slot
  and A to confirm. D-pad navigation matches the two-column, four-row grid.
- Check (or Y): show the item picture inside the lower panel. A/B or a touch
  closes inspection; Start returns directly to gameplay.
- Pause → Explore rooms: shortcuts to the first three scenes and a paged All rooms browser;
  inventory and picked-item flags are retained. Normal doors work too.
- Pause → Test supplies: replace inventory with a handgun containing 10 rounds,
  30 spare rounds, knife, green, red and blue herbs. Combining handgun and
  ammo gives 18 loaded / 22 spare; green + red makes mixed herbs.
- Pause → Reset exploration: start again at the opening street, clearing picked-item flags and
  restoring the normal knife / 18-round handgun loadout.
- Pause → Swap screens: exchange room and inventory roles.

The lower screen now uses Leon's original portrait, metallic square frames,
a two-column item grid, quantity/equipped markers and an equipped-weapon
panel. Test tools stay in the pause menu. Health is not
fabricated: a condition display awaits combat/health integration.

Confirmed pickups add the scripted quantity to the actual inventory. A full inventory
leaves the item available; successful collection sets the engine's picked
flag and hides the model and prevents duplication when revisiting the room. Gun-shop ammo
pickups contain 15 rounds each; the alley pickup contains 30.

The renderer uses the converted backgrounds, Leon/weapon models, animation
root motion and foreground depth masks with OpenGL ES 2. Movement/collision
and camera zones are adapted from `source/main.c` at 30 ticks/second. Floor heights, stairs and slopes now follow the upstream movement rules.
Scripted climbing and jumping moves are still missing.
Backgrounds now use the original decoded 320×240 RGB pixels, without the
DS conversion's resize or 256-colour quantisation. The Android build reads
`build/bg/room*_cam*.ppm` and stages a small RBA1 header (magic, u16 width,
u16 height) followed by unchanged RGB bytes. DS assets remain unchanged.
Foreground masks retain their original screen-coordinate geometry. Models
render at the actual viewport resolution.

Gameplay fills a centred 4:3 viewport: 1440×1080 on the Thor's 1920×1080
upper display. The remaining width is black side bars. Headers, diagnostics
and permanent buttons are removed. No replacement or AI-generated artwork
is used. The original backgrounds are enlarged with nearest-neighbour
sampling; their source detail remains 320×240, not native HD.

## Lifecycle and displays

The main Activity stays on display 0; a Presentation owns the lower display.
Launching from the lower launcher redirects to display 0 with a state
snapshot. The Activity owns controller input. The Presentation uses Android's
[`FLAG_NOT_FOCUSABLE`](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_FOCUSABLE)
to remain touchable without taking key focus from the Activity. Item checks
render inside the inventory rather than opening another dialog. All exits
share a resume path that closes messages/inspection, clears stale held input
and restores the Activity view focus. Losing
the second display falls back to a split layout. Backgrounding clears held
input. Saved Activity state retains inventory, room/position, picked flags
and locks. Script threads restart on Activity recreation; their instruction
pointers are not saved. This is lifecycle restoration, not an in-game save.

## Build

Convert the user's NTSC-U disc first, following the
[conversion notes](../docs/ntsc-u.md). The build stages all 125 rooms,
Leon/weapon models and menu artwork under ignored `build/android-assets`.
Missing assets stop the build. The resulting local APK contains game data;
keep it and converted assets out of the source repository and public releases.
An asset import workflow is still needed for a distributable app.

Requirements: JDK 17, SDK platform 35, Build Tools 34.0.0,
NDK 27.0.12077973, CMake 3.22.1. Gradle 8.9 is pinned; AGP is 8.7.3.
The APK targets API 33, supports API 26+ and builds arm64-v8a for the Thor.

```sh
tools/build_android.sh
adb install -r build/android/re2-thor-exploration.apk
adb shell am start -W -n org.re2ds.thor/.MainActivity
```

`JAVA_HOME` / `ANDROID_HOME` override the local toolchain under
`build/android-tools`. The build runs Java tests against the complete staged asset catalog and Android lint.
Native checks:

```sh
cc -Wall -Wextra -Werror -fsanitize=undefined -Isource \
  tests/inventory_host.c source/inventory.c -o build/inventory_host
build/inventory_host
cc -Wall -Wextra -Werror -fsanitize=address,undefined \
  -Isource -Iandroid/app/src/main/cpp tests/world_host.c \
  android/app/src/main/cpp/world.c source/script.c source/inventory.c \
  -o build/world_host
ASAN_OPTIONS=detect_leaks=0 build/world_host
```

LeakSanitizer is disabled in the latter command because it cannot operate
under this workspace's tracing sandbox; address/undefined-behaviour checks
remain enabled. The world test uses real room files to check both directions
of 100↔101↔102, the build boundary, full-inventory rejection, pickup quantity,
no duplicate pickup after returning, and flag snapshot restoration.

## Validation

Version 0.4: ten Java tests pass, including asset decoding for all three
rooms, movement, reach/facing, floor matching and overlapping item/text
priority. The native world integration checks pass with ASan/UBSan.
Android lint reports zero errors. Installation and room switching through
the exploration selector work on the connected Thor; a device capture shows
the gun shop and Leon with foreground occlusion. The user confirmed A-button door transitions and collecting handgun
ammunition into the lower inventory. The final APK signature was verified
and the build installed after the state-restoration ordering fix.

Earlier 0.3 hardware validation confirmed movement/turning, equipment changes
on the lower display and resuming with Start. On-device touch checks verified
ammo loading (10→18 loaded, 30→22 spare) and herb combination/slot consumption.
The 0.2 probe verified lower-launcher routing, lid recovery, controller focus
after touch and screen-role swaps. Those earlier results do not substitute
for rechecking lifecycle behavior in the larger exploration build.

The shared C inventory extraction was also checked with the upstream script
simulation across 125 converted rooms, plus enemy and cutscene host runs.
These are compatibility checks, not proof of game completion. No DS toolchain
build has been run for the extraction.


Version 0.5: build, ten Java tests and Android lint pass (zero errors).
Device captures verify the redesigned inventory. A lower-display item tap
followed by an untargeted injected Start key resumes play; `dumpsys input`
continues to report focused display 0 and the game's Activity as its input
window. Physical pickup/resume verification is pending the user's check.


Version 0.6: native ASan/UBSan checks cover visible placed items, uncommitted
pickup prompts, cancellation, successful collection/model hiding, capacity
failure, stale confirmation after a room change and persistence on revisit.
The Java asset checks also decode actual room-object geometry and verify
texture dimensions and normalized UVs. The lower-screen prompt is drawn
inside the inventory view, preserving the Activity's controller focus.

On-device captures for 0.6 confirm the ammo-box mesh on the gun-shop counter
and the themed “Will you take the H. Gun Bullets?” Yes/No prompt, using the
original item picture. Ten Java tests and native integration checks pass;
lint reports zero errors.

The user confirmed on the Thor that the ammo box is visible, declining leaves
it in place, and accepting removes it and adds the ammunition to inventory.


Version 0.7: all 30 staged backgrounds were compared byte for byte with the
original decoded RGB images. Ten Java tests pass using the staged Android
assets, and lint reports no errors. Device captures confirm full-height
1440×1080 gameplay and the overlay pause menu. Source resolution and aspect
ratio are preserved; the source artwork itself has not been enhanced.

Door animations (v0.8): Start opens the pause menu; “Door animations: On/Off”
changes a preference saved across app restarts. On is the default and plays the
original door-opening sequence before returning control in the destination room.
Off changes rooms immediately. Explore rooms and Reset exploration remain instant.
The two original door types cover all four supported directions between the three
rooms. Door audio remains unfinished, alongside the rest of the game's audio.

Asset staging compiles the host door interpreter and exports its part transforms
and fades at NTSC 60 Hz, using the original door type/variant from the converted
room JSON. Android renders the original models and textures through GLES at the
same 4:3 viewport as gameplay. No replacement artwork or prerendered video is used.
Input and inventory actions are blocked during playback. Playback stops while the
activity is suspended or the screens are off; activity recreation restores the
already-entered destination room instead of leaving a pending transition.

Validation: DoorAnimationTest checks both directions, real geometry, moving parts,
fades, timing and rejection of a truncated track. On hardware, cross a gun-shop
door with the option On, then with it Off; restart to check the setting persists.

Thor verification, 2026-10-18: user confirmed the original door animation plays
with On, Off changes rooms instantly, and control returns to Leon in both modes.
All 12 JVM tests and Android lint passed; v0.8 installed successfully. The saved
Off preference was also verified in the app's settings file on the device.


## Work remaining for Leon A and B

The full campaigns require new engine work as well as Android integration.
The upstream [roadmap](../ROADMAP.md) lists scenarios/zapping and most enemy types
as unfinished. Enabling a B label or opening the room catalog would not establish
correct B behavior.

Implementation order:

1. Reuse the native enemy and `plc` character controllers on Android. Wire actor
   poses, cinematic camera/player hooks, voices and script waits; validate the
   opening street and Kendo sequence before exposing event triggers. Remove the
   exploration-only fallback doors once normal story events work.
2. Integrate aiming, shooting, damage, health, healing, death and item boxes.
   Port the existing handgun/knife/zombie behavior; implement the missing weapons,
   enemy AI and boss encounters rather than presenting zombies as other enemies.
3. Establish original NTSC-U Leon A and B initialization, start positions,
   scenario flag conditions and cross-scenario choices from executable/script
   evidence. Keep scenario identity in a versioned save format. The current
   all-zero script state is exploration state, not validated campaign state.
4. Add typewriter saves, loading, puzzles/key use, scripted traversal, audio,
   movies, endings and results. Check save/load at story checkpoints and every
   required branch of both routes.
5. Complete physical-device A and B playthroughs including required bosses and
   endings. Room/asset decoding and script smoke tests cannot certify completion.

Room 30C has only one source background for its 17 camera definitions; staging
reuses its original camera 0 image, matching the existing DS renderer fallback.
Door file 28 intentionally has no geometry. These source-data exceptions are
covered in the broader validation instead of replacing artwork.

Version 0.9 validation, 2026-10-18: 14 JVM tests pass, including all 125 rooms,
1,253 camera definitions, all 106 door tracks, shared player assets, floor/height
movement and truncated-track rejection. The native ASan/UBSan test enters and
runs scripts in all 125 rooms, alongside the existing pickup/door regression
checks. Lint and APK signature verification pass. Installed versionCode 9 on the
Thor; the paged browser loaded room 103 and a device capture verified its scene.
One post-load memory sample was about 146 MiB PSS, with no Re2Thor errors logged.
This does not establish campaign completion or peak-memory behavior.

Door files 32, 34 and 36 have no active model parts in their original programs:
they fade and schedule sound. Until Android audio is connected, those sequences
appear as black transitions. The animation preference can skip them.
