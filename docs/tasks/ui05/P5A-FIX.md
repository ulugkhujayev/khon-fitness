# UI05 P5A-FIX: fixes from the Sol deep review of P5A

Class: Risky care (protocol step 6, fixer): items 1 and 2 write stored
data. Read `COMMON.md`, `P5-stretch-window-cardio.md` and `P5A-writes.md`
in this folder first. Base: `7982e16` on `ui05-port`. Fix every instance
of each class, search for siblings, add tests. Do not start P5B or P5C
work (the screens' look); change `StretchEditors.kt` only as item 1 says.
No Room schema change, no migration.

Paths are under `android/app/src/` unless they start with `docs/`.

Sol accepted deviations 2-4 of the P5A report (`stretchResult` and
`stillOpen(repo)` in `TimerService.kt`; blank name keeps the stored name;
`createRoutine` with an existing id writes nothing). Deviation 1 is
rejected (item 2).

## 1 (P2): a deleted stretch's editor becomes a create path

`main/.../ui/screens/StretchEditors.kt:145-151`: `opened` and `draft` are
keyed by `existing?.id`; when the stretch is deleted elsewhere, both reset
and Done calls `createStretch`, making a new stretch.

Decision: the editor's mode comes from the route only: `id == "new"` is
create mode, any other id is edit mode for that id for the screen's whole
life. In edit mode the opened row is kept once seen (`remember(id)`, set
on the first non-null load, never cleared); Done calls `saveStretchEdits`
(which writes nothing for a deleted row) and never `createStretch`. If
the row is gone when the editor opens or while it is open, Back and Done
just leave. Search the other editors (routine, exercise, modality,
program, block) for the same "keyed by the loaded row's id" create path
and fix each the same way, or list it as safe with the reason.

Tests: Robolectric screen test (as
`test/.../ui/screens/CardioIntervalsKeptTest.kt`): open an existing
stretch, delete it through the DAO, type a name, Done: no stretch exists.
Plus one per sibling you fix. Each fails on `7982e16`.

## 2 (P2): import changes an intentional zero-active routine state

`main/.../data/Dao.kt:466-477`. Export with A active, turn A off, merge
the export: A is active again. Replace with a backup that has no
routines: the seed makes `daily` active although the backup has none
active.

Decision (replaces the P5A deviation and fixes `SPEC.md` to match
contract row 18):
- Pick the winner before any write. Merge: if this phone has any routine,
  the winner is this phone's active routine, or none when it has none
  (zero active stays zero); if the phone has no routine at all, the
  winner is the backup's first active routine by `sortOrder, id`, or
  none. Replace: the backup's first active routine by `sortOrder, id`, or
  none.
- After all writes, including a seed, one statement sets
  `active = (id = :winner)` for every routine (none: all off). A seeded
  `daily` routine is active only when it is the winner (it never is
  after an import, since it is not in the backup; first start without an
  import keeps today's seed, `daily` active).
- `BackupTest`'s round trip (export, merge into an empty database) keeps
  the backup's active routine by the empty-phone rule.

Tests (Room, in-memory): the two sequences above; merge onto a phone
with B active keeps B; merge onto an empty phone takes the backup's
active; replace with zero, one, two active. The two Sol sequences fail
on `7982e16`.

## 3 (P2): the source guard misses instances

`test/.../WriteGuardTest.kt:54,66`: a repository method that takes
`List<RoutineStretch>` (or any collection of a row type) is not caught,
and `db.dao ()` with a space is not caught.

Decision: check the repository's API by reflection, not by regex: every
public method of `KhonRepository` whose generic parameter types mention a
row type (`Stretch`, `StretchRoutine`, `RoutineStretch`,
`StretchSession`, `EatingWindow`), directly or as a type argument (List,
Set, Collection, Array, Map values), is a violation unless its name is
in the allowed set. Keep the source scan only for DAO access outside
`data/`, with a pattern that allows whitespace and line breaks between
`dao` and `(` and catches `.dao` and `::dao` references; `KhonApp.kt`'s
repository construction stays allowed. Tests: deliberate breaks for both
Sol instances (a repository-shaped class in the test with a
`List<RoutineStretch>` method checked through the same function; a
source text with `db.dao ()`), plus the current tree passes.

## Done

`COMMON.md` checks pass (full suite, assemble, lint). Report under 25
lines: commits, the command and the tail of its output, the test count,
each item with the file:line of its fix and tests, which new tests fail
on `7982e16` with their failure lines, and the sibling result for item 1.
