# UI05 P5B-FIX4: "+" saves a new routine at once; the routine draft goes

Class: Normal, Risky care (stored writes). It replaces `P5B-FIX3.md`,
which Sol's attack rated open-ended twice (`P5-ATTACK.md`). Owner
decision (2026-11-02): "New routine" saves an empty routine on tap; Back
keeps it. Read `COMMON.md` and `P5B-stretching.md` in this folder first.
Base: `5a98da0` on `ui05-port`.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `android/app/src/test/` or `docs/`.

## Decision

1. **One command makes the routine.** Add `createNewRoutine(id: String)`
   to `KhonDao` (one `@Transaction`) and `KhonRepository`: insert
   `StretchRoutine(id, NEW_ROUTINE_NAME, active = no routine exists yet)`
   at the next sort order, and make it the only active one when active.
   The "no routine exists" check runs inside the transaction, not from
   the routines stream (it starts empty). Keep `createRoutine` and its
   tests unchanged. `WriteGuardTest` must still pass.
2. **"+" on Stretching** (`StretchEditors.kt:30`): make a new id, run
   `createNewRoutine(id)` through `vm.run`, and when it returns, navigate
   to `Routes.routine(id)` only if the Stretching screen is still the
   current entry (the user may have left). While that write runs, "+"
   does nothing (a `remember` flag), so a double tap makes one routine.
3. **The routine editor edits only saved routines.** Remove
   `RoutineDraft`, `PendingRoutines`, `vm.pendingRoutines`,
   `vm.runSerial`, and every draft branch in `RoutineEditorScreen`. The
   editor works as the saved branch does today: Name and Active show the
   stored row and write at once through `vm.run`; Add, seconds, move,
   remove and Delete as now; Done and Back leave. The route `routine/new`
   no longer exists.
4. **Row identity.** Wrap each stretch row of the editor's two loops in
   `key(rs.id) { ... }`, so a number field's held state never moves to
   another row when rows change under it.
5. **Text.** `DESIGN.md` (~:190-199) and `SPEC.md` (~:121-131): a new
   routine is saved when "+" is tapped, as "New routine", active only
   when it is the first routine; it opens in its editor; leaving keeps
   it; the user deletes an unwanted one with Delete. Remove the draft,
   pending and restore text for routines. Keep the program draft text.
   Update the KDoc at `StretchEditors.kt:52`.

Out of scope (all saved editors share them; note them in the report, do
not fix): import or Reset while an edit write is in flight, and a typed
name that the stored value replaces while a write is in flight.

## Tests

Remove `RoutineDraftScreenTest.kt` and the draft cases of
`StretchRulesTest.kt`. Add:

- DAO (Room, Robolectric): `createNewRoutine` on an empty table makes one
  active routine named "New routine"; with routines present it is not
  active and the old active one keeps the mark; a second call with the
  same id writes nothing; sort order is last.
- Screen (as the old screen test, real Room database): "+" opens the
  editor of one new routine; a double tap makes one routine; Back from
  the editor keeps it; Name and Active edits are stored; Delete removes
  it; rotation in the editor keeps the same routine.
- `key` case: two rows, a held plus on row A, A removed through the DAO,
  release: row B's seconds unchanged.

## Done

`COMMON.md` checks pass. Report under 20 lines: commits, the command and
the tail of its output, the test count, and the file:line of each
change. Do not end your turn while a check you started is still running.
