# UI05 P5B-FIX3: a new routine commits on its first stretch; one write queue (redesign, design alarm)

Class: Risky (protocol step 6 after a design alarm; it decides every
write of the routine editor). Read `COMMON.md`, `P5B-stretching.md`,
`P5B-FIX.md` and `P5B-FIX2.md` in this folder first. Base: `a931162` on
`ui05-port`. Change only `ui/screens/StretchEditors.kt`
(`RoutineEditorScreen`), `ui/screens/StretchRules.kt`, `ui/AppViewModel.kt`,
their tests, and the routine draft text in `DESIGN.md` and `SPEC.md`.
Leave the look, the P5A commands in `data/Dao.kt` and the number field
alone.

Paths are under `android/app/src/main/java/dev/mirzohidkhon/khonfitness/`
unless they start with `android/app/src/test/` or `docs/`.

## Why the last designs failed

Every failure came from one rule: a first stretch deleted meanwhile made
the first save fail and the editor return to a draft (attack case E11 on
the screen). A save that can fail made the phase uncertain while it ran,
and each fix added another path around that. Decisions were also read
from values captured during composition, and writes went through several
paths (`vm.run`, `vm.runSerial`).

## Decision (lead)

1. **The first stretch commits the draft, and the commit cannot fail.**
   Picking the first stretch does what Done does (`createRoutine(r)` with
   the draft's name and Active, no first stretch), then
   `addRoutineStretch(id, stretchId)`. If that stretch was deleted
   meanwhile, the routine is saved with no stretch (the guarded add
   writes nothing). `createRoutine(r, firstStretchId)` stays in the DAO
   with its tests (E11 holds for the command); the screen no longer uses
   it.
2. **The phase flips at the call.** `RoutineDraft` keeps `committed:
   Boolean` in saved state with the id, name and Active. The click
   handler (pick, Done) reads `committed` from the state object at call
   time, never a value captured in composition, and sets it to true in
   the same handler before it queues any write. So a second pick, even
   before a recomposition, sees `committed` and only adds its stretch.
3. **One ordered queue for every write of this screen.** Create, add
   stretch, name, Active, seconds, move, remove and delete all go through
   `vm.runSerial` (one view-model `Mutex`, call order). Remove
   `pendingRoutines` and the per-phase branches.
4. **What the screen shows.** Not committed: the draft's name and Active
   from `RoutineDraft`. Committed: the stored row once it is in the
   routines list (until then, the draft's values; Add stays on, since the
   queue orders any add after the create). Edits in committed mode are
   targeted writes through the queue; a missing row makes them write
   nothing.
5. **Done.** Not committed: set `committed`, queue `createRoutine(latest)`,
   leave. Committed: leave (each edit was written as made). Done never
   creates for a committed routine, so a routine deleted elsewhere is not
   re-created.
6. **Restore after process death.** No write survives the process. If
   `committed` is true and the database has no row with the id, the
   editor becomes a draft again (`committed = false`) with the saved name
   and Active; the first stretch picked before the death is lost, and the
   user's Done or next pick saves it. Rotation keeps the view model and
   its queue, so nothing is lost there.

`SPEC.md` and `DESIGN.md`: the first stretch saves the routine, also when
that stretch was deleted meanwhile (then with no stretch); Add stays on;
the lost-first-pick case after process death is an accepted limit.

## Tests

Room plus Robolectric screen tests (as `RoutineDraftScreenTest.kt`; keep
its `Hold` helper to hold the queue): two picks before a recomposition
(one routine, both stretches in order); first stretch deleted before the
held create runs (one routine, no stretch, latest name and Active); edits
to Name and Active while the create is held, then Back (stored with the
edits); a committed routine deleted through the DAO while a held name
write waits, then Done (no routine); rotation while held; process death
after `committed` with no row (draft again, same id, Done saves one);
Done on an uncommitted draft (one routine, latest values). Sol's two
round 2 sequences (two picks before recomposition; a saved edit behind an
older queued write) fail on `a931162`.

## Done

`COMMON.md` checks pass. Report under 20 lines: commits, the command and
the tail of its output, the test count, the file:line of each change, and
which tests fail on the base with their failure lines.
