# RE2 Thor: opening story, combat and classic inventory


Version 0.16 fixes three faults found in the first hardware test of that scene:
the Android loader rejected animation-only RBJ files; persistent arrival flags
let later movement waits finish too early; and depth-zero foreground masks
collapsed at the camera, exposing legs through the counter. Room animations now
load, arrival flags follow the original scheduler's one-pass lifetime, and near
foreground masks retain their screen rectangles. Loop back-edges no longer add
an extra yield that could miss a one-tick arrival. The lock-cue regression checks
Kendo has reached the original doorway waypoint, not just that the scene ends.


Version 0.15 connects the original NTSC-U gun-shop scripts to Leon's PLC controller,
room animation set, forced cameras, camera remapping and automatic/manual event
triggers. Both the welcome conversation and window attack return control through
the original script flags. Gun-shop doors respect live AOT replacements: the front
entrance becomes dialogue/blocked scenery; the rear exit remains a door. Other
rooms retain the exploration fallback until their own events are integrated.

Eleven original stage-one XA dialogue clips are decoded directly from the local
NTSC-U disc to mono 37,800 Hz WAV, without normalization or resampling. MediaPlayer
streams dialogue and pauses with simulation, display sleep and Activity backgrounding.
Native voice waits use the exact decoded sample duration at 30 simulation ticks/sec.
The original room sound bank and world-positioned glass effects are also connected.
The scene does not open blocking inventory message panels for the converter's
incomplete dialogue text. Full subtitles, neck tracking, object/bone-attached
script effects and dismemberment remain unfinished; this is not a claim of complete
cinematic parity or full campaign support.

Normal pause/lid/background resume keeps the scene in memory. If Android recreates
the Activity during a scene, the room-entry checkpoint is restored with matching
inventory, HP and story flags, paused, so the encounter can restart without trapping
Leon or duplicating pickups. This is recovery behavior, not an in-game save system.
Development room warps and equipment changes are blocked during a scene; Start,
inventory viewing, screen swapping, restart and quit remain available.

Version 0.14 adds the original combat presentation. Headless `effect.c` and
`roomfx.c` animate the converted CORE00 sprites; GLES draws depth-tested,
transparent billboards using the original indexed pages and palette rows.
Handgun keyframe 1 emits muzzle flash/fire, keyframe 3 emits smoke and a
thrown casing, anchored to the animated hand with the upstream coordinates.
Hits and bites use the native blood callbacks; casings fall and expire.
Temporary effects clear on room changes and stop advancing while paused.

Weapon, reload, empty-gun, zombie and Leon reaction samples play through
Android SoundPool. The bank/note mapping follows the native engine. Staging
wraps the original converted mono PCM in WAV headers without resampling;
weapon/player banks, the gun-shop sound bank and the current enemy bank are loaded at runtime.
Pause, display sleep and Activity backgrounding stop playback; stale pending
sounds are discarded. SoundPool is released when the Activity is destroyed.
Music, door audio, and room audio/speech outside the gun shop remain unconnected.

Version 0.13 adds handgun/knife combat in rooms 100–102. Hold R1 or RT to
aim; A fires/stabs. The original raise, hold, fire and reload moves set the
cadence. Raising the weapon selects a visible target; left/right turns and
up/down selects the original aiming posture. An empty handgun reloads on a
new A press if carried ammunition is available. Reload fills the magazine
on original keyframe 22 and consumes only the rounds transferred.

Zombies react to hits, stagger, fall and die. They can now grab Leon; the
original victim animation drives his skeleton and position. Repeated A
presses help him struggle free. Bites apply native damage (20 becomes 30),
updating the lower-screen HP/condition. At zero HP the death animation ends
in a restart/quit menu. This preview uses immediate death at zero HP rather
than the upstream one-time zero-HP escape. Pause/inventory and sleeping
displays stop the simulation. Returning from menus clears held combat input.

Healing and the full original death presentation remain unfinished.
Combat sound and sprites were added in v0.14 as described above. Enemy deaths are not persisted across room
re-entry or Activity recreation. In-progress grabs restart as standing Leon
with the retained HP after Activity recreation; this is not a save system.
Kendo story orchestration is connected in v0.15; further progression and other
enemy types remain separate work.

Version 0.12 connects the original actor simulation to Android in rooms 100,
101 and 102. Native `enemy.c` and `plc.c` run without DS graphics calls;
GLES draws immutable snapshots of their blended skeletal poses using the
original enemy/NPC meshes and textures. Room scripts can spawn, hide and move
actors, including Kendo. Native room collision, path zones and sight rules
are used; solid walls and foreground masks occlude actors. Models are cached
only for the current room and released from the native simulation on departure.
Other rooms retain their earlier exploration behaviour.

