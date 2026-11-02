# UI05 P5B-FIX2: routine draft edits and picks while the first create is pending

Class: Normal (protocol step 6, fixer, second and last fix round before a
design alarm), Risky care: drafts and stored writes. Read `COMMON.md`,
`P5B-stretching.md` and `P5B-FIX.md` in this folder first. Base:
`20203f5` on `ui05-port`. Change only the routine draft logic and its
tests. Do not change the P5A commands in `data/Dao.kt`. Another builder
fixes `NumberField` in `ui/components/Components.kt` in a worktree; do
not edit that file.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `android/app/src/test/` or `docs/`.

## Findings (Sol verify of P5B-FIX)

1. Edits lost: hold the first create (from the first stretch); change
   Name and Active; Done; release the create (it succeeds). Expected the
   routine with the new name and Active; actual the values from the pick
   (`ui/screens/StretchEditors.kt:75,80,86,90`). Done's `createRoutine`
   writes nothing because the id now exists.
2. Second pick accepted: hold the first create (stretch Fold); pick Reach;
   delete Fold; release. The first create is refused, the second creates
   the routine with Reach, so a pick made while a create was pending
   saved the routine (`StretchEditors.kt:75`, `ui/screens/StretchRules.kt:40`).

## Decision

- Pending creates live in the view model, keyed by routine id, for the
  process lifetime (as `openPicks`): `vm.pendingRoutines`, an observable
  set. The first stretch adds the id before it queues `createRoutine`
  through `runSerial`, and removes it when that write ends (success or
  refusal). A restored editor (configuration change) sees the same set;
  after process death it is empty and the database decides, as now.
- While the id is pending: Add and the pick sheet are off (a pick is
  ignored, the sheet closes); Name and Active still edit the draft's
  fields on screen, and each such edit also queues the matching targeted
  write (`setRoutineName`, `setRoutineActive`) through `runSerial`, so it
  lands after the create (and writes nothing if the create was refused).
- Done in draft mode queues `createRoutine(r)` with the draft's current
  name and Active, then, through the same queue, `setRoutineName` and
  `setRoutineActive` with those values (they write nothing if the
  routine does not exist and change nothing if it already matches), then
  leaves. So whichever of the first-stretch create and Done's create
  wins, the stored routine has the draft's latest name and Active.

## Tests

Robolectric screen tests (as `RoutineDraftScreenTest.kt`, with its
`Hold` helper): Sol's two sequences; edits to Name and Active while
pending, then Back (not Done), then the create succeeds: the routine has
the edits; a configuration change while pending keeps Add off until the
create ends. Each of Sol's two sequences fails on `20203f5`.

## Done

`COMMON.md` checks pass. Report under 20 lines: commits, the command and
the tail of its output, the test count, the file:line of each change,
and which tests fail on the base with their failure lines.
