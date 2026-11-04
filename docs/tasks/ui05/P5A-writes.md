# UI05 P5A: P5 data commands (first half of P5)

Class: Risky (protocol steps 4-7; the attack step is done, verdict
closed). Read `COMMON.md` and `P5-stretch-window-cardio.md` in this
folder first. This task builds only the data side of P5; the screens are
P5B, a later task. Base: the `ui05-port` head when you start.

Paths are under `android/app/src/` unless they start with `docs/`.

## Scope

Build everything in the P5 task file's sections "Writes: one set of
guarded commands (closed design)" (the command table, Ordering,
Activation, Import and seed, Guard, the two narrow changes in
`timer/TimerService.kt` and `window/WindowScheduler.kt`) and the data
side of contract rows 12, 13, 16, 17, 18, 19, 20.

Rewire the existing callers to the new commands with the smallest change
that keeps today's screens working as they are now: `StretchScreen.kt`
(new routine), `StretchEditors.kt`, `WindowSheet.kt`, `TimerService.kt`
`recordTimer`, the backup and Reset follow-ups in `ProgramsScreen.kt`.
Keep the current screen layout and behavior; P5B rebuilds the screens.
One exception: the stretch editor's Delete uses `deleteStretchIfUnused`
and shows the existing refusal text when it returns false (row 12). The
routine draft UI (rows 3, 4) is P5B; until then the "+" on Stretching may
call `createRoutine` at once as today.

## Tests

Every attack case in the P5 task file's "Attack cases" except the UI-only
parts of E4 and E5 (P5B tests those on the screen; here test
`createRoutine` with and without a first stretch). Room tests against an
in-memory database, as `test/.../data/EditorWritesTest.kt`; for a race
between a step's read and its write use the `Race` helper
(`test/.../data/Race.kt`, `race.step(at, write) { body }`; read its doc
and `RaceTest` first). Pure rules (W4 window text, kept-days count, run
progress text and segments) are P5B. The Guard test is a plain JVM test
over the source tree.

Each sequence test fails on the old write path where a sequence can show
it: run those tests against the base commit's code in a scratch worktree
at `../khon-fitness-wt/ui05-p5a-base` (adapt the calls to the old
methods), record the failure lines, then remove the worktree.

## Done

`COMMON.md` checks pass (full suite, assemble, lint). `DESIGN.md` and
`SPEC.md` describe the accepted limits (P2 replay, P4 restored snapshot,
zero active routines) and the import repairs. Report under 30 lines:
commits, the command and the tail of its output, the test count, each
command with the file:line of its code and test, which tests fail on the
base and their failure lines, and every caller you rewired.
