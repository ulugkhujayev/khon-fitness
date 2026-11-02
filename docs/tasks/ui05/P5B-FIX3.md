# UI05 P5B-FIX3: one ordered writer per new routine (redesign, design alarm)

Class: Normal (protocol step 6, fixer, after a design alarm), Risky care:
drafts and stored writes. Read `COMMON.md`, `P5B-stretching.md`,
`P5B-FIX.md` and `P5B-FIX2.md` in this folder first. Base: `a931162` on
`ui05-port`. Change only the new-routine draft logic in
`ui/screens/StretchEditors.kt` (`RoutineEditorScreen`),
`ui/screens/StretchRules.kt`, `ui/AppViewModel.kt` and their tests; leave
the look, the P5A commands in `data/Dao.kt`, and the number field alone.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `android/app/src/test/` or `docs/`.

## Why the last two fixes failed (Sol)

The screen decides from values it captured during composition
(`pending`, `routine`, `isDraft`) and writes through three paths
(`vm.run`, `vm.runSerial`, and a create queued from a pick). (1) Two picks
before a recomposition both see "not pending" and both create; with the
first stretch deleted, the second saves the routine
(`StretchEditors.kt:73,76,79`). (2) An edit after the routine is saved
goes through `vm.run` while older draft writes still wait in the serial
queue, so the older value lands last (`:88,97,99,102`).

## Decision: the writer owns the state and every write

Add `RoutineWriter` (plain Kotlin in `ui/screens/StretchRules.kt` or its
own file, testable without Compose), one per routine id, held by the view
model for the process lifetime (`vm.routineWriter(id, ...)`).

- State, read and changed only under the writer's lock at the moment of a
  call (never from composition): the latest name and Active, and the
  phase: `Draft`, `Creating`, `Saved`. The screen shows the writer's
  name, Active and phase (observable state), and nothing else decides.
- One ordered queue: every write for this routine (create, add stretch,
  name, Active, Done) is appended to the writer's single queue and runs in
  call order on the view model scope. No other path writes this
  routine's name, Active or its first create; seconds, move, remove and
  delete keep their own commands (they act on rows, not on the draft).
- `pick(stretchId)`: under the lock: `Draft` -> phase `Creating`, queue
  `createRoutine(latest name and Active, stretchId)`; when it returns,
  phase `Saved` if created, else back to `Draft`. `Creating` -> ignored
  (returns false; the screen keeps Add off). `Saved` -> queue
  `addRoutineStretch`.
- `setName(v)`, `setActive(v)`: under the lock, store the latest value;
  in `Creating` or `Saved` also queue the targeted write. In `Draft`
  nothing is written.
- `done()`: queue `createRoutine(latest)` (writes nothing when the id
  exists), then `setRoutineName` and `setRoutineActive` with the latest
  values; the screen leaves at once.
- Start: a writer for an existing routine (route id, or a restored new
  routine whose id the database holds) starts `Saved` with the stored
  values; a new one starts `Draft` with the saved-state name and Active
  (`rememberSaveable` keeps id, name and Active for process death; after
  process death no write is in flight and the database decides the
  phase).
- Remove `pendingRoutines`, `canPick`, `runSerial` (if nothing else uses
  it) and the per-field branches in the screen.

## Tests

Unit tests of `RoutineWriter` against an in-memory Room database (as
`test/.../data/EditorWritesTest.kt`), with a gate that holds the queue:
two picks before the first create ends (the second ignored; with the
first stretch deleted, no routine); edits in each phase, including after
`Saved` while older writes wait (the latest values win); Done in each
phase; a refused create returns to `Draft` and a later pick or Done
saves. Keep the screen tests in `RoutineDraftScreenTest.kt` passing
(adapt them to the writer where their setup changes) and add Sol's two
sequences there. Sol's two sequences fail on `a931162`.

## Done

`COMMON.md` checks pass. `DESIGN.md` routine draft text still true (fix
it if not). Report under 20 lines: commits, the command and the tail of
its output, the test count, the file:line of each change, and which tests
fail on the base with their failure lines.
