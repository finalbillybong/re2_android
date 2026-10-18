# Project instructions

## Fidelity and verification

This is an Android Resident Evil 2 port for the AYN Thor, based on re2ds.
The user expects faithful original game behaviour and must not have to explain
each encounter or discover omissions that inspecting the game data would reveal.
These requirements apply to future work throughout this repository.

- Before implementing or changing an encounter, trace the original room scripts,
  model and animation banks, and relevant executable or overlay handlers. Read
  `docs/engine-notes.md` for existing findings. Do not guess behaviour from opcode
  names, a single animation, or the current partial implementation.
- Identify dependencies across the full encounter: conditions and branches,
  actor slots and models, animation banks and frames, movement, camera changes,
  collision, sound timing, inventory changes, and persistent flags. Distinguish
  unknown commands from recognised commands that the interpreter skips or stubs.
  Record unresolved behaviour explicitly; do not silently substitute a guess.
- Check the entire lifecycle: entry, idle, trigger, scripted action, return of
  player control, aftermath, approaching bodies or actors, leaving, and re-entry.
  Check relevant alternative branches and scenario differences against the data.
- Verify the actual Android output path. Raw actor state, event completion flags,
  playing audio, and successful room changes do not prove correct rendering or
  animation. Check the poses and visibility delivered to the renderer, including
  camera and player-position changes. A snapshot test does not prove GPU output.
- Add meaningful regressions for discovered defects, using original local data
  where appropriate. Run relevant native, JVM, and build checks. Use device visual
  and control checks for integration when available, after source-based checks.
  Do not use the user's testing as a substitute for doing this investigation.
- Report separately what is implemented, what automated checks establish, and
  what has actually been visually verified on hardware. Installation or launch
  alone is not encounter verification. Never describe an entire route or scenario
  as complete based only on a narrow test or a partially implemented sequence.
- When a defect reveals a shared engine issue, inspect other affected callers
  and encounters rather than applying only a scene-specific workaround. Keep
  evidence, handler addresses, limitations, and regression coverage in
  `docs/engine-notes.md` so future sessions retain the findings.

The Kendo fixes documented under “Encounter validation and v0.22 corrections”
are a concrete example of why these requirements exist. Do not promise that
mistakes are impossible; follow this process and communicate remaining gaps.

## Assets and scope

- Preserve original artwork; replacement artwork is outside the user's scope.
- Keep original disc data, extracted assets, and generated build outputs out of
  commits. Preserve existing unrelated and accumulated uncommitted work.
- The intended distribution is an APK that imports the user's locally supplied
  Leon and Claire discs from Android storage. Do not confuse this eventual goal
  with features already implemented or verified.