**Version 0.12 was the actor-rendering milestone; v0.13 adds the opening
combat described above.** Version 0.15 adds gun-shop movement, speech and events;
healing and story support outside that room remain unfinished.
Actor state restarts from room scripts after Activity recreation; this is not
an in-game save system. Further campaign progression remains unfinished.

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

Outside the gun shop this remains an **exploration build**. Actors are enabled
only in the opening three rooms. The gun shop now uses player cinematic hooks,
speech, live event triggers and script-controlled doors. Elsewhere, cinematic
hooks remain inactive and converted door definitions bypass story AOT replacements;
these rooms cannot yet validate story progression or timing.
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
  A/B/Start dismiss a pickup or examination message
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

- Inventory Exit, Pause → Quit game, or Android Back opens an in-view quit
  confirmation. No is selected by default; B/Start cancels. Yes closes the app
  and both displays. Start/B remain the inventory-to-gameplay controls.

The lower screen now uses Leon's original portrait, metallic square frames,
a two-column item grid, quantity/equipped markers and an equipped-weapon
panel. Test tools stay in the pause menu. The condition panel now reads a shared player-health state (200 maximum HP),
using the native engine's thresholds: Fine above 100, Caution from 21–100,
Danger from 1–20 and Dead at 0. HP is retained during Activity recreation and
room changes and reset for a new exploration session. Combat damage now updates this display in the opening rooms. Healing remains
unconnected; resetting exploration restores Fine / 200 HP.

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


Version 0.10 adds the health/condition panel and a quit confirmation drawn inside
the existing inventory View, preserving controller focus on the main Activity.
No restores the previous pause state; Yes dismisses the secondary Presentation
and removes the Activity task. Message-area touches continue to dismiss messages,
while the explicit Exit button now means quit. Recreated confirmations default to
No. Health tests cover native thresholds, restoration, bounds and reset.

Distribution target: a standalone engine APK with on-device import of user-supplied
Leon and Claire discs from an Android folder. The app must validate disc identity,
revision and completeness, prepare/cache assets with progress reporting, and reuse
that cache on later launches. Import must tolerate interruption and report errors
without destroying an existing prepared game. This is the target architecture;
the current build still requires desktop conversion and embeds local assets.

Version 0.10 validation: all 16 JVM tests and Android lint pass; the signed debug
APK was installed on the Thor. Device captures verify the health panel and the
quit prompt with No selected by default. An untargeted controller A cancelled
that prompt and left the game active. Touching Yes on a subsequent prompt left
zero resumed game activities and zero game windows across the displays. The app
was then reopened successfully. Combat is still unconnected, so this verifies the
health presentation/state boundaries, not gameplay damage or healing.


Version 0.11 animates the condition trace as a scrolling heartbeat at approximately
30 fps, including while managing inventory. Its colour still follows player HP;
zero HP renders a flat line. The existing foreground frame loop drives redraws,
which stop when the Activity is suspended or displays are off. Covered pickup/quit
prompts do not schedule heartbeat redraws, and animation does not change HP.

Version 0.11 validation: the build, all 16 JVM tests, lint and APK signature check
pass. Installed on the Thor; two lower-screen captures taken 250 ms apart while
inventory was paused show the waveform scrolling, with HP and condition unchanged.

## Opening actor verification (0.12)

Run `tools/test_android_world.sh` after disc conversion. It builds the production
Android simulation bridge for the host with address/undefined-behaviour sanitizers.
Tests cover all 125 room scripts and prior pickup/door behaviour, original opening
actor assets, animated poses, hide/unhide, Kendo movement commands and room cleanup.
Android JVM tests additionally decode original actor skeletons and verify render
snapshots, cache release and malformed snapshot rejection. Hardware rendering and
controller checks are separate from these simulation tests.

Local v0.12 validation: 18 JVM tests, lint, APK signature verification and both
sanitized native suites pass. Installed on the connected Thor; captured original
street zombies rendering in multiple camera views and the lower inventory still
visible. Full user control/room traversal verification remains pending.

## Combat verification (0.13)

`tools/test_android_world.sh` also exercises the production combat bridge with
original assets: cadence and ammo, recoil and enemy death, empty-gun behaviour,
reload transfer at keyframe 22, knife ammo, actual grabs/victim poses, struggling
free, lethal damage, death completion and reset. JVM tests check pose/health
snapshots and release of combat input on pause/focus loss. Hardware controls
and presentation require a separate check on the Thor.

Local v0.13 validation: 21 JVM tests, all three sanitized native suites, lint
and APK signature verification pass. On the Thor, a three-second aim/fire
check spent four rounds (18 → 14); an unattended zombie attack reached zero
HP and the restart option restored 200 HP. Final build installed and left
paused on both displays. Physical controller aiming, struggling and normal
room traversal remain for the user's hands-on check.

