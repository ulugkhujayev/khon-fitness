# UI05 P5C: eating window and cardio screens (third part of P5)

Class: Normal for the screens; the writes they call are Risky and done
(P5A, P4-FIX2, P4-FIX3). Read `COMMON.md` and
`P5-stretch-window-cardio.md` in this folder first. Use only the existing
repository commands for stored writes (window: `setWindowEnabled`,
`setWindowRemind`, `setWindowStart`, `setWindowEnd`, `openWindowNow`,
`closeWindowNow`; cardio: `saveCardioEdits` with per-field interval
edits, `setSession*`); `test/.../data/WriteGuardTest.kt` must keep
passing. No Room schema change.

Work in the worktree the lead gives you, on its branch; the lead merges
it into `ui05-port`. A second builder does P5B (stretching) at the same
time on `ui05-port`. Do not edit `StretchScreen.kt`, `StretchEditors.kt`,
`StretchDemonstration.kt`, existing files in `ui/components/` (add new
files there instead), or the stretching parts of `DESIGN.md`.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `docs/`, `expo/` or `android/app/src/test/`.

## Scope

Contract rows 8, 9, 10, 22 of `P5-stretch-window-cardio.md`, on
`ui/screens/WindowSheet.kt`, `ui/screens/CardioScreen.kt`,
`ui/screens/TimerScreen.kt` and new files in `ui/components/`. Expo
references are listed in the P5 task file ("Eating window", "Cardio").

Keep every 0.4.8 feature (`git show 2b73707:<path>`): the window's Open
now / Close now, reminders and widget; the cardio form's stored fields,
swim laps math, interval inputs, delete confirm; the timer's foreground
service, TTS, cue gate and background-permission action. The cardio
form keeps its current save logic (per-field interval edits,
`saveCardioEdits`); restyle around it, do not rewrite it, and keep
`test/.../ui/screens/CardioIntervalsKeptTest.kt` passing unchanged.

## Tests

Pure rules in plain Kotlin with unit tests: the window state text (open,
closed, off; at a boundary; around midnight per `EatingWindowRules`), the
day-bar geometry, the kept-days count text ("Kept the hours on 20 of 27
days"; today excluded). Each new test fails on the base commit where the
rule is new or changed.

## Done

`COMMON.md` checks pass. Root `DESIGN.md` "Eating window" and cardio
parts updated. Report as `COMMON.md` says, under 30 lines.
