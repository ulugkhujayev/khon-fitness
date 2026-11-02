# UI05 P5B-FIX: a pending routine create counts as saved

Class: Normal (protocol step 6, fixer), Risky care: drafts and stored
writes. Read `COMMON.md` and `P5B-stretching.md` in this folder first.
Base: `47deb73` on `ui05-port`. Change only the routine draft logic and
its tests; leave the look alone. Do not change the P5A commands in
`data/Dao.kt`. A second builder changes `NumberField` callers (including
`StretchEditors.kt` number fields) in another worktree; keep your edits
in `StretchEditors.kt` to the draft logic so the merge stays small.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `android/app/src/test/` or `docs/`.

## Finding (P2, drafts)

`ui/screens/StretchRules.kt:54` sets `savedId` before the database
create finishes. (1) Restore (process death or recreation) while the
first-stretch create is pending, then the create is refused (its stretch
was deleted): the restored editor keeps the id, hides Add
(`StretchEditors.kt:64,106`), and Done creates nothing. (2) Done while
that create is pending, then it is refused: Done has left without
saving, and no routine exists, against contract row 16.

## Decision

1. The new routine's id is made once, when the `new` editor opens, and
   kept in `RoutineDraft`'s saved state from the start (not when a save
   starts).
2. Saved or draft comes from the database, not a local flag: the editor
   is in saved mode exactly when the routines list holds that id; else
   it is a draft and shows the draft's name and Active choice, with Add.
   Remove `savedId`, `refused` and the "loading" guess; a pick while the
   first create is still pending is ignored (the pick sheet closes), as
   now.
3. Draft writes run in order: the view model gets one serial runner (a
   `Mutex` around `run`, used only by the routine draft: `runSerial`).
   The first stretch queues `createRoutine(r, stretchId)`; Done queues
   `createRoutine(r)` and leaves at once. Done after a pending create
   that succeeds writes nothing (the id exists); after one that is
   refused it creates the routine without stretches (row 16).
4. Process death: nothing pending survives, and the restored editor reads
   the database: the routine exists (saved mode) or not (draft, Add
   visible, same id).

## Tests

Unit tests of `RoutineDraft` (the new rules) and Robolectric screen tests
(as `RoutineDraftScreenTest.kt`), holding the create pending with a gate
(for example the `Race` helper in `test/.../data/Race.kt`, or a test
repository wrapper that suspends `createRoutine` until released):
restore while the create is pending, then refuse it: draft mode, Add
visible, same id, then Done saves one routine without stretches; Done
while the create is pending, then refuse it: one routine without
stretches; Done while it is pending, then let it succeed: one routine
with that stretch, no second routine. Each fails on `47deb73`.

## Done

`COMMON.md` checks pass. Report under 20 lines: commits, the command and
the tail of its output, the test count, the file:line of each change,
and which tests fail on the base with their failure lines.