## Combat effects verification (0.14)

The fourth sanitized native suite checks blood after real hits, gunshot events,
keyframe flash/smoke/casing emission and anchors, bounded snapshots, particle
expiry, dry-fire/knife exclusions and clearing on room change. JVM tests decode
the original blood, flash, casing and smoke pages and palette transparency.
`python3 -I tests/android_audio_host.py` verifies staged WAV samples against the
original converted bank PCM byte for byte, including rates and note ranges.
No replacement artwork, synthesized sound or resampled audio is introduced.

Thor validation: the user confirmed muzzle flashes, blood, gunshots and zombie
sounds in the installed v0.14 build. Combat itself was confirmed in v0.13.

Local v0.14 validation: all 23 JVM tests, four sanitized native suites, Android
lint and APK signature verification pass. All 1,365 staged WAV samples match
the original converted PCM and sample rates.


## Gun-shop story verification (0.15)

`tools/test_android_world.sh` includes a fifth sanitized production-native suite.
It walks into the real welcome AOT, completes all eight welcome voice cues and
scripted movements, checks control return, arms/triggers the window attack, checks
its two voice cues, room sounds and glass sprites, exits through the rear door,
and re-enters with the persisted post-attack flags and camera remapping. It also
checks that the front door is no longer an exploration bypass and interaction is
blocked during the scene. JVM checks cover cinematic pose/camera application,
automatic-camera restoration, manual event reach and original glass page decoding.
Local validation: 27 JVM tests, five ASan/UBSan native suites, Android lint and
APK signature verification pass. All 1,379 short sound samples match converted
PCM byte for byte. v0.15 is installed on the Thor. Device screenshots show the
welcome cinematic views and return to the normal interaction hint, with no
app/MediaPlayer errors in the checked logs. User confirmation of audible dialogue,
physical pause/resume and the complete window-attack/rear-exit route is pending.


### v0.15 hardware feedback and v0.16 regression coverage

The user reported incorrect motions, legs visible through the counter, and a lock
sound before Kendo reached the door. The earlier completion-only test was insufficient.
New checks reproduced both RBJ loading failure and the lock cue at the wrong position
before the fixes. The actor suite now compares the rendered no-blend cinematic pose
against the original RBJ frame. The story suite checks transient vs persistent flags
and Kendo's location when the lock cue fires. `tests/android_masks_host.py` checks
screen coverage and near-plane visibility for every gun-shop mask corner.
The v0.16 hardware replay confirmed the encounter fixes, with a remaining pre-conversation
idle issue corrected in v0.17. Neck tracking and other previously
listed cinematic limitations still apply.

### v0.17 Kendo starting idle

The default NPC idle selected Kendo's main-set animation 2, a non-cyclic
15-frame weapon motion, causing repeated rocking before the conversation.
Kendo now uses main-set animation 1, his existing single-frame neutral stance,
for default idle and return from script control. Scripted PLC motions are unchanged.
The actor integration test checks the neutral pose against the converted model
for 90 ticks, then exercises walking and a room animation. Hardware confirmation
of the starting stance is pending.

### v0.18 Footsteps, inventory audio and zombie appearances

Player movement preserves the original EDD foot-contact flags, playing the
current room's FLR walking/running sample on contact frames (including backing
up and turning). Scripted Leon movement uses the same floor lookup. FLR enable
and disable instructions are supported. All 125 converted room sound banks are
staged; only the current room and enemy banks are loaded with shared weapon and
player banks. Inventory cursor, equip/confirm, cancel, rejected combinations
and ammo-loading actions use original CORE00 notes 4–8. Menu playback is separate
from paused gameplay and stops on backgrounding or screen sleep.

Type 0x1f zombies now preserve the SCE_EM_SET model selector. Explicit variants
are honored; unspecified appearances choose one of the original three body
textures once at spawn. The renderer keeps the authored head/hand textures,
mesh geometry and animation. This restores clothing variation in the opening
rooms without substituting unrelated enemy types. Native tests check explicit
spawn choices and floor sounds; JVM tests check contact timing, silent idle/pause,
and variant texture routing. On-device sound and appearance confirmation pending.

### v0.19 Item action selection

Confirming an occupied inventory slot opens a vertical item-action menu instead
of immediately equipping it. D-pad or stick chooses Equip/Unequip (Use for
non-weapons), Combn or Check; A executes, B returns to the item grid, and Start
resumes gameplay. Check returns to the action menu. Combn selects a second slot;
B cancels back to the original item's actions. Touch uses the same action menu.
The existing native equipment toggle now has the matching Equip/Unequip label.
Contextual Use/key-item interactions remain unfinished and reject the action
without consuming the item; this change does not claim complete PS1 inventory
feature parity. Automated tests cover selection without mutation, cancel,
empty slots and command labels. Hardware confirmation is pending.

