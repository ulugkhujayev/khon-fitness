# UI05 P5B: stretching screens (second part of P5)

Class: Normal for the screens; the writes they call are Risky and done
(P5A). Read `COMMON.md` and `P5-stretch-window-cardio.md` in this folder
first. P5A built every stored write these screens need; read its commands
in `android/app/src/main/java/dev/mirzohidkhon/khonfitness/data/Dao.kt`
and `data/Repository.kt` (stretch, routine, routine row, stretch session)
and `test/.../data/WriteGuardTest.kt`. Use only those commands; the guard
test must keep passing. No Room schema change.

A second builder does P5C (eating window and cardio) at the same time in
another worktree. Do not edit `WindowSheet.kt`, `CardioScreen.kt`,
`TimerScreen.kt`, or the "Eating window" and cardio parts of `DESIGN.md`.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `docs/`, `expo/` or `android/app/src/test/`.

## Scope

Contract rows 1-7, 11-17, 21 of `P5-stretch-window-cardio.md`, on
`ui/screens/StretchScreen.kt`, `ui/screens/StretchEditors.kt`,
`ui/components/StretchDemonstration.kt` and new or shared files in
`ui/components/`. Expo references are listed in the P5 task file
("Stretching").

Routine drafts (rows 3, 4, 16; `COMMON.md` "Drafts"; P5 task file
"Routine drafts follow the program rule"): a new routine (`new` route)
holds name and Active in `rememberSaveable`; leaving without Done and
without a stretch writes nothing; the first stretch or Done calls
`createRoutine`, and from then the editor keeps the id in
`rememberSaveable` and edits that routine. The "+" on Stretching opens
the `new` route instead of creating a routine at once (P5A left
`createRoutine` there as a stopgap).

Keep every 0.4.8 feature of these screens (`git show 2b73707:<path>`),
including the stretch player's timer service, cue gate, TTS and
notifications: the player's look changes, its behavior does not.

## Tests

Pure rules in plain Kotlin with unit tests: run progress text and
segments (row 7), the routine draft rules, the demo step-timeline state
(row 6). Robolectric Compose tests (as
`test/.../ui/screens/CardioIntervalsKeptTest.kt`) for: E4 (new routine,
set name and Active, leave: no rows; again with Done: one routine with
those values), E5 (new routine, first stretch, recreate the screen from
saved state, Done: same id, one routine row), row 12 (refused delete
shows the reason), row 21 (demo play never writes or starts the run
clock). Each new test fails on the base commit where the old screen
behaves differently.

## Done

`COMMON.md` checks pass. Root `DESIGN.md` "Stretch player", stretch demo
and routine editor parts and `SPEC.md` (routine drafts) updated. Report
as `COMMON.md` says, under 30 lines.