Zombie clarification: v0.18 restored three original type-0x1f body texture
variants, not a newly created mesh. Opening room scripts use this shared type;
random selection can repeat. Separate original enemy types remain separate
model assets. No zombie appearance changes were made in v0.19.

### v0.20 Healing and door-key Use

Use now restores native combat health and updates the lower-screen condition
immediately. Green herbs restore 50/200 HP, double-green mixtures 100, and
first-aid spray, triple-green and green/red mixtures restore up to full health.
Blue-containing mixtures retain the corresponding healing amount. Items are
consumed only on successful healing; full health, death and grabs reject use.
Poison is not implemented, so pure blue herbs remain unusable and unconsumed.

Use on a matching key unlocks a manual door in Leon's current floor and reach,
including his forward interaction probe. Wrong keys, distant doors, unlocked
doors and locks from the other side reject Use without changing inventory.
Unlocks share the existing saved room-lock state and carried-key interaction.
Keys remain reusable; last-use/discard prompts and arbitrary puzzle-item event
scripts remain future work. Rooms outside the gun shop still use the existing
exploration fallback, so this does not complete scenario progression.

Validation: six native ASan/UBSan suites include healing boundaries, consumption,
wrong keys, reach/facing/floor checks, one-way locks and unlock persistence.
All 32 JVM tests and Android lint pass; the debug APK builds successfully.
Hardware check: take damage, use a green herb from Pause → Test supplies, verify
condition updates and the herb disappears; at full health Use must retain it.
Do not use Test supplies while checking key retention: it replaces the loadout.

### v0.21 Leon A route to the RPD entrance

Enabled original enemy simulation and room events for 102 → 118 → 119 → 11A
(bus) → 11B → 103 (RPD courtyard), keeping the existing gun-shop encounter.
The destination 200 is reachable; RPD interior progression remains unfinished.
This is an incremental opening-route build, not full Leon A/B support.

The bridge now runs recurring main-script function 1 after room setup, allowing
the bus approach's camera check to start its event. Alley sound/voice cues,
gate movement and zombie activation use the original scripts. Room effects,
pathfinding blocks and available room animations are staged for the route.
Room sounds use the current room bank rather than always the gun shop, and
late-spawned enemy sound banks load when first needed.

Camera switches retain the original RVD enable flags. CUT_BE_SET updates the
indexed record within a camera's RVD group; disabled switches are excluded from
both normal camera transitions and the view-area fallback. The feeding zombies
honor script member 4 (substate) and their original get-up move before pursuit.

Native integration checks the alley branches, gate and reinforcements, bus
camera event and zombie release, bus ammo confirmation/retention across re-entry,
and door destinations through the RPD main entrance. JVM checks cover disabled
camera switching and all route effect pages. Existing gun-shop, combat, health
and inventory tests remain part of the regression run.

Validation: all seven ASan/UBSan suites, 34 JVM tests and lint pass. Version
0.21 installed and launched on the Thor; traversal confirmation is pending.

Hardware check: traverse the route normally, checking gate clearance, camera
changes, combat and pickup interaction. Confirm that entering the RPD returns
control on the top screen with inventory input still working below. Full music,
lighting/shadow fidelity, special enemy behaviours and RPD puzzles/save systems
are still outside this build's completed coverage.

### v0.22 Kendo visibility/death and shove collisions

Removed whole-actor wall-box culling that depended on Leon's position; original
foreground masks and depth testing now provide render occlusion. Fixed routine
8 substate-zero initialization so the zombie attack actually assigns and plays
Kendo's death pose. Re-entry uses his authored room body motion. Added bounded
WORK_COPY operands and DIR_CK facing checks used by the original scripts.

A zombie pushed off Leon now contacts standing zombies during release keys
7–24 and starts their knockdown reaction. Normal walking crowd collision is
retained, with height and grounded-body exclusions. Tests cover crowd spacing,
shove contact, unaffected NPCs/other floors, original death/body frames and
visibility through the Android snapshot bridge. The script operand fix also
exposed the RPD courtyard's ninth spawn; Android now supports 32 enemy slots.

Validation covers native simulation and render snapshots; it does not certify
full visual parity for every encounter. Read the encounter-validation guidance
in docs/engine-notes.md before expanding the route further.

All seven native ASan/UBSan suites, 34 JVM tests and lint pass. v0.22 was
installed and launched on the Thor; the encounter changes have automated
pose/snapshot coverage, with on-device visual confirmation still pending.
